package com.cry.zero_camera.activity.ppt.render;

import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.Log;

import com.cry.zero_camera.activity.ppt.TextureMovieEncoder2D;
import com.cry.zero_camera.render.fliter.Show2DFilter;
import com.cry.zero_common.opengl.GLESUtils;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SimpleRender implements GLSurfaceView.Renderer {
    private static final String TAG = "SimpleRender";
    //bitmap buffer
    private Bitmap mBitmap;

    //存放fbo的id
    private int mFrameBuffer = 0;
    private int mRenderBuffer = 0;
    private int mOffscreenTextureId = 0;
    private static final int RECORDING_OFF = 0;
    private Show2DFilter mShow2DFilter;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    private AnimateFilter mPhotoFilter;
    private long frameNanos;
    private TextureMovieEncoder2D mVideoEncoder;
    private int mRecordingStatus;
    private boolean mRecordingEnabled;
    private File mOutputFile;
    private int width;
    private int height;

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    public SimpleRender() {
        mPhotoFilter = new AnimateFilter();
        mShow2DFilter = new Show2DFilter();
        mVideoEncoder = new TextureMovieEncoder2D();
    }

    //render call back
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {


        mRecordingEnabled = mVideoEncoder.isRecording();
        if (mRecordingEnabled) {
            mRecordingStatus = RECORDING_RESUMED;
        } else {
            mRecordingStatus = RECORDING_OFF;
        }

        //创建program
        mPhotoFilter.onCreate();
        mShow2DFilter.onCreate();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        this.width = width;
        this.height = height;
        //将纹理0传入？
        mPhotoFilter.onSizeChange(width, height);
        mShow2DFilter.onSizeChange(width, height);


        int[] offscreenIds = GLESUtils.createOffscreenIds(width, height);

        mOffscreenTextureId = offscreenIds[0];
        mFrameBuffer = offscreenIds[1];
        mRenderBuffer = offscreenIds[2];
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            //进行录制
            if (mRecordingEnabled) {
                switch (mRecordingStatus) {
                    case RECORDING_OFF:
                        Log.d(TAG, "START recording");
                        mOutputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "camera-test" + System.currentTimeMillis() + ".mp4");
                        Log.d(TAG, "file path = " + mOutputFile.getAbsolutePath());
                        // start recording
                        mVideoEncoder.startRecording(new TextureMovieEncoder2D.EncoderConfig(
                                mOutputFile, width, height, 64000000, EGL14.eglGetCurrentContext()));
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

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer);

//            if (mPhotoFilter.getBitmap() == null || mPhotoFilter.getBitmap() != mBitmap) {
            mPhotoFilter.setBitmap(mBitmap);
//            }

            mPhotoFilter.onDrawFrame();
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);


            //进行编码
            // Set the video encoder's texture name.  We only need to do this once, but in the
            // current implementation it has to happen after the video encoder is started, so
            // we just do it here.
            //
            // TODO: be less lame.
            mVideoEncoder.setTextureId(mOffscreenTextureId);

            // Tell the video encoder thread that a new frame is available.
            // This will be ignored if we're not actually recording.
            mVideoEncoder.frameAvailable(null, frameNanos);

            mShow2DFilter.setTextureId(mOffscreenTextureId);
            mShow2DFilter.onDrawFrame();

        }
    }

    public void changeRecordingState(boolean isRecording) {
        this.mRecordingEnabled = isRecording;
    }

    public void doFrame(long frame, float difSec, long duration) {
        frameNanos = frame;
        if (duration == 0) {
            return;
        }
//        mPhotoFilter.doFrame(difSec, duration);
    }

    public int getAnimateType() {
        return mPhotoFilter.getAnimateType();
    }

    public void setAnimateType(int resultType) {
        mPhotoFilter.setAnimateType(resultType);
    }

    public File getOutputFile() {
        return mOutputFile;
    }
}
