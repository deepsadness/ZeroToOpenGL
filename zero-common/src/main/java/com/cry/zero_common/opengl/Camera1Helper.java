package com.cry.zero_common.opengl;

import android.hardware.Camera;

/**
 * DESCRIPTION:
 * Author: Cry
 * DATE: 2018/5/9 下午10:07
 */

public class Camera1Helper {
    /**
     * 得到默认的CameraId.默认取后置摄像头
     *
     * @return CameraId
     */
    public static int getDefaultCameraId() {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        return 0;
    }
}
