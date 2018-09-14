package com.cry.zero_camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.cry.zero_common.permission.ConfirmationDialogFragment;
import com.tbruyelle.rxpermissions2.RxPermissions;

public class CameraActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 2;
    private FrameLayout mContainer;
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
            mCameraView = new CameraView(this);
            mContainer.addView(mCameraView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }
}
