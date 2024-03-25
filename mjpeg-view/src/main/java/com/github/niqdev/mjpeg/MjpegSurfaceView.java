package com.github.niqdev.mjpeg;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.StyleableRes;

public class MjpegSurfaceView extends SurfaceView implements SurfaceHolder.Callback, MjpegView {

    private static final int DEFAULT_TYPE = 0;
    // issue in attrs.xml - verify reserved keywords
    private static final SparseArray<Mjpeg.Type> TYPE;



    static {
        TYPE = new SparseArray<>();
        TYPE.put(0, Mjpeg.Type.DEFAULT);
        TYPE.put(1, Mjpeg.Type.NATIVE);
    }

    private MjpegView mMjpegView;

    public MjpegSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        boolean transparentBackground = getPropertyBoolean(attrs, R.styleable.MjpegSurfaceView, R.styleable.MjpegSurfaceView_transparentBackground);
        int backgroundColor = getPropertyColor(attrs, R.styleable.MjpegSurfaceView, R.styleable.MjpegSurfaceView_backgroundColor);

        if (transparentBackground) {
            setZOrderOnTop(true);
            getHolder().setFormat(PixelFormat.TRANSPARENT);
        }

        mMjpegView = new MjpegViewDefault(this, this, transparentBackground);

        if (mMjpegView != null && backgroundColor != -1) {
            this.setCustomBackgroundColor(backgroundColor);
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
        mMjpegView.onSurfaceCreated(holder);
    }
    @Override
    public void onSurfaceCreated(SurfaceHolder holder) {
        mMjpegView.onSurfaceCreated(holder);
    }
    @Override
    public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mMjpegView.onSurfaceChanged(holder, format, width, height);
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mMjpegView.onSurfaceChanged(holder, format, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mMjpegView.onSurfaceDestroyed(holder);
    }
    @Override
    public void onSurfaceDestroyed(SurfaceHolder holder) {
        mMjpegView.onSurfaceDestroyed(holder);
    }

    @Override
    public void setSource(@NonNull MjpegInputStream stream) {
        mMjpegView.setSource(stream);
    }

    @Override
    public void setDisplayMode(@NonNull DisplayMode mode) {
        mMjpegView.setDisplayMode(mode);
    }

    @Override
    public void showFps(boolean show) {
        mMjpegView.showFps(show);
    }

    @Override
    public void flipSource(boolean flip) {
        mMjpegView.flipSource(flip);
    }

    @Override
    public void flipHorizontal(boolean flip) {
        mMjpegView.flipHorizontal(flip);
    }

    @Override
    public void flipVertical(boolean flip) {
        mMjpegView.flipVertical(flip);
    }

    @Override
    public void setRotate(float degrees) {
        mMjpegView.setRotate(degrees);
    }

    @Override
    public void stopPlayback() {
        mMjpegView.stopPlayback();
    }

    @Override
    public boolean isStreaming() {
        return mMjpegView.isStreaming();
    }

    @Override
    public void setResolution(int width, int height) {
        mMjpegView.setResolution(width, height);
    }

    @Override
    public void freeCameraMemory() {
        mMjpegView.freeCameraMemory();
    }

    @Override
    public void setOnFrameCapturedListener(@NonNull OnFrameCapturedListener onFrameCapturedListener) {
        mMjpegView.setOnFrameCapturedListener(onFrameCapturedListener);
    }

    @Override
    public void setCustomBackgroundColor(int backgroundColor) {
        mMjpegView.setCustomBackgroundColor(backgroundColor);
    }

    @Override
    public void setFpsOverlayBackgroundColor(int overlayBackgroundColor) {
        mMjpegView.setFpsOverlayBackgroundColor(overlayBackgroundColor);
    }

    @Override
    public void setFpsOverlayTextColor(int overlayTextColor) {
        mMjpegView.setFpsOverlayTextColor(overlayTextColor);
    }

    @NonNull
    @Override
    public SurfaceView getSurfaceView() {
        return this;
    }

    @Override
    public void resetTransparentBackground() {
        mMjpegView.resetTransparentBackground();
    }

    @Override
    public void setTransparentBackground() {
        mMjpegView.setTransparentBackground();
    }

    @Override
    public void clearStream() {
        mMjpegView.clearStream();
    }
}
