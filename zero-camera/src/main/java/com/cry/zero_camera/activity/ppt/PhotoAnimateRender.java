package com.cry.zero_camera.activity.ppt;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.cry.zero_camera.ref.TextureMovieEncoder2D;
import com.cry.zero_camera.render.ColorFiler;
import com.cry.zero_camera.render.PhotoFilter;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 *
 */
public class PhotoAnimateRender implements GLSurfaceView.Renderer {
    private static final String TAG = "PhotoAnimateRender";
    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    private final ColorFiler mColorFilter;
    private final ColorFiler mShowFilter;
    File mOutputFile;
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
    private Bitmap bitmap;
    private float[] srcBitmapMatrix;
    private PhotoFilter photoFilter;
    private float[] transfrom = new float[16];

    public PhotoAnimateRender(Resources res) {
        mColorFilter = new ColorFiler(ColorFiler.Filter.COOL);
        mShowFilter = new ColorFiler(ColorFiler.Filter.NONE);
        mVideoEncoder = new TextureMovieEncoder2D();
        photoFilter = new PhotoFilter(res);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//        mRecordingEnabled = mVideoEncoder.isRecording();
//        if (mRecordingEnabled) {
//            mRecordingStatus = RECORDING_RESUMED;
//        } else {
//            mRecordingStatus = RECORDING_OFF;
//        }

        //绑定上对应的路径
        mColorFilter.onCreate();
        mColorFilter.setToFbo(true);
        mShowFilter.onCreate();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //在这里监听到尺寸的改变。做出对应的变化
        prepareFramebuffer(width, height);

        this.mSurfaceWidth = width;
        this.mSurfaceHeight = height;
        calculateMatrix();

        mColorFilter.onSizeChange(width, height);
        mShowFilter.onSizeChange(width, height);
    }

    //生成frameBuffer的时机
    private void prepareFramebuffer(int width, int height) {
        int[] values = new int[1];
        GLES20.glGenTextures(1, values, 0);
        mOffscreenTextureId = values[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOffscreenTextureId);
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
        // Create framebuffer object and bind it.
        GLES20.glGenFramebuffers(1, values, 0);
        mFrameBuffer = values[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);
        GLES20.glGenRenderbuffers(1, values, 0);
        mRenderBuffer = values[0];
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mRenderBuffer);
        // Allocate storage for the depth buffer.
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);
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
    }

    //计算需要变化的矩阵
    private void calculateMatrix() {
//        得到通用的显示的matrix
//        Gl2Utils.getShowMatrix(mModelMatrix, mPreviewWidth, mPreviewHeight, this.mSurfaceWidth, this.mSurfaceHeight);
//        Gl2Utils.flip(mModelMatrix, false, true);
//        mOesFilter.setMatrix(mModelMatrix);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //进行录制
//        if (mRecordingEnabled) {
//            switch (mRecordingStatus) {
//                case RECORDING_OFF:
//                    Log.d(TAG, "START recording");
//                    mOutputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "camera-test" + System.currentTimeMillis() + ".mp4");
//                    Log.d(TAG, "file path = " + mOutputFile.getAbsolutePath());
//                    // start recording
//                    mVideoEncoder.startRecording(new TextureMovieEncoder2D.EncoderConfig(
//                            mOutputFile,  mPreviewWidth, mPreviewHeight,64000000, EGL14.eglGetCurrentContext()));
//                    mRecordingStatus = RECORDING_ON;
//                    break;
//                case RECORDING_RESUMED:
//                    Log.d(TAG, "RESUME recording");
//                    mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
//                    mRecordingStatus = RECORDING_ON;
//                    break;
//                case RECORDING_ON:
//                    // yay
//                    break;
//                default:
//                    throw new RuntimeException("unknown status " + mRecordingStatus);
//            }
//        } else {
//            switch (mRecordingStatus) {
//                case RECORDING_ON:
//                case RECORDING_RESUMED:
//                    // stop recording
//                    Log.d(TAG, "STOP recording");
//                    mVideoEncoder.stopRecording();
//                    mRecordingStatus = RECORDING_OFF;
//                    break;
//                case RECORDING_OFF:
//                    // yay
//                    break;
//                default:
//                    throw new RuntimeException("unknown status " + mRecordingStatus);
//            }
//        }


        //绑定上。就会绘制到fbo中
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);
        draw();
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
//        mVideoEncoder.setTextureId(outputTextureId);

        // Tell the video encoder thread that a new frame is available.
        // This will be ignored if we're not actually recording.
//        mVideoEncoder.frameAvailable(mSurfaceTexture);

        //在显示出来
        mShowFilter.setTextureId(outputTextureId);
        mShowFilter.onDrawFrame();
    }

    private void draw() {
        photoFilter.draw();
    }


    public void setPreviewSize(int previewWidth, int previewHeight) {
        this.mPreviewWidth = previewWidth;
        this.mPreviewHeight = previewHeight;
        calculateMatrix();
    }

    public void changeRecordingState(boolean isRecording) {
        Log.d(TAG, "changeRecordingState: was " + mRecordingEnabled + " now " + isRecording);
        mRecordingEnabled = isRecording;
    }

    public void setBitmap(Bitmap bitmap, float[] bitmapMatrix) {
        this.bitmap = bitmap;
        this.srcBitmapMatrix = bitmapMatrix;
        photoFilter.setBitmap(bitmap, srcBitmapMatrix, this.mSurfaceWidth,
                this.mSurfaceHeight);
    }
}
