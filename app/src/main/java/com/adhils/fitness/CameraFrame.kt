package com.adhils.fitness

import android.graphics.Bitmap
import android.graphics.Matrix
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage

internal data class AnalyzedCameraFrame<T>(val preview: Bitmap, val result: T)

/** Consumes source. Only the returned preview belongs to the UI; MPImage.close recycles its bitmap. */
internal fun <T> analyzeCameraFrame(
    source: Bitmap, rotationDegrees: Int, front: Boolean, analyze: (MPImage) -> T
): AnalyzedCameraFrame<T> {
    var rotated: Bitmap? = null
    var preview: Bitmap? = null
    try {
        rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height,
            Matrix().apply { postRotate(rotationDegrees.toFloat()) }, true)
        // Always copy, including zero rotation. createBitmap may otherwise return the input itself.
        preview = requireNotNull(rotated.copy(Bitmap.Config.ARGB_8888, false))
        if (front) {
            val flipped = Bitmap.createBitmap(preview, 0, 0, preview.width, preview.height,
                Matrix().apply { preScale(-1f, 1f) }, true)
            if (flipped !== preview) preview.recycle()
            preview = flipped
        }
        val result = BitmapImageBuilder(rotated).build().use { analyze(it) }
        return AnalyzedCameraFrame(preview, result)
    } catch (failure: Throwable) {
        preview?.recycle()
        throw failure
    } finally {
        // MPImage normally releases rotated; also clean up if setup/inference failed early.
        rotated?.let { if (!it.isRecycled) it.recycle() }
        if (!source.isRecycled) source.recycle()
    }
}
