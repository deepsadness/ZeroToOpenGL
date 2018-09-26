package com.cry.zero_camera.activity.ppt.render;

import android.opengl.Matrix;

import com.cry.zero_camera.render.fliter.PhotoAlphaFilter;

import java.util.Arrays;


public class AnimateAlphaFilter extends PhotoAlphaFilter {
    private float[] srcMatrix;
    private int animateType = 0;
    private float scale = 1;

    public AnimateAlphaFilter() {
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
        if (animateType == 0) {
            float v = difSec * 1f / duration * targetDiff + 1f;
//            System.out.println("scale=" + v);
            Matrix.scaleM(mModelMatrix, 0, v, v, 0f);
        } else if (animateType == 1) {
            float v = difSec * 1f / duration * targetDiff * 360;
//            System.out.println("rotate=" + v);
            Matrix.rotateM(mModelMatrix, 0, v, 0f, 0f, 1f);
        } else if (animateType == 2) {
            float v = difSec * 1f / duration * 1f * 2;

            float offset = width * v;
//            System.out.println("transform=" + v);
            Matrix.translateM(mModelMatrix, 0, v, 0f, 0f);

        } else if (animateType == 3) {
            float v = difSec * 1f / duration * 1f * 2;
//            System.out.println("transform=" + v);
            Matrix.translateM(mModelMatrix, 0, 0, v, 0f);
        } else if (animateType == 4) {
            //缩小
            float smaller = 1f - difSec * 1f / duration * targetDiff;
            Matrix.scaleM(mModelMatrix, 0, smaller, smaller, 0f);
            //移动
//        float moveScale = 1f - difSec * 1f / duration * targetDiff;
//        float translateX = width * moveScale;
//        Matrix.translateM(mModelMatrix, 0, translateX, 0f, 0f);

//            setAlpha(smaller-0.5f);
        }


        setAnimateMatrix(mModelMatrix);
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

}
