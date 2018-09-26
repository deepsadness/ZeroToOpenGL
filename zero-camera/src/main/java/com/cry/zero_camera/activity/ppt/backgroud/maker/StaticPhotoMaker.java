package com.cry.zero_camera.activity.ppt.backgroud.maker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.cry.zero_camera.activity.ppt.backgroud.MovieMaker;
import com.cry.zero_camera.render.fliter.PhotoFilter;

public class StaticPhotoMaker implements MovieMaker {
    PhotoFilter photoFilter;

    String filePath;

    public StaticPhotoMaker(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void onGLCreate() {
        photoFilter = new PhotoFilter();
        photoFilter.onCreate();
    }

    @Override
    public void setSize(int width, int height) {
        photoFilter.onSizeChange(width, height);
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        photoFilter.setBitmap(bitmap);
    }

    @Override
    public long getDurationAsNano() {
        return 3 * ONE_BILLION;
    }

    @Override
    public void generateFrame(long curTime) {
        photoFilter.onDrawFrame();
    }

    @Override
    public void release() {
        photoFilter.release();
    }
}
