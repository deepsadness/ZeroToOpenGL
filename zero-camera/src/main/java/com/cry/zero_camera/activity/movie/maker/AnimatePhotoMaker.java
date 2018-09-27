package com.cry.zero_camera.activity.movie.maker;

import android.graphics.Bitmap;
import android.opengl.Matrix;

import com.cry.zero_camera.activity.movie.MovieMaker;
import com.cry.zero_camera.render.fliter.PhotoFilter;
import com.cry.zero_common.BitmapHelper;

import java.util.Arrays;

public class AnimatePhotoMaker implements MovieMaker {
    PhotoFilter photoFilter;
    long startTime = 0;
    String filePath;
    int animateType = 2;
    float[] srcMatrix = null;
    private int width;

    public AnimatePhotoMaker(String filePath) {
        this.filePath = filePath;
    }

    public AnimatePhotoMaker(String filePath, int animateType) {
        this.filePath = filePath;
        this.animateType = animateType;
    }

    @Override
    public void onGLCreate() {
        photoFilter = new PhotoFilter();
        photoFilter.onCreate();
    }

    @Override
    public void setSize(int width, int height) {
        this.width = width;
        photoFilter.onSizeChange(width, height);
        Bitmap bitmap = BitmapHelper.decodeBitmap(520, filePath);
        photoFilter.setBitmap(bitmap);
    }

    @Override
    public long getDurationAsNano() {
        return 3 * ONE_BILLION;
    }

    @Override
    public void generateFrame(long curTime) {
        if (curTime == 0) {
            startTime = curTime;
        }
        float dif = (curTime - startTime) * 1f / getDurationAsNano();
        transform(dif);
        photoFilter.onDrawFrame();
    }

    //进行动画的变化
    private void transform(float dif) {
        System.out.println("dif = " + dif);
        if (srcMatrix == null) {
            srcMatrix = photoFilter.getMVPMatrix();
        }
        float[] mModelMatrix = Arrays.copyOf(srcMatrix, 16);
        float v;
        switch (animateType) {
            case 0:
                v = dif * 0.2f + 1f;
                Matrix.scaleM(mModelMatrix, 0, v, v, 0f);
                break;
            case 1:

                v = 1f - dif * 0.2f;
                Matrix.scaleM(mModelMatrix, 0, v, v, 0f);
                break;
            case 2:
                if (dif >= 0.9888889) {
                    break;
                }
                v = 2 - dif * 2;
                int offset = (int) (width * (v / 2));
                System.out.println("translateM v = " + v);
                Matrix.translateM(mModelMatrix, 0, v, 0f, 0f);
                break;
        }
        photoFilter.setMVPMatrix(mModelMatrix);
    }

    @Override
    public void release() {
        photoFilter.release();
    }
}
