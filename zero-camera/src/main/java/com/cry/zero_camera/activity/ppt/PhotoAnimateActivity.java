package com.cry.zero_camera.activity.ppt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.cry.zero_camera.R;
import com.cry.zero_common.permission.ConfirmationDialogFragment;
import com.tbruyelle.rxpermissions2.RxPermissions;

public class PhotoAnimateActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 2;
    public PhotoAnimateRenderView mPhotoView;
    private FrameLayout mContainer;
    private RxPermissions rxPermissions;
    private boolean mRecordingEnabled;      // controls button state
    private Button buttonStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //去除状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_photo);
        mContainer = (FrameLayout) findViewById(R.id.container);
        rxPermissions = new RxPermissions(this);

        buttonStart = findViewById(R.id.fab);
        buttonStart.setOnClickListener(v -> {
            rxPermissions
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe(granted -> {
                        if (granted && mPhotoView != null) {
                            startOrStopToRecord();
                        }
                    });
        });
        findViewById(R.id.pick).setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            i.setType("image/*");
            startActivityForResult(i, 1);
        });

    }

    private void startOrStopToRecord() {
        mRecordingEnabled = !mRecordingEnabled;
        if (mRecordingEnabled) {
            buttonStart.setText("stop");
        } else {
            buttonStart.setText("start");
        }
        mPhotoView.changeRecordingState(mRecordingEnabled);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPhotoView != null) {
            mPhotoView.onPause();
        }
    }

    @SuppressLint("CheckResult")
    @Override
    protected void onResume() {
        super.onResume();
        if (mPhotoView != null) {
            mPhotoView.onResume();
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
        if (mPhotoView == null) {
            mPhotoView = new PhotoAnimateRenderView(this);
            mContainer.addView(mPhotoView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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

                    mPhotoView.setBitmap(bitmap);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
    }
}
