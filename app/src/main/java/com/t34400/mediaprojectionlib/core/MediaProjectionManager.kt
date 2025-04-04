package com.t34400.mediaprojectionlib.core

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Log
import kotlin.math.roundToInt

class MediaProjectionManager (
    context: Context,
    private val callback: MediaProjection.Callback? = null
): IMediaProjectionManager {
    private val width: Int
    private val height: Int
    private val densityDpi : Int

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    init {
        val metrics = context.resources.displayMetrics
        val rawWidth = metrics.widthPixels
        val rawHeight = metrics.heightPixels

        val scale = if (maxOf(rawWidth, rawHeight) > 960) {
            960f / maxOf(rawWidth, rawHeight)
        } else 1f

        width = (rawWidth * scale).roundToInt()
        height = (rawHeight * scale).roundToInt()
        densityDpi = metrics.densityDpi

        MediaProjectionRequestActivity.requestMediaProjection(context, this::registerMediaProjection)
    }

    override fun getCapturedScreenData(): ICapturedScreenData? {
        imageReader?.acquireLatestImage()?.let { image ->
            return CapturedScreenDataARGB(image)
        }

        return projection?.let { mediaProjection ->
            callback?.let {
                mediaProjection.registerCallback(callback, null)
            }

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

            imageReader?.let { imageReader ->
                val imageSurface = imageReader.surface

                virtualDisplay = mediaProjection.createVirtualDisplay(
                    "Projection",
                    width,
                    height,
                    densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageSurface,
                    null,
                    null
                )
            }

            return this.imageReader?.acquireLatestImage()?.let { image ->
                return CapturedScreenDataARGB(image)
            }
        }
    }

    @Suppress("unused")
    private fun stopMediaProjection(context: Context) {
        MediaProjectionRequestActivity.stopMediaProjection(context)

        projection?.stop()
        virtualDisplay?.release()
        imageReader?.close()

        projection = null
        virtualDisplay = null
        imageReader = null

        Log.d(TAG, "stopMediaProjection")
    }

    private fun registerMediaProjection(context: Context, resultData: Intent) {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        projection = projectionManager.getMediaProjection(RESULT_OK, resultData)

        Log.d(TAG, "registerMediaProjection")
    }

    companion object {
        private val TAG = MediaProjectionManager::class.java.simpleName
    }
}