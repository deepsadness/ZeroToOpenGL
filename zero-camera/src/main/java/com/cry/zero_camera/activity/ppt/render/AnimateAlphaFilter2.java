package com.cry.zero_camera.activity.ppt.render;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.cry.zero_camera.render.fliter.PhotoAlphaFilter;

import java.util.Arrays;


public class AnimateAlphaFilter2 extends PhotoAlphaFilter {
    private float[] srcMatrix;
    private int animateType = 0;
    private float scale = 1;

    public AnimateAlphaFilter2() {
        setAlpha(1f);
    }

    @Override
    public void onClear() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void setMVPMatrix(float[] mMVPMatrix) {
        super.setMVPMatrix(mMVPMatrix);
        this.srcMatrix = mMVPMatrix;
    }

    public float[] getSrcMatrix() {
        return srcMatrix;
    }

    public void setAnimateMatrix(float[] mMVPMatrix) {
        this.mMVPMatrix = mMVPMatrix;
    }

    public int getAnimateType() {
        return animateType;
    }

    public void setAnimateType(int animateType) {
        this.animateType = animateType;
    }

    public void doFrame(float difSec, long duration) {
        if (duration == 0) {
            return;
        }
        float[] mModelMatrix = Arrays.copyOf(srcMatrix, 16);
        float targetDiff = 0.3f;
        //缩小
        float smaller = 1f - difSec * 1f / duration * targetDiff;
        Matrix.scaleM(mModelMatrix, 0, smaller, smaller, 0f);
        //移动
//        float moveScale = 1f - difSec * 1f / duration * targetDiff;
//        float translateX = width * moveScale;
//        Matrix.translateM(mModelMatrix, 0, translateX, 0f, 0f);

        setAlpha(smaller - 0.5f);

        setAnimateMatrix(mModelMatrix);
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

}
