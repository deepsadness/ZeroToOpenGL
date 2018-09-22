package com.cry.zero_camera.render;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.cry.zero_common.opengl.GLESUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by slack
 * on 18/1/14 下午6:12
 */

public class ColorFiler {

    private int vChangeType;
    private int vChangeColor;
    private Filter mFilter;
    private int mFrameBuffer;
    private int mRenderBuffer;
    private boolean isToFbo;

    public void setToFbo(boolean toFbo) {
        isToFbo = toFbo;
    }

    public ColorFiler(Filter mFilter) {
        this.mFilter = mFilter;
        initBuffer();
    }

    String obtainVertex() {
        return "attribute vec4 vPosition;\n" +
                "attribute vec2 vCoordinate;\n" +
                "uniform mat4 vMatrix;\n" +
                "varying vec2 aCoordinate;\n" +
                "void main(){\n" +
                "    gl_Position=vMatrix*vPosition;\n" +
                "    aCoordinate=vCoordinate;\n" +
                "}";
    }

    String obtainFragment() {
        return "precision mediump float;\n" +
                "uniform sampler2D vTexture;\n" +
                "uniform int vChangeType;\n" +
                "uniform vec3 vChangeColor;\n" +
                "varying vec2 aCoordinate;\n" +
                "void modifyColor(vec4 color){\n" +
                "    color.r=max(min(color.r,1.0),0.0);\n" +
                "    color.g=max(min(color.g,1.0),0.0);\n" +
                "    color.b=max(min(color.b,1.0),0.0);\n" +
                "    color.a=max(min(color.a,1.0),0.0);\n" +
                "}\n" +
                "void main(){\n" +
                "    vec4 nColor=texture2D(vTexture,aCoordinate);\n" +
                "   if(vChangeType==1){\n" +
                "        float c=nColor.r*vChangeColor.r+nColor.g*vChangeColor.g+nColor.b*vChangeColor.b;\n" +
                "        gl_FragColor=vec4(c,c,c,nColor.a);\n" +
                "    }else if(vChangeType==2){\n" +
                "        vec4 deltaColor=nColor+vec4(vChangeColor,0.0);\n" +
                "        modifyColor(deltaColor);\n" +
                "        gl_FragColor=deltaColor;\n" +
                "    }else{\n" +
                "        gl_FragColor=nColor;\n" +
                "    }\n" +
                "}";
    }

    void onExtraCreated(int mProgram) {
        vChangeType = GLES20.glGetUniformLocation(mProgram, "vChangeType");
        vChangeColor = GLES20.glGetUniformLocation(mProgram, "vChangeColor");
    }

    public void onExtraData() {
        GLES20.glUniformMatrix4fv(glMatrix, 1, false, mMVPMatrix, 0);
        GLES20.glUniform1i(vChangeType, mFilter.getType());
        GLES20.glUniform3fv(vChangeColor, 1, mFilter.data(), 0);
    }

    private FloatBuffer mVerBuffer;
    private FloatBuffer mTexBuffer;

    private int mProgram;

    private int glPosition;
    private int glTexture;
    private int glCoordinate;
    private int glMatrix;

    private float[] mViewMatrix = new float[16];
    private float[] mProjectMatrix = new float[16];
    protected float[] mMVPMatrix = new float[16];

    private int mTextureId;
    private int mOutputTextureId;

    public int getOutputTextureId() {
        return mOutputTextureId;
    }

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

