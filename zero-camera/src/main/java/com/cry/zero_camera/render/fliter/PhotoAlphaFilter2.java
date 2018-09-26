package com.cry.zero_camera.render.fliter;

import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.cry.zero_common.opengl.GLESUtils;
import com.cry.zero_common.opengl.MatrixUtils;

public class PhotoAlphaFilter2 extends I2DFilter {
    private Bitmap mBitmap;
    private int uAlphaLocation;
    private float mAlpha = 0.2f;
    ;

    public PhotoAlphaFilter2() {
    }

    @Override
    public String obtainVertex() {
        return "attribute vec4 aPosition;\n" +
                "attribute vec2 aCoordinate;\n" +
                "uniform mat4 uMatrix;\n" +
                "varying vec2 vTextureCoordinate;\n" +
                "\n" +
                "void main(){\n" +
                "    gl_Position = uMatrix*aPosition;\n" +
                "    vTextureCoordinate = aCoordinate;\n" +
                "}";
    }

    @Override
    public String obtainFragment() {
        return "precision mediump float;\n" +
                "\n" +
                "varying vec2 vTextureCoordinate;\n" +
                "uniform sampler2D uTexture;\n" +
                "uniform float uAlphaLocation;\n" +
                "void main() {\n" +
                "    gl_FragColor = vec4(texture2D(uTexture,vTextureCoordinate).rgb,uAlphaLocation);\n" +
                "}";
    }

    @Override
    public void onClear() {

    }

    @Override
    public void onExtraCreated(int mProgram) {
        uAlphaLocation = GLES20.glGetUniformLocation(mProgram, "uAlphaLocation");
    }

    @Override
    protected void onExtraData() {
        super.onExtraData();

        GLES20.glUniform1f(uAlphaLocation, mAlpha);
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
//        if (this.mBitmap == bitmap) {
//            return;
//        }
        this.mBitmap = bitmap;
//        deleteTextureId();
        setTextureId(GLESUtils.createTexture(bitmap));
        setMVPMatrix(MatrixUtils.calculateMatrixForBitmap(bitmap, width, height));
    }

    private void deleteTextureId() {
        int preTextureId = getTextureId();
        if (preTextureId != 0) {
            int[] values = new int[1];
            values[0] = preTextureId;
            GLES20.glDeleteTextures(1, values, 0);
        }
    }

    public void setAlpha(float mAlpha) {
        this.mAlpha = mAlpha;
    }

    @Override
    public void release() {
        releaseBitmap();
        super.release();
    }

    private void releaseBitmap() {
        if (this.mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }
}
