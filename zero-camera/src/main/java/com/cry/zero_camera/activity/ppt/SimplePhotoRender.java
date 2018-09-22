package com.cry.zero_camera.activity.ppt;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.cry.zero_common.opengl.GLESUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SimplePhotoRender implements GLSurfaceView.Renderer {
    private static final String TAG = "SimplePhotoRender";
    /**
     * 更新shader的位置
     */
    private static final String VERTEX_SHADER_FILE = "texture_vertex_shader.glsl";
    private static final String FRAGMENT_SHADER_FILE = "texture_fragment_shader.glsl";
    private static final String A_POSITION = "a_Position";
    private static final String A_COORDINATE = "a_TextureCoordinates";
    private static final String U_TEXTURE = "u_TextureUnit";
    private static final String U_MATRIX = "u_Matrix";

    private static final int COORDS_PER_VERTEX = 2;
    private static final int COORDS_PER_ST = 2;
    private static final int TOTAL_COMPONENT_COUNT = COORDS_PER_VERTEX + COORDS_PER_ST;
    private static final int STRIDE = TOTAL_COMPONENT_COUNT * 4;

    //顶点的坐标系
    private static float TEXTURE_COORDS[] = {
            //Order of coordinates: X, Y,S,T
            -1.0f, 1.0f, 0.0f, 0.0f,
            -1.0f, -1.0f, 0.0f, 1.0f, //bottom left
            1.0f, 1.0f, 1.0f, 0.0f, // top right
            1.0f, -1.0f, 1.0f, 1.0f, // bottom right
    };


    private static final int VERTEX_COUNT = TEXTURE_COORDS.length / TOTAL_COMPONENT_COUNT;
    private final Context context;
    //顶点数据的内存映射
    private final FloatBuffer mVertexFloatBuffer;
    //pragram的指针
    private int mProgramObjectId;
    //模型矩阵
    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];

    //投影矩阵
    private float[] mProjectMatrix = new float[16];
    private int uMatrix;
    private int uTexture;
    private int mTextureId;
    private Bitmap bitmap;
    private int height;
    private int width;


    //    private boolean mRecordingEnabled;
//    private int mRecordingStatus;
//    private static final int RECORDING_OFF = 0;
//    private static final int RECORDING_ON = 1;
//    private static final int RECORDING_RESUMED = 2;
//    private TextureMovieEncoder2D mVideoEncoder;
//    File mOutputFile;
    private long frame;
    //    private int mOffscreenTextureId;
//    private ColorFiler mShowFilter;
//    private int mFrameBuffer;
//    private int mRenderBuffer;
    private float[] srcMatrix = new float[16];
    private int animateType = 0;

    public SimplePhotoRender(Context context) {
        this.context = context;

        mVertexFloatBuffer = ByteBuffer
                .allocateDirect(TEXTURE_COORDS.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(TEXTURE_COORDS);
        mVertexFloatBuffer.position(0);
        Matrix.setIdentityM(mModelMatrix, 0);

//        mVideoEncoder = new TextureMovieEncoder2D();
//        mShowFilter = new ColorFiler(ColorFiler.Filter.NONE);

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        String vertexShaderCode = GLESUtils.readAssetShaderCode(context, VERTEX_SHADER_FILE);
        String fragmentShaderCode = GLESUtils.readAssetShaderCode(context, FRAGMENT_SHADER_FILE);
        int vertexShaderObjectId = GLESUtils.compileShaderCode(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShaderObjectId = GLESUtils.compileShaderCode(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgramObjectId = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgramObjectId, vertexShaderObjectId);
        GLES20.glAttachShader(mProgramObjectId, fragmentShaderObjectId);
        GLES20.glLinkProgram(mProgramObjectId);

        GLES20.glUseProgram(mProgramObjectId);

        int aPosition = GLES20.glGetAttribLocation(mProgramObjectId, A_POSITION);
        mVertexFloatBuffer.position(0);
        GLES20.glVertexAttribPointer(
                aPosition,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                STRIDE,
                mVertexFloatBuffer);

        GLES20.glEnableVertexAttribArray(aPosition);


        int aCoordinate = GLES20.glGetAttribLocation(mProgramObjectId, A_COORDINATE);
        mVertexFloatBuffer.position(COORDS_PER_VERTEX);
        GLES20.glVertexAttribPointer(
                aCoordinate,
                COORDS_PER_ST,
                GLES20.GL_FLOAT, false,
                STRIDE,
                mVertexFloatBuffer);

        GLES20.glEnableVertexAttribArray(aCoordinate);

        uMatrix = GLES20.glGetUniformLocation(mProgramObjectId, U_MATRIX);

        uTexture = GLES20.glGetUniformLocation(mProgramObjectId, U_TEXTURE);

        mTextureId = createTexture2();

//        mRecordingEnabled = mVideoEncoder.isRecording();
//        if (mRecordingEnabled) {
//            mRecordingStatus = RECORDING_RESUMED;
//        } else {
//            mRecordingStatus = RECORDING_OFF;
//        }
    }

    private int createTexture2() {
        final Bitmap mBitmap = bitmap;

        //加载Bitmap
        //保存到textureObjectId
        int[] textureObjectId = new int[1];
        if (mBitmap != null && !mBitmap.isRecycled()) {
            //生成一个纹理，保存到这个数组中
            GLES20.glGenTextures(1, textureObjectId, 0);
            //绑定GL_TEXTURE_2D
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureObjectId[0]);
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

//            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//

            //根据以上指定的参数，生成一个2D纹理
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);

            //因为使用贴图的方式来生成纹理,故需要生成纹理
//            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

            //回收释放
            mBitmap.recycle();
            //因为我们已经复制成功了。所以就进行解除绑定。防止修改
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

            return textureObjectId[0];
        }
        return 0;
    }

    //生成frameBuffer的时机
