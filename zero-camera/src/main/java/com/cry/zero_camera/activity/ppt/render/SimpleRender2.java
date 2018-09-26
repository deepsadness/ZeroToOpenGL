package com.cry.zero_camera.activity.ppt.render;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.cry.zero_camera.activity.ppt.RecordStateManager;
import com.cry.zero_camera.render.fliter.Show2DFilter;
import com.cry.zero_common.opengl.GLESUtils;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SimpleRender2 implements GLSurfaceView.Renderer {
    private static final String TAG = "SimpleRender";
    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    RecordStateManager mRecordStateManager;
    //bitmap buffer
    private Bitmap mBitmap;
    //存放fbo的id
    private int mFrameBuffer = 0;
    private int mRenderBuffer = 0;
    private int mOffscreenTextureId = 0;
    private Show2DFilter mShow2DFilter;
    private AnimateFilter mPhotoFilter;
    private long frameNanos;

    public SimpleRender2() {
        mPhotoFilter = new AnimateFilter();
        mShow2DFilter = new Show2DFilter();
        mRecordStateManager = new RecordStateManager();
    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    //render call back
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mRecordStateManager.onCreate();

        //创建program
        mPhotoFilter.onCreate();
        mShow2DFilter.onCreate();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mRecordStateManager.onSizeChange(width, height);
//        this.width = width;
//        this.height = height;
        //将纹理0传入？
        mPhotoFilter.onSizeChange(width, height);
        mShow2DFilter.onSizeChange(width, height);


        int[] offscreenIds = GLESUtils.createOffscreenIds(width, height);

        mOffscreenTextureId = offscreenIds[0];
        mFrameBuffer = offscreenIds[1];
        mRenderBuffer = offscreenIds[2];
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mRecordStateManager.onConsiderState();
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);

//            if (mPhotoFilter.getBitmap() == null || mPhotoFilter.getBitmap() != mBitmap) {
            mPhotoFilter.setBitmap(mBitmap);
//            }

            mPhotoFilter.onDrawFrame();
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

            mRecordStateManager.onDrawFrame(null, frameNanos);

            mShow2DFilter.setTextureId(mOffscreenTextureId);
            mShow2DFilter.onDrawFrame();

        }
    }

    public void changeRecordingState(boolean isRecording) {
        mRecordStateManager.changeRecordingState(isRecording);
    }

    public void doFrame(long frame, float difSec, long duration) {
        frameNanos = frame;
        if (duration == 0) {
            return;
        }
//        mPhotoFilter.doFrame(difSec, duration);
    }

    public int getAnimateType() {
        return mPhotoFilter.getAnimateType();
    }

    public void setAnimateType(int resultType) {
        mPhotoFilter.setAnimateType(resultType);
    }

    public File getOutputFile() {
        return mRecordStateManager.getOutputFile();
    }
}
