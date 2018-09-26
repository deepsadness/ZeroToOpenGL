package com.cry.zero_camera.activity.ppt.render;

import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.Log;

import com.cry.zero_camera.activity.ppt.TextureMovieEncoder2D;
import com.cry.zero_camera.render.fliter.PhotoFilter;
import com.cry.zero_camera.render.fliter.Show2DFilter;
import com.cry.zero_common.opengl.GLESUtils;

import java.io.File;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class SimpleRender3 implements GLSurfaceView.Renderer {
    private static final String TAG = "SimpleRender";
    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    //bitmap buffer
    private ArrayList<Bitmap> mBitmaps = new ArrayList<>();
    //存放fbo的id
    private int mFrameBuffer = 0;
    private int mRenderBuffer = 0;
    private int mOffscreenTextureId = 0;
    private Show2DFilter mShow2DFilter;
    private AnimateFilter mPhotoFilter;
    private AnimateFilter mPhotoAlphaFilter;
    private long frameNanos;
    private TextureMovieEncoder2D mVideoEncoder;
    private int mRecordingStatus;
    private boolean mRecordingEnabled;
    private File mOutputFile;
    private int width;
    private int height;
    private int bitmapIndex = -1;

    public SimpleRender3() {
        mPhotoFilter = new AnimateFilter();
        mShow2DFilter = new Show2DFilter();
        mVideoEncoder = new TextureMovieEncoder2D();

        mPhotoAlphaFilter = new AnimateFilter();
        mPhotoAlphaFilter.setAnimateType(4);
    }

    public void addBitmap(Bitmap bitmap) {
        mBitmaps.add(bitmap);
//        mPhotoFilter.setBitmap(mBitmap);
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
        mPhotoAlphaFilter.onCreate();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        this.width = width;
        this.height = height;
        //将纹理0传入？
        mPhotoFilter.onSizeChange(width, height);
        mShow2DFilter.onSizeChange(width, height);
        mPhotoAlphaFilter.onSizeChange(width, height);


        int[] offscreenIds = GLESUtils.createOffscreenIds(width, height);

        mOffscreenTextureId = offscreenIds[0];
        mFrameBuffer = offscreenIds[1];
        mRenderBuffer = offscreenIds[2];

        if (mBitmaps.size() > 1) {
            mPhotoFilter.setBitmap(mBitmaps.get(0));
            mPhotoAlphaFilter.setBitmap(mBitmaps.get(1));
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!mBitmaps.isEmpty()) {
            //进行录制
            if (mRecordingEnabled) {
                switch (mRecordingStatus) {
                    case RECORDING_OFF:
                        Log.d(TAG, "START recording");
                        mOutputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "camera-test" + System.currentTimeMillis() + ".mp4");
                        Log.d(TAG, "file path = " + mOutputFile.getAbsolutePath());
                        // start recording
                        mVideoEncoder.startRecording(new TextureMovieEncoder2D.EncoderConfig(
                                mOutputFile, width, height, 10000000, EGL14.eglGetCurrentContext()));
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

//            }
//            GLES20.glEnable(GLES20.GL_BLEND);
//            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_DST_ALPHA);
            System.out.println("mPhotoAlphaFilter draw textureId=" + mPhotoAlphaFilter.getTextureId());
//            if (bitmapIndex%2==1){
            mPhotoFilter.onDrawFrame();
//                mPhotoAlphaFilter.onDrawFrame();
//            }else {
//                mPhotoAlphaFilter.onDrawFrame();
            mPhotoFilter.onDrawFrame();
//            }

//            GLES20.glDisable(GLES20.GL_BLEND);
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

    public synchronized void doFrame(long frame, float difSec, long duration) {
        frameNanos = frame;
        if (duration == 0) {
            change();
            return;
        }
        mPhotoAlphaFilter.doFrame(difSec, duration);
        mPhotoFilter.doFrame(difSec, duration);
    }

    public void change() {
//        System.out.println("bitmapIndex=" + bitmapIndex);
//        bitmapIndex++;
//
//        if (bitmapIndex + 1 < mBitmaps.size()) {
//            Bitmap bitmap = mBitmaps.get(bitmapIndex);
//            moveTextureId(mPhotoFilter, mPhotoAlphaFilter, bitmap);
//
//            mPhotoAlphaFilter.setAnimateType(4);
//
//            if (bitmapIndex + 2 < mBitmaps.size()) {
//                mPhotoFilter.setBitmap(mBitmaps.get(bitmapIndex + 1));
//            } else {
//                mPhotoFilter.setBitmap(mBitmaps.get(0));
//            }
//        } else {
//            bitmapIndex = 0;
//            Bitmap bitmap = mBitmaps.get(bitmapIndex);
//            moveTextureId(mPhotoFilter, mPhotoAlphaFilter, bitmap);
//            mPhotoFilter.setBitmap(mBitmaps.get(bitmapIndex + 1));
//        }
        bitmapIndex++;
        if (bitmapIndex % 2 == 1) {
            mPhotoFilter.setAnimateType(4);
            mPhotoAlphaFilter.setAnimateType(2);
        } else {
            mPhotoFilter.setAnimateType(2);
            mPhotoAlphaFilter.setAnimateType(4);
        }
    }

    private void moveTextureId(PhotoFilter src, PhotoFilter dst, Bitmap bitmap) {
        int textureId = src.getTextureId();
        if (textureId != 0) {
            dst.setTextureId(textureId);
        } else {
            dst.setBitmap(bitmap);
        }
        System.out.println("mPhotoAlphaFilter change textureId=" + mPhotoAlphaFilter.getTextureId());
    }

    public int getAnimateType() {
        return mPhotoFilter.getAnimateType();
    }

    public void setAnimateType(int resultType) {
//        mPhotoAlphaFilter.setAnimateType(resultType);
        mPhotoFilter.setAnimateType(resultType);
    }

    public File getOutputFile() {
        return mOutputFile;
    }
}