//    private void prepareFramebuffer(int width, int height) {
//        int[] values = new int[1];
//        GLES20.glGenTextures(1, values, 0);
////        GlUtil.checkGlError("glGenTextures");
//        mOffscreenTextureId = values[0];
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOffscreenTextureId);
////        GlUtil.checkGlError("glBindTexture " + mOffscreenTextureId);
//
//        // Create texture storage.
//        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
//                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
//
//        // Set parameters.  We're probably using non-power-of-two dimensions, so
//        // some values may not be available for use.
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
//                GLES20.GL_NEAREST);
//        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
//                GLES20.GL_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
//                GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
//                GLES20.GL_CLAMP_TO_EDGE);
////        GlUtil.checkGlError("glTexParameter");
//
//        // Create framebuffer object and bind it.
//        GLES20.glGenFramebuffers(1, values, 0);
//        mFrameBuffer = values[0];
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);
////        GlUtil.checkGlError("glBindFramebuffer " + mFrameBuffer);
//
//        // Create a depth buffer and bind it.
//        GLES20.glGenRenderbuffers(1, values, 0);
//        mRenderBuffer = values[0];
//        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mRenderBuffer);
////        GlUtil.checkGlError("glBindFramebuffer " + mRenderBuffer);
//
//        // Allocate storage for the depth buffer.
//        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);
////        GlUtil.checkGlError("glRenderbufferStorage");
//
//        // Attach the depth buffer and the texture (color buffer) to the framebuffer object.
//        // 将renderBuffer挂载到frameBuffer的depth attachment 上
//        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mRenderBuffer);
////        GlUtil.checkGlError("glFramebufferRenderbuffer");
//        // 将text2d挂载到frameBuffer的color attachment上
//        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mOffscreenTextureId, 0);
////        GlUtil.checkGlError("glFramebufferTexture2D");
//
//        // See if GLES is happy with all this.
//        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
//        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
//            throw new RuntimeException("Framebuffer not complete, status=" + status);
//        }
//
//        // 先不使用FrameBuffer
//        // Switch back to the default framebuffer.
//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//
////        GlUtil.checkGlError("prepareFramebuffer done");
//    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        this.width = width;
        this.height = height;
