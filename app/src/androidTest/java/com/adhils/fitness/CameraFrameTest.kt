package com.adhils.fitness

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.mediapipe.framework.image.BitmapImageBuilder
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraFrameTest {
    @Test fun closingMediaPipeImageReproducesOriginalInvalidPreview() {
        val source = Bitmap.createBitmap(8, 4, Bitmap.Config.ARGB_8888)
        BitmapImageBuilder(source).build().close()
        assertTrue(source.isRecycled)
        val destination = Bitmap.createBitmap(8, 4, Bitmap.Config.ARGB_8888)
        try {
            assertThrows(RuntimeException::class.java) { Canvas(destination).drawBitmap(source, 0f, 0f, null) }
        } finally { destination.recycle() }
    }

    @Test fun rearPreviewStaysDrawableAfterInferenceAtEveryDeviceRotation() {
        for (rotation in listOf(0, 90, 180, 270)) {
            val source = Bitmap.createBitmap(8, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
            val frame = analyzeCameraFrame(source, rotation, false) { "analysis complete" }
            assertTrue(source.isRecycled)
            assertFalse(frame.preview.isRecycled)
            assertEquals("analysis complete", frame.result)
            assertEquals(if (rotation % 180 == 0) 8 else 4, frame.preview.width)
            val destination = Bitmap.createBitmap(frame.preview.width, frame.preview.height, Bitmap.Config.ARGB_8888)
            try {
                Canvas(destination).drawBitmap(frame.preview, 0f, 0f, null)
                assertEquals(Color.GREEN, destination.getPixel(1, 1))
            } finally { destination.recycle(); frame.preview.recycle() }
        }
    }

    @Test fun frontPreviewIsMirroredAndRemainsDrawable() {
        val source = Bitmap.createBitmap(8, 4, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLUE)
            for (x in 0..3) for (y in 0..3) setPixel(x, y, Color.RED)
        }
        val frame = analyzeCameraFrame(source, 0, true) { Unit }
        val destination = Bitmap.createBitmap(8, 4, Bitmap.Config.ARGB_8888)
        try {
            Canvas(destination).drawBitmap(frame.preview, 0f, 0f, null)
            assertEquals(Color.BLUE, destination.getPixel(1, 1))
            assertEquals(Color.RED, destination.getPixel(6, 1))
        } finally { destination.recycle(); frame.preview.recycle() }
    }

    @Test fun failedInferenceReleasesSourceAndPreservesError() {
        val source = Bitmap.createBitmap(8, 4, Bitmap.Config.ARGB_8888)
        val failure = IllegalStateException("Test inference failure")
        val thrown = assertThrows(IllegalStateException::class.java) {
            analyzeCameraFrame(source, 90, false) { throw failure }
        }
        assertSame(failure, thrown)
        assertTrue(source.isRecycled)
    }
}
