package com.cry.zero_camera.activity.video;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.cry.zero_camera.R;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;

public class VideoCaptureFilterActivity extends AppCompatActivity {
    public VideoCaptureFilterView mVideoView;
    private FrameLayout mContainer;
    private RxPermissions rxPermissions;
    private boolean mRecordingEnabled;      // controls button state
    private Button buttonStart;
    private File mCurrentFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //去除状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_decode);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.hide();
        }
        mContainer = (FrameLayout) findViewById(R.id.container);
        rxPermissions = new RxPermissions(this);
        addVideoView();
        buttonStart = findViewById(R.id.fab);
        buttonStart.setOnClickListener(v -> {
            rxPermissions
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe(granted -> {
                        if (granted && mVideoView != null) {
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
            mVideoView.start(mCurrentFile);
        } else {
            buttonStart.setText("start");
            mVideoView.stop();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView != null) {
            mVideoView.onPause();
        }
    }

    @SuppressLint("CheckResult")
    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null) {
            mVideoView.onResume();
        }
    }

    private void addVideoView() {
        if (mVideoView == null) {
            mVideoView = new VideoCaptureFilterView(this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.CENTER;
            mContainer.addView(mVideoView, params);
        }
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
}
