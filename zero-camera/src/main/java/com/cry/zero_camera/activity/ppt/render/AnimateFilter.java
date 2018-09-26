package com.cry.zero_camera.activity.ppt.render;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.cry.zero_camera.render.fliter.PhotoFilter;

import java.util.Arrays;


public class AnimateFilter extends PhotoFilter {
    private float[] srcMatrix;
    public static long oneSecond = 1000000000;
    private float scale = 1;
    long startTime = 0;

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

    long mAnimateDuration = 3 * oneSecond;
    private int animateType = 0;
    private float offset;

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void doFrame(float difSec, long duration) {
        if (duration == 0) {
            return;
        }
        if (difSec > duration) {
            difSec = duration;
        }


        float[] mModelMatrix = Arrays.copyOf(srcMatrix, 16);
        float targetDiff = 0.2f;
        if (animateType == 0) {
            float v = difSec * 1f / duration * targetDiff + 1f;
//            System.out.println("scale=" + v);
            Matrix.scaleM(mModelMatrix, 0, v, v, 0f);
        } else if (animateType == 1) {
            float v = difSec * 1f / duration * targetDiff * 360;
//            System.out.println("rotate=" + v);
            Matrix.rotateM(mModelMatrix, 0, v, 0f, 0f, 1f);
        } else if (animateType == 2) {
            float v = 2 - difSec * 1f / duration * 1f * 2;
            offset = width * (v / 2);
            System.out.println("width=" + width + ",offset =" + offset + ",v=" + v / 2);
//            System.out.println("transform=" + v);
            Matrix.translateM(mModelMatrix, 0, v, 0f, 0f);

        } else if (animateType == 3) {
            float v = difSec * 1f / duration * 1f * 2;
//            System.out.println("transform=" + v);
            Matrix.translateM(mModelMatrix, 0, 0, v, 0f);
        } else if (animateType == 4) {
            float smaller = 1f - difSec * 1f / duration * targetDiff;
            if (smaller - 0.9 > 0) {
                smaller = 1;
            } else {
                smaller += 0.1;
            }
            System.out.println("animateType smaller=" + smaller);
            Matrix.scaleM(mModelMatrix, 0, smaller, smaller, 0f);
        }


        setAnimateMatrix(mModelMatrix);
    }

    @Override
    protected void beforeDraw() {
        super.beforeDraw();
        if (animateType == 2) {
            GLES20.glViewport((int) offset, 0, width, height);
        }

    }

    @Override
    protected void afterDraw() {
        super.afterDraw();
        if (animateType == 2) {
            GLES20.glViewport(0, 0, width, height);
        }
    }

    @Override
    protected void onClear() {
//        if (animateType == 4) {
        super.onClear();
//        }
    }

    @Override
    public void setBitmap(Bitmap bitmap) {
        super.setBitmap(bitmap);
        if (animateType == 2) {
            float v = 2;
            float[] mModelMatrix = Arrays.copyOf(srcMatrix, 16);
            offset = width * (v / 2);
            System.out.println("width=" + width + ",offset =" + offset + ",v=" + v / 2);
//            System.out.println("transform=" + v);
            Matrix.translateM(mModelMatrix, 0, v, 0f, 0f);
        }

    }

    public void update(long timestampNanos) {
        if (startTime == 0) {
            startTime = timestampNanos;
        } else {
            long diffTime = timestampNanos - startTime;
            doFrame(diffTime / oneSecond, mAnimateDuration / oneSecond);
        }
    }


}
