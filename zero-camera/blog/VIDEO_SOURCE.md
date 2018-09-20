之前，我们是将相机提供的预览画面输入到Surface当中，然后进行滤镜处理和录制。
那我们可以切换输入源为视频吗？

## 整体流程理解

![整体流程.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/video_source_process.png)

### 对比
对比上文的整体流程，和本文的主题，当然唯一不同的就是我们的输入到Surface内的输入源了。
其他的部分，都没发生变化。
![cmp_camera_video.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/cmp_camera_video.png)

#### 以相机作为输入源
- 设置`SurfaceTexture`
主要就是通过`setPreviewTexture`方法，将我们包含有Oes纹理的`SurfaceTexture`作为输入。
```java
    mCameraApi.setPreviewTexture(mCameraRender.getSurfaceTexture());
```
- 获取纹理数据的时机
这样，当Camera会将数据喂入整个`SurfaceTextur`e当中。（整个过程是由Android都封装好了，我们不能控制。）每当生成一个`Frame`,就会回调`OnFrameAvaliable`方法。这时候，我们会去要求`GLView`进行刷新，从`SurfaceTexture`中的`Oes`纹理中获取数据进行处理。
```java
  //监听setOnFrameAvailableListener回调
  mCameraRender.getSurfaceTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                //一旦由Frame时，要求GLView刷新，就去获取纹理中的数据，进行后续的处理（Draw方法中）
                requestRender();
            }
        });
```
#### 以视频作为输入源
![简化流程.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/simple_video_src.png)

同样，如上图，我们也需要将视频的Frame输入到Surface。
流程因为需要我们手动来进行解码，所以显的比相机复杂一点。
##### 整体流程
![将视频输入到Surface.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/complete_video_src.png)

###### 配置`SurfaceTexture`
![video_decoder_config.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/video_decoder_config.png)

- 设置`SurfaceTexture`
同样，我们首先先关注如何将整体的解码流程和`Surface`联系到一起.
配置`MediaCodec`的`Decoder`时，我们可以配置`OutputSurface`

1. 因为`Decoder`在单独的线程内，所以通过内部的`handler`进行线程通信。
同相机时类似，将我们配置好`Oes纹理`的`SurfaceTexture`发送给`DecoderThread`
```java
mDecodeThread.sendSurface(mVideoRender.getSurfaceTexture());
```
2. 将收到的`SurfaceTexture`转成`Surface`
```java
   //使用Surface包裹住SurfaceTexture
   mOutputSurface = new Surface(surfaceTexture);
```
3. `MediaCodec`的`Decoder`关联在一起
```java
        MediaExtractor extractor = null;
        MediaCodec decoder = null;

        //检查我们的文件是否可读，不可读的话，就GG
        if (!mSourceFile.canRead()) {
            throw new FileNotFoundException("Unable to read " + mSourceFile);
        }

        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(mSourceFile.toString());
            int trackIndex = selectTrack(extractor);
            if (trackIndex < 0) {
                throw new RuntimeException("No video track found in " + mSourceFile);
            }
            extractor.selectTrack(trackIndex);

            MediaFormat format = extractor.getTrackFormat(trackIndex);
            //这里一定要通过这种方式来拿到video的MediaFormat。因为MediaExtrator会帮我们封装好我们的MediaFormat,特别是csd0/CSD-1 等数据
            String mime = format.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);
            //重点在这里，将outputSurface加入decoder的配置当中
            decoder.configure(format, mOutputSurface, null, 0);
            //开启解码器
            decoder.start();
```

- 获取纹理数据的时机
同上，解码器会在解码成功后，将数据喂入整个`SurfaceTexture`当中。每当生成一个`Frame`,就会回调`OnFrameAvaliable`方法。这时候，我们会去要求`GLView`进行刷新，从`SurfaceTexture`中的`Oes`纹理中获取数据进行处理。
```java
  //监听setOnFrameAvailableListener回调
  mCameraRender.getSurfaceTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                //一旦由Frame时，要求GLView刷新，就去获取纹理中的数据，进行后续的处理（Draw方法中）
                requestRender();
            }
        });
```
###### 解码的过程
上面的相机整体的喂数据的过程，我们是不可见的。但是，在这里。是我们手动进行的。最终间数据喂入`Surface`
![解码的流程.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/video_decoder_decode.png)

