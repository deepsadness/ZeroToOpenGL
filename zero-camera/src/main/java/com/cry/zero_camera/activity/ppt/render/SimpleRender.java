package com.cry.zero_camera.activity.ppt.render;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.cry.zero_camera.render.fliter.PhotoFilter;
import com.cry.zero_camera.render.fliter.Show2DFilter;
import com.cry.zero_common.opengl.GLESUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SimpleRender implements GLSurfaceView.Renderer {
    //bitmap buffer
    private Bitmap mBitmap;

    //存放fbo的id
    private int mFrameBuffer = 0;
    private int mRenderBuffer = 0;
    private int mOffscreenTextureId = 0;

    private PhotoFilter mPhotoFilter;
    private Show2DFilter mShow2DFilter;

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    //render call back
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mPhotoFilter = new PhotoFilter();
        mShow2DFilter = new Show2DFilter();
        //创建program
        mPhotoFilter.onCreate();
        mShow2DFilter.onCreate();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
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

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);
            mPhotoFilter.setBitmap(mBitmap);
            mPhotoFilter.onDrawFrame();
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

            mShow2DFilter.setTextureId(mOffscreenTextureId);
            mShow2DFilter.onDrawFrame();

        }
    }

}
