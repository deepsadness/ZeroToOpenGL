package com.cry.zero_common.opengl;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLES20;
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

}
