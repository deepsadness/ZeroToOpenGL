package com.cry.zero_camera.activity.double_input;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.ViewGroup;

import com.cry.zero_camera.camera.CameraApi14;
import com.cry.zero_camera.camera.CameraSize;
import com.cry.zero_camera.camera.ICamera;

import java.io.File;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 创建一个OpenGL环境。在这里创建Camera 和 Camera输出的SurfaceView
 */
public class DoubleInputView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private static final String TAG = "DoubleInputView";
    int takePhotoFromGL = 0;
    private ICamera mCameraApi;
    private int mCameraIdDefault = 1;
    private DoubleInputRender mCameraRender;
    private int width;
    private int height;
    private DecodeThread2 mDecodeThread;

    public DoubleInputView(Context context) {
        super(context);
        initEGL();
        initCameraApi(context);
    }

    private void initEGL() {
        //open gl step 1
        setEGLContextClientVersion(2);
        setRenderer(this);
        //只有刷新之后，才会去重绘
        setRenderMode(RENDERMODE_WHEN_DIRTY);

    }

    private void initCameraApi(Context context) {
//        mCameraApi = new KitkatCamera();
        mDecodeThread = new DecodeThread2(this);

        mCameraApi = new CameraApi14();
        mCameraRender = new DoubleInputRender(context.getResources());
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCameraRender.onSurfaceCreated(gl, config);

        mCameraApi.open(mCameraIdDefault);
        mCameraRender.setCameraId(mCameraIdDefault);


        CameraSize previewSize = mCameraApi.getPreviewSize();
        int previewSizeWidth = previewSize.getWidth();
        int previewSizeHeight = previewSize.getHeight();

//      Point previewSize = mCameraApi.getPreviewSize();
//        int previewSizeWidth = previewSize.x;
//        int previewSizeHeight = previewSize.y;

        mCameraRender.setPreviewSize(previewSizeWidth, previewSizeHeight);
        mDecodeThread.sendSurface(mCameraRender.getVideoSurfaceTexture());

        mCameraApi.setPreviewTexture(mCameraRender.getCameraSurfaceTexture());
        //默认使用的GLThread.每次刷新的时候，都强制要求是刷新这个GLSurfaceView
        mCameraRender.getCameraSurfaceTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                if (!mCameraRender.isRecordingEnabled()) {
                    requestRender();
                }
            }
        }); //默认使用的GLThread.每次刷新的时候，都强制要求是刷新这个GLSurfaceView
        mCameraRender.getVideoSurfaceTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                if (mCameraRender.isRecordingEnabled()) {
                    requestRender();
                }
//                requestRender();
            }
        });
        mCameraApi.preview();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mCameraRender.onSurfaceChanged(gl, width, height);
        //设置ViewPort是必须要做的
        GLES20.glViewport(0, 0, width, height);
        this.width = width;
        this.height = height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mCameraRender.onDrawFrame(gl);
    }

    @Override
    public void onPause() {
        super.onPause();
        mCameraApi.close();

        queueEvent(new Runnable() {
            @Override
            public void run() {
                // Tell the renderer that it's about to be paused so it can clean up.
                mCameraRender.notifyPausing();
            }
        });
        Log.d(TAG, "onPause complete");
    }

    public void takePhoto(ICamera.TakePhotoCallback callback) {
        if (takePhotoFromGL != 1) {
            if (mCameraApi != null) {
                float[] mtx = new float[16];
                mCameraRender.getCameraSurfaceTexture().getTransformMatrix(mtx);
                mCameraApi.takePhoto(callback);
            }
        } else {
            //直接使用OpenGL的方式
            queueEvent(() -> {
                //发送到GLThread中进行
                //这里传递的长宽。其实就是openGL surface的长款，一定要注意了！！
                ByteBuffer rgbaBuf = ByteBuffer.allocateDirect(width * height * 4);
                rgbaBuf.position(0);
                long start = System.nanoTime();
                GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                        rgbaBuf);
                long end = System.nanoTime();
                Log.d(TAG, "gl glReadPixels cost = " + (end - start));
                callback.onTakePhoto(rgbaBuf.array(), width, height);
            });
        }

    }

    public void changeRecordingState(boolean mRecordingEnabled) {
        queueEvent(() -> {
            // notify the renderer that we want to change the encoder's state
            mCameraRender.changeRecordingState(mRecordingEnabled);
        });
    }

    public void adjustAspectRatio(int videoWidth, int videoHeight) {
//        int viewWidth = getWidth();
//        int viewHeight = getHeight();
//        double aspectRatio = (double) videoHeight / videoWidth;
//
//        int newWidth, newHeight;
//        if (viewHeight > (int) (viewWidth * aspectRatio)) {
//            // limited by narrow width; restrict height
//            newWidth = viewWidth;
//            newHeight = (int) (viewWidth * aspectRatio);
//        } else {
//            // limited by short height; restrict width
//            newWidth = (int) (viewHeight / aspectRatio);
//            newHeight = viewHeight;
//        }
//        int xoff = (viewWidth - newWidth) / 2;
//        int yoff = (viewHeight - newHeight) / 2;
//        Log.v(TAG, "video=" + videoWidth + "x" + videoHeight +
//                " view=" + viewWidth + "x" + viewHeight +
//                " newView=" + newWidth + "x" + newHeight +
//                " off=" + xoff + "," + yoff);
//

//        GLES20.glViewport(0, -396, newWidth, newHeight);

        float aspect = videoHeight * 1f / videoWidth;
        float surfaceAspect = height * 1f / width;
        //设置ViewPort是必须要做的
        int offsetX = 0;
        int offsetY = 0;
        if (aspect > surfaceAspect) {
            width = (int) (height / surfaceAspect);
        } else if (aspect < surfaceAspect) {
            int newHeight = (int) (width * aspect);
            offsetY = (height - newHeight) / 2;
            height = newHeight;
        }
        int finalOffset = 0;
        post(() -> {
            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            layoutParams.height = height;
            setLayoutParams(layoutParams);
        });


    }

    public void playbackStopped() {
        queueEvent(() -> {
            // notify the renderer that we want to change the encoder's state
            mCameraRender.changeRecordingState(false);
            requestRender();
        });
    }

    public void start(File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            return;
        }
        mDecodeThread.sendSrcFile(file);
        mDecodeThread.sendStart();
        //设置开始
        changeRecordingState(true);

    }

    public void stop() {
        mDecodeThread.sendStop();
        changeRecordingState(false);
    }


}
