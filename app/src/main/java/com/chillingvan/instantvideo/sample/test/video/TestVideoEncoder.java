/*
 *
 *  *
 *  *  * Copyright (C) 2017 ChillingVan
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

package com.chillingvan.instantvideo.sample.test.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.support.annotation.Nullable;
import android.util.Log;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.Loggers;
import com.chillingvan.canvasgl.glcanvas.BasicTexture;
import com.chillingvan.canvasgl.glcanvas.GLPaint;
import com.chillingvan.canvasgl.glcanvas.RawTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.instantvideo.sample.R;
import com.chillingvan.lib.encoder.video.H264Encoder;
import com.chillingvan.lib.encoder.MediaCodecInputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by Chilling on 2016/12/10.
 */

public class TestVideoEncoder {

    private H264Encoder h264Encoder;
    private byte[] writeBuffer = new byte[1024 * 64];
    private Context ctx;
    private EglContextWrapper eglCtx;
    public static int drawCnt;
    private BasicTexture outsideTexture;
    private SurfaceTexture outsideSurfaceTexture;
    private OutputStream os;

    public TestVideoEncoder(Context ctx, final EglContextWrapper eglCtx) {
        this.ctx = ctx;
        this.eglCtx = eglCtx;

        try {
            os = new FileOutputStream(ctx.getExternalFilesDir(null) + File.separator + "test_h264_encode.h264");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void prepareEncoder() {
        try {
            h264Encoder = new H264Encoder(640, 480, 2949120, 30, 5, eglCtx);
            h264Encoder.setSharedTexture(outsideTexture, outsideSurfaceTexture);
            final Bitmap bitmap = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.lenna);
            h264Encoder.setOnDrawListener(new H264Encoder.OnDrawListener() {
                @Override
                public void onGLDraw(ICanvasGL canvasGL, SurfaceTexture producedSurfaceTexture, RawTexture rawTexture, @Nullable SurfaceTexture outsideSurfaceTexture, @Nullable BasicTexture outsideTexture) {
                    drawCnt++;

                    if (drawCnt == 19 || drawCnt == 39) {
                        canvasGL.drawBitmap(bitmap, 0, 0);
                    }
                    drawRect(canvasGL, drawCnt);

                    if (drawCnt >= 60) {
                        drawCnt = 0;
                    }
                    Log.i("TestVideoEncoder", "gl draw");
                }
            });
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void setSharedTexture(BasicTexture outsideTexture, SurfaceTexture outsideSurfaceTexture) {
        this.outsideTexture = outsideTexture;
        this.outsideSurfaceTexture = outsideSurfaceTexture;
    }

    public static void drawRect(ICanvasGL canvasGL, int drawCnt) {
        GLPaint glPaint = new GLPaint();
        glPaint.setColor(Color.BLUE);
        canvasGL.drawRect(new Rect(10 * drawCnt - 20, 50, 10 * drawCnt, 100), glPaint);
    }


    public void start() {
        Log.d("TestVideoEncoder", "start: ");
        h264Encoder.start();
    }

    public void stop() {
        if (h264Encoder == null) {
            return;
        }
        h264Encoder.stop();
    }

    public void destroy() {
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (h264Encoder == null) {
            return;
        }
        h264Encoder.release();
    }

    public void writeAFrame() {
        Loggers.d("TestVideoEncoder", "writeAFrame");
        h264Encoder.requestRenderAndWait();
    }

    public void write() {
        MediaCodecInputStream mediaCodecInputStream = h264Encoder.getMediaCodecInputStream();
        MediaCodecInputStream.readAll(mediaCodecInputStream, writeBuffer, 0, new MediaCodecInputStream.OnReadAllCallback() {
            @Override
            public void onReadOnce(byte[] buffer, int readSize, int mediaBufferSize) {
                Loggers.d("TestVideoEncoder", String.format("onReadOnce: readSize:%d", readSize));
                if (readSize <= 0) {
                    return;
                }
                try {
                    os.write(buffer, 0, readSize);
                    os.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}