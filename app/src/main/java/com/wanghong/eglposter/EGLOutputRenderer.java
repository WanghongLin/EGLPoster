/*
 * Copyright (C) 2016 Get Remark
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

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.os.Build;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by wanghong on 10/27/16.
 */

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
public class EGLOutputRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = EGLOutputRenderer.class.getSimpleName();

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

    private static final String FRAGMENT_SHADER_OES = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "uniform sampler2D sTexture2D;\n" +
            "uniform int tex;\n" +
            "\n" +
            "varying vec2 vTexCoord;\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 tc;\n" +
            "    if (tex == 0) {\n" +
            "        tc = texture2D(sTexture, vTexCoord);\n" +
            "    } else {\n" +
            "        tc = texture2D(sTexture2D, vTexCoord);\n" +
            "    }\n" +
            "    gl_FragColor = tc;\n" +
            "}";

    private WeakReference<GLSurfaceView> attachedGLSurfaceView;
    private EGLOutputSurfaceCallback eglOutputSurfaceCallback;
    private SurfaceTexture surfaceTexture;
    private int texture;
    private int texture2D;
    private int program;

    public static final int OUTPUT_RENDER_TYPE_STILL_BITMAP = 501;
    public static final int OUTPUT_RENDER_TYPE_CONTINUOUS_PICTURES = 616;

    private int outputRenderType = OUTPUT_RENDER_TYPE_STILL_BITMAP;
    private Bitmap stillBitmap;

    public EGLOutputRenderer(GLSurfaceView attachedGLSurfaceView) {
        this.attachedGLSurfaceView = new WeakReference<>(attachedGLSurfaceView);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        texture = createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        surfaceTexture = new SurfaceTexture(texture);
        program = createProgram();

        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                if (attachedGLSurfaceView != null && attachedGLSurfaceView.get() != null) {
                    attachedGLSurfaceView.get().requestRender();
                }
            }
        });
        if (eglOutputSurfaceCallback != null) {
            eglOutputSurfaceCallback.onOutputSurfaceCreated(surfaceTexture);
        }
    }

    private int createProgram() {
        int program = GLES20.glCreateProgram();
        checkGLESError("create program");

        int vertexShader = createShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = createShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_OES);
        GLES20.glAttachShader(program, vertexShader);
        checkGLESError("attach shader " + vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        checkGLESError("attach shader " + fragmentShader);

        GLES20.glLinkProgram(program);
        int[] linked = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            checkGLESError("link program");
        }

        return program;
    }

    private int createShader(int shaderType, String shaderSource) {
        int shader = GLES20.glCreateShader(shaderType);
        checkGLESError("create shader " + shaderType);
        GLES20.glShaderSource(shader, shaderSource);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(shader);
            checkGLESError("compile shader " + GLES20.glGetShaderInfoLog(shader));
        }

        return shader;
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

        if (target == GLES20.GL_TEXTURE_2D && stillBitmap != null && !stillBitmap.isRecycled()) {
            GLUtils.texImage2D(target, 0, stillBitmap, 0);
        }

        if (target == GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        } else if (target == GLES20.GL_TEXTURE_2D) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        }

        return textures[0];
    }

    private void checkGLESError(String what) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "checkGLESError() called with: what = [" + what + "]");
            throw new RuntimeException(what + ": " + GLU.gluErrorString(error));
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (eglOutputSurfaceCallback != null) {
            eglOutputSurfaceCallback.onOutputSurfaceChanged(width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        if (outputRenderType == OUTPUT_RENDER_TYPE_CONTINUOUS_PICTURES) {
            if (surfaceTexture != null) {
                surfaceTexture.updateTexImage();
            }
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(program);
        int positionIndex = GLES20.glGetAttribLocation(program, "aPosition");
        int texcoordIndex = GLES20.glGetAttribLocation(program, "aTexCoord");

        GLES20.glEnableVertexAttribArray(positionIndex);
        GLES20.glEnableVertexAttribArray(texcoordIndex);

        GLES20.glVertexAttribPointer(positionIndex, 3, GLES20.GL_FLOAT, false, 0, vertexByteBuffer);
        GLES20.glVertexAttribPointer(texcoordIndex, 2, GLES20.GL_FLOAT, false, 0, textureByteBuffer);

        int tex = 0;
        if (outputRenderType == OUTPUT_RENDER_TYPE_STILL_BITMAP) {
            tex = 1;
        } else if (outputRenderType == OUTPUT_RENDER_TYPE_CONTINUOUS_PICTURES) {
            tex = 0;
        }
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "tex"), tex);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture);
        int sTexture = GLES20.glGetUniformLocation(program, "sTexture");
        GLES20.glUniform1i(sTexture, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture2D);
        int sTexture2D = GLES20.glGetUniformLocation(program, "sTexture2D");
        GLES20.glUniform1i(sTexture2D, 1);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(positionIndex);
        GLES20.glDisableVertexAttribArray(texcoordIndex);
    }

    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public void setEglOutputSurfaceCallback(EGLOutputSurfaceCallback eglOutputSurfaceCallback) {
        this.eglOutputSurfaceCallback = eglOutputSurfaceCallback;
    }

    public void setStillBitmap(Bitmap stillBitmap) {
        this.stillBitmap = stillBitmap;
        requestRenderStillBitmap();
    }

    private void requestRenderStillBitmap() {
        texture2D = createTexture(GLES20.GL_TEXTURE_2D);
        if (attachedGLSurfaceView != null && attachedGLSurfaceView.get() != null) {
            attachedGLSurfaceView.get().requestRender();
        }
    }

    public void setOutputRenderType(int outputRenderType) {
        this.outputRenderType = outputRenderType;
    }
}