这部分之前的文章其实有涉及到，就是一个`MediaCodec`的解码的经典流程。
主要就是分为`Input`部分和`Output`部分
- `Input`部分
`Input`部分，就是使用再`Decoder`中可用的内存位置，将数据从`MediaExtrator`中读取，再喂入`Decoder`中进行解码,再通过`extrator.advance`的读取的循环。
```java
 // Feed more data to the decoder.
            if (!inputDone) {
                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {
                    if (firstInputTimeNsec == -1) {
                        firstInputTimeNsec = System.nanoTime();
                    }
                    ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                    // Read the sample data into the ByteBuffer.  This neither respects nor
                    // updates inputBuf's position, limit, etc.
                    int chunkSize = extractor.readSampleData(inputBuf, 0);
                    if (chunkSize < 0) {
                        // End of stream -- send EMPTY frame with EOS flag set.
                        decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                        if (VERBOSE) Log.d(TAG, "sent input EOS");
                    } else {
                        if (extractor.getSampleTrackIndex() != trackIndex) {
                            Log.w(TAG, "WEIRD: got sample from track " +
                                    extractor.getSampleTrackIndex() + ", expected " + trackIndex);
                        }
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufIndex, 0, chunkSize,
                                presentationTimeUs, 0 /*flags*/);
                        if (VERBOSE) {
                            Log.d(TAG, "submitted frame " + inputChunk + " to dec, size=" +
                                    chunkSize);
                        }
                        inputChunk++;
                        extractor.advance();
                    }
                } else {
                    if (VERBOSE) Log.d(TAG, "input buffer not available");
                }
            }

```
- `Output`部分
因为我们设置`Surface`作为输入的目的。所以Output的部分，我们先查询当前是解码的状态，如果是正在解码，就释放掉解码部分的内存，喂入`Surface`。同时因为播放的帧之间需要延迟，所以还需要对比进行对应`Thread.sleep`防止跑的过快。如果是循环播放的话，播到最后时，需要将extrator重新seek到0，开始播放。
- 整体的流程
```java
 if (!outputDone) {
                //通过BufferInfo来查询解码器的状态
                int decoderStatus = decoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    //正在解码当中
                    if (VERBOSE) Log.d(TAG, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not important for us, since we're using Surface
                    if (VERBOSE) Log.d(TAG, "decoder output buffers changed");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = decoder.getOutputFormat();
                    if (VERBOSE) Log.d(TAG, "decoder output format changed: " + newFormat);
                } else if (decoderStatus < 0) {
                    throw new RuntimeException(
                            "unexpected result from decoder.dequeueOutputBuffer: " +
                                    decoderStatus);
                } else { // decoderStatus >= 0
                    //当前解码成功
                    if (firstInputTimeNsec != 0) {
                        // Log the delay from the first buffer of input to the first buffer
                        // of output.
                        //答应输入到解码成功所需要的时间
                        long nowNsec = System.nanoTime();
                        Log.d(TAG, "startup lag " + ((nowNsec - firstInputTimeNsec) / 1000000.0) + " ms");
                        firstInputTimeNsec = 0;
                    }
                    boolean doLoop = false;
                    if (VERBOSE) Log.d(TAG, "surface decoder given buffer " + decoderStatus +
                            " (size=" + mBufferInfo.size + ")");
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (VERBOSE) Log.d(TAG, "output EOS");
                        if (mLoop) {
                            doLoop = true;
                        } else {
                            outputDone = true;
                        }
                    }
                    //判断是否解码成功，解码是否有数据
                    boolean doRender = (mBufferInfo.size != 0);

                    // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                    // to SurfaceTexture to convert to a texture.  We can't control when it
                    // appears on-screen, but we can manage the pace at which we release
                    // the buffers.
                    // 当我们调用releaseOutputBuffer时，数据就会喂入SurfaceTexture当中，转成texture。我们先回调一个方法。来控制我们送入Surface的速度，来保持我们想要的帧率
                    if (doRender && frameCallback != null) {
                        frameCallback.preRender(mBufferInfo.presentationTimeUs);
                    }
                    //相当于喂入Surface
                    decoder.releaseOutputBuffer(decoderStatus, doRender);
                    //回调
                    if (doRender && frameCallback != null) {
                        frameCallback.postRender();
                    }
                    /如果时循环播放，那继续来
                    if (doLoop) {
                        Log.d(TAG, "Reached EOS, looping");
                        extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                        inputDone = false;
                        decoder.flush();    // reset decoder state
                        frameCallback.loopReset();
                    }
                }
            }

```

