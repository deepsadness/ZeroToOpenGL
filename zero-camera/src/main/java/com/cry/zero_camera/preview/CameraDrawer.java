package com.cry.zero_camera.preview;

import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.cry.zero_camera.render.OesFilter;
import com.cry.zero_common.opengl.Gl2Utils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 0->接受来自相机的纹理
 * 1->给Camera提供SurfaceView
 */
public class CameraDrawer implements GLSurfaceView.Renderer {
//    private final AFilter mOesFilter;
    private final OesFilter mOesFilter;
    //相机的id
    private int mCameraId = 0;
    //相机输出的surfaceView
    private SurfaceTexture mSurfaceTexture;
    //绘制的纹理ID
    private int mTextureId;

    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private int mPreviewWidth;
    private int mPreviewHeight;

    //视图矩阵。控制旋转和变化
    private float[] mModelMatrix = new float[16];

    public CameraDrawer(Resources res) {
        mOesFilter = new OesFilter(res);
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //生成纹理
        mTextureId = genOesTextureId();
        //创建内部的surfaceView
        mSurfaceTexture = new SurfaceTexture(mTextureId);

        //创建滤镜.同时绑定滤镜上
        mOesFilter.create();
        mOesFilter.setTextureId(mTextureId);
    }

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

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //在这里监听到尺寸的改变。做出对应的变化
        this.mSurfaceWidth = width;
        this.mSurfaceHeight = height;
        calculateMatrix();
    }

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

    @Override
    public void onDrawFrame(GL10 gl) {
        //每次绘制后，都通知texture刷新
        if (mSurfaceTexture != null) {
            //It will implicitly bind its texture to the GL_TEXTURE_EXTERNAL_OES texture target.
            mSurfaceTexture.updateTexImage();
        }
        mOesFilter.draw();
    }

    public void setCameraId(int cameraId) {
        this.mCameraId = cameraId;
    }

    public void setPreviewSize(int previewWidth, int previewHeight) {
        this.mPreviewWidth = previewWidth;
        this.mPreviewHeight = previewHeight;
        calculateMatrix();
    }
}
