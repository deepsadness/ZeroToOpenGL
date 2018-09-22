package com.cry.zero_camera.activity.ppt;

import android.opengl.EGL14;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordStateManager {
    private static final String TAG = "RecordStateManager";
    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    private TextureMovieEncoder2D mVideoEncoder;
    private int mRecordingStatus;
    private boolean mRecordingEnabled;
    private File mOutputFile;
    private int width;
    private int height;

    public RecordStateManager() {
        mVideoEncoder = new TextureMovieEncoder2D();
    }

    public void onCreate() {
        mRecordingEnabled = mVideoEncoder.isRecording();
        if (mRecordingEnabled) {
            mRecordingStatus = RECORDING_RESUMED;
        } else {
            mRecordingStatus = RECORDING_OFF;
        }
    }

    public void onSizeChange(int width, int height) {
        this.width = width;
        this.height = height;

    }

    public void onConsiderState() {
        //进行录制
        if (mRecordingEnabled) {
            switch (mRecordingStatus) {
                case RECORDING_OFF:
                    Log.d(TAG, "START recording");
                    SimpleDateFormat format = new SimpleDateFormat("MM-dd-HH:mm:ss", Locale.ENGLISH);
                    String formatTime = format.format(new Date());
                    formatTime = System.currentTimeMillis() + "";
                    mOutputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "camera-test-" + formatTime + ".mp4");
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
    }

    public void onDrawFrame(float[] transform, long timestamp) {
        mVideoEncoder.frameAvailable(transform, timestamp);
    }

    public void configureTextureId(int inTextureId) {
        mVideoEncoder.setTextureId(inTextureId);
    }

    public void changeRecordingState(boolean isRecording) {
        this.mRecordingEnabled = isRecording;
    }

    public File getOutputFile() {
        return mOutputFile;
    }
}
