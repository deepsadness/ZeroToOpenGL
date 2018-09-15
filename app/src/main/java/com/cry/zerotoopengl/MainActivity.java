package com.cry.zerotoopengl;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.cry.zero_camera.CameraActivity;
import com.cry.zero_camera.CameraCaptureActivity;

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
}
