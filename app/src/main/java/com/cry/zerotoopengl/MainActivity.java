package com.cry.zerotoopengl;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.cry.zero_camera.camera_filter.CameraCaptureFilterActivity;
import com.cry.zero_camera.capture.CameraCaptureActivity;
import com.cry.zero_camera.preview.CameraActivity;
import com.cry.zero_camera.video_source.VideoCaptureFilterActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void ToZeroCamera(View view) {
        startActivity(new Intent(this, CameraActivity.class));
    }

    public void ToZeroCameraCapture(View view) {
        startActivity(new Intent(this, CameraCaptureActivity.class));
    }

    public void ToZeroCameraCaptureFilter(View view) {
        startActivity(new Intent(this, CameraCaptureFilterActivity.class));
    }

    public void ToZeroVideoCaptureFilter(View view) {
        startActivity(new Intent(this, VideoCaptureFilterActivity.class));
    }
}