- `Thread.Sleep`控制播放的速度
如果我们设置了固定的帧率的话，就想起转成我们要求的时间，进行等待。
如果没有的话，就默认计算两次的时间戳的差值，进行等待。
需要注意的时，第一正，我们不需要进行sleep
```java
 // runs on decode thread
    @Override
    public void preRender(long presentationTimeUsec) {
        // For the first frame, we grab the presentation time from the video
        // and the current monotonic clock time.  For subsequent frames, we
        // sleep for a bit to try to ensure that we're rendering frames at the
        // pace dictated by the video stream.
        //
        // If the frame rate is faster than vsync we should be dropping frames.  On
        // Android 4.4 this may not be happening.
        //如果时第一帧，我们就记录下时间就可以了。
        if (mPrevMonoUsec == 0) {
            // Latch current values, then return immediately.
            mPrevMonoUsec = System.nanoTime() / 1000;
            mPrevPresentUsec = presentationTimeUsec;
        } else {
            //计算两次的插值
            // Compute the desired time delta between the previous frame and this frame.
            long frameDelta;
            //如果设置了循环，我们需要相当是第一帧
            if (mLoopReset) {
                // We don't get an indication of how long the last frame should appear
                // on-screen, so we just throw a reasonable value in.  We could probably
                // do better by using a previous frame duration or some sort of average;
                // for now we just use 30fps.
                mPrevPresentUsec = presentationTimeUsec - ONE_MILLION / 30;
                mLoopReset = false;
            }
            //如果是固定的帧率，就按照我们的来。忽略PTS
            if (mFixedFrameDurationUsec != 0) {
                frameDelta = mFixedFrameDurationUsec;
            } else {
                //计算视频流中的PTS
                frameDelta = presentationTimeUsec - mPrevPresentUsec;
            }
            //如果小于0，这不可能。等于0，也说明有问题
            if (frameDelta < 0) {
                Log.w(TAG, "Weird, video times went backward");
                frameDelta = 0;
            } else if (frameDelta == 0) {
                // This suggests a possible bug in movie generation.
                Log.i(TAG, "Warning: current frame and previous frame had same timestamp");
            } else if (frameDelta > 10 * ONE_MILLION) {
                //如果I帧的间隔大于10s，那就强行改成5s
                Log.i(TAG, "Inter-frame pause was " + (frameDelta / ONE_MILLION) +
                        "sec, capping at 5 sec");
                frameDelta = 5 * ONE_MILLION;
            }

            //目标的时间
            long desiredUsec = mPrevMonoUsec + frameDelta;  // when we want to wake up
            long nowUsec = System.nanoTime() / 1000;
            //通过while循环进行sleep
            while (nowUsec < (desiredUsec - 100) /*&& mState == RUNNING*/) {
                // Sleep until it's time to wake up.  To be responsive to "stop" commands
                // we're going to wake up every half a second even if the sleep is supposed
                // to be longer (which should be rare).  The alternative would be
                // to interrupt the thread, but that requires more work.
                //
                // The precision of the sleep call varies widely from one device to another;
                // we may wake early or late.  Different devices will have a minimum possible
                // sleep time. If we're within 100us of the target time, we'll probably
                // overshoot if we try to sleep, so just go ahead and continue on.
                long sleepTimeUsec = desiredUsec - nowUsec;
                //如果sleep的时间大于500000，则分为500000 来Sleep一次
                if (sleepTimeUsec > 500000) {
                    sleepTimeUsec = 500000;
                }
                try {
                    //如果检查sleeptime的话，就打印对应的log
                    if (CHECK_SLEEP_TIME) {
                        long startNsec = System.nanoTime();
                        Thread.sleep(sleepTimeUsec / 1000, (int) (sleepTimeUsec % 1000) * 1000);
                        long actualSleepNsec = System.nanoTime() - startNsec;
                        Log.d(TAG, "sleep=" + sleepTimeUsec + " actual=" + (actualSleepNsec / 1000) +
                                " diff=" + (Math.abs(actualSleepNsec / 1000 - sleepTimeUsec)) +
                                " (usec)");
                    } else {
                        Thread.sleep(sleepTimeUsec / 1000, (int) (sleepTimeUsec % 1000) * 1000);
                    }
                } catch (InterruptedException ie) {
                }
                nowUsec = System.nanoTime() / 1000;
            }

            // Advance times using calculated time values, not the post-sleep monotonic
            // clock time, to avoid drifting.
            //偏移下一帧的时间
            //单调的时间，只会增长
            mPrevMonoUsec += frameDelta;
            //PTS，会因为Loop,而减少一帧30fps
            mPrevPresentUsec += frameDelta;
        }
    }
```

### 除了输入的部分
其他的部分，维持之前的就可以了。

## 最后
对比两次的绘制流程。我们发现一个通用的OpenGL的处理思路。
![process_summary.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/process_summary.png)
#### 输入
我们可以选择多种源输入Surface,转成给纹理，这样就可以在OpenGL中进行处理。
我们已经尝试将Camera的预览数据和视频数据作为输入，转成了纹理数据。我们还可以将FFmpeg的数据，通过Ndk的方式，输入Surface当中，做处理吗？
#### EGL environment
- ElgContext.纹理和线程之间的关系
纹理和`EglContext`相关。`EglContext`和线程相关。不同线程的GLThread只要能共享`Egl Context`就能对同一个纹理进行处理。
- 滤镜处理
有了纹理和`EglContext`，我们可以通过`FBO`的转换，进行任意的`Offscreen`绘制,结合`glsl`，添加我们想要的滤镜。
#### 输出
作为输出，我们已经尝试
- 直接将数据绘制到Screen上显示。
- 通过MediaCodec进行编码，通过`MediaMuxer`进行封装。保存到文件中。
- 更多
我们后续还可以 将得到的数据送入FFmpeg当中进行软编码吗？!
或者考虑，将编码好的数据，进行推流直播吗？

#### 缺陷
本系列文章中，对其的处理，都只是视频部分，无音频部分的处理

#### 后续
后续的文章中，将继续更多的滤镜处理和上面思考部分的探究。


