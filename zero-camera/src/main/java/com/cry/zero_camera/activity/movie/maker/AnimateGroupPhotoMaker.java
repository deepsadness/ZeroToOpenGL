package com.cry.zero_camera.activity.movie.maker;

import android.graphics.Bitmap;
import android.opengl.Matrix;

import com.cry.zero_camera.activity.movie.MovieMaker;
import com.cry.zero_camera.render.fliter.PhotoAlphaFilter2;
import com.cry.zero_common.BitmapHelper;

import java.util.ArrayList;
import java.util.Arrays;

public class AnimateGroupPhotoMaker implements MovieMaker {
    ArrayList<PhotoAlphaFilter2> photoFilters;

    long startTime = 0;
    ArrayList<String> filePaths;
    float[] srcMatrix = null;
    private int width;

    public AnimateGroupPhotoMaker(String... filePaths) {
        this.filePaths = new ArrayList<>();
        this.filePaths.addAll(Arrays.asList(filePaths));
    }

    @Override
    public void onGLCreate() {
        this.photoFilters = new ArrayList<>(2);
        ;
        photoFilters.add(new PhotoAlphaFilter2());
        photoFilters.add(new PhotoAlphaFilter2());
        for (PhotoAlphaFilter2 photoFilter : photoFilters) {
            photoFilter.onCreate();
        }
    }

    @Override
    public void setSize(int width, int height) {
        this.width = width;
        for (int i = 0; i < photoFilters.size(); i++) {
            PhotoAlphaFilter2 photoFilter = photoFilters.get(i);
            String filePath = filePaths.get(i);
            Bitmap bitmap = BitmapHelper.decodeBitmap(520, filePath);
            photoFilter.onSizeChange(width, height);
            photoFilter.setBitmap(bitmap);
        }
    }

    @Override
    public long getDurationAsNano() {
        return (long) (0.35 * ONE_BILLION);
    }

    @Override
    public void generateFrame(long curTime) {
        if (curTime == 0) {
            startTime = curTime;
        }
        float dif = (curTime - startTime) * 1f / getDurationAsNano();
        for (int i = 0; i < photoFilters.size(); i++) {
            PhotoAlphaFilter2 photoFilter = photoFilters.get(i);
            transform(photoFilter, dif, i);
            photoFilter.onDrawFrame();
        }
    }

    //进行动画的变化
    private void transform(PhotoAlphaFilter2 photoFilter, float dif, int i) {
        System.out.println("dif = " + dif);
        if (srcMatrix == null) {
            srcMatrix = photoFilter.getMVPMatrix();
        }
        float[] mModelMatrix = Arrays.copyOf(srcMatrix, 16);
        float v;
        switch (i) {
            case 0:
                v = 1f - dif * 0.1f;
                Matrix.scaleM(mModelMatrix, 0, v, v, 0f);
                photoFilter.setAlpha(1 - dif * 0.5f);
                break;
            case 1:
                v = 2 - dif * 2f;
                int offset = (int) (width * (v / 2));
                System.out.println("translateM v = " + v);
                Matrix.translateM(mModelMatrix, 0, v, 0f, 0f);
                break;
        }
        if (i == 1 && dif >= 0.9666667) {
            photoFilter.setMVPMatrix(srcMatrix);
        } else {
            photoFilter.setMVPMatrix(mModelMatrix);
        }
    }

    @Override
    public void release() {
        for (int i = 0; i < photoFilters.size(); i++) {
            PhotoAlphaFilter2 photoFilter = photoFilters.get(i);
            photoFilter.release();
        }
    }
}
