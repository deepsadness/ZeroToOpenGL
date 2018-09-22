package com.cry.zero_camera.activity.ppt;


import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.cry.zero_common.opengl.Gl2Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SimplePhotoRender2 implements GLSurfaceView.Renderer {
    private static final String A_POSITION = "aPosition";
    private static final String A_COORDINATE = "aCoordinate";
    private static final String U_TEXTURE = "uTexture";
    private static final String U_MATRIX = "uMatrix";
    //顶点坐标
    private final float[] sPos = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, 1.0f,
            1.0f, -1.0f
    };
    private final float[] sCoord = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };
    //pos vertex
    private FloatBuffer mVerBuffer;
    //这里用两个FloatBuffer来表示，也可以将两个FloatBuffer合并到一起
    private FloatBuffer mTextureCoordinate;
    //矩阵
    private float[] mViewMatrix = new float[16];
    private float[] mProjectMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    private Bitmap mBitmap;
    //纹理
    private int textureId = -1;
    private int mProgramObjectId;
    private int mAPosition;
    private int mACoord;
    private int mUMatrix;
    private int mUTexture;
    private float width;
    private float height;

    public SimplePhotoRender2() {
        //每个float占用4个字节
        mVerBuffer = ByteBuffer
                .allocateDirect(sPos.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(sPos);
        mVerBuffer.position(0);


        mTextureCoordinate = ByteBuffer
                .allocateDirect(sCoord.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(sCoord);
        mTextureCoordinate.position(0);
    }

    //着色器代码
    private String obtainVertex() {
        return "attribute vec4 aPosition;\n" +
                "attribute vec2 aCoordinate;\n" +
                "uniform mat4 uMatrix;\n" +
                "varying vec2 vCoordinate;\n" +
                "void main(){\n" +
                "    gl_Position=uMatrix*aPosition;\n" +
                "    vCoordinate=aCoordinate;\n" +
                "}";
    }

    private String obtainFragment() {
        return "precision mediump float;\n" +
                "uniform sampler2D uTexture;\n" +
                "varying vec2 vCoordinate;\n" +
                "void main(){\n" +
                "   gl_FragColor=texture2D(uTexture,vCoordinate);\n" +
                "}";
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        mProgramObjectId = Gl2Utils.createGlProgram(obtainVertex(), obtainFragment());
        mAPosition = GLES20.glGetAttribLocation(mProgramObjectId, A_POSITION);
        mACoord = GLES20.glGetAttribLocation(mProgramObjectId, A_COORDINATE);
        //两个uniform texture 和 matrix
        mUMatrix = GLES20.glGetUniformLocation(mProgramObjectId, U_MATRIX);
        mUTexture = GLES20.glGetUniformLocation(mProgramObjectId, U_TEXTURE);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        this.width = width;
        this.height = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if (textureId == -1) {
            return;
        }
        //传递给着色器
        GLES20.glUniformMatrix4fv(mUMatrix, 1, false, mMVPMatrix, 0);

        //绑定和激活纹理
        //因为我们生成了MIP，放到了GL_TEXTURE0 中，所以重新激活纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //重新去半丁纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        //设置纹理的坐标
        GLES20.glUniform1i(mUTexture, 0);


        GLES20.glEnableVertexAttribArray(mAPosition);
        GLES20.glVertexAttribPointer(
                mAPosition,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                mVerBuffer);
        //
        GLES20.glEnableVertexAttribArray(mACoord);
        GLES20.glVertexAttribPointer(
                mACoord,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                mTextureCoordinate);

        //绘制三角形.
        //draw arrays的几种方式 GL_TRIANGLES三角形 GL_TRIANGLE_STRIP三角形带的方式(开始的3个点描述一个三角形，后面每多一个点，多一个三角形) GL_TRIANGLE_FAN扇形(可以描述圆形)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(mAPosition);
        GLES20.glDisableVertexAttribArray(mACoord);
    }

    private void calculateMatrix() {
        int w = mBitmap.getWidth();
        int h = mBitmap.getHeight();
        float sWH = w / (float) h;
        float sWidthHeight = this.width / (float) this.height;
//        uXY=sWidthHeight;
        if (this.width > this.height) {
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

    private int createTexture() {
        int[] texture = new int[1];
        if (mBitmap != null && !mBitmap.isRecycled()) {
            //生成纹理
            GLES20.glGenTextures(1, texture, 0);
            //生成纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            //根据以上指定的参数，生成一个2D纹理
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
            mBitmap.recycle();
            //因为我们已经复制成功了。所以就进行解除绑定。防止修改
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            return texture[0];
        }
        return 0;
    }

    public void setBitmap(Bitmap mBitmap) {
        this.mBitmap = mBitmap;
        textureId = createTexture();
        Matrix.setIdentityM(mMVPMatrix, 0);
//        calculateMatrix();
    }
}
