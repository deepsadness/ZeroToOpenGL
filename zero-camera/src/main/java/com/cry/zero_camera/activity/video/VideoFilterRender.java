package com.cry.zero_camera.activity.video;

import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Environment;
import android.util.Log;

import com.cry.zero_camera.ref.TextureMovieEncoder2D;
import com.cry.zero_camera.render.ColorFiler;
import com.cry.zero_camera.render.OesRecordFilter;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 *
 */
public class VideoFilterRender implements GLSurfaceView.Renderer {
    private static final String TAG = "CameraRender";
    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    //    private final AFilter mOesFilter;
    private final OesRecordFilter mOesFilter;
    private final ColorFiler mColorFilter;
    private final ColorFiler mShowFilter;
    File mOutputFile;
    //相机输出的surfaceView
    private SurfaceTexture mSurfaceTexture;
    //绘制的纹理ID
    private int mTextureId;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mPreviewWidth;
    private int mPreviewHeight;
    //视图矩阵。控制旋转和变化
    private float[] mModelMatrix = new float[16];
    private boolean mRecordingEnabled;
    private int mRecordingStatus;
    private TextureMovieEncoder2D mVideoEncoder;
    private int mOffscreenTextureId;
    private int mFrameBuffer;
    private int mRenderBuffer;
    private float[] transfrom = new float[16];

    public VideoFilterRender(Resources res) {
        mOesFilter = new OesRecordFilter(res);
        mColorFilter = new ColorFiler(ColorFiler.Filter.WARM);
        mShowFilter = new ColorFiler(ColorFiler.Filter.NONE);
        mVideoEncoder = new TextureMovieEncoder2D();
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // We're starting up or coming back.  Either way we've got a new EGLContext that will
        // need to be shared with the video encoder, so figure out if a recording is already
        // in progress.
        mRecordingEnabled = mVideoEncoder.isRecording();
        if (mRecordingEnabled) {
            mRecordingStatus = RECORDING_RESUMED;
        } else {
            mRecordingStatus = RECORDING_OFF;
        }

        //生成纹理
        mTextureId = genOesTextureId();
        //创建内部的surfaceView
        mSurfaceTexture = new SurfaceTexture(mTextureId);

        //创建滤镜.同时绑定滤镜上
        mOesFilter.create();
        mOesFilter.setTextureId(mTextureId);

        //绑定上对应的路径
        mColorFilter.onCreate();
        mColorFilter.setToFbo(true);
        mShowFilter.onCreate();
    }

    private int genOesTextureId() {
        int[] textureObjectId = new int[1];
        GLES20.glGenTextures(1, textureObjectId, 0);
        //绑定纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureObjectId[0]);
        //设置放大缩小。设置边缘测量
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return textureObjectId[0];
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //在这里监听到尺寸的改变。做出对应的变化
        prepareFramebuffer(width, height);

        this.mSurfaceWidth = width;
        this.mSurfaceHeight = height;
        this.mPreviewWidth = width;
        this.mPreviewHeight = height;
        calculateMatrix();


        mColorFilter.onSizeChange(width, height);
        mShowFilter.onSizeChange(width, height);
    }

    //生成frameBuffer的时机
    private void prepareFramebuffer(int width, int height) {
        int[] values = new int[1];
        values[0] = 55;
        GLES20.glGenTextures(1, values, 0);
//        GlUtil.checkGlError("glGenTextures");
        mOffscreenTextureId = values[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOffscreenTextureId);
//        GlUtil.checkGlError("glBindTexture " + mOffscreenTextureId);

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
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mOffscreenTextureId, 0);
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

    //计算需要变化的矩阵
    private void calculateMatrix() {
//        得到通用的显示的matrix
//        Gl2Utils.getShowMatrix(mModelMatrix, mPreviewWidth, mPreviewHeight, this.mSurfaceWidth, this.mSurfaceHeight);
//
//        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {  //前置摄像头
//            Gl2Utils.flip(mModelMatrix, true, false);
//            Gl2Utils.rotate(mModelMatrix, 90);
//        } else {  //后置摄像头
//            int rotateAngle = 270;
//            Gl2Utils.rotate(mModelMatrix, rotateAngle);
//        }
        Matrix.setIdentityM(mModelMatrix, 0);
        mOesFilter.setMatrix(mModelMatrix);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //每次绘制后，都通知texture刷新
        if (mSurfaceTexture != null) {
            //It will implicitly bind its texture to the GL_TEXTURE_EXTERNAL_OES texture target.
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(transfrom);
//            mOesFilter.setCoordMatrix(transfrom);
        }

        //进行录制
        if (mRecordingEnabled) {
            switch (mRecordingStatus) {
                case RECORDING_OFF:
                    Log.d(TAG, "START recording");
                    mOutputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "camera-test" + System.currentTimeMillis() + ".mp4");
                    Log.d(TAG, "file path = " + mOutputFile.getAbsolutePath());
                    // start recording
                    mVideoEncoder.startRecording(new TextureMovieEncoder2D.EncoderConfig(
                            mOutputFile, mPreviewWidth, mPreviewHeight, 64000000, EGL14.eglGetCurrentContext()));
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_RESUMED:
                    Log.d(TAG, "RESUME recording");
                    mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        } else {
            switch (mRecordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    // stop recording
                    Log.d(TAG, "STOP recording");
                    mVideoEncoder.stopRecording();
                    mRecordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        }


        //绑定上。就会绘制到fbo中
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);
        mOesFilter.draw();
        //解除绑定
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        //经过路径处理
        mColorFilter.setTextureId(mOffscreenTextureId);
        mColorFilter.onDrawFrame();
        int outputTextureId = mColorFilter.getOutputTextureId();

        //进行编码
        // Set the video encoder's texture name.  We only need to do this once, but in the
        // current implementation it has to happen after the video encoder is started, so
        // we just do it here.
        //
        // TODO: be less lame.
        mVideoEncoder.setTextureId(outputTextureId);

        // Tell the video encoder thread that a new frame is available.
        // This will be ignored if we're not actually recording.
        mVideoEncoder.frameAvailable(mSurfaceTexture);

        //在显示出来
        mShowFilter.setTextureId(outputTextureId);
        mShowFilter.onDrawFrame();
    }

    public void setPreviewSize(int previewWidth, int previewHeight) {
        this.mPreviewWidth = previewWidth;
        this.mPreviewHeight = previewHeight;
        calculateMatrix();
    }

    /**
     * Notifies the renderer thread that the activity is pausing.
     * <p>
     * For best results, call this *after* disabling Camera preview.
     */
    public void notifyPausing() {
        if (mSurfaceTexture != null) {
            Log.d(TAG, "renderer pausing -- releasing SurfaceTexture");
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
//        if (mOesFilter != null) {
//            mOesFilter.release(false);     // assume the GLSurfaceView EGL context is about
//            mFullScreen = null;             //  to be destroyed
//        }
//        mIncomingWidth = mIncomingHeight = -/1;
    }

    public void changeRecordingState(boolean isRecording) {
        Log.d(TAG, "changeRecordingState: was " + mRecordingEnabled + " now " + isRecording);
        mRecordingEnabled = isRecording;
    }
}
