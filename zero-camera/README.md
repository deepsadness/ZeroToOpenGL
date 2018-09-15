## 整体流程理解
---
![image.png](https://github.com/deepsadness/ZeroToOpenGL/blob/master/art/camera_with_opengl.png)

1. 将`Camera`中得到的`ImageStream`由`SurfaceTexture`接受，并转换成`OpenGL ES`纹理。
2. 创建`GLSurfaceView`。在`OpenGL`环境下,用`GLSurfaceView.Render`将这个纹理绘制出来。
3. 整体的`ImageStream`的流向就是
```
Camera ==>SurfaceTexture==>texture(samplerExternalOES) ==>draw to GLSurfaceView
```



## 各个部分详解
---
### Camra Api
首先是相机的Api的书写。
#### Camera Interface
为我们相机的操作定义一个接口。因为我们的相机Api。有Camera2和Camera的区别。这里还是简单的使用Camera来完成。

```java
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
```
定义一个相机的接口。我们知道。我们需要相机做的几个通常的操作。
#### CameraApi14
```java
/**
 * for api 14
 * <p>
 * Camera主要涉及参数
 * 1. 预览画面的大小
 * 2. pic图片的大小
 * 3. 对焦模式
 * 4. 闪光灯模式
 */
public class CameraApi14 implements ICamera {
    /*
    当前的相机Id
     */
    private int mCameraId;
    /*
    当前的相机对象
     */
    private Camera mCamera;
    /*
    当前的相机参数
     */
    private Camera.Parameters mCameraParameters;


    //想要的尺寸。
    private int mDesiredHeight = 1920;
    private int mDesiredWidth = 1080;
    private boolean mAutoFocus;
    public CameraSize mPreviewSize;
    public CameraSize mPicSize;
    /*
     * 当前相机的高宽比
     */
    private AspectRatio mDesiredAspectRatio;


    public CameraApi14() {
        mDesiredHeight = 1920;
        mDesiredWidth = 1080;
        //创建默认的比例.因为后置摄像头的比例，默认的情况下，都是旋转了270
        mDesiredAspectRatio = AspectRatio.of(mDesiredWidth, mDesiredHeight).inverse();
    }

    @Override
    public boolean open(int cameraId) {
        /*
            预览的尺寸和照片的尺寸
        */
        final CameraSize.ISizeMap mPreviewSizes = new CameraSize.ISizeMap();
        final CameraSize.ISizeMap mPictureSizes = new CameraSize.ISizeMap();
        if (mCamera != null) {
            releaseCamera();
        }
        mCameraId = cameraId;
        mCamera = Camera.open(mCameraId);
        if (mCamera != null) {
            mCameraParameters = mCamera.getParameters();

            mPreviewSizes.clear();
            //先收集参数.因为每个手机能够得到的摄像头参数都不一致。所以将可能的尺寸都得到。
            for (Camera.Size size : mCameraParameters.getSupportedPreviewSizes()) {
                mPreviewSizes.add(new CameraSize(size.width, size.height));
            }

            mPictureSizes.clear();
            for (Camera.Size size : mCameraParameters.getSupportedPictureSizes()) {
                mPictureSizes.add(new CameraSize(size.width, size.height));
            }
            //挑选出最需要的参数
            adJustParametersByAspectRatio2(mPreviewSizes, mPictureSizes);
            return true;
        }
        return false;
    }

    private void adJustParametersByAspectRatio(CameraSize.ISizeMap previewSizes, CameraSize.ISizeMap pictureSizes) {
        //得到当前预期比例的size
        SortedSet<CameraSize> sizes = previewSizes.sizes(mDesiredAspectRatio);
        if (sizes == null) {  //表示不支持.
            // TODO: 2018/9/14 这里应该抛出异常？
            return;
        }
        //当前先不考虑Orientation
        CameraSize previewSize;
        mPreviewSize = new CameraSize(mDesiredWidth, mDesiredHeight);
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mPreviewSize = new CameraSize(mDesiredHeight, mDesiredWidth);
            mCameraParameters.setRotation(90);
        } else {
//            previewSize = mPreviewSize;
        }

        //默认去取最大的尺寸
        mPicSize = pictureSizes.sizes(mDesiredAspectRatio).first();

        mCameraParameters.setPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mCameraParameters.setPictureSize(mPicSize.getWidth(), mPicSize.getHeight());

        //设置对角和闪光灯
        setAutoFocusInternal(mAutoFocus);
        //先不设置闪光灯
//        mCameraParameters.setFlashMode("FLASH_MODE_OFF");

        //设置到camera中
//        mCameraParameters.setRotation(90);
        mCamera.setParameters(mCameraParameters);
//        mCamera.setDisplayOrientation(90);
//        setCameraDisplayOrientation();
    }

    private void adJustParametersByAspectRatio2(CameraSize.ISizeMap previewSizes, CameraSize.ISizeMap pictureSizes) {
        //得到当前预期比例的size
        SortedSet<CameraSize> sizes = previewSizes.sizes(mDesiredAspectRatio);
        if (sizes == null) {  //表示不支持.
            // TODO: 2018/9/14 这里应该抛出异常？
            return;
        }
        //当前先不考虑Orientation
        mPreviewSize = sizes.first();
        //默认去取最大的尺寸
        mPicSize = pictureSizes.sizes(mDesiredAspectRatio).first();
        mCameraParameters.setPreviewSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mCameraParameters.setPictureSize(mPicSize.getWidth(), mPicSize.getHeight());

        mPreviewSize = mPreviewSize.inverse();
        mPicSize = mPicSize.inverse();
        //设置对角和闪光灯
        setAutoFocusInternal(mAutoFocus);
        //先不设置闪光灯
//        mCameraParameters.setFlashMode("FLASH_MODE_OFF");

        //设置到camera中
//        mCameraParameters.setRotation(90);
        mCamera.setParameters(mCameraParameters);
//        mCamera.setDisplayOrientation(90);
//        setCameraDisplayOrientation();
    }

    private boolean setAutoFocusInternal(boolean autoFocus) {
        mAutoFocus = autoFocus;
//        if (isCameraOpened()) {
        final List<String> modes = mCameraParameters.getSupportedFocusModes();
        if (autoFocus && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
            mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
        } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
            mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
        } else {
            mCameraParameters.setFocusMode(modes.get(0));
        }
        return true;
//        } else {
//            return false;
//        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void setAspectRatio(AspectRatio aspectRatio) {
        this.mDesiredAspectRatio = aspectRatio;
    }

    @Override
    public boolean preview() {
        if (mCamera != null) {
            mCamera.startPreview();
            return true;
        }
        return false;
    }

    @Override
    public boolean close() {
        if (mCamera != null) {
            try {
                //stop preview时，可能爆出异常
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return false;
    }

    @Override
    public void setPreviewTexture(SurfaceTexture surfaceTexture) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(surfaceTexture);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public CameraSize getPreviewSize() {
        return mPreviewSize;
    }

    @Override
    public CameraSize getPictureSize() {
        return mPicSize;
    }
}

```
这里的代码就是使用`Camera`来实现上面的功能。

1. 因为使用我们期望将Camera中得到的数据传递到纹理上，所以需要`setPreviewTexture(SurfaceTexture texture)`。让这个`SurfaceTexture`来承载。

```java
@Override
    public void setPreviewTexture(SurfaceTexture surfaceTexture) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(surfaceTexture);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
```
2. 选择相机的预览的尺寸和旋转的角度
相机的`parameter`的选择，只要选对了对应的想要的比例就行了。没有其他需要的。
设备坐标和纹理坐标之间的方向不同问题，由后面纹理的矩阵来控制就好了。

---
### SurfaceTexture
可以从图像流中捕获帧作为`OpenGL ES`纹理。
1. 直接使用创建的纹理，来创建SurfaceTexture就可以了。
```java
mSurfaceTexture = new SurfaceTexture(mTextureId);
```
2. 然后再将其设置给Camera.同时每次SurfaceTexture刷新的时候，都必须刷新GLSurfaceView。
```java
  mCameraApi.setPreviewTexture(mCameraDrawer.getSurfaceTexture());
  //默认使用的GLThread.每次刷新的时候，都强制要求是刷新这个GLSurfaceView
  mCameraDrawer.getSurfaceTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                requestRender();
            }
        });
```
#### 注意事项
使用时必须要注意的是
1. **纹理对象使用`GL_TEXTURE_EXTERNAL_OES`纹理目标，该目标由[GL_OES_EGL_image_external](http://www.khronos.org/registry/gles/extensions/OES/OES_EGL_image_external.txt)OpenGL ES扩展定义。**
每次绑定纹理时，它必须绑定到`GL_TEXTURE_EXTERNAL_OES`目标而不是`GL_TEXTURE_2D`目标。在OpenGL ES 2.0着色器必须使用
```glsl
#extension GL_OES_EGL_image_external：require
```
2. 着色器还必须使用samplerExternalOES GLSL采样器类型访问纹理。
```glsl
uniform samplerExternalOES uTexture;
```


---
### GLSurfaceView.Render
#### GLSL部分
- **oes_base_vertex.glsl**
```glsl
attribute vec4 aPosition;
attribute vec2 aCoordinate;
uniform mat4 uMatrix;
uniform mat4 uCoordinateMatrix;
varying vec2 vTextureCoordinate;

void main(){
    gl_Position = uMatrix*aPosition;
    vTextureCoordinate = (uCoordinateMatrix*vec4(aCoordinate,0.1,0.1)).xy;
}
```
顶点着色器对比相对简单。这里需要注意的就是矩阵相乘的顺序问题。
```glsl
//这个是正确的顺序。相称的顺序相反，图像是反的！！！
gl_Position = uMatrix*aPosition;
```

- **oes_base_fragment.glsl**
这里就是如上面注意事项中说的。必须使用`samplerExternalOES `来采样。
```glsl
#extension GL_OES_EGL_image_external : require
precision mediump float;

varying vec2 vTextureCoordinate;
uniform samplerExternalOES uTexture;
void main() {
    gl_FragColor = texture2D(uTexture,vTextureCoordinate);
}
```
#### 其他的部分
其他的部分和前几编文章中提到的相差不多。
##### 0. 生成纹理
这里就是上面所说的。**只能用`GLES11Ext.GL_TEXTURE_EXTERNAL_OES`这种纹理。**
```java
 private int genOesTextureId() {
        int[] textureObjectId = new int[1];
        GLES20.glGenTextures(1, textureObjectId, 0);
        //绑定纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureObjectId[0]);
        //设置放大缩小。设置边缘测量
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return textureObjectId[0];
    }
```
##### 1. 视图矩阵。
因为设备坐标和纹理的坐标不同。而前置摄像头和后置摄像头的翻转的方向也不同。所以要做下面的处理
```java
   //计算需要变化的矩阵
    private void calculateMatrix() {
        //得到通用的显示的matrix
        Gl2Utils.getShowMatrix(mModelMatrix, mPreviewWidth, mPreviewHeight, this.mSurfaceWidth, this.mSurfaceHeight);

        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {  //前置摄像头
            Gl2Utils.flip(mModelMatrix, true, false);
            Gl2Utils.rotate(mModelMatrix, 90);
        } else {  //后置摄像头
            int rotateAngle = 270;
            Gl2Utils.rotate(mModelMatrix, rotateAngle);
        }
        mOesFilter.setMatrix(mModelMatrix);
    }
```
1. 得到标准的透视视图矩阵
```java
 public static void getShowMatrix(float[] matrix,int imgWidth,int imgHeight,int viewWidth,int
        viewHeight){
        if(imgHeight>0&&imgWidth>0&&viewWidth>0&&viewHeight>0){
            float sWhView=(float)viewWidth/viewHeight;
            float sWhImg=(float)imgWidth/imgHeight;
            float[] projection=new float[16];
            float[] camera=new float[16];
            if(sWhImg>sWhView){
                Matrix.orthoM(projection,0,-sWhView/sWhImg,sWhView/sWhImg,-1,1,1,3);
            }else{
                Matrix.orthoM(projection,0,-1,1,-sWhImg/sWhView,sWhImg/sWhView,1,3);
            }
            Matrix.setLookAtM(camera,0,0,0,1,0,0,0,0,1,0);
            Matrix.multiplyMM(matrix,0,projection,0,camera,0);
        }
    }
```
这部分就是标准的处理方式了。谁的比例大，用谁的。
2. 处理不同摄像头的旋转
如果是前置摄像头的话，需要进行左右的翻转。然后旋转90度。
后置摄像头的话，只需要旋转270度就可以了。

##### 2. 绘制图形
**重温一下绘制整体的流程**
```java
//draw step
public void draw() {
   //step0 clear
   onClear();
   //step1 use program
   onUseProgram();
   //step2 active and bind custom data
   onSetExpandData();
   //step3 bind texture
   onBindTexture();
   //step4 normal draw
   onDraw();
 }
```
- `onClear`
```java
   GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
   GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
```
这里有一个疑问。这里其实不用每次都清除的。只要初始化清除就可以了吧？

- `onUseProgram`
这一步，就是使用我们之前已经创建和`link`好的`program`
```java
 private void onUseProgram() {
        GLES20.glUseProgram(mProgram);
    }
```

- `onSetExpandData`
这里做的其实就是我们额外给`glsl`添加的属性。应用上面我们变化的矩阵。
```java
 private void onSetExpandData() {
        GLES20.glUniformMatrix4fv(mUMatrix, 1, false, matrix, 0);
        GLES20.glUniformMatrix4fv(mUCoordMatrix, 1, false, mCoordMatrix, 0);
 }
```

- `onBindTexture`
接着就是激活和绑定纹理数据.
```java
 private void onBindTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + getTextureType());
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, getTextureId());
        GLES20.glUniform1i(mUTexture, getTextureType());
    }
```
我们的纹理都挂载在`GLES20.GL_TEXTURE0 + getTextureType()`上。**同时一定要注意的是，相机这里需要的是`GLES11Ext.GL_TEXTURE_EXTERNAL_OES`这种拓展类型的纹理采样。**

- `onDraw`
绘制图像的话，同之前相同，只需要绘制一个长方形就可以了。
```java
 private void onDraw() {
        //设置定点数据
        GLES20.glEnableVertexAttribArray(mAPosition);
        GLES20.glVertexAttribPointer(
                mAPosition,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                mVerBuffer);
        //
        GLES20.glEnableVertexAttribArray(mACoord);
        GLES20.glVertexAttribPointer(
                mACoord,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                mTextureCoordinate);
        //绘制三角形带
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(mAPosition);
        GLES20.glDisableVertexAttribArray(mACoord);
    }
```
---
### GLSurfaceView
最后在GLSurfaceView的对应的生命周期内调用方法就可以了~~

## 最后
整编文章就重要的部分还是在理解整个纹理中数据传递的路线。
后续，会对这里的相机的预览添加其他的滤镜。
Demo位置
https://github.com/deepsadness/ZeroToOpenGL