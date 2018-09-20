package com.cry.zero_camera.activity.video;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.ViewGroup;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 创建一个OpenGL环境。在这里创建Camera 和 Camera输出的SurfaceView
 */
public class VideoCaptureFilterView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private static final String TAG = "CameraView";
    private VideoFilterRender mVideoRender;
    private int width;
    private int height;
    private DecodeThread mDecodeThread;

    public VideoCaptureFilterView(Context context) {
        super(context);
        initEGL();
        initVideoDecode(context);
    }

    private void initEGL() {
        //open gl step 1
        setEGLContextClientVersion(2);
        setRenderer(this);
        //只有刷新之后，才会去重绘
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    private void initVideoDecode(Context context) {
        mDecodeThread = new DecodeThread(this);
        mVideoRender = new VideoFilterRender(context.getResources());
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mVideoRender.onSurfaceCreated(gl, config);
        mDecodeThread.sendSurface(mVideoRender.getSurfaceTexture());
        //默认使用的GLThread.每次刷新的时候，都强制要求是刷新这个GLSurfaceView
        mVideoRender.getSurfaceTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                requestRender();
            }
        });
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mVideoRender.onSurfaceChanged(gl, width, height);
        //设置ViewPort是必须要做的
        GLES20.glViewport(0, 0, width, height);
        this.width = width;
        this.height = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mVideoRender.onDrawFrame(gl);
    }

    @Override
    public void onPause() {
        super.onPause();
        mDecodeThread.sendOnPause();

        queueEvent(new Runnable() {
            @Override
            public void run() {
                // Tell the renderer that it's about to be paused so it can clean up.
                mVideoRender.notifyPausing();
            }
        });
        Log.d(TAG, "onPause complete");
    }


    public void changeRecordingState(boolean mRecordingEnabled) {
        queueEvent(() -> {
            // notify the renderer that we want to change the encoder's state
            mVideoRender.changeRecordingState(mRecordingEnabled);
        });
    }

    public void adjustAspectRatio(int videoWidth, int videoHeight) {
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        Log.v(TAG, "video=" + videoWidth + "x" + videoHeight +
                " view=" + viewWidth + "x" + viewHeight +
                " newView=" + newWidth + "x" + newHeight +
                " off=" + xoff + "," + yoff);

        post(() -> {
            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            layoutParams.height = newHeight;
            layoutParams.width = newWidth;
            setLayoutParams(layoutParams);
        });

    }

    public void playbackStopped() {
        queueEvent(() -> {
            // notify the renderer that we want to change the encoder's state
            mVideoRender.changeRecordingState(false);
            requestRender();
        });
    }

    public void start(File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            return;
        }
        mDecodeThread.sendSrcFile(file);
        mDecodeThread.sendStart();
        //设置开始
        changeRecordingState(true);

    }

    public void stop() {
        mDecodeThread.sendStop();
        changeRecordingState(false);
    }
}
