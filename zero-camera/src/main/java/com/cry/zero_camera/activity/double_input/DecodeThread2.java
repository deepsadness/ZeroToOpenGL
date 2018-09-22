package com.cry.zero_camera.activity.double_input;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;

public class DecodeThread2 extends HandlerThread implements VideoDecoderCore.PlayerFeedback {
    private static final String TAG = "DecodeThread";
    private static final int MSG_SET_SURFACE = 0;
    private static final int MSG_SRC_FILE = 1;
    private static final int MSG_DECODE_START = 2;
    private static final int MSG_DECODE_STOP = 3;
    private static final int MSG_ON_PAUSE = 4;

    private final DecodeHandler mDecodeHandler;
    private VideoDecoderCore mVideoDecoder;
    private WeakReference<DoubleInputView> viewRef;
    private VideoDecoderCore.PlayTask mPlayTask;

    public DecodeThread2(DoubleInputView captureFilterView) {
        super("DecodeThread");
        start();
        mVideoDecoder = VideoDecoderCore.EMPTY();
        this.viewRef = new WeakReference<>(captureFilterView);
        mDecodeHandler = new DecodeHandler(getLooper(), new WeakReference<DecodeThread2>(this));
    }

    private void startDecode() {
        if (mPlayTask != null) {
            Log.w(TAG, "movie already playing");
            return;
        }
        Log.d(TAG, "starting movie");
        SpeedControlCallback callback = new SpeedControlCallback();

//        if (((CheckBox) findViewById(R.id.locked60fps_checkbox)).isChecked()) {
        // TODO: consider changing this to be "free running" mode
//            callback.setFixedPlaybackRate(60);
//        }
        mVideoDecoder.setFrameCallback(callback);
        mPlayTask = new VideoDecoderCore.PlayTask(mVideoDecoder, this);
//        mPlayTask.setLoopMode(true);
        mPlayTask.execute();
    }

    private void onPause() {
        if (mPlayTask != null) {
            requestStop();
            mPlayTask.waitForStop();
            mPlayTask = null;
        }
    }

    private void requestStop() {
        if (mPlayTask != null) {
            mPlayTask.requestStop();
        }
    }

    private void setSourceFile(File src) {
        mVideoDecoder.setSourceFile(src);
        //设置好之后，要回调设置长宽
        DoubleInputView videoCaptureFilterView = viewRef.get();
        if (videoCaptureFilterView != null) {
            videoCaptureFilterView.adjustAspectRatio(mVideoDecoder.getVideoWidth(), mVideoDecoder.getVideoHeight());
        }
        //提前读取一帧？

    }

    @Override
    public void playbackStopped() {
        mPlayTask = null;
        DoubleInputView videoCaptureFilterView = viewRef.get();
        if (videoCaptureFilterView != null) {
            videoCaptureFilterView.playbackStopped();
        }
    }


    public void sendSurface(SurfaceTexture obj) {
        Message.obtain(mDecodeHandler, MSG_SET_SURFACE, obj).sendToTarget();
    }

    public void sendSrcFile(File obj) {
        Message.obtain(mDecodeHandler, MSG_SRC_FILE, obj).sendToTarget();
    }

    public void sendStart() {
        Message.obtain(mDecodeHandler, MSG_DECODE_START).sendToTarget();
    }

    public void sendStop() {
        Message.obtain(mDecodeHandler, MSG_DECODE_STOP).sendToTarget();
    }

    public void sendOnPause() {
        Message.obtain(mDecodeHandler, MSG_ON_PAUSE).sendToTarget();
    }


    private final static class DecodeHandler extends Handler {
        private final WeakReference<DecodeThread2> ref;


        public DecodeHandler(Looper looper, WeakReference<DecodeThread2> decodeThread) {
            super(looper);
            this.ref = decodeThread;
        }

        @Override
        public void handleMessage(Message msg) {
            DecodeThread2 decodeThread = ref.get();
            if (decodeThread == null) {
                return;
            }
            switch (msg.what) {
                case MSG_SET_SURFACE:
                    decodeThread.mVideoDecoder.setSurface((SurfaceTexture) msg.obj);
                    break;
                case MSG_SRC_FILE:
                    decodeThread.setSourceFile((File) msg.obj);
                    break;
                case MSG_DECODE_START:
                    decodeThread.startDecode();
                    break;
                case MSG_DECODE_STOP:
                    decodeThread.requestStop();
                    break;
                case MSG_ON_PAUSE:
                    decodeThread.onPause();
                    break;
            }
        }
    }

}
