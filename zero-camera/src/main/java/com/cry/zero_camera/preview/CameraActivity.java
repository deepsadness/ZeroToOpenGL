package com.cry.zero_camera.preview;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.cry.zero_camera.R;
import com.cry.zero_common.permission.ConfirmationDialogFragment;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.nio.ByteBuffer;

public class CameraActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 2;
    private FrameLayout mContainer;
    //    public CameraViewO mCameraView;
    public CameraView mCameraView;
    private RxPermissions rxPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //去除状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        mContainer = (FrameLayout) findViewById(R.id.container);
        rxPermissions = new RxPermissions(this);

        findViewById(R.id.fab).setOnClickListener(v -> {
            rxPermissions
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe(granted -> {
                        if (granted) {
                            takePhoto();
                        }
                    });
        });

    }

    private void takePhoto() {
        if (mCameraView != null) {
            mCameraView.takePhoto((bytes, width, height) -> {
                long end = System.currentTimeMillis();
                Bitmap bitmap = null;
                if (mCameraView.takePhotoFromGL != 1) {
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                    String fileName = "photo" + end + ".png";
//                    FileUtils.saveFile(bitmap, fileName);

                    final Bitmap finalBitmap = bitmap;
                    runOnUiThread(() -> {
                        CameraActivity context = CameraActivity.this;
                        ImageView imageView = new ImageView(context);
                        imageView.setImageBitmap(finalBitmap);
                        //因为读到的图上下翻转了。所以scale
                        imageView.setRotation(90);
                        new AlertDialog.Builder(context).setView(imageView).setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
//                    Toast.makeText(this, "save success!!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    //这里这个是从GL中读取现存
                    //从GL中读取的换成是上下颠倒的
                    ByteBuffer wrap = ByteBuffer.wrap(bytes);
                    String fileName = "photo" + end + ".png";
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(wrap);
//                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), fileName);
//                    FileUtils.saveRgb2Bitmap(wrap, file, width, height);
                    final Bitmap finalBitmap = bitmap;
                    runOnUiThread(() -> {
                        CameraActivity context = CameraActivity.this;
                        ImageView imageView = new ImageView(context);
                        imageView.setImageBitmap(finalBitmap);
                        //因为读到的图上下翻转了。所以scale
                        imageView.setScaleY(-1);
                        new AlertDialog.Builder(context).setView(imageView).setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
//                    Toast.makeText(this, "save success!!", Toast.LENGTH_SHORT).show();
                    });
                }

            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraView != null) {
            mCameraView.onPause();
        }
    }

    @SuppressLint("CheckResult")
    @Override
    protected void onResume() {
        super.onResume();
        if (mCameraView != null) {
            mCameraView.onResume();
        } else {
            rxPermissions
                    .request(Manifest.permission.CAMERA)
                    .subscribe(granted -> {
                        if (granted) {
                            startCamera();
                        } else {
                            ConfirmationDialogFragment
                                    .newInstance("camera_permission_confirmation",
                                            new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                            REQUEST_CAMERA_PERMISSION,
                                            "camera_permission_not_granted")
                                    .show(getSupportFragmentManager(), "FRAGMENT_DIALOG");
                        }
                    });
        }
    }

    private void startCamera() {
        if (mCameraView == null) {
//            mCameraView = new CameraViewO(this);
            mCameraView = new CameraView(this);
            mContainer.addView(mCameraView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }
}
