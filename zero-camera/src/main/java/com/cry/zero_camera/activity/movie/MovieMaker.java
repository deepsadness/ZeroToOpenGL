package com.cry.zero_camera.activity.movie;

public interface MovieMaker {

    long ONE_BILLION = 1000000000;

    void onGLCreate();

    void setSize(int width, int height);

    long getDurationAsNano();

    void generateFrame(long curTime);

    void release();


}
