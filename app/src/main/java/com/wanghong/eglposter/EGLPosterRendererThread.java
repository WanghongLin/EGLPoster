/*
 * Copyright (C) 2016 wanghong
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wanghong.eglposter;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * Created by wanghong on 10/27/16.
 */

public class EGLPosterRendererThread extends Thread {

    private static final String TAG = EGLPosterRendererThread.class.getSimpleName();
    private static final int EGL_OPENGL_ES2_BIT = 0x4;
    private static final int EGL_BACK_BUFFER = 0x3084;
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    private static final int[] EGL_CONFIG_ATTRIBUTE_LIST = new int[] {
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT,
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 0,
            EGL10.EGL_STENCIL_SIZE, 0,
            EGL10.EGL_NONE
    };

    private static final int[] EGL_SURFACE_ATTRIBUTE_LIST = new int[] {
            EGL10.EGL_RENDER_BUFFER, EGL_BACK_BUFFER,
            EGL10.EGL_NONE
    };

    private static final int[] EGL_CONTEXT_ATTRIBUTE_LIST = new int[] {
            EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL10.EGL_NONE
    };

    private static final String VERTEX_SHADER = "" +
            "attribute vec4 aPosition;\n" +
            "attribute vec2 aTexCoord;\n" +
            "\n" +
            "varying vec2 vTexCoord;\n" +
            "\n" +
            "void main() {\n" +
            "    vTexCoord = aTexCoord;\n" +
            "    gl_Position = aPosition;\n" +
            "}";

    private static final String FRAGMENT_SHADER = "" +
            "precision mediump float;\n" +
            "uniform sampler2D sTexture;\n" +
            "\n" +
            "varying vec2 vTexCoord;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(sTexture, vTexCoord);\n" +
            "}";

    private static final String FRAGMENT_SHADER_OES = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "\n" +
            "varying vec2 vTexCoord;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(sTexture, vTexCoord);\n" +
            "}";

    private static final float[] VERTEX_COORDINATE = new float[] {
            -1.0f, +1.0f, 0.0f,
            +1.0f, +1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            +1.0f, -1.0f, 0.0f
    };

    private static final float[] TEXTURE_COORDINATE = new float[] {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };

    private static ByteBuffer vertexByteBuffer;
    private static ByteBuffer textureByteBuffer;
    static {
        vertexByteBuffer = ByteBuffer.allocateDirect(VERTEX_COORDINATE.length * 4);
        vertexByteBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer().put(VERTEX_COORDINATE);
        vertexByteBuffer.rewind();

        textureByteBuffer = ByteBuffer.allocateDirect(TEXTURE_COORDINATE.length * 4);
        textureByteBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer().put(TEXTURE_COORDINATE);
        textureByteBuffer.rewind();
    }

    private Bitmap bitmap;
    private boolean recycleBitmap;
    private Surface surface;
    private SurfaceTexture surfaceTexture;
    private Object nativeWindow;

    private EGL10 egl10;
    private EGLDisplay eglDisplay;
    private EGLSurface eglSurface;
    private EGLContext eglContext;

    public EGLPosterRendererThread(Bitmap bitmap, boolean recycleBitmap, Surface surface) {
        this.bitmap = bitmap;
        this.recycleBitmap = recycleBitmap;
        this.surface = surface;
        this.nativeWindow = surface;
    }

    public EGLPosterRendererThread(Bitmap bitmap, boolean recycleBitmap, SurfaceTexture surfaceTexture) {
        this.bitmap = bitmap;
        this.recycleBitmap = recycleBitmap;
        this.surfaceTexture = surfaceTexture;
        this.nativeWindow = surfaceTexture;
    }

    @Override
    public void run() {
        super.run();
        initEGL();

        performRenderPoster();

        shutdownEGL();
    }

