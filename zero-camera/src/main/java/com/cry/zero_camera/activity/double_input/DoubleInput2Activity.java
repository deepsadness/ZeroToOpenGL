package com.cry.zero_camera.activity.double_input;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.cry.zero_camera.R;
import com.cry.zero_common.permission.ConfirmationDialogFragment;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;

public class DoubleInput2Activity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 2;
    public DoubleInputView mCameraView;
    private LinearLayout mContainer;
    private RxPermissions rxPermissions;
    private boolean mRecordingEnabled;
    private Button buttonStart;
    private File mCurrentFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //去除状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_double);
        mContainer = (LinearLayout) findViewById(R.id.container);
        rxPermissions = new RxPermissions(this);

        buttonStart = findViewById(R.id.fab);
        buttonStart.setOnClickListener(v -> {
            rxPermissions
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe(granted -> {
                        if (granted && mCameraView != null) {
                            startOrStopToRecord();
                        }
                    });
        });
        findViewById(R.id.pick).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent();
//                intent.setType("video/*"); //选择视频 （mp4 3gp 是android支持的视频格式）
//                intent.setAction(Intent.ACTION_GET_CONTENT);
//                /* 取得相片后返回本画面 */
//                startActivityForResult(intent, 1);
                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, 1);
            }
        });
    }

    private void startOrStopToRecord() {
        mRecordingEnabled = !mRecordingEnabled;
        if (mRecordingEnabled) {
            buttonStart.setText("stop");
            mCameraView.start(mCurrentFile);
//            Choreographer.getInstance().postFrameCallback(this);
        } else {
            buttonStart.setText("start");
            mCameraView.stop();
//            Choreographer.getInstance().removeFrameCallback(this);
        }
        mCameraView.changeRecordingState(mRecordingEnabled);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraView != null) {
            mCameraView.onPause();
        }
//        Choreographer.getInstance().removeFrameCallback(this);
    }

    @SuppressLint("CheckResult")
    @Override
    protected void onResume() {
        super.onResume();
        if (mCameraView != null) {
            mCameraView.onResume();
//            if (mRecordingEnabled) {
//                Choreographer.getInstance().postFrameCallback(this);
//            }
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
            mCameraView = new DoubleInputView(this);
            int width = getScreenWidth();
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mContainer.addView(mCameraView, params);
        }
    }

    private int getScreenWidth() {
        WindowManager wm = (WindowManager) this
                .getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay().getWidth();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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
                    Toast.makeText(this, "choose file path=" + filePath, Toast.LENGTH_SHORT).show();
                    mCurrentFile = new File(filePath);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }
    }

//
//    @Override
//    public void doFrame(long frameTimeNanos) {
//        mCameraView.requestRender();
//        Choreographer.getInstance().postFrameCallback(this);
//    }
}
