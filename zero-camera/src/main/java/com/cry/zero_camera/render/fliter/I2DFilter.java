package com.cry.zero_camera.render.fliter;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.cry.zero_common.opengl.GLESUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public abstract class I2DFilter {
    protected float[] mMVPMatrix = new float[16];
    protected int width;
    protected int height;
    //顶点坐标
    private float sPos[] = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, 1.0f,
            1.0f, -1.0f,
    };
    //纹理坐标
    private float[] sCoord = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };
    private FloatBuffer mVerBuffer;
    private FloatBuffer mTexBuffer;
    private int mProgram;
    private int glPosition;
    private int glTexture;
    private int glCoordinate;
    private int glMatrix;
    private float[] mViewMatrix = new float[16];
    private float[] mProjectMatrix = new float[16];
    private int mTextureId;


    public I2DFilter() {
        initBuffer();
    }

    private void initBuffer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(sPos.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mVerBuffer = bb.asFloatBuffer();
        mVerBuffer.put(sPos);
        mVerBuffer.position(0);
        ByteBuffer cc = ByteBuffer.allocateDirect(sCoord.length * 4);
        cc.order(ByteOrder.nativeOrder());
        mTexBuffer = cc.asFloatBuffer();
        mTexBuffer.put(sCoord);
        mTexBuffer.position(0);
    }

    public void onCreate() {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        mProgram = GLESUtils.createProgram(obtainVertex(), obtainFragment());
        glPosition = GLES20.glGetAttribLocation(mProgram, "aPosition");
        glCoordinate = GLES20.glGetAttribLocation(mProgram, "aCoordinate");
        glMatrix = GLES20.glGetUniformLocation(mProgram, "uMatrix");
        glTexture = GLES20.glGetUniformLocation(mProgram, "uTexture");
        onExtraCreated(mProgram);
    }

    protected abstract String obtainVertex();

    protected abstract String obtainFragment();

    protected abstract void onExtraCreated(int mProgram);

    public void onSizeChange(int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        this.width = width;
        this.height = height;
        float sWidthHeight = width / (float) height;
        float sWH = sWidthHeight;
        if (width > height) {
            if (sWH > sWidthHeight) {
                Matrix.orthoM(mProjectMatrix, 0, -sWidthHeight * sWH, sWidthHeight * sWH, -1, 1, 3, 5);
            } else {
                Matrix.orthoM(mProjectMatrix, 0, -sWidthHeight / sWH, sWidthHeight / sWH, -1, 1, 3, 5);
            }
        } else {
            if (sWH > sWidthHeight) {
                Matrix.orthoM(mProjectMatrix, 0, -1, 1, -1 / sWidthHeight * sWH, 1 / sWidthHeight * sWH, 3, 5);
            } else {
                Matrix.orthoM(mProjectMatrix, 0, -1, 1, -sWH / sWidthHeight, sWH / sWidthHeight, 3, 5);
            }
        }
        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 5.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        //计算变换矩阵
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0);
    }

    public void onDrawFrame() {
        beforeDraw();
        onClear();
        onUseProgram();
        onExtraData();
        onBindTexture();
        onDraw();
        afterDraw();
    }


    protected void beforeDraw() {

    }

    protected void afterDraw() {

    }

    protected void onClear() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    protected void onUseProgram() {
        GLES20.glUseProgram(mProgram);
    }

    protected void onExtraData() {
        GLES20.glUniformMatrix4fv(glMatrix, 1, false, mMVPMatrix, 0);
    }

    protected void onBindTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, getTextureId());
        GLES20.glUniform1i(glTexture, 0);
    }

    protected void onDraw() {
        GLES20.glEnableVertexAttribArray(glPosition);
        GLES20.glVertexAttribPointer(glPosition, 2, GLES20.GL_FLOAT, false, 0, mVerBuffer);
        GLES20.glEnableVertexAttribArray(glCoordinate);
        GLES20.glVertexAttribPointer(glCoordinate, 2, GLES20.GL_FLOAT, false, 0, mTexBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(glPosition);
        GLES20.glDisableVertexAttribArray(glCoordinate);
    }


    public void release() {
        if (mTextureId != 0) {
            int[] values = new int[1];
            values[0] = mTextureId;
            GLES20.glDeleteTextures(1, values, 0);
            mTextureId = 0;
        }
        if (mProgram != 0) {
            GLES20.glDeleteProgram(mProgram);
            mProgram = 0;
        }
    }

    public int getTextureId() {
        return mTextureId;
    }

    public void setTextureId(int mTextureId) {
        this.mTextureId = mTextureId;
    }

    public void setMVPMatrix(float[] mMVPMatrix) {
        this.mMVPMatrix = mMVPMatrix;
    }

    public float[] getMVPMatrix() {
        return mMVPMatrix;
    }
}
