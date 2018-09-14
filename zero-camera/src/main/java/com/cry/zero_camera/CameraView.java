package com.cry.zero_camera;

import android.content.Context;
import android.opengl.GLSurfaceView;

import com.cry.zero_camera.camera.CameraApi14;
import com.cry.zero_camera.camera.ICamera;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 创建一个OpenGL环境。在这里创建Camera 和 Camera输出的SurfaceView
 */
public class CameraView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private ICamera mCameraApi;
    private int mCameraIdDefault = 0;

    public CameraView(Context context) {
        super(context);
        initCameraApi(context);
    }

    private void initCameraApi(Context context) {
        mCameraApi = new CameraApi14();
    }

    public void onPause() {
        super.onPause();
        mCameraApi.close();
    }

    public void onResume() {

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initEGL();
        //打开相机
        mCameraApi.open(mCameraIdDefault);
        mCameraApi.preview();
    }

    private void initEGL() {
        //open gl step 1
        setEGLContextClientVersion(2);
        setRenderer(this);
        //只有刷新之后，才会去重绘
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    @Override
    public void onDrawFrame(GL10 gl) {

    }
}
