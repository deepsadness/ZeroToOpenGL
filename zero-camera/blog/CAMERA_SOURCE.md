![cover.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/cover.png)


上文中我们已经实现了在纹理上添加滤镜的效果。这编文章就是将OpenGl和相机结合到一起。

## 预览与拍照
### 整体流程理解

![预览的整体流程.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/process1.png)

1. 将`Camera`中得到的`ImageStream`由`SurfaceTexture`接受，并转换成`OpenGL ES`纹理。
2. 创建`GLSurfaceView`。在`OpenGL`环境下,用`GLSurfaceView.Render`将这个纹理绘制出来。
3. 整体的`ImageStream`的流向就是
```
Camera ==>SurfaceTexture==>texture(samplerExternalOES) ==>draw to GLSurfaceView
```

### 各个部分详解
#### Camra Api
首先是相机的Api的书写。
##### Camera Interface
为我们相机的操作定义一个接口。因为我们的相机Api。有Camera2和Camera的区别。这里还是简单的使用Camera来完成。

```java
/**
 * 定义个相机的功能接口
 */
public interface ICamera {
    boolean open(int cameraId);

    /**
     * 设置画面的比例
     */
    void setAspectRatio(AspectRatio aspectRatio);

    /**
     * 开启预览
     */
    boolean preview();

    /**
     * 关闭相机
     *
     * @return
     */
    boolean close();

    /**
     * 使用SurfaceTexture 来作为预览的画面
     *
     * @param surfaceTexture
     */
    void setPreviewTexture(SurfaceTexture surfaceTexture);

    CameraSize getPreviewSize();
    CameraSize getPictureSize();
}
```
定义一个相机的接口。我们知道。我们需要相机做的几个通常的操作。
##### CameraApi14
```java
/**
 * for api 14
 * <p>
 * Camera主要涉及参数
 * 1. 预览画面的大小
 * 2. pic图片的大小
 * 3. 对焦模式
 * 4. 闪光灯模式
 */
public class CameraApi14 implements ICamera {
    /*
    当前的相机Id
     */
    private int mCameraId;
    /*
    当前的相机对象
     */
    private Camera mCamera;
    /*
    当前的相机参数
     */
    private Camera.Parameters mCameraParameters;


    //想要的尺寸。
    private int mDesiredHeight = 1920;
    private int mDesiredWidth = 1080;
    private boolean mAutoFocus;
    public CameraSize mPreviewSize;
    public CameraSize mPicSize;
    /*
     * 当前相机的高宽比
     */
    private AspectRatio mDesiredAspectRatio;


    public CameraApi14() {
        mDesiredHeight = 1920;
        mDesiredWidth = 1080;
        //创建默认的比例.因为后置摄像头的比例，默认的情况下，都是旋转了270
        mDesiredAspectRatio = AspectRatio.of(mDesiredWidth, mDesiredHeight).inverse();
    }

    @Override
    public boolean open(int cameraId) {
        /*
            预览的尺寸和照片的尺寸
        */
        final CameraSize.ISizeMap mPreviewSizes = new CameraSize.ISizeMap();
        final CameraSize.ISizeMap mPictureSizes = new CameraSize.ISizeMap();
        if (mCamera != null) {
            releaseCamera();
        }
        mCameraId = cameraId;
        mCamera = Camera.open(mCameraId);
        if (mCamera != null) {
            mCameraParameters = mCamera.getParameters();

            mPreviewSizes.clear();
            //先收集参数.因为每个手机能够得到的摄像头参数都不一致。所以将可能的尺寸都得到。
            for (Camera.Size size : mCameraParameters.getSupportedPreviewSizes()) {
                mPreviewSizes.add(new CameraSize(size.width, size.height));
            }

            mPictureSizes.clear();
            for (Camera.Size size : mCameraParameters.getSupportedPictureSizes()) {
                mPictureSizes.add(new CameraSize(size.width, size.height));
            }
            //挑选出最需要的参数
            adJustParametersByAspectRatio2(mPreviewSizes, mPictureSizes);
            return true;
        }
        return false;
    }

    private void adJustParametersByAspectRatio(CameraSize.ISizeMap previewSizes, CameraSize.ISizeMap pictureSizes) {
        //得到当前预期比例的size
        SortedSet<CameraSize> sizes = previewSizes.sizes(mDesiredAspectRatio);
        if (sizes == null) {  //表示不支持.
            // TODO: 2018/9/14 这里应该抛出异常？
            return;
        }
        //当前先不考虑Orientation
        CameraSize previewSize;
        mPreviewSize = new CameraSize(mDesiredWidth, mDesiredHeight);
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mPreviewSize = new CameraSize(mDesiredHeight, mDesiredWidth);
            mCameraParameters.setRotation(90);
        } else {
//            previewSize = mPreviewSize;
        }

        //默认去取最大的尺寸
        mPicSize = pictureSizes.sizes(mDesiredAspectRatio).first();

        mCameraParameters.setPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mCameraParameters.setPictureSize(mPicSize.getWidth(), mPicSize.getHeight());

        //设置对角和闪光灯
        setAutoFocusInternal(mAutoFocus);
        //先不设置闪光灯
//        mCameraParameters.setFlashMode("FLASH_MODE_OFF");

        //设置到camera中
//        mCameraParameters.setRotation(90);
        mCamera.setParameters(mCameraParameters);
//        mCamera.setDisplayOrientation(90);
//        setCameraDisplayOrientation();
    }

    private void adJustParametersByAspectRatio2(CameraSize.ISizeMap previewSizes, CameraSize.ISizeMap pictureSizes) {
        //得到当前预期比例的size
        SortedSet<CameraSize> sizes = previewSizes.sizes(mDesiredAspectRatio);
        if (sizes == null) {  //表示不支持.
            // TODO: 2018/9/14 这里应该抛出异常？
            return;
        }
        //当前先不考虑Orientation
        mPreviewSize = sizes.first();
        //默认去取最大的尺寸
        mPicSize = pictureSizes.sizes(mDesiredAspectRatio).first();
        mCameraParameters.setPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mCameraParameters.setPictureSize(mPicSize.getWidth(), mPicSize.getHeight());

        mPreviewSize = mPreviewSize.inverse();
        mPicSize = mPicSize.inverse();
        //设置对角和闪光灯
        setAutoFocusInternal(mAutoFocus);
        //先不设置闪光灯
//        mCameraParameters.setFlashMode("FLASH_MODE_OFF");

        //设置到camera中
//        mCameraParameters.setRotation(90);
        mCamera.setParameters(mCameraParameters);
//        mCamera.setDisplayOrientation(90);
//        setCameraDisplayOrientation();
    }

    private boolean setAutoFocusInternal(boolean autoFocus) {
        mAutoFocus = autoFocus;
//        if (isCameraOpened()) {
        final List<String> modes = mCameraParameters.getSupportedFocusModes();
        if (autoFocus && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
            mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
        } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
            mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
        } else {
            mCameraParameters.setFocusMode(modes.get(0));
        }
        return true;
//        } else {
//            return false;
//        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void setAspectRatio(AspectRatio aspectRatio) {
        this.mDesiredAspectRatio = aspectRatio;
    }

    @Override
    public boolean preview() {
        if (mCamera != null) {
            mCamera.startPreview();
            return true;
        }
        return false;
    }

    @Override
    public boolean close() {
        if (mCamera != null) {
            try {
                //stop preview时，可能爆出异常
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return false;
    }

    @Override
    public void setPreviewTexture(SurfaceTexture surfaceTexture) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(surfaceTexture);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public CameraSize getPreviewSize() {
        return mPreviewSize;
    }

    @Override
    public CameraSize getPictureSize() {
        return mPicSize;
    }
}

```
这里的代码就是使用`Camera`来实现上面的功能。

