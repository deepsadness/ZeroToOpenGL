package com.cry.zero_camera.activity.ppt;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 创建一个OpenGL环境。在这里创建Camera 和 Camera输出的SurfaceView
 */
public class PhotoAnimateRenderView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private static final String TAG = "CameraView";
    private PhotoAnimateRender mPhotoView;
    private int width;
    private int height;
    private Bitmap bitmap;

    public PhotoAnimateRenderView(Context context) {
        super(context);
        initEGL();
        initCameraApi(context);
    }

    private void initEGL() {
        //open gl step 1
        setEGLContextClientVersion(2);
        setRenderer(this);
        //只有刷新之后，才会去重绘
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    private void initCameraApi(Context context) {
        mPhotoView = new PhotoAnimateRender(context.getResources());
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mPhotoView.onSurfaceCreated(gl, config);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mPhotoView.onSurfaceChanged(gl, width, height);
        //设置ViewPort是必须要做的
        GLES20.glViewport(0, 0, width, height);
        this.width = width;
        this.height = height;
        mPhotoView.setPreviewSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mPhotoView.onDrawFrame(gl);
    }

    public void changeRecordingState(boolean mRecordingEnabled) {
        queueEvent(() -> {
            // notify the renderer that we want to change the encoder's state
            mPhotoView.changeRecordingState(mRecordingEnabled);
        });
    }

    public void setBitmap(Bitmap bmp) {
        int height = bmp.getHeight();
        int width = bmp.getWidth();
        float[] bitmapMatrix = new float[16];
        Matrix.setIdentityM(bitmapMatrix, 0);

        int scaleX = this.width / width;
        int scaleY = this.height / height;
        Matrix.scaleM(bitmapMatrix, 0, scaleX, scaleY, 0);

        mPhotoView.setBitmap(bmp, bitmapMatrix);
    }
}
