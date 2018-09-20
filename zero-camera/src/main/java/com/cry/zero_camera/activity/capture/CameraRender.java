package com.cry.zero_camera.activity.capture;

import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.Log;

import com.cry.zero_camera.ref.TextureMovieEncoder;
import com.cry.zero_camera.render.OesRecordFilter;
import com.cry.zero_common.opengl.Gl2Utils;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 *
 */
public class CameraRender implements GLSurfaceView.Renderer {
    private static final String TAG = "CameraRender";
    //    private final AFilter mOesFilter;
    private final OesRecordFilter mOesFilter;
    //相机的id
    private int mCameraId = 0;
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
    File mOutputFile;
    private boolean mRecordingEnabled;
    private int mRecordingStatus;
    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    private TextureMovieEncoder mVideoEncoder;

    public CameraRender(Resources res) {
        mOesFilter = new OesRecordFilter(res);
        mVideoEncoder = new TextureMovieEncoder();
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
        this.mSurfaceWidth = width;
        this.mSurfaceHeight = height;
        calculateMatrix();
    }

    //计算需要变化的矩阵
    private void calculateMatrix() {
//        得到通用的显示的matrix
        Gl2Utils.getShowMatrix(mModelMatrix, mPreviewWidth, mPreviewHeight, this.mSurfaceWidth, this.mSurfaceHeight);

        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {  //前置摄像头
            Gl2Utils.flip(mModelMatrix, true, false);
            Gl2Utils.rotate(mModelMatrix, 90);
        } else {  //后置摄像头
            int rotateAngle = 270;
            Gl2Utils.rotate(mModelMatrix, rotateAngle);
        }
        mOesFilter.setMatrix(mModelMatrix);
    }

    private float[] transfrom = new float[16];

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
                    mVideoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
                            mOutputFile, mPreviewHeight, mPreviewWidth, 1000000, EGL14.eglGetCurrentContext()));
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

        // Set the video encoder's texture name.  We only need to do this once, but in the
        // current implementation it has to happen after the video encoder is started, so
        // we just do it here.
        //
        // TODO: be less lame.
        mVideoEncoder.setTextureId(mTextureId);

        // Tell the video encoder thread that a new frame is available.
        // This will be ignored if we're not actually recording.
        mVideoEncoder.frameAvailable(mSurfaceTexture);

        mOesFilter.draw();
    }

    public void setCameraId(int cameraId) {
        this.mCameraId = cameraId;
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
//        mIncomingWidth = mIncomingHeight = -1;
    }

    public void changeRecordingState(boolean isRecording) {
        Log.d(TAG, "changeRecordingState: was " + mRecordingEnabled + " now " + isRecording);
        mRecordingEnabled = isRecording;
    }
}