    //生成frameBuffer的时机
    private void prepareFramebuffer(int width, int height) {
        int[] values = new int[1];
        GLES20.glGenTextures(1, values, 0);
        mOutputTextureId = values[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOutputTextureId);
//        GlUtil.checkGlError("glBindTexture " + mOutputTextureId);

        // Create texture storage.
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        // Set parameters.  We're probably using non-power-of-two dimensions, so
        // some values may not be available for use.
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
//        GlUtil.checkGlError("glTexParameter");

        // Create framebuffer object and bind it.
        GLES20.glGenFramebuffers(1, values, 0);
        mFrameBuffer = values[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);
//        GlUtil.checkGlError("glBindFramebuffer " + mFrameBuffer);

        // Create a depth buffer and bind it.
        GLES20.glGenRenderbuffers(1, values, 0);
        mRenderBuffer = values[0];
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mRenderBuffer);
//        GlUtil.checkGlError("glBindFramebuffer " + mRenderBuffer);

        // Allocate storage for the depth buffer.
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);
//        GlUtil.checkGlError("glRenderbufferStorage");

        // Attach the depth buffer and the texture (color buffer) to the framebuffer object.
        // 将renderBuffer挂载到frameBuffer的depth attachment 上
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mRenderBuffer);
//        GlUtil.checkGlError("glFramebufferRenderbuffer");
        // 将text2d挂载到frameBuffer的color attachment上
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mOutputTextureId, 0);
//        GlUtil.checkGlError("glFramebufferTexture2D");

        // See if GLES is happy with all this.
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer not complete, status=" + status);
        }

        // 先不使用FrameBuffer
        // Switch back to the default framebuffer.
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

//        GlUtil.checkGlError("prepareFramebuffer done");
    }

    public void onCreate() {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        mProgram = GLESUtils.createProgram(obtainVertex(), obtainFragment());
        glPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
        glCoordinate = GLES20.glGetAttribLocation(mProgram, "vCoordinate");
        glMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        glTexture = GLES20.glGetUniformLocation(mProgram, "vTexture");
        onExtraCreated(mProgram);
    }

    public void setTexBuffer(float[] coord) {
        mTexBuffer.clear();
        mTexBuffer.put(coord);
        mTexBuffer.position(0);
    }

    public void onSizeChange(int width, int height) {
        if (isToFbo) {
            prepareFramebuffer(width, height);
        }
        Matrix.setIdentityM(mMVPMatrix, 0);
//        flip(mMVPMatrix, false, true);
    }

    public void onDrawFrame() {
        onClear();
        onUseProgram();
        onExtraData();
        onBindTexture();
        onDraw();
    }

    protected void onDraw() {
        if (isToFbo) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);
        }

        GLES20.glEnableVertexAttribArray(glPosition);
        GLES20.glVertexAttribPointer(glPosition, 2, GLES20.GL_FLOAT, false, 0, mVerBuffer);
        GLES20.glEnableVertexAttribArray(glCoordinate);
        GLES20.glVertexAttribPointer(glCoordinate, 2, GLES20.GL_FLOAT, false, 0, mTexBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(glPosition);
        GLES20.glDisableVertexAttribArray(glCoordinate);

        if (isToFbo) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }

    }

    protected void onBindTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, getTextureId());
        GLES20.glUniform1i(glTexture, 0);
    }

    protected void onUseProgram() {
        GLES20.glUseProgram(mProgram);
    }

    protected void onClear() {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    public int getTextureId() {
        return mTextureId;
    }

    public void setTextureId(int mTextureId) {
        this.mTextureId = mTextureId;
    }

    public enum Filter {

        NONE(0, new float[]{0.0f, 0.0f, 0.0f}),
        GRAY(1, new float[]{0.299f, 0.587f, 0.114f}),
        COOL(2, new float[]{0.0f, 0.0f, 0.1f}),
        WARM(2, new float[]{0.1f, 0.1f, 0.0f}),
        BLUR(3, new float[]{0.006f, 0.004f, 0.002f}),
        MAGN(4, new float[]{0.0f, 0.0f, 0.4f});


        private int vChangeType;
        private float[] data;

        Filter(int vChangeType, float[] data) {
            this.vChangeType = vChangeType;
            this.data = data;
        }

        public int getType() {
            return vChangeType;
        }

        public float[] data() {
            return data;
        }

    }
}
