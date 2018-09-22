package com.cry.zero_common.opengl;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by a2957 on 2018/5/3.
 */

public class GLESUtils {
    private static final String TAG = "GLESUtils";
    /**
     * open gl 2的版本
     */
    private static final int GLES_VERSION_2 = 0x20000;

    /**
     * 判断是否支持es2.0
     *
     * @param context
     * @return
     */
    public static boolean isSupportEs2(Context context) {
        //检查是否支持2.0
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            ConfigurationInfo deviceConfigurationInfo = activityManager.getDeviceConfigurationInfo();
            int reqGlEsVersion = deviceConfigurationInfo.reqGlEsVersion;
            return reqGlEsVersion >= GLES_VERSION_2 || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                    && (Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK build for x86")));
        } else {
            return false;
        }

    }

    /**
     * 从Assert中读取ShaderCode
     *
     * @param context        context
     * @param shaderCodeName shader file name
     * @return shaderCodeString
     */
    public static String readRawShaderCode(Context context, int shaderCodeName) {
        StringBuilder body = new StringBuilder();
        InputStream open = null;
        try {
            open = context.getResources().openRawResource(shaderCodeName);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(open));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                body.append(line);
                body.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return body.toString();
    }

    /**
     * 从Assert中读取ShaderCode
     *
     * @param context        context
     * @param shaderCodeName shader file name
     * @return shaderCodeString
     */
    public static String readAssetShaderCode(Context context, String shaderCodeName) {
        StringBuilder body = new StringBuilder();
        InputStream open = null;
        try {
            open = context.getAssets().open(shaderCodeName);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(open));
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                body.append(line);
                body.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return body.toString();
    }

    /**
     * 对ShaderCode进行编译
     *
     * @param type       shader的type
     * @param shaderCode 进行编译的Shader代码
     * @return shaderObjectId
     */
    public static int compileShaderCode(int type, String shaderCode) {
        //得到一个着色器的ID。主要是对ID进行操作
        int shaderObjectId = GLES20.glCreateShader(type);

        //如果着色器的id不为0，则表示是可以用的
        if (shaderObjectId != 0) {
            //0.上传代码
            GLES20.glShaderSource(shaderObjectId, shaderCode);
            //1.编译代码.根据刚刚和代码绑定的ShaderObjectId进行编译
            GLES20.glCompileShader(shaderObjectId);

            //2.查询编译的状态
            int[] status = new int[1];
            //调用getShaderIv ，传入GL_COMPILE_STATUS进行查询
            GLES20.glGetShaderiv(shaderObjectId, GLES20.GL_COMPILE_STATUS, status, 0);

            if (status[0] == 0) { //等于0。则表示失败
                //失败的话，需要释放资源，就是删除这个引用
                GLES20.glDeleteShader(shaderObjectId);
                Log.w("OpenGL Utils", "compile failed!");
                return 0;
            }
        }
        //最后都会去返回这个shader的引用id
        return shaderObjectId;
    }

    public static int createTexture(Bitmap bitmap) {
        int[] texture = new int[1];

        //生成纹理
        GLES20.glGenTextures(1, texture, 0);
        //生成纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        if (bitmap != null && !bitmap.isRecycled()) {
            //根据以上指定的参数，生成一个2D纹理
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        }
        return texture[0];
    }

    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (0 != shader) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader:" + shaderType);
                Log.e(TAG, "GLES20 Error:" + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    public static int loadShader(Resources res, int shaderType, String resName) {
        return loadShader(shaderType, loadFromAssetsFile(resName, res));
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertex = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertex == 0) return 0;
        int fragment = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragment == 0) return 0;
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertex);
//            checkGLError("Attach Vertex Shader");
            GLES20.glAttachShader(program, fragment);
//            checkGLError("Attach Fragment Shader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program:" + GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    public static int createProgram(Resources res, String vertexRes, String fragmentRes) {
        return createProgram(loadFromAssetsFile(vertexRes, res), loadFromAssetsFile(fragmentRes, res));
    }

    public static String loadFromAssetsFile(String fname, Resources res) {
        StringBuilder result = new StringBuilder();
        try {
            InputStream is = res.getAssets().open(fname);
            int ch;
            byte[] buffer = new byte[1024];
            while (-1 != (ch = is.read(buffer))) {
                result.append(new String(buffer, 0, ch));
            }
        } catch (Exception e) {
            return null;
        }
        return result.toString().replaceAll("\\r\\n", "\n");
    }


    public static int[] createOffscreenIds(int width, int height) {
        int[] result = new int[3];
        int[] values = new int[1];
        values[0] = 55;
        GLES20.glGenTextures(1, values, 0);
//        GlUtil.checkGlError("glGenTextures");
        result[0] = values[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, result[0]);
//        GlUtil.checkGlError("glBindTexture " + mOffscreenTextureId);

        // Create texture storage.
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        // Set parameters.  We're probably using non-power-of-two dimensions, so
        // some values may not be available for use.
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
//        GlUtil.checkGlError("glTexParameter");

        // Create framebuffer object and bind it.
        GLES20.glGenFramebuffers(1, values, 0);
        result[1] = values[0];
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, result[1]);
//        GlUtil.checkGlError("glBindFramebuffer " + mFrameBuffer);

        // Create a depth buffer and bind it.
        GLES20.glGenRenderbuffers(1, values, 0);
        result[2] = values[0];
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, result[2]);
//        GlUtil.checkGlError("glBindFramebuffer " + mRenderBuffer);

        // Allocate storage for the depth buffer.
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);
//        GlUtil.checkGlError("glRenderbufferStorage");

        // Attach the depth buffer and the texture (color buffer) to the framebuffer object.
        // 将renderBuffer挂载到frameBuffer的depth attachment 上
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, result[2]);
//        GlUtil.checkGlError("glFramebufferRenderbuffer");
        // 将text2d挂载到frameBuffer的color attachment上
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, result[0], 0);
//        GlUtil.checkGlError("glFramebufferTexture2D");

        // See if GLES is happy with all this.
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer not complete, status=" + status);
        }

        // 先不使用FrameBuffer
        // Switch back to the default framebuffer.
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//        GlUtil.checkGlError("prepareFramebuffer done");
        return result;
    }


    public static void deleteOffscreenIds(int[] ids) {
        int[] values = new int[1];
        values[0] = ids[0];
        GLES20.glDeleteFramebuffers(1, values, 0);
        values[0] = ids[1];
        GLES20.glDeleteRenderbuffers(1, values, 0);

    }

}
