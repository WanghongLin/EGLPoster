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
import android.view.Surface;

/**
 * Created by wanghong on 10/27/16.
 */

public class EGLPosterRenderer {

    private Bitmap bitmap;
    private boolean recycleBitmap;

    private EGLPosterRenderer(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public static EGLPosterRenderer render(Bitmap bitmap) {
        return new EGLPosterRenderer(bitmap);
    }

    public EGLPosterRenderer recycleBitmap(boolean recycleBitmap) {
        this.recycleBitmap = recycleBitmap;
        return this;
    }

    public void into(Surface surface) {
        new EGLPosterRendererThread(bitmap, recycleBitmap, surface).start();
    }

    public void into(SurfaceTexture surfaceTexture) {
        new EGLPosterRendererThread(bitmap, recycleBitmap, surfaceTexture).start();
    }
}
