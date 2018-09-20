package com.cry.zero_camera.activity.camera_filter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.cry.zero_camera.R;
import com.cry.zero_common.permission.ConfirmationDialogFragment;
import com.tbruyelle.rxpermissions2.RxPermissions;

public class CameraCaptureFilterActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 2;
    private FrameLayout mContainer;
    //    public CameraViewO mCameraView;
    public CameraCaptureFilterView mCameraView;
    private RxPermissions rxPermissions;
    private boolean mRecordingEnabled;      // controls button state
    private Button buttonStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //去除状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        mContainer = (FrameLayout) findViewById(R.id.container);
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

    }

    private void startOrStopToRecord() {
        mRecordingEnabled = !mRecordingEnabled;
        if (mRecordingEnabled) {
            buttonStart.setText("stop");
        } else {
            buttonStart.setText("start");
        }
        mCameraView.changeRecordingState(mRecordingEnabled);
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
            mCameraView = new CameraCaptureFilterView(this);
            mContainer.addView(mCameraView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }
}
