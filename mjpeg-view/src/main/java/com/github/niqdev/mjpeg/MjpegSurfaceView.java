package com.github.niqdev.mjpeg;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.StyleableRes;

public class MjpegSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final int DEFAULT_TYPE = 0;
    private static final String TAG = MjpegSurfaceView.class.getSimpleName();

    private final boolean transparentBackground;
    private MjpegViewThread thread;
    private MjpegInputStream mIn = null;
    private boolean showFps = false;
    private boolean flipHorizontal = false;
    private boolean flipVertical = false;
    private float rotateDegrees = 0;
    private volatile boolean mRun = false;
    private volatile boolean surfaceDone = false;
    private Paint overlayPaint;
    private int overlayTextColor;
    private int overlayBackgroundColor;
    private int backgroundColor;
    private int ovlPos;
    private int dispWidth;
    private int dispHeight;
    private int displayMode;
    private boolean resume = false;
    private MjpegRecordingHandler onFrameCapturedListener;

    public final int  POSITION_UPPER_LEFT = 9;
    public final int  POSITION_UPPER_RIGHT = 3;
    public final int  POSITION_LOWER_LEFT = 12;
    public final int  POSITION_LOWER_RIGHT = 6;
    public final int  SIZE_STANDARD = 1;
    public final int  SIZE_BEST_FIT = 4;
    public final int  SIZE_SCALE_FIT = 16;
    public final int  SIZE_FULLSCREEN = 8;
    private static final SparseArray<Mjpeg.Type> TYPE;



    static {
        TYPE = new SparseArray<>();
        TYPE.put(0, Mjpeg.Type.DEFAULT);
        TYPE.put(1, Mjpeg.Type.NATIVE);
    }


    public MjpegSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        boolean transparentBackground = getPropertyBoolean(attrs, R.styleable.MjpegSurfaceView, R.styleable.MjpegSurfaceView_transparentBackground);
        int backgroundColor = getPropertyColor(attrs, R.styleable.MjpegSurfaceView, R.styleable.MjpegSurfaceView_backgroundColor);

        if (transparentBackground) {
            setZOrderOnTop(true);
            getHolder().setFormat(PixelFormat.TRANSPARENT);
        }

        if (backgroundColor != -1) {
            setCustomBackgroundColor(backgroundColor);
        }
        this.transparentBackground = transparentBackground;
        init();
    }
    private void init() {
        SurfaceHolder holder = this.getHolder();
        holder.addCallback(this);
        thread = new MjpegViewThread(holder);
        this.setFocusable(true);
        if (!resume) {
            resume = true;
            overlayPaint = new Paint();
            overlayPaint.setTextAlign(Paint.Align.LEFT);
            overlayPaint.setTextSize(12);
            overlayPaint.setTypeface(Typeface.DEFAULT);
            overlayTextColor = Color.WHITE;
            overlayBackgroundColor = Color.BLACK;
            backgroundColor = Color.BLACK;
            ovlPos = this.POSITION_LOWER_RIGHT;
            displayMode = this.SIZE_STANDARD;
            dispWidth = this.getWidth();
            dispHeight = this.getHeight();
        }
    }
    public Mjpeg.Type getPropertyType(AttributeSet attributeSet, @StyleableRes int[] attrs, int attrIndex) {
        TypedArray typedArray = getContext().getTheme()
                .obtainStyledAttributes(attributeSet, attrs, 0, 0);
        try {
            int typeIndex = typedArray.getInt(attrIndex, DEFAULT_TYPE);
            Mjpeg.Type type = TYPE.get(typeIndex);
            return type != null ? type : TYPE.get(DEFAULT_TYPE);
        } finally {
            typedArray.recycle();
        }
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
        onSurfaceCreated(holder);
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        onSurfaceChanged(holder, format, width, height);
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        onSurfaceDestroyed(holder);
    }
    public void onSurfaceCreated(SurfaceHolder holder) {
        surfaceDone = true;
    }
    public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (thread != null) {
            thread.setSurfaceSize(width, height);
        }
    }
    public void onSurfaceDestroyed(SurfaceHolder holder) {
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

    void setDisplayMode(int s) {
        displayMode = s;
    }

    public void setDisplayMode(@NonNull DisplayMode mode) {
        setDisplayMode(mode.getValue());
    }

    public void showFps(boolean show) {
        showFps = show;
    }

    public void flipHorizontal(boolean flip) {
        flipHorizontal = flip;
    }

    public void flipVertical(boolean flip) {
        flipVertical = flip;
    }

    public void setRotate(float degrees) {
        rotateDegrees = degrees;
    }

    public boolean isStreaming() {
        return mRun;
    }

    public void freeCameraMemory() {
        throw new UnsupportedOperationException("not implemented");
    }

    public void setOnFrameCapturedListener(@NonNull MjpegRecordingHandler onFrameCapturedListener) {
        this.onFrameCapturedListener = onFrameCapturedListener;
    }

    public void setCustomBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setFpsOverlayBackgroundColor(int overlayBackgroundColor) {
        this.overlayBackgroundColor = overlayBackgroundColor;
    }

    public void setFpsOverlayTextColor(int overlayTextColor) {
        this.overlayTextColor = overlayTextColor;
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
            mRun = true;
            /*
             * clear canvas cache
             * @see https://github.com/niqdev/ipcam-view/issues/14
             */
            destroyDrawingCache();
            thread.start();
        }
    }

    void resumePlayback() {
        mRun = true;
        init();
        thread.start();
    }

    /*
     * @see https://github.com/niqdev/ipcam-view/issues/14
     */

    public synchronized void stopPlayback() {
        mRun = false;
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
    Bitmap flip(Bitmap src) {
        Matrix m = new Matrix();
        float sx = flipHorizontal ? -1 : 1;
        float sy = flipVertical ? -1 : 1;
        m.preScale(sx, sy);
        Bitmap dst = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, false);
        dst.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        return dst;
    }

    Bitmap rotate(Bitmap src, float degrees) {
        Matrix m = new Matrix();
        m.setRotate(degrees);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, false);
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
            if (displayMode == SIZE_STANDARD) {
                tempx = (dispWidth / 2) - (bmw / 2);
                tempy = (dispHeight / 2) - (bmh / 2);
                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
            }
            if (displayMode == SIZE_BEST_FIT) {
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
            if (displayMode == SIZE_SCALE_FIT) {
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
            if (displayMode == SIZE_FULLSCREEN)
                return new Rect(0, 0, dispWidth, dispHeight);
            return null;
        }

        // no more accessible
        void setSurfaceSize(int width, int height) {
            synchronized (mSurfaceHolder) {
                dispWidth = width;
                dispHeight = height;
            }
        }

        private Bitmap makeFpsOverlay(Paint p, String text) {
            Rect b = new Rect();
            p.getTextBounds(text, 0, text.length(), b);
            int bwidth = b.width() + 2;
            int bheight = b.height() + 2;
            Bitmap bm = Bitmap.createBitmap(bwidth, bheight,
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            p.setColor(overlayBackgroundColor);
            c.drawRect(0, 0, bwidth, bheight, p);
            p.setColor(overlayTextColor);
            c.drawText(text, -b.left + 1,
                    (bheight / 2) - ((p.ascent() + p.descent()) / 2) + 1, p);
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
            while (mRun) {
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
                                if (flipHorizontal || flipVertical)
                                    bm = flip(bm);
                                if (rotateDegrees != 0)
                                    bm = rotate(bm, rotateDegrees);

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
                                        height = ((ovlPos & 1) == 1) ? destRect.top
                                                : destRect.bottom
                                                - ovl.getHeight();
                                        width = ((ovlPos & 8) == 8) ? destRect.left
                                                : destRect.right
                                                - ovl.getWidth();
                                        c.drawBitmap(ovl, width, height, null);
                                    }
                                    p.setXfermode(null);
                                    frameCounter++;
                                    if ((System.currentTimeMillis() - start) >= 1000) {
                                        fps = frameCounter
                                                + "fps";
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
