package com.cry.zero_camera.render.fliter;

import android.opengl.Matrix;

import com.cry.zero_common.opengl.MatrixUtils;

public class Show2DFilter extends I2DFilter {
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
                "void main() {\n" +
                "    gl_FragColor = texture2D(uTexture,vTextureCoordinate);\n" +
                "}";
    }


    @Override
    protected void onExtraCreated(int mProgram) {

    }

    @Override
    public void onSizeChange(int width, int height) {
        this.width = width;
        this.height = height;
        Matrix.setIdentityM(mMVPMatrix, 0);
        MatrixUtils.flip(mMVPMatrix, false, true);
    }
}
