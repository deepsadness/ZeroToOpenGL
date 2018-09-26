package com.cry.zero_camera.activity.ppt.backgroud;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.cry.zero_camera.R;
import com.cry.zero_camera.activity.ppt.backgroud.maker.AnimateGroupPhotoMaker;
import com.cry.zero_camera.activity.ppt.backgroud.maker.StaticPhotoMaker;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.disposables.CompositeDisposable;

public class GenerateMovieActivity extends AppCompatActivity {
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private MovieEngine engine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_movie);
        getSupportActionBar().hide();

        RxPermissions rxPermissions = new RxPermissions(this);
        compositeDisposable.add(rxPermissions
                .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe());
    }

    @SuppressLint("StaticFieldLeak")
    public void startGenerate(View view) {
        engine = new MovieEngine.MovieBuilder()
//                .maker(new AnimatePhotoMaker("/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1529911150337.png", 1))
                .maker(new StaticPhotoMaker("/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1529734446397.png"))
                .maker(new AnimateGroupPhotoMaker("/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1529734446397.png", "/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1531208871527.png"))
                .maker(new StaticPhotoMaker("/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1531208871527.png"))
                .maker(new AnimateGroupPhotoMaker("/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1531208871527.png", "/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1529911150337.png"))
                .maker(new StaticPhotoMaker("/storage/emulated/0/tencent/MicroMsg/WeiXin/mmexport1529911150337.png"))
                .width(720)
                .height(1280)
                .listener(new MovieEngine.ProgressListener() {

                    private ProgressDialog progressDialog;
                    private long startTime;

                    @Override
                    public void onStart() {
                        startTime = System.currentTimeMillis();
                        Toast.makeText(GenerateMovieActivity.this, "onStart!!", Toast.LENGTH_SHORT).show();
                        progressDialog = new ProgressDialog(GenerateMovieActivity.this);
                        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        progressDialog.show();
                        progressDialog.setMax(100);
                    }

                    @Override
                    public void onCompleted(String absolutePath) {
                        progressDialog.hide();
                        long endTime = System.currentTimeMillis();
                        Toast.makeText(GenerateMovieActivity.this, "file path=" + absolutePath + ",cost time = " + (endTime - startTime), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProgress(long current, long totalDuration) {
                        float progress = current * 1f / totalDuration * 1f;
                        progressDialog.setProgress((int) (progress * 100));
                    }
                }).build();
        engine.make();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        engine.quit();
    }
}
