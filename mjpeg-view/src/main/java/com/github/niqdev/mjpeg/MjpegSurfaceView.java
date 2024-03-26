package com.github.niqdev.mjpeg;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import androidx.annotation.NonNull;
import androidx.annotation.StyleableRes;

public class MjpegSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    public enum DisplayMode {
        STANDARD, BEST_FIT, SCALE_FIT, FULLSCREEN
    }
    private static final String TAG = MjpegSurfaceView.class.getSimpleName();
    private final boolean transparentBackground;
    private MjpegViewThread thread;
    private MjpegInputStream mIn = null;
    public boolean showFps = true;
    private volatile boolean isRunning = false;
    private volatile boolean surfaceDone = false;
    private Paint overlayPaint;
    private final int overlayTextColor = Color.WHITE;
    private final int overlayBackgroundColor = Color.DKGRAY;
    private final int backgroundColor = Color.BLACK;
    private int dispWidth;
    private int dispHeight;
    private DisplayMode displayMode;
    private boolean resume = false;
    private MjpegRecordingHandler onFrameCapturedListener;


    public MjpegSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        boolean transparentBackground = getPropertyBoolean(attrs, R.styleable.MjpegSurfaceView, R.styleable.MjpegSurfaceView_transparentBackground);

        if (transparentBackground) {
            setZOrderOnTop(true);
            getHolder().setFormat(PixelFormat.TRANSPARENT);
        }
        this.transparentBackground = transparentBackground;
        overlayPaint = new Paint();
        overlayPaint.setTextAlign(Paint.Align.LEFT);
        overlayPaint.setTextSize(12);
        overlayPaint.setTypeface(Typeface.DEFAULT);
        displayMode = DisplayMode.STANDARD;
        dispWidth = getWidth();
        dispHeight = getHeight();
        init();
    }
    private void init() {
        SurfaceHolder holder = this.getHolder();
        holder.addCallback(this);
        thread = new MjpegViewThread(holder);
        this.setFocusable(true);
        resume = true;
    }

    public boolean getPropertyBoolean(AttributeSet attributeSet, @StyleableRes int[] attrs, int attrIndex) {
        TypedArray typedArray = getContext().getTheme()
                .obtainStyledAttributes(attributeSet, attrs, 0, 0);
        try {
            return typedArray.getBoolean(attrIndex, false);
        } finally {
            typedArray.recycle();
        }
    }

    public int getPropertyColor(AttributeSet attributeSet, @StyleableRes int[] attrs, int attrIndex) {
        TypedArray typedArray = getContext().getTheme()
                .obtainStyledAttributes(attributeSet, attrs, 0, 0);
        try {
            return typedArray.getColor(attrIndex, -1);
        } finally {
            typedArray.recycle();
        }
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceDone = true;
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        synchronized (holder) {
            dispWidth = width;
            dispHeight = height;
        }
        if (thread != null) {
            thread.setSurfaceSize(width, height);
        }
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceDone = false;
        stopPlayback();
        if (thread != null) {
            thread = null;
        }
    }

    public void setSource(@NonNull MjpegInputStream stream) {
        mIn = stream;
        // make sure resume is calling resumePlayback()
        if (!resume) {
            startPlayback();
        } else {
            resumePlayback();
        }
    }

    public void setDisplayMode(@NonNull DisplayMode mode) {
        displayMode = mode;
    }
    public void setOnFrameCapturedListener(@NonNull MjpegRecordingHandler onFrameCapturedListener) {
        this.onFrameCapturedListener = onFrameCapturedListener;
    }
    public void resetTransparentBackground() {
        setZOrderOnTop(false);
        getHolder().setFormat(PixelFormat.OPAQUE);
    }

    public void setTransparentBackground() {
        setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSPARENT);
    }

    public void clearStream() {
        Canvas c = null;

        try {
            c = getHolder().lockCanvas();
            c.drawColor(0, PorterDuff.Mode.CLEAR);
        } finally {
            if (c != null) {
                getHolder().unlockCanvasAndPost(c);
            } else {
                Log.w(TAG, "couldn't unlock surface canvas");
            }
        }
    }
    void startPlayback() {
        if (mIn != null && thread != null) {
            isRunning = true;
            /*
             * clear canvas cache
             * @see https://github.com/niqdev/ipcam-view/issues/14
             */
            destroyDrawingCache();
            thread.start();
        }
    }

    void resumePlayback() {
        isRunning = true;
        init();
        thread.start();
    }

    /*
     * @see https://github.com/niqdev/ipcam-view/issues/14
     */

    public synchronized void stopPlayback() {
        isRunning = false;
        boolean retry = true;
        while (retry) {
            try {
                // make sure the thread is not null
                if (thread != null) {
                    thread.join(500);
                }
                retry = false;
            } catch (InterruptedException e) {
                Log.e(TAG, "error stopping playback thread", e);
            }
        }

        // close the connection
        if (mIn != null) {
            try {
                mIn.close();
            } catch (IOException e) {
                Log.e(TAG, "error closing input stream", e);
            }
            mIn = null;
        }
    }
    class MjpegViewThread extends Thread {
        private final SurfaceHolder mSurfaceHolder;
        private int frameCounter = 0;
        private Bitmap ovl;

        // no more accessible
        MjpegViewThread(SurfaceHolder surfaceHolder) {
            mSurfaceHolder = surfaceHolder;
        }

        private Rect destRect(int bmw, int bmh) {

            int tempx;
            int tempy;
            if (displayMode == DisplayMode.STANDARD) {
                tempx = (dispWidth / 2) - (bmw / 2);
                tempy = (dispHeight / 2) - (bmh / 2);
                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
            }
            if (displayMode == DisplayMode.BEST_FIT) {
                float bmasp = (float) bmw / (float) bmh;
                bmw = dispWidth;
                bmh = (int) (dispWidth / bmasp);
                if (bmh > dispHeight) {
                    bmh = dispHeight;
                    bmw = (int) (dispHeight * bmasp);
                }
                tempx = (dispWidth / 2) - (bmw / 2);
                tempy = (dispHeight / 2) - (bmh / 2);
                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
            }
            if (displayMode == DisplayMode.SCALE_FIT) {
                float bmasp = ((float) bmw / (float) bmh);
                tempx = 0;
                tempy = 0;
                if (bmw < dispWidth) {
                    bmw = dispWidth;
                    // cross-multiplication using aspect ratio
                    bmh = (int) (dispWidth / bmasp);
                    // set it to the center height
                    tempy = (dispHeight - bmh) / 4;
                }
                return new Rect(tempx, tempy, bmw, bmh + tempy);
            }
            if (displayMode == DisplayMode.FULLSCREEN)
                return new Rect(0, 0, dispWidth, dispHeight);
            return null;
        }

        // no more accessible
        void setSurfaceSize(int width, int height) {

        }

        private Bitmap makeFpsOverlay(Paint p, String text) {
            Rect b = new Rect();
            p.getTextBounds(text, 0, text.length(), b);
            final int bwidth = b.width() + 2;
            final int bheight = b.height() + 2;
            Bitmap bm = Bitmap.createBitmap(bwidth, bheight, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            p.setColor(overlayBackgroundColor);
            c.drawRect(0, 0, bwidth, bheight, p);
            p.setColor(overlayTextColor);
            c.drawText(text, -b.left + 1,
                    ((float) bheight / 2) - ((p.ascent() + p.descent()) / 2) + 1, p);
            return bm;
        }
        void frameCapturedWithByteData(byte[] imageByte, byte[] header) {
            if (onFrameCapturedListener != null) {
                onFrameCapturedListener.onFrameCapturedWithHeader(imageByte, header);
            }
        }

        void frameCapturedWithBitmap(Bitmap bitmap) {
            if (onFrameCapturedListener != null) {
                onFrameCapturedListener.onFrameCaptured(bitmap);
            }
        }

        public void run() {
            long start = System.currentTimeMillis();
            PorterDuffXfermode mode = new PorterDuffXfermode(
                    PorterDuff.Mode.DST_OVER);
            Bitmap bm;
            int width;
            int height;
            Rect destRect;
            Canvas c = null;
            Paint p = new Paint();
            String fps;
            while (isRunning) {
                if (surfaceDone) {
                    try {
                        c = mSurfaceHolder.lockCanvas();

                        if (c == null) {
                            Log.w(TAG, "null canvas, skipping render");
                            continue;
                        }
                        synchronized (mSurfaceHolder) {
                            try {
                                byte[] header = mIn.readHeader();
                                byte[] imageData = mIn.readMjpegFrame(header);
                                bm = BitmapFactory.decodeStream(new ByteArrayInputStream(imageData));
                                frameCapturedWithByteData(imageData, header);
                                frameCapturedWithBitmap(bm);
                                destRect = destRect(bm.getWidth(),
                                        bm.getHeight());

                                if (transparentBackground) {
                                    c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                                } else {
                                    c.drawColor(backgroundColor);
                                }

                                c.drawBitmap(bm, null, destRect, p);

                                if (showFps) {
                                    p.setXfermode(mode);
                                    if (ovl != null) {
                                        height = destRect.bottom - ovl.getHeight();
                                        width = destRect.right - ovl.getWidth();
                                        c.drawBitmap(ovl, width, height, null);
                                    }
                                    p.setXfermode(null);
                                    frameCounter++;
                                    if ((System.currentTimeMillis() - start) >= 1000) {
                                        fps = frameCounter + "fps";
                                        frameCounter = 0;
                                        start = System.currentTimeMillis();
                                        ovl = makeFpsOverlay(overlayPaint, fps);
                                    }
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "encountered exception during render", e);
                            }
                        }
                    } finally {
                        if (c != null) {
                            mSurfaceHolder.unlockCanvasAndPost(c);
                        } else {
                            Log.w(TAG, "couldn't unlock surface canvas");
                        }
                    }
                }
            }
        }
    }
}
