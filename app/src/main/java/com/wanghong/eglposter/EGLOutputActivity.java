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

import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import java.io.IOException;

/**
 * In this example, we use GLSurfaceView to play video from media player<br/>
 * We create {@link SurfaceTexture} manually after GLSurfaceView initialized, <br/>
 * and use this SurfaceTexture as media player output<br/>
 * <br/>
 * In the shader language, we use another 2D texture to render a single picture from bitmap.
 * <br/> The renderer can switch to different renderer mode, {@link EGLOutputRenderer#OUTPUT_RENDER_TYPE_CONTINUOUS_PICTURES} to play video, and
 * {@link EGLOutputRenderer#OUTPUT_RENDER_TYPE_STILL_BITMAP} to display a video poster
 */
public class EGLOutputActivity extends AppCompatActivity {

    private static final String TAG = EGLOutputActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final EGLOutputSurface eglOutputSurface = new EGLOutputSurface(this);
        eglOutputSurface.setEglOutputSurfaceCallback(new EGLOutputSurfaceCallback() {
            @Override
            public void onOutputSurfaceCreated(SurfaceTexture surfaceTexture) {
                Log.d(TAG, "onOutputSurfaceCreated");
                eglOutputSurface.setOutputRenderType(EGLOutputRenderer.OUTPUT_RENDER_TYPE_STILL_BITMAP);
                eglOutputSurface.setStillBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
            }

            @Override
            public void onOutputSurfaceChanged(int width, int height) {

            }
        });
        eglOutputSurface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eglOutputSurface.setOutputRenderType(EGLOutputRenderer.OUTPUT_RENDER_TYPE_CONTINUOUS_PICTURES);
                AssetFileDescriptor assetFileDescriptor = null;
                try {
                    assetFileDescriptor = getAssets().openFd("testsrc2.mp4");
                    final MediaPlayer mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(),
                            assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
                    final Surface surface = new Surface(eglOutputSurface.getSurfaceTexture());
                    mediaPlayer.setSurface(surface);
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mp.start();
                        }
                    });
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            mediaPlayer.release();
                            surface.release();
                        }
                    });
                    mediaPlayer.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (assetFileDescriptor != null) {
                        try {
                            assetFileDescriptor.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        setContentView(eglOutputSurface);
    }
}