//        prepareFramebuffer(width, height);

    }

    //在OnDrawFrame中进行绘制
    @Override
    public void onDrawFrame(GL10 gl) {
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

//        //进行录制
////        if (mRecordingEnabled) {
////            switch (mRecordingStatus) {
////                case RECORDING_OFF:
////                    Log.d(TAG, "START recording");
//////                    mOutputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "camera-test" + System.currentTimeMillis() + ".mp4");
//////                    Log.d(TAG, "file path = " + mOutputFile.getAbsolutePath());
//////                    // start recording
//////                    mVideoEncoder.startRecording(new TextureMovieEncoder2D.EncoderConfig(
//////                            mOutputFile,  width, height,64000000, EGL14.eglGetCurrentContext()));
////                    mRecordingStatus = RECORDING_ON;
////                    break;
////                case RECORDING_RESUMED:
////                    Log.d(TAG, "RESUME recording");
////                    mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
////                    mRecordingStatus = RECORDING_ON;
////                    break;
////                case RECORDING_ON:
////                    // yay
////                    break;
////                default:
////                    throw new RuntimeException("unknown status " + mRecordingStatus);
////            }
////        } else {
////            switch (mRecordingStatus) {
////                case RECORDING_ON:
////                case RECORDING_RESUMED:
////                    // stop recording
////                    Log.d(TAG, "STOP recording");
////                    mVideoEncoder.stopRecording();
////                    mRecordingStatus = RECORDING_OFF;
////                    break;
////                case RECORDING_OFF:
////                    // yay
////                    break;
////                default:
////                    throw new RuntimeException("unknown status " + mRecordingStatus);
////            }
////        }


//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);

        //传递给着色器
        GLES20.glUniformMatrix4fv(uMatrix, 1, false, mModelMatrix, 0);

        //绑定和激活纹理
        //因为我们生成了MIP，放到了GL_TEXTURE0 中，所以重新激活纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //重新去半丁纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);


        //设置纹理的坐标
        GLES20.glUniform1i(uTexture, 0);

        //绘制三角形.
        //draw arrays的几种方式 GL_TRIANGLES三角形 GL_TRIANGLE_STRIP三角形带的方式(开始的3个点描述一个三角形，后面每多一个点，多一个三角形) GL_TRIANGLE_FAN扇形(可以描述圆形)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_COUNT);

//        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

//        mVideoEncoder.setTextureId(mOffscreenTextureId);
//
//        // Tell the video encoder thread that a new frame is available.
//        // This will be ignored if we're not actually recording.
//        mVideoEncoder.frameAvailable(frame);
//        mShowFilter.setTextureId(mOffscreenTextureId);
//        mShowFilter.onDrawFrame();
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        calculateMatrix();
    }

    private void calculateMatrix() {
        Bitmap mBitmap = this.bitmap;
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
        Matrix.multiplyMM(mModelMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0);

        srcMatrix = Arrays.copyOf(mModelMatrix, 16);

    }

    public int getAnimateType() {
        return animateType;
    }

    public void setAnimateType(int animateType) {
        this.animateType = animateType;
    }

    public void doFrame(long frame, float difSec, long duration) {
        this.frame = frame;
        if (duration == 0) {
//            mRecordingEnabled = false;
            return;
        }
//        mRecordingEnabled=true;
        if (animateType == 0) {
            float v = difSec * 1f / duration * 0.5f + 1f;
            System.out.println("scale=" + v);
            mModelMatrix = Arrays.copyOf(srcMatrix, 16);
            Matrix.scaleM(mModelMatrix, 0, v, v, 0f);
        } else if (animateType == 1) {
            float v = difSec * 1f / duration * 0.5f * 360;
            System.out.println("rotate=" + v);
            mModelMatrix = Arrays.copyOf(srcMatrix, 16);
            Matrix.rotateM(mModelMatrix, 0, v, 0f, 0f, 1f);
        } else if (animateType == 2) {
            float v = difSec * 1f / duration * 1f * 2;
            System.out.println("scale=" + v);
            mModelMatrix = Arrays.copyOf(srcMatrix, 16);
            Matrix.translateM(mModelMatrix, 0, v, 0f, 0f);
        } else if (animateType == 3) {
            float v = difSec * 1f / duration * 1f * 2;
            System.out.println("scale=" + v);
            mModelMatrix = Arrays.copyOf(srcMatrix, 16);
            Matrix.translateM(mModelMatrix, 0, 0, v, 0f);
        }
    }

}
