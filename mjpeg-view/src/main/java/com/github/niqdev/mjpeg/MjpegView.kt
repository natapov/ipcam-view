package com.github.niqdev.mjpeg

import android.view.SurfaceHolder
import android.view.SurfaceView

interface MjpegView {
    fun setSource(stream: MjpegInputStream)
    fun setDisplayMode(mode: DisplayMode)
    fun showFps(show: Boolean)
    fun flipSource(flip: Boolean)
    fun flipHorizontal(flip: Boolean)
    fun flipVertical(flip: Boolean)
    fun setRotate(degrees: Float)
    fun stopPlayback()
    val isStreaming: Boolean
    fun setResolution(width: Int, height: Int)
    fun freeCameraMemory()
    fun setOnFrameCapturedListener(onFrameCapturedListener: MjpegRecordingHandler)
    fun setCustomBackgroundColor(backgroundColor: Int)
    fun setFpsOverlayBackgroundColor(overlayBackgroundColor: Int)
    fun setFpsOverlayTextColor(overlayTextColor: Int)
    val surfaceView: SurfaceView
    fun resetTransparentBackground()
    fun setTransparentBackground()
    fun clearStream()
    fun onSurfaceCreated(holder: SurfaceHolder)
    fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int)
    fun onSurfaceDestroyed(holder: SurfaceHolder)

}
