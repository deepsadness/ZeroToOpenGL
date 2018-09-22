package com.cry.zero_camera.activity.ppt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.Choreographer;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.cry.zero_camera.R;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.disposables.CompositeDisposable;

public class PhotoAnimateSimpleActivity extends AppCompatActivity implements Choreographer.FrameCallback {
    private static final int REQUEST_CAMERA_PERMISSION = 2;
    private GLSurfaceView mGLView;
    private Button buttonStart;
    private SimplePhotoRender simplePhotoRender;
    private RxPermissions rxPermissions;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private long startTime = 0;
    private long duration = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rxPermissions = new RxPermissions(this);
        compositeDisposable.add(rxPermissions
                .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe());

        //去除状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_photo_simple);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mGLView = (GLSurfaceView) findViewById(R.id.surface);
        buttonStart = findViewById(R.id.fab);

        initGL();

        findViewById(R.id.pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            i.setType("image/*");
            startActivityForResult(i, 1);
        });

        findViewById(R.id.change).setOnClickListener(v -> {
            int animateType = simplePhotoRender.getAnimateType();
            int resultType = 0;
            if (animateType == 3) {
            } else {
                resultType = animateType + 1;
            }
            String result = "";
            switch (resultType) {
                case 0:
                    result = "放大";
                    break;
                case 1:
                    result = "旋转";
                    break;
                case 2:
                case 3:
                    result = "平移X";
                    break;
                case 4:
                    result = "平移Y";
                    break;
            }
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();

            simplePhotoRender.setAnimateType(resultType);
        });
        buttonStart.setOnClickListener(v -> {
            Choreographer.getInstance().postFrameCallback(this);
        });


    }

    private void initGL() {
        mGLView.setEGLContextClientVersion(2);
        simplePhotoRender = new SimplePhotoRender(this);
        mGLView.setRenderer(simplePhotoRender);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGLView != null) {
            mGLView.onPause();
        }
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @SuppressLint("CheckResult")
    @Override
    protected void onResume() {
        super.onResume();
        if (mGLView != null) {
            mGLView.onResume();
            Choreographer.getInstance().postFrameCallback(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 选取图片的返回值
        if (requestCode == 1) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                Uri uri = data.getData();
                Cursor cursor = null;
                try {
                    String[] filePathColumn = {MediaStore.Video.Media.DATA};

                    cursor = getContentResolver().query(uri,
                            filePathColumn, null, null, null);
                    if (cursor == null) {
                        return;
                    }
                    cursor.moveToFirst();
                    String filePath = cursor.getString(0); // 图片编号


                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    int height = bitmap.getHeight();
                    int width = bitmap.getWidth();
                    Toast.makeText(this, "bitmap height=" + height + ",width=" + width, Toast.LENGTH_SHORT).show();
                    simplePhotoRender.setBitmap(bitmap);

                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        final long frame = frameTimeNanos;
        if (startTime == 0) {
            startTime = frameTimeNanos;
            Choreographer.getInstance().postFrameCallback(this);
        } else {
            float difSec = (frameTimeNanos - startTime) * 1f / 1000000000;
            if (duration >= difSec && difSec > 0) {
                mGLView.queueEvent(() -> {
                    simplePhotoRender.doFrame(frame, difSec, duration);
                });
                mGLView.requestRender();
                Choreographer.getInstance().postFrameCallback(this);
            } else {
                startTime = 0;
                mGLView.queueEvent(() -> {
                    simplePhotoRender.doFrame(frame, 0, 0);
                });
                Choreographer.getInstance().removeFrameCallback(this);
            }
        }
    }
}
