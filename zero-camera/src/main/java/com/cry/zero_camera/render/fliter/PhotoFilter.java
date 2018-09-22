package com.cry.zero_camera.render.fliter;

import android.graphics.Bitmap;

import com.cry.zero_common.opengl.GLESUtils;
import com.cry.zero_common.opengl.MatrixUtils;

public class PhotoFilter extends I2DFilter {
    private Bitmap mBitmap;

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
    public void onExtraCreated(int mProgram) {

    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
        setTextureId(GLESUtils.createTexture(bitmap));
        setMVPMatrix(MatrixUtils.calculateMatrixForBitmap(bitmap, width, height));
    }
}
