package com.cry.zero_camera.activity.ppt.render;

import android.opengl.Matrix;

import com.cry.zero_camera.render.fliter.PhotoFilter;

import java.util.Arrays;


public class AnimateFilter extends PhotoFilter {
    private float[] srcMatrix;
    private int animateType;
    private float scale = 1;

    public AnimateFilter() {
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
        if (animateType == 0) {
            float v = difSec * 1f / duration * 0.5f + 1f;
//            System.out.println("scale=" + v);
            Matrix.scaleM(mModelMatrix, 0, v, v, 0f);
        } else if (animateType == 1) {
            float v = difSec * 1f / duration * 0.5f * 360;
//            System.out.println("rotate=" + v);
            Matrix.rotateM(mModelMatrix, 0, v, 0f, 0f, 1f);
        } else if (animateType == 2) {
            float v = difSec * 1f / duration * 1f * 2;
//            System.out.println("transform=" + v);
            Matrix.translateM(mModelMatrix, 0, v, 0f, 0f);
        } else if (animateType == 3) {
            float v = difSec * 1f / duration * 1f * 2;
//            System.out.println("transform=" + v);

            Matrix.translateM(mModelMatrix, 0, 0, v, 0f);
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
