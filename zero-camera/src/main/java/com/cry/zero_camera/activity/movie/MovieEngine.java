package com.cry.zero_camera.activity.movie;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;

import com.cry.zero_camera.ref.VideoEncoderCore;
import com.cry.zero_camera.ref.gles.EglCore;
import com.cry.zero_camera.ref.gles.WindowSurface;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static com.cry.zero_camera.activity.movie.MovieMaker.ONE_BILLION;

public class MovieEngine extends HandlerThread {


    private final UiHandler uiHandler;
    int width;
    //    private FullFrameRect mFullScreen;
    int height;
    int bitRate;
    File outputFile;
    ProgressListener listener;
    ArrayList<MovieMaker> movieMakers;
    private EglCore mEglCore;
    private WindowSurface mWindowSurface;
    private MovieHandler mMovieHandler;
    private VideoEncoderCore mVideoEncoder;
    private long[] timeSections;
    private boolean stop = false;

    private MovieEngine() {
        super("MovieEngine-thread");
        uiHandler = new UiHandler();
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        //初始化handler
        getMovieHandler();
    }

    private MovieHandler getMovieHandler() {
        if (mMovieHandler == null) {
            mMovieHandler = new MovieHandler(getLooper(), this);
        }
        return mMovieHandler;
    }

    public void make() {
        synchronized (this) {
            getMovieHandler().sendEmptyMessage(MovieHandler.MSG_MAKE_MOVIES);
        }
    }

    private void makeMovie() {
        //不断绘制。
        boolean isCompleted = false;
        try {
            //初始化GL环境
            mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE);

            mVideoEncoder = new VideoEncoderCore(width, height, bitRate, outputFile);
            Surface encoderInputSurface = mVideoEncoder.getInputSurface();
            mWindowSurface = new WindowSurface(mEglCore, encoderInputSurface, true);
            mWindowSurface.makeCurrent();
//
//            mFullScreen = new FullFrameRect(
//                    new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D));

            //绘制
//            计算时长
            long totalDuration = 0;
            timeSections = new long[movieMakers.size()];
            for (int i = 0; i < movieMakers.size(); i++) {
                MovieMaker movieMaker = movieMakers.get(i);
                movieMaker.onGLCreate();
                movieMaker.setSize(width, height);
                timeSections[i] = totalDuration;
                totalDuration += movieMaker.getDurationAsNano();
            }
            if (listener != null) {
                uiHandler.post(() -> {
                    listener.onStart();
                });
            }
            long tempTime = 0;
            int frameIndex = 0;
            while (tempTime <= totalDuration + ONE_BILLION / 30) {
                mVideoEncoder.drainEncoder(false);
                generateFrame(tempTime);
                long presentationTimeNsec = computePresentationTimeNsec(frameIndex);
                submitFrame(presentationTimeNsec);
                updateProgress(tempTime, totalDuration);
                frameIndex++;
                tempTime = presentationTimeNsec;

                if (stop) {
                    break;
                }
            }
            System.out.println("total frames =" + (frameIndex));
            //finish
            mVideoEncoder.drainEncoder(true);
            isCompleted = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //结束
            try {
                releaseEncoder();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (isCompleted && listener != null) {
                uiHandler.post(() -> {
                    listener.onCompleted(outputFile.getAbsolutePath());
                });
            }
        }

    }

    private void updateProgress(final long tempTime, final long totalDuration) {
        if (listener != null) {
            uiHandler.post(() -> listener.onProgress(tempTime, totalDuration));
        }
    }

    private void submitFrame(long presentationTimeNsec) {
        mWindowSurface.setPresentationTime(presentationTimeNsec);
        mWindowSurface.swapBuffers();
    }

    //fps 30
    private long computePresentationTimeNsec(int frameIndex) {
        final long ONE_BILLION = 1000000000;
        return frameIndex * ONE_BILLION / 30;
    }

    private void generateFrame(long tempTime) {
        int movieIndex = 0;
        boolean find = false;
        for (int i = 0; i < timeSections.length; i++) {
            if (i + 1 < timeSections.length && tempTime >= timeSections[i] && tempTime < timeSections[i + 1]) {
                find = true;
                movieIndex = i;
                break;
            }
        }
        if (!find) {
            movieIndex = timeSections.length - 1;
        }
        long curTime = tempTime - timeSections[movieIndex];
        MovieMaker movieMaker = movieMakers.get(movieIndex);
        movieMaker.generateFrame(curTime);
    }

    private void releaseEncoder() {
        mVideoEncoder.release();
        if (mWindowSurface != null) {
            mWindowSurface.release();
            mWindowSurface = null;
        }
//        if (mFullScreen != null) {
//            mFullScreen.release(false);
//            mFullScreen = null;
//        }
        for (int i = 0; i < movieMakers.size(); i++) {
            movieMakers.get(i).release();
        }

        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

    @Override
    public boolean quit() {
        stop = true;

        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
        return super.quit();
    }

    public interface ProgressListener {

        void onStart();

        void onCompleted(String path);

        void onProgress(long current, long totalDuration);

    }

    public static class MovieBuilder {
        private int width = 1280;
        private int height = 720;
        private int bitRate = 10000000;
        private File outputFile;
        private ProgressListener listener;
        private ArrayList<MovieMaker> movieMakers = new ArrayList<>();

        public MovieBuilder width(int width) {
            this.width = width;
            return this;
        }

        public MovieBuilder height(int height) {
            this.height = height;
            return this;
        }

        public MovieBuilder bitRate(int bitRate) {
            this.bitRate = bitRate;
            return this;
        }

        public MovieBuilder outputFile(File outputFile) {
            this.outputFile = outputFile;
            return this;
        }

        public MovieBuilder listener(ProgressListener listener) {
            this.listener = listener;
            return this;
        }

        public MovieBuilder maker(MovieMaker maker) {
            movieMakers.add(maker);
            return this;
        }

        public MovieEngine build() {
            MovieEngine engine = new MovieEngine();

            engine.width = width;
            engine.height = height;
            engine.bitRate = bitRate;

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd-HH:mm:ss", Locale.ENGLISH);
            String format = simpleDateFormat.format(new Date());

            if (outputFile == null) {
                outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "movie-" + format + ".mp4");
            }
            if (!outputFile.getParentFile().exists()) {
                boolean mkdir = outputFile.getParentFile().mkdir();
            }

            engine.outputFile = outputFile;
            engine.listener = listener;
            engine.movieMakers = new ArrayList<>(movieMakers.size());
            engine.movieMakers.addAll(movieMakers);
            engine.start();
            return engine;
        }
    }

    public static class MovieHandler extends Handler {

        public static final int MSG_MAKE_MOVIES = 1;
        private final WeakReference<MovieEngine> engineRef;


        public MovieHandler(Looper looper, MovieEngine engine) {
            super(looper);
            engineRef = new WeakReference<>(engine);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            MovieEngine engine = engineRef.get();
            if (engine == null) {
                return;
            }
            switch (msg.what) {
                case MSG_MAKE_MOVIES:
                    engine.makeMovie();
                    break;
            }
        }
    }

    public static class UiHandler extends Handler {
        public UiHandler() {
            super(Looper.getMainLooper());
        }
    }
}