1. 因为使用我们期望将Camera中得到的数据传递到纹理上，所以需要`setPreviewTexture(SurfaceTexture texture)`。让这个`SurfaceTexture`来承载。

```java
@Override
    public void setPreviewTexture(SurfaceTexture surfaceTexture) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(surfaceTexture);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
```
2. 选择相机的预览的尺寸和旋转的角度
相机的`parameter`的选择，只要选对了对应的想要的比例就行了。没有其他需要的。
设备坐标和纹理坐标之间的方向不同问题，由后面纹理的矩阵来控制就好了。


#### SurfaceTexture
可以从图像流中捕获帧作为`OpenGL ES`纹理。
1. 直接使用创建的纹理，来创建SurfaceTexture就可以了。
```java
mSurfaceTexture = new SurfaceTexture(mTextureId);
```
2. 然后再将其设置给Camera.同时每次SurfaceTexture刷新的时候，都必须刷新GLSurfaceView。
```java
  mCameraApi.setPreviewTexture(mCameraDrawer.getSurfaceTexture());
  //默认使用的GLThread.每次刷新的时候，都强制要求是刷新这个GLSurfaceView
  mCameraDrawer.getSurfaceTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                requestRender();
            }
        });
```
##### 注意事项
使用时必须要注意的是
1. **纹理对象使用`GL_TEXTURE_EXTERNAL_OES`纹理目标，该目标由[GL_OES_EGL_image_external](http://www.khronos.org/registry/gles/extensions/OES/OES_EGL_image_external.txt)OpenGL ES扩展定义。**
每次绑定纹理时，它必须绑定到`GL_TEXTURE_EXTERNAL_OES`目标而不是`GL_TEXTURE_2D`目标。在OpenGL ES 2.0着色器必须使用
```glsl
#extension GL_OES_EGL_image_external：require
```
2. 着色器还必须使用samplerExternalOES GLSL采样器类型访问纹理。
```glsl
uniform samplerExternalOES uTexture;
```


---
#### GLSurfaceView.Render
##### GLSL部分
- **oes_base_vertex.glsl**
```glsl
attribute vec4 aPosition;
attribute vec2 aCoordinate;
uniform mat4 uMatrix;
uniform mat4 uCoordinateMatrix;
varying vec2 vTextureCoordinate;

void main(){
    gl_Position = uMatrix*aPosition;
    vTextureCoordinate = (uCoordinateMatrix*vec4(aCoordinate,0.1,0.1)).xy;
}
```
顶点着色器对比相对简单。这里需要注意的就是矩阵相乘的顺序问题。
```glsl
//这个是正确的顺序。相称的顺序相反，图像是反的！！！
gl_Position = uMatrix*aPosition;
```

- **oes_base_fragment.glsl**
这里就是如上面注意事项中说的。必须使用`samplerExternalOES `来采样。
```glsl
#extension GL_OES_EGL_image_external : require
precision mediump float;

varying vec2 vTextureCoordinate;
uniform samplerExternalOES uTexture;
void main() {
    gl_FragColor = texture2D(uTexture,vTextureCoordinate);
}
```
##### 其他的部分
其他的部分和前几编文章中提到的相差不多。
###### 0. 生成纹理
这里就是上面所说的。**只能用`GLES11Ext.GL_TEXTURE_EXTERNAL_OES`这种纹理。**
```java
 private int genOesTextureId() {
        int[] textureObjectId = new int[1];
        GLES20.glGenTextures(1, textureObjectId, 0);
        //绑定纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureObjectId[0]);
        //设置放大缩小。设置边缘测量
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return textureObjectId[0];
    }
```
###### 1. 视图矩阵。
因为设备坐标和纹理的坐标不同。而前置摄像头和后置摄像头的翻转的方向也不同。所以要做下面的处理
```java
   //计算需要变化的矩阵
    private void calculateMatrix() {
        //得到通用的显示的matrix
        Gl2Utils.getShowMatrix(mModelMatrix, mPreviewWidth, mPreviewHeight, this.mSurfaceWidth, this.mSurfaceHeight);

        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {  //前置摄像头
            Gl2Utils.flip(mModelMatrix, true, false);
            Gl2Utils.rotate(mModelMatrix, 90);
        } else {  //后置摄像头
            int rotateAngle = 270;
            Gl2Utils.rotate(mModelMatrix, rotateAngle);
        }
        mOesFilter.setMatrix(mModelMatrix);
    }
```
1. 得到标准的透视视图矩阵
```java
 public static void getShowMatrix(float[] matrix,int imgWidth,int imgHeight,int viewWidth,int
        viewHeight){
        if(imgHeight>0&&imgWidth>0&&viewWidth>0&&viewHeight>0){
            float sWhView=(float)viewWidth/viewHeight;
            float sWhImg=(float)imgWidth/imgHeight;
            float[] projection=new float[16];
            float[] camera=new float[16];
            if(sWhImg>sWhView){
                Matrix.orthoM(projection,0,-sWhView/sWhImg,sWhView/sWhImg,-1,1,1,3);
            }else{
                Matrix.orthoM(projection,0,-1,1,-sWhImg/sWhView,sWhImg/sWhView,1,3);
            }
            Matrix.setLookAtM(camera,0,0,0,1,0,0,0,0,1,0);
            Matrix.multiplyMM(matrix,0,projection,0,camera,0);
        }
    }
```
这部分就是标准的处理方式了。谁的比例大，用谁的。
2. 处理不同摄像头的旋转
如果是前置摄像头的话，需要进行左右的翻转。然后旋转90度。
后置摄像头的话，只需要旋转270度就可以了。

###### 2. 绘制图形
**重温一下绘制整体的流程**
```java
//draw step
public void draw() {
   //step0 clear
   onClear();
   //step1 use program
   onUseProgram();
   //step2 active and bind custom data
   onSetExpandData();
   //step3 bind texture
   onBindTexture();
   //step4 normal draw
   onDraw();
 }
```
- `onClear`
```java
   GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
   GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
```
这里有一个疑问。这里其实不用每次都清除的。只要初始化清除就可以了吧？

- `onUseProgram`
这一步，就是使用我们之前已经创建和`link`好的`program`
```java
 private void onUseProgram() {
        GLES20.glUseProgram(mProgram);
    }
```

- `onSetExpandData`
这里做的其实就是我们额外给`glsl`添加的属性。应用上面我们变化的矩阵。
```java
 private void onSetExpandData() {
        GLES20.glUniformMatrix4fv(mUMatrix, 1, false, matrix, 0);
        GLES20.glUniformMatrix4fv(mUCoordMatrix, 1, false, mCoordMatrix, 0);
 }
```

- `onBindTexture`
接着就是激活和绑定纹理数据.
```java
 private void onBindTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + getTextureType());
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, getTextureId());
        GLES20.glUniform1i(mUTexture, getTextureType());
    }
```
我们的纹理都挂载在`GLES20.GL_TEXTURE0 + getTextureType()`上。**同时一定要注意的是，相机这里需要的是`GLES11Ext.GL_TEXTURE_EXTERNAL_OES`这种拓展类型的纹理采样。**

- `onDraw`
绘制图像的话，同之前相同，只需要绘制一个长方形就可以了。
```java
 private void onDraw() {
        //设置定点数据
        GLES20.glEnableVertexAttribArray(mAPosition);
        GLES20.glVertexAttribPointer(
                mAPosition,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                mVerBuffer);
        //
        GLES20.glEnableVertexAttribArray(mACoord);
        GLES20.glVertexAttribPointer(
                mACoord,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                mTextureCoordinate);
        //绘制三角形带
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(mAPosition);
        GLES20.glDisableVertexAttribArray(mACoord);
    }
```

#### GLSurfaceView
最后在GLSurfaceView的对应的生命周期内调用方法就可以了~~

## 录制
### 整体流程理解

![录制的整体流程.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/process_encoder.png)

在原来预览的基础上，我们需要加入`MediaCodec`进行视频编码。
0. 图中的`EglCore`着保存`EglContext`。`EglSurface`和`EglConfig`的配置。`WindowSurface`就是将`EglContext`和`Surface`相互关联的帮助类。
1. `Encoder`是在`EncoderThread`中进行。两个线程(和原来的`GLTHread`)需要共享`EGLContext`。
2. 使用`MediaCodec`进行视频编码，只要通过它的`InputSurface`，将数据输入就可以。所以通过共享的`EGLContext`，来创建一个`WindowSurface`。然后再通过在该线程内GL的`draw`方法，就可以将`EGLContext`中的Oes纹理，绘制到`Surface`上。这样`MediaCodec`就可以得到数据。

3. **整体流向**
当我们接受到frame时，我们需要
- 在`GLSurfaceView`的渲染线程,将数据渲染到`SurfaceView`
- 在`Encoder`的线程，将`frame`渲染到`Mediacodec`的`InputSurface`中
```
Camera==>
  SurfaceTexture.onFrameAvailable==>
  GLSurfaceView.requestRender ==>
  {
    //通知更新下一帧
    mSurfaceTexture.updateTexImage()
    //在`Encoder`的线程，将`frame`渲染到`Mediacodec`的`InputSurface`中。通知编码器线程绘制并编码
    mVideoEncoder.frameAvailable(mSurfaceTexture) ==>
      {
        //通知编码器进行编码
        mVideoEncoder.drainEncoder(false);
        //刷入数据
        mFullScreen.drawFrame(mTextureId, transform);
        //给InputWindow设置时间戳
        mInputWindowSurface.setPresentationTime(timestampNanos);
        //刷新之后，编码器得到数据？
        mInputWindowSurface.swapBuffers();
      }
    //同时Render绘制到屏幕上。在`GLSurfaceView`的渲染线程,将数据渲染到`SurfaceView`
    mOesFilter.draw();
  }

```
### 各个部分详解
#### TextureMovieEncoder
主要还是添加了一个这个类。
> 理想状态下，我们创建Video Encoder，然后为它创建EGLContext，然后将这个context传入GLSurfaceView来共享。 但是这里的Api做不到这样，所以我们只能反着来。当GLSurfaceView torn down时，（可能时我们旋转了屏幕），EGLContext也会同样被抛弃。这样意味这当它回来的时候，我们就需要重新为Video encoder创建EGLContext.(而且，"preserve EGLContext on pause" 这样的功能，也不启作用。就是上一个暂停状态的EGLContext，在这里也不能用)我们可以通过使用TextureView 来替代GLSurfaceView来做一些简化。但是这样会由一点性能的问题。

##### 创建EGL环境
###### 获取`EGLContext`
可以直接在`GLThread`中通过`EGL14.eglGetCurrentContext()`,就可以得到和线程绑定的`EGLContext`(`EGLContext`其实也是存在于`ThreadLocal`当中)
```java
 private void startRecord() {
        mOutputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "camera-test" + System.currentTimeMillis() + ".mp4");
        Log.d(TAG, "file path = " + mOutputFile.getAbsolutePath());
        // start recording
        mVideoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
                mOutputFile, mPreviewHeight, mPreviewWidth, 1000000, EGL14.eglGetCurrentContext()));
        mRecordingStatus = RECORDING_ON;
    }
```
###### 线程的通信是通过`Handler`来完成。
```java
public void startRecording(EncoderConfig config) {
        Log.d(TAG, "Encoder: startRecording()");
        //mReadyFence 这个锁是来锁这个线程的所有操作的。包括开始。停止。绘制。
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread already running");
                return;
            }
            mRunning = true;
            new Thread(this, "TextureMovieEncoder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
    }
```
###### 创建`WindowSurface`。将`EGLContext`和`Encoder.InputSurface`关联在一起
```java
 private void prepareEncoder(EGLContext sharedContext, int width, int height, int bitRate,
                                File outputFile) {
        try {
            //这个就算MediaCodec的封装。包括MediaCodec进行编码。MediaMuxer进行视频封装
            mVideoEncoder = new VideoEncoderCore(width, height, bitRate, outputFile);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        //通过EglContext创建EglCore
        mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);
        //创建inputWindowSurface
        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
        //在完成EGL的初始化之后,需要通过eglMakeCurrent()函数来将当前的上下文切换,这样opengl的函数才能启动作用。
        mInputWindowSurface.makeCurrent();

        mFullScreen = new FullFrameRect(
                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
    }
```
这里进行了一系列的初始化工作。
- 初始化了`VideoEncoderCore`。它是`MediaCodec`和`MediaMuxer`的封装。
```java
public VideoEncoderCore(int width, int height, int bitRate, File outputFile)
            throws IOException {
        //MediaCodec的BufferInfo的缓存。通过这个BufferInfo不断的运输数据。（原始=>编码后的）
        mBufferInfo = new MediaCodec.BufferInfo();
        //创建MediaFormat MIME_TYPE = "video/avc"
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);

        //设置我们想要的参数。如果参数不合法的话，在configure时，就会报错的
        //这个ColorFormat很重要，这里一定要设置COLOR_FormatSurface
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        //这里的设置5 seconds 在相邻的 I-frames，why?
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        if (VERBOSE) Log.d(TAG, "format: " + format);

        //创建编码器
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //得到对应InputSureface
        mInputSurface = mEncoder.createInputSurface();
        //启动
        mEncoder.start();

        //创建MediaMuxer。我们不能直接在这里开始muxer.因为MediaFormat 还没得到输入。必须要在编码器得到输入之后，才能添加。这里先不添加音频。
        mMuxer = new MediaMuxer(outputFile.toString(),
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        mTrackIndex = -1;
        mMuxerStarted = false;
    }
```
- 初始化了`EglCore`。主要是管理EGL的state，包括 (display, context, config)。
```java
  //主要是初始化display 和EglContext
  public EglCore(EGLContext sharedContext, int flags) {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("EGL already set up");
        }

        if (sharedContext == null) {
            sharedContext = EGL14.EGL_NO_CONTEXT;
        }
        //先创建一个默认的Display
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }
        //检查是否创建成功
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null;
            throw new RuntimeException("unable to initialize EGL14");
        }

        // Try to get a GLES3 context, if requested.
        if ((flags & FLAG_TRY_GLES3) != 0) {
            //Log.d(TAG, "Trying GLES 3");
            EGLConfig config = getConfig(flags, 3);
            if (config != null) {
                int[] attrib3_list = {
                        EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                        EGL14.EGL_NONE
                };
                EGLContext context = EGL14.eglCreateContext(mEGLDisplay, config, sharedContext,
                        attrib3_list, 0);

                if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
                    //Log.d(TAG, "Got GLES 3 config");
                    mEGLConfig = config;
                    mEGLContext = context;
                    mGlVersion = 3;
                }
            }
        }
        if (mEGLContext == EGL14.EGL_NO_CONTEXT) {  // GLES 2 only, or GLES 3 attempt failed
            //Log.d(TAG, "Trying GLES 2");
            //获取GL的Config
            EGLConfig config = getConfig(flags, 2);
            if (config == null) {
                throw new RuntimeException("Unable to find a suitable EGLConfig");
            }
            int[] attrib2_list = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };
            //获取EGLContext关键方法就是它，EGL14.eglCreateContext
            EGLContext context = EGL14.eglCreateContext(mEGLDisplay, config, sharedContext,
                    attrib2_list, 0);
            checkEglError("eglCreateContext");
            mEGLConfig = config;
            mEGLContext = context;
            mGlVersion = 2;
        }

        // Confirm with query.
        int[] values = new int[1];
        EGL14.eglQueryContext(mEGLDisplay, mEGLContext, EGL14.EGL_CONTEXT_CLIENT_VERSION,
                values, 0);
        Log.d(TAG, "EGLContext created, client version " + values[0]);
    }

    //EGL的配置。类似键值对的数组。
   private EGLConfig getConfig(int flags, int version) {
        int renderableType = EGL14.EGL_OPENGL_ES2_BIT;
        if (version >= 3) {
            renderableType |= EGLExt.EGL_OPENGL_ES3_BIT_KHR;
        }

        // The actual surface is generally RGBA or RGBX, so situationally omitting alpha
        // doesn't really help.  It can also lead to a huge performance hit on glReadPixels()
        // when reading into a GL_RGBA buffer.
        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                //EGL14.EGL_DEPTH_SIZE, 16,
                //EGL14.EGL_STENCIL_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, renderableType,
                EGL14.EGL_NONE, 0,      // placeholder for recordable [@-3]
                EGL14.EGL_NONE
        };
        if ((flags & FLAG_RECORDABLE) != 0) {
            attribList[attribList.length - 3] = EGL_RECORDABLE_ANDROID;
            attribList[attribList.length - 2] = 1;
        }
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                numConfigs, 0)) {
            Log.w(TAG, "unable to find RGB8888 / " + version + " EGLConfig");
            return null;
        }
        return configs[0];
    }
```
- 初始化了`WindowSurface`。并通过eglMakeCurrent（）函数，切换到当前的上下文。
```java
    /**
     * 将EGL和原生的window surface关联在一起
     * 如果传入releaseSurface为true的话，当你调用release方法时，这个Surface就会自动被release。
     * 但时如果是使用了SurfaceView的Surface等，Android框架创建的Surface时需要注意，
     * 它会干涉原生框架的调用，比如上述的SurfaceView的Surface,release之后，surfaceDestroyed()回调将不会再收到
     */
    public WindowSurface(EglCore eglCore, Surface surface, boolean releaseSurface) {
        super(eglCore);
        createWindowSurface(surface);
        mSurface = surface;
        mReleaseSurface = releaseSurface;
    }
    /**
     * 创建 window surface.我们的之前的信息提前就保存再EglCore内了
     * <p>
     * @param surface May be a Surface or SurfaceTexture.
     */
    public void createWindowSurface(Object surface) {
        if (mEGLSurface != EGL14.EGL_NO_SURFACE) {
            throw new IllegalStateException("surface already created");
        }
        mEGLSurface = mEglCore.createWindowSurface(surface);
    }

    /**
     * 如果我们是为了MediaCodec创建，那么EGLConfig需要有"recordable"的attribute.这个部分，在上面初始化EglCore时，已经完成了EGLConfig和EGLDisplay的配置
     */
    public EGLSurface createWindowSurface(Object surface) {
        if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture)) {
            throw new RuntimeException("invalid surface: " + surface);
        }

        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
        //这就是我们想要的EGLSurface的创建方式  EGL14.eglCreateWindowSurface
        EGLSurface eglSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, mEGLConfig, surface,
                surfaceAttribs, 0);
        checkEglError("eglCreateWindowSurface");
        if (eglSurface == null) {
            throw new RuntimeException("surface was null");
        }
        return eglSurface;
    }
```
- 初始化`FullFrameRect`。它是OpenGl绘制命令等的封装。

##### frameAvailable
在接受到`Frame`时，进行编码
```java
mVideoEncoder.frameAvailable(mSurfaceTexture);
```
- 时间戳和tranfrom矩阵
```java
  float[] transform = new float[16];      // TODO - avoid alloc every frame
        st.getTransformMatrix(transform);
        long timestamp = st.getTimestamp();
        if (timestamp == 0) {
            // Seeing this after device is toggled off/on with power button.  The
            // first frame back has a zero timestamp.
            //
            // MPEG4Writer thinks this is cause to abort() in native code, so it's very
            // important that we just ignore the frame.
            Log.w(TAG, "HEY: got SurfaceTexture with timestamp of zero");
            return;
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE,
                (int) (timestamp >> 32), (int) timestamp, transform));
```
- 编码
```java
  private void handleFrameAvailable(float[] transform, long timestampNanos) {
        if (VERBOSE) Log.d(TAG, "handleFrameAvailable tr=" + transform);
        //视频编码
        mVideoEncoder.drainEncoder(false);
        mFullScreen.drawFrame(mTextureId, transform);
        //设置时间戳
        mInputWindowSurface.setPresentationTime(timestampNanos);
        mInputWindowSurface.swapBuffers();
    }

  /**
     * 从encoder中得到数据，再写入到muxer中。
     * 下面这段代码就是通用的编码的代码了
     */
    public void drainEncoder(boolean endOfStream) {
        final int TIMEOUT_USEC = 10000;
        if (VERBOSE) Log.d(TAG, "drainEncoder(" + endOfStream + ")");

        //如果通知编码器结束，就会signalEndOfInputStream
        if (endOfStream) {
            if (VERBOSE) Log.d(TAG, "sending EOS to encoder");
            mEncoder.signalEndOfInputStream();
        }

        //得到outputBuffer
        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        //不断循环，当读取所有数据时
        while (true) {
            //上面换成的BufferInfo，送入到Encoder中，去查询状态
            int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            //如果时继续等待，就暂时不用处理。大多数情况，都是从这儿跳出循环
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break;      // out of while
                } else {
                    if (VERBOSE) Log.d(TAG, "no output available, spinning to await EOS");
                }
            //outputBuffer发生变化了。就重新去获取
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            //格式发生变化。这个第一次configure之后也会调用一次。在这里进行muxer的初始化
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = mEncoder.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxer.addTrack(newFormat);
                mMuxer.start();
                mMuxerStarted = true;
            } else if (encoderStatus < 0) {
                Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                        encoderStatus);
                // let's ignore it
            } else {
                //写入数据
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }
                    //切到对应的位置，进行书写
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                    //写入
                    mMuxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    if (VERBOSE) {
                        Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                mBufferInfo.presentationTimeUs);
                    }
                }
                //重新释放，为了下一次的输入
                mEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    //到达最后了，就跳出循环
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                        if (VERBOSE) Log.d(TAG, "end of stream reached");
                    }
                    break;      // out of while
                }
            }
        }
    }
```

## 添加滤镜
### 整体流程理解
![添加滤镜后的整体流程.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/process_filter.png)

上面，我们是直接绘制OES的纹理。这里，因为要添加滤镜的效果。所以我们需要将纹理进行处理。
#### 离屏绘制
![离屏绘制.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/process-offscreen-render.png)
先将`OES`纹理，绑定到`FrameBuffer`上。同时会在`FrameBuffer`上绑定一个新的`textureId`(这里命名为`OffscreenTextureId`)。然后调用绘制`OES`纹理的方法，数据就会传递到`FBO`上。而我们可以通过绑定在其上的`OffscreenTextureId`得到其数据。通常情况下，我们把绑定`FrameBuffer`和绘制这个新的`OffscreenTextureId`代表的纹理的过程，称为离屏绘制。
##### 绑定和生成`FrameBuffer`的时机
创建`FrameBuffer`。因为RenderBuffer的存储大小要和当前的显示的宽和高相关。所以会在`onSurfaceChanged`生命周期方法时候调用。
```java
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //在这里监听到尺寸的改变。做出对应的变化
        prepareFramebuffer(width, height);
        //...
    }

    //生成frameBuffer的时机
    private void prepareFramebuffer(int width, int height) {
        int[] values = new int[1];
        //申请一个与FrameBuffer绑定的textureId
        GLES20.glGenTextures(1, values, 0);
        mOffscreenTextureId = values[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOffscreenTextureId);
         // Create texture storage.
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        // Set parameters.  We're probably using non-power-of-two dimensions, so
        // some values may not be available for use.
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        //创建FrameBuffer Object并且绑定它
        GLES20.glGenFramebuffers(1, values, 0);
        mFrameBuffer = values[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);

        // 创建RenderBuffer Object并且绑定它
        GLES20.glGenRenderbuffers(1, values, 0);
        mRenderBuffer = values[0];
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mRenderBuffer);

        //为我们的RenderBuffer申请存储空间
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);

        // 将renderBuffer挂载到frameBuffer的depth attachment 上。就上面申请了OffScreenId和FrameBuffer相关联
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mRenderBuffer);
        // 将text2d挂载到frameBuffer的color attachment上
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mOffscreenTextureId, 0);

        // See if GLES is happy with all this.
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer not complete, status=" + status);
        }
        // 先不使用FrameBuffer，将其切换掉。到开始绘制的时候，在绑定回来
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    //在onDrawFrame中添加代码
    @Override
    public void onDrawFrame(GL10 gl) {
        //...省略

        //重新切换到FrameBuffer上。
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);
        //这里的绘制，就会将数据挂载到FrameBuffer上了。
        mOesFilter.draw();
        //解除绑定，结束FrameBuffer部分的数据写入
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        //....省略
    }


```

- `FrameBuffer` 帧缓冲对象
![openGL绘制流程.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/opengl_draw_process.png)

我们自己创建的FrameBuffer其实只是一个容器。所以我们要将数据挂载上去，它才算是完整。

![FrameBuffer.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/frameBuffer.png)

所以，我们可以看到申请FrameBuffer需要进行下面的三步
1. 生成一个FrameBuffer
2. 申请一个RenderBuffer,并且挂载`GL_DEPTH_ATTACHMENT`上。
>`RenderBuffer`也是一个渲染缓冲区对象。`RenderBuffer`对象是新引入的用于离屏渲染。它允许将场景直接渲染到`Renderbuffer`对象，而不是渲染到纹理对象。
`Renderbuffe`r只是一个包含可渲染内部格式的单个映像的数据存储对象。它用于存储没有相应纹理格式的`OpenGL`逻辑缓冲区，如模板或深度缓冲区。

3. 申请一个`textureId`,挂载到`GL_COLOR_ATTACHMENT0`上。
4. 重新切换到FrameBuffer上（绑定），然后绘制。

我们就可以通过这个纹理，得到保存在`FBO`上的数据了

##### 添加滤镜的绘制
![添加滤镜.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/process-add-filter.png)

我们可以通过`FBO`，进行滤镜处理。我们将得到的数据，再次进行绘制，在这次的绘制中，我们就可以添加上我们想要的滤镜处理了。

但是这里不仅仅是要绘制到屏幕上，同时要在开启录制的时候，输入给Encoder进行视频的编码和封装。
所以我们需要将数据再写写入一个新的FrameBuffer中，然后再其输出的outputTexture中，就可以得到应用了纹理的数据了。

- 滤镜处理
就算将上面的OffscreenTextureId作为这里滤镜的输入Id.
```java
    @Override
    public void onDrawFrame(GL10 gl) {
         //...省略
        //经过路径处理
        mColorFilter.setTextureId(mOffscreenTextureId);
        mColorFilter.onDrawFrame();
        int outputTextureId = mColorFilter.getOutputTextureId();
        //...省略
    }
```
同时滤镜内，也按照上面的`FrameBuffer`的处理流程。将数据挂载到`FrameBuffer`上。得到挂载在`FrameBuffer`上的`outputTextureId`
```java
代码同上，省略
```

##### 将应用了滤镜的纹理分别绘制到`GLView`和`Encoder`当中
![image.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/process-draw-display-encoder.png)

```java
@Override
    public void onDrawFrame(GL10 gl) {
        //省略...
        //经过滤镜处理
        mColorFilter.setTextureId(mOffscreenTextureId);
        mColorFilter.onDrawFrame();
        int outputTextureId = mColorFilter.getOutputTextureId();

        //将得到的outputTextureId，输入encoder,进行编码
        mVideoEncoder.setTextureId(outputTextureId);

        mVideoEncoder.frameAvailable(mSurfaceTexture);

        //将得到的outputTextureId,再次Draw,因为没有FrameBuffer,所以这次Draw的数据，就直接到了Surface上了。
        mShowFilter.setTextureId(outputTextureId);
        mShowFilter.onDrawFrame();
    }
```
1. 将得到的`outputTextureId`，输入`Encoder`的`InputSurface`中,通知内部进行`draw` 和进行编码。
2. 将得到的`outputTextureId`,再次Draw,因为没有`FrameBuffer`,所以这次`draw`的数据，就直接到了`Surface`上了。也就是直接绘制到了我们的`GLSurfaceView`上了。

### 小结
1. 对比之前绘制流程。上文是直接将纹理绘制到了`GLView`上显示，而这里是将纹理绘制到绑定的`FrameBuffer`中，而且
绘制的结果不直接显示出来。所以可以形象的理解离屏绘制，就是将绘制的结果保存在与`FrameBuffer`绑定的一个新的`textureId`（`OffscreenTextureId`）中，不直接绘制到屏幕上。


## 最后
整编文章就重要的部分还是在理解整个纹理中数据传递的路线。
把握好整体流程之后，这部分的处理也会变得简单起来。后面就可以如何添加更加炫酷的滤镜和玩法了。