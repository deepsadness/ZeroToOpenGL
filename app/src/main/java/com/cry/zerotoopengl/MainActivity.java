package com.cry.zerotoopengl;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.cry.zero_camera.activity.camera_filter.CameraCaptureFilterActivity;
import com.cry.zero_camera.activity.capture.CameraCaptureActivity;
import com.cry.zero_camera.activity.double_input.DoubleInput2Activity;
import com.cry.zero_camera.activity.ppt.backgroud.GenerateMovieActivity;
import com.cry.zero_camera.activity.video.VideoCaptureFilterActivity;
import com.cry.zero_camera.preview.CameraActivity;

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

    public void ToZeroDoubleInput2(View view) {
        startActivity(new Intent(this, DoubleInput2Activity.class));
    }

//    public void ToPhotoAnimate(View view) {
//        startActivity(new Intent(this, PhotoAnimateSimpleActivity.class));
//    }
//
//    public void ToRecordFBOActivity(View view) {
//        startActivity(new Intent(this, RecordFBOActivity.class));
//    }


    public void startGenerate(View view) {
        startActivity(new Intent(this, GenerateMovieActivity.class));
    }
}
