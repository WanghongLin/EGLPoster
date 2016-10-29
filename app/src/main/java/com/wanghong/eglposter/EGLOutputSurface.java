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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * Created by wanghong on 10/27/16.
 */

public class EGLOutputSurface extends GLSurfaceView {

    private EGLOutputRenderer eglOutputRenderer;

    public EGLOutputSurface(Context context) {
        super(context);
        initView(context, null);
    }

    public EGLOutputSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        eglOutputRenderer = new EGLOutputRenderer(this);
        setEGLContextClientVersion(2);
        setRenderer(eglOutputRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public void setEglOutputSurfaceCallback(EGLOutputSurfaceCallback eglOutputSurfaceCallback) {
        eglOutputRenderer.setEglOutputSurfaceCallback(eglOutputSurfaceCallback);
    }

    public void setStillBitmap(Bitmap stillBitmap) {
        eglOutputRenderer.setStillBitmap(stillBitmap);
    }

    public SurfaceTexture getSurfaceTexture() {
        return eglOutputRenderer.getSurfaceTexture();
    }

    public void setOutputRenderType(int outputRenderType) {
        eglOutputRenderer.setOutputRenderType(outputRenderType);
    }
}
