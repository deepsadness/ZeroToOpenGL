package com.cry.zero_camera.camera;

import android.graphics.SurfaceTexture;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.util.SparseArrayCompat;

/**
 * 定义个相机的功能接口
 */
public interface ICamera {
    boolean open(int cameraId);

    /**
     * 设置画面的比例
     */
    void setAspectRatio(AspectRatio aspectRatio);

    /**
     * 开启预览
     */
    boolean preview();

    /**
     * 关闭相机
     *
     * @return
     */
    boolean close();

    /**
     * 使用SurfaceTexture 来作为预览的画面
     *
     * @param surfaceTexture
     */
    void setPreviewTexture(SurfaceTexture surfaceTexture);

    CameraSize getPreviewSize();
    CameraSize getPictureSize();
}