    private void performRenderPoster() {
        int texture = createTexture(GLES20.GL_TEXTURE_2D);

        int program = GLES20.glCreateProgram();
        if (program == 0) {
            checkGLESError("create program");
        }

        int vertexShader = createShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = createShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        GLES20.glAttachShader(program, vertexShader);
        checkGLESError("attach shader " + vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        checkGLESError("attach shader " + fragmentShader);

        GLES20.glLinkProgram(program);
        int[] linked = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            checkGLESError("link program " + GLES20.glGetProgramInfoLog(program));
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(program);
        checkGLESError("use program");

        int positionIndex = GLES20.glGetAttribLocation(program, "aPosition");
        int texcoordIndex = GLES20.glGetAttribLocation(program, "aTexCoord");

        GLES20.glVertexAttribPointer(positionIndex, 3, GLES20.GL_FLOAT, false, 0, vertexByteBuffer);
        GLES20.glVertexAttribPointer(texcoordIndex, 2, GLES20.GL_FLOAT, false, 0, textureByteBuffer);

        GLES20.glEnableVertexAttribArray(positionIndex);
        GLES20.glEnableVertexAttribArray(texcoordIndex);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        int tex = GLES20.glGetUniformLocation(program, "sTexture");
        GLES20.glUniform1i(tex, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        egl10.eglSwapBuffers(eglDisplay, eglSurface);

        Log.d(TAG, "performRenderPoster");
        GLES20.glDisableVertexAttribArray(positionIndex);
        GLES20.glDisableVertexAttribArray(texcoordIndex);
    }

    private int createTexture(int target) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        checkGLESError("gen texture");
        GLES20.glBindTexture(target, textures[0]);
        checkGLESError("bind texture");
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(target, 0, bitmap, 0);
        if (recycleBitmap && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        checkGLESError("tex image 2d");
        return textures[0];
    }

    private int createShader(int shaderType, String shaderString) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader == 0) {
            checkGLESError("create shader " + shaderType);
        }

        GLES20.glShaderSource(shader, shaderString);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(shader);
            checkGLESError("compile shader " + shaderType);
        }
        return shader;
    }

    private void shutdownEGL() {
        Log.d(TAG, "shutdownEGL");
        egl10.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl10.eglDestroyContext(eglDisplay, eglContext);
        egl10.eglDestroySurface(eglDisplay, eglSurface);
        egl10.eglTerminate(eglDisplay);
        eglContext = EGL10.EGL_NO_CONTEXT;
        eglSurface = EGL10.EGL_NO_SURFACE;
        eglDisplay = EGL10.EGL_NO_DISPLAY;
    }

    private void initEGL() {
        egl10 = (EGL10) EGLContext.getEGL();
        eglDisplay = egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
            raiseEGLInitError();
        }

        int[] majorMinorVersions = new int[2];
        if (!egl10.eglInitialize(eglDisplay, majorMinorVersions)) {
            raiseEGLInitError();
        }

        EGLConfig[] eglConfigs = new EGLConfig[1];
        int[] numOfConfigs = new int[1];
        if (!egl10.eglChooseConfig(eglDisplay, EGL_CONFIG_ATTRIBUTE_LIST, eglConfigs, 1, numOfConfigs)) {
            raiseEGLInitError();
        }

        eglSurface = egl10.eglCreateWindowSurface(eglDisplay, eglConfigs[0], nativeWindow, EGL_SURFACE_ATTRIBUTE_LIST);
        if (eglSurface == EGL10.EGL_NO_SURFACE) {
            raiseEGLInitError();
        }

        eglContext = egl10.eglCreateContext(eglDisplay, eglConfigs[0], EGL10.EGL_NO_CONTEXT, EGL_CONTEXT_ATTRIBUTE_LIST);
        if (eglContext == EGL10.EGL_NO_CONTEXT) {
            raiseEGLInitError();
        }

        if (!egl10.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            raiseEGLInitError();
        }

        Log.d(TAG, "initEGL");
    }

    private void raiseEGLInitError() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            throw new RuntimeException("EGL INIT ERROR " + egl10.eglGetError() + " " +
                    GLUtils.getEGLErrorString(egl10.eglGetError()));
        }
    }

    private void checkGLESError(String where) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "checkGLESError: " + GLU.gluErrorString(error));
            throw new RuntimeException(where);
        }
    }
}
