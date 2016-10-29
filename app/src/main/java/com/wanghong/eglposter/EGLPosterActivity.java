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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import java.io.IOException;

/**
 * This example is simple and straightforward.<br/>
 * <br/>
 * <li>After {@link TextureView} created and initialized, create EGL context as a producer to render a <br/>
 * bitmap, then detach it from surface.<br/></li>
 * <li>Click to play the video, media player is the producer of the surface</li>
 * <li>After playback finished, the media player release the surface</li>
 * <li>Create EGL context again to clear the screen to black</li>
 */
public class EGLPosterActivity extends AppCompatActivity {

    private Surface mSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final TextureView textureView = new TextureView(this);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mSurface = new Surface(surface);
                new EGLPosterRetriever().execute();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
        textureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSurface == null) {
                    return;
                }
                AssetFileDescriptor assetFileDescriptor = null;
                try {
                    assetFileDescriptor = getAssets().openFd("testsrc2.mp4");
                    MediaPlayer mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(),
                            assetFileDescriptor.getLength());
                    mediaPlayer.setSurface(mSurface);
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mp.start();
                        }
                    });
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            mp.release();
                            EGLPosterRenderer.render(colorBitmap(Color.BLACK)).recycleBitmap(true).into(mSurface);
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
        setContentView(textureView);
    }

    private static Bitmap colorBitmap(int color) {
        Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(color);

        return bitmap;
    }

    protected class EGLPosterRetriever extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Void... params) {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            AssetFileDescriptor assetFileDescriptor = null;
            try {
                assetFileDescriptor = getAssets().openFd("testsrc2.mp4");
                mediaMetadataRetriever.setDataSource(assetFileDescriptor.getFileDescriptor(),
                        assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
                Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(-1);
                if (bitmap != null) {
                    return bitmap;
                }
            } catch (Exception e) {
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
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null && mSurface != null) {
                EGLPosterRenderer.render(bitmap).recycleBitmap(true).into(mSurface);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
    }
}
