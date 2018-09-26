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
import android.util.DisplayMetrics;
import android.view.Choreographer;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.cry.zero_camera.R;
import com.cry.zero_camera.activity.ppt.render.SimpleRender3;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;

import io.reactivex.disposables.CompositeDisposable;

public class PhotoAnimateSimpleActivity extends AppCompatActivity implements Choreographer.FrameCallback {
    private GLSurfaceView mGLView;
    private Button buttonStart;
    private SimpleRender3 renderController;
    //    private SimpleRender2 renderController;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private long startTime = 0;
    private long duration = 1;
    private boolean isRecord;
    private long pauseTime = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RxPermissions rxPermissions = new RxPermissions(this);
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
//        Executors
        initGL();

        findViewById(R.id.pick).setOnClickListener(v -> {
//            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
//            i.setType("image/*");
//            startActivityForResult(i, 1);

            startActivityForResult(new Intent(PhotoAnimateSimpleActivity.this, PickMorePicsActivity.class), 5);
        });

        findViewById(R.id.change).setOnClickListener(v -> {
            int animateType = renderController.getAnimateType();
            int resultType = 0;
            if (animateType == 4) {
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
                    result = "缩小";
                    break;
            }
            final int type = resultType;
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            mGLView.queueEvent(() -> renderController.setAnimateType(type));

        });

        buttonStart.setOnClickListener(v -> {
//            isRecord = true;
            Choreographer.getInstance().postFrameCallback(this);
//            mGLView.queueEvent(() -> renderController.changeRecordingState(isRecord));
        });


        String f1 = "/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1531208871527.png";
        String f2 = "/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1529911150337.png";
        String f3 = "/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1529734446397.png";
        addBitmap(f1);
        addBitmap(f2);
        addBitmap(f3);
    }

    private void initGL() {
        mGLView.setEGLContextClientVersion(2);
//        renderController = new SimpleRender2();
        renderController = new SimpleRender3();
        mGLView.setRenderer(renderController);
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
        }
        if (isRecord) {
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
//                    simplePhotoRender.setBitmap(bitmap);


                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        } else if (requestCode == 5) {
            ArrayList<String> filePaths = data.getStringArrayListExtra("bitmapPath");
//            ArrayList<Bitmap> bitmaps = new ArrayList<>();
            for (String filePath : filePaths) {
                addBitmap(filePath);
            }
            mGLView.requestRender();
        }
    }

    private void addBitmap(String filePath) {
        System.out.println("filePath=" + filePath);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        BitmapFactory.decodeFile(filePath, options);
        int widthPixels = displayMetrics.widthPixels / 2;
        System.out.println("widthPixels=" + widthPixels);
        int outWidth = options.outWidth;
        System.out.println("outWidth=" + outWidth);
        options.inJustDecodeBounds = false;
        int scale = ((int) (outWidth * 1f / widthPixels * 1f)) >> 1 << 1;
        options.inSampleSize = scale;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
//                bitmaps.add(bitmap);
        mGLView.queueEvent(() -> renderController.addBitmap(bitmap));
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        final long frame = frameTimeNanos;
        if (startTime == 0) {
            startTime = frameTimeNanos;
            Choreographer.getInstance().postFrameCallback(this);
        } else {
            float difSec = (frameTimeNanos - startTime) * 1f / 1000000000;
            if (duration >= difSec && difSec >= 0) {
                mGLView.queueEvent(() -> {
                    renderController.doFrame(frame, difSec, duration);
                });
                mGLView.requestRender();
                Choreographer.getInstance().postFrameCallback(this);
            } else if (duration + pauseTime >= difSec) {
                mGLView.queueEvent(() -> {
                    renderController.doFrame(frame, difSec, duration);
                });
                mGLView.requestRender();
                Choreographer.getInstance().postFrameCallback(this);
            } else {
                startTime = 0;
//                isRecord = false;
                mGLView.queueEvent(() -> {
//                    renderController.changeRecordingState(isRecord);
                    renderController.doFrame(frame, 0, 0);
                });
                mGLView.requestRender();
                Choreographer.getInstance().postFrameCallback(this);
//                Choreographer.getInstance().removeFrameCallback(this);
//                File outputFile = renderController.getOutputFile();
//                String result = outputFile.getAbsolutePath();
//                runOnUiThread(() -> Toast.makeText(this, "save file path = " + result, Toast.LENGTH_SHORT).show());
            }
        }
    }
}
