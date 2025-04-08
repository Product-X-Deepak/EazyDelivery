package com.eazydelivery.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for taking screenshots for UI analysis
 */
@Singleton
class ScreenshotUtil @Inject constructor(
    @ApplicationContext private val context: Context,
    private val errorHandler: ErrorHandler
) {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayMetrics = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        context.resources.displayMetrics
    } else {
        DisplayMetrics().also { windowManager.defaultDisplay.getMetrics(it) }
    }

    private val screenWidth = displayMetrics.widthPixels
    private val screenHeight = displayMetrics.heightPixels
    private val screenDensity = displayMetrics.densityDpi

    /**
     * Sets up the media projection for taking screenshots
     *
     * @param resultCode The result code from the permission request
     * @param data The intent data from the permission request
     */
    fun setupMediaProjection(resultCode: Int, data: android.content.Intent) {
        try {
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)

            setupVirtualDisplay()

            Timber.d("Media projection set up successfully")
        } catch (e: Exception) {
            errorHandler.handleException("ScreenshotUtil.setupMediaProjection", e)
        }
    }

    /**
     * Sets up the virtual display for capturing screenshots
     */
    private fun setupVirtualDisplay() {
        try {
            imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                handler
            )

            Timber.d("Virtual display set up with dimensions: ${screenWidth}x${screenHeight}")
        } catch (e: Exception) {
            errorHandler.handleException("ScreenshotUtil.setupVirtualDisplay", e)
        }
    }

    /**
     * Takes a screenshot of the current screen
     *
     * @return The screenshot as a bitmap, or null if failed
     */
    fun takeScreenshot(): Bitmap? {
        try {
            if (mediaProjection == null || virtualDisplay == null || imageReader == null) {
                Timber.w("Cannot take screenshot: media projection not set up")
                return null
            }

            val image: Image? = imageReader?.acquireLatestImage()
            if (image == null) {
                Timber.w("Failed to acquire image")
                return null
            }

            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth

            // Create bitmap
            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            image.close()

            // Crop to exact screen size
            val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
            bitmap.recycle()

            return croppedBitmap
        } catch (e: Exception) {
            errorHandler.handleException("ScreenshotUtil.takeScreenshot", e)
            return null
        }
    }

    /**
     * Releases resources used for screenshots
     */
    fun release() {
        try {
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()

            virtualDisplay = null
            imageReader = null
            mediaProjection = null

            Timber.d("Screenshot resources released")
        } catch (e: Exception) {
            errorHandler.handleException("ScreenshotUtil.release", e)
        }
    }
}

