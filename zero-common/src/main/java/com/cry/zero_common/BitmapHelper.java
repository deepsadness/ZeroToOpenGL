package com.cry.zero_common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapHelper {
    public static Bitmap decodeBitmap(int targetWidth, String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        int outWidth = options.outWidth;
        options.inJustDecodeBounds = false;
        options.inSampleSize = ((int) (outWidth * 1f / targetWidth * 1f)) >> 1 << 1;
        return BitmapFactory.decodeFile(filePath, options);
    }
}
