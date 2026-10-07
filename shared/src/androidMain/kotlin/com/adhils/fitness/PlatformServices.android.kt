package com.adhils.fitness

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

actual object PlatformSound {
    private var toneGenerator: ToneGenerator? = null

    private fun getToneGen(): ToneGenerator? {
        if (toneGenerator == null) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 95)
            } catch (_: Exception) {}
        }
        return toneGenerator
    }

    actual fun playCountdownBeep() {
        try {
            getToneGen()?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (_: Exception) {}
    }

    actual fun playTimerFinishedChime() {
        try {
            getToneGen()?.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
        } catch (_: Exception) {}
    }
}

actual object PlatformStorage {
    var appContext: Context? = null
    var storageDir: File? = null

    private fun getFile(): File? {
        val dir = storageDir ?: appContext?.filesDir ?: return null
        return File(dir, "adhils-fitness-snapshot.json")
    }

    actual fun loadSnapshot(): String? {
        val file = getFile() ?: return null
        return try {
            if (file.exists()) file.readText(Charsets.UTF_8) else null
        } catch (_: Exception) {
            null
        }
    }

    actual fun saveSnapshot(json: String) {
        val file = getFile() ?: return
        try {
            file.writeText(json, Charsets.UTF_8)
        } catch (_: Exception) {}
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

actual object PlatformHealth {
    actual val platformName: String = "Samsung Health"
    actual val wearableName: String = "Galaxy Watch"
    actual val defaultWearableModel: String = "Galaxy Watch6"

    var onSyncWorkout: ((sessionId: String, calories: Int) -> Unit)? = null
    var onRequestAuthorization: (((Boolean) -> Unit) -> Unit)? = null

    actual fun requestAuthorization(onResult: (Boolean) -> Unit) {
        onRequestAuthorization?.invoke(onResult) ?: onResult(true)
    }

    actual fun syncWorkout(sessionId: String, calories: Int) {
        onSyncWorkout?.invoke(sessionId, calories)
    }
}

actual object PlatformNotification {
    var onCheckPermission: (((Boolean) -> Unit) -> Unit)? = null
    var onRequestPermission: (((Boolean) -> Unit) -> Unit)? = null
    var onScheduleReminder: ((hour: Int, minute: Int, daysOfWeek: Set<Int>) -> Unit)? = null
    var onSendTestNotification: ((title: String, message: String) -> Unit)? = null
    var onOpenSettings: (() -> Unit)? = null

    actual fun checkPermission(onResult: (Boolean) -> Unit) {
        onCheckPermission?.invoke(onResult) ?: onResult(true)
    }

    actual fun requestPermission(onResult: (Boolean) -> Unit) {
        onRequestPermission?.invoke(onResult) ?: onResult(true)
    }

    actual fun scheduleReminder(hour: Int, minute: Int, daysOfWeek: Set<Int>) {
        onScheduleReminder?.invoke(hour, minute, daysOfWeek)
    }

    actual fun sendTestNotification(title: String, message: String) {
        onSendTestNotification?.invoke(title, message)
    }

    actual fun openNotificationSettings() {
        onOpenSettings?.invoke()
    }
}

actual object PlatformImageLoader {
    actual suspend fun loadExerciseImage(exerciseId: String, url: String): androidx.compose.ui.graphics.ImageBitmap? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val context = PlatformStorage.appContext
        val baseDir = context?.cacheDir ?: PlatformStorage.storageDir ?: context?.filesDir
        val cacheDir = if (baseDir != null) File(baseDir, "exercise_thumbs").apply { mkdirs() } else null
        val diskFile = if (cacheDir != null) File(cacheDir, "$exerciseId.jpg") else null

        if (diskFile != null && diskFile.exists() && diskFile.length() > 0) {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeFile(diskFile.absolutePath)
                if (bitmap != null) return@withContext bitmap.asImageBitmap()
            } catch (_: Exception) {}
        }

        try {
            val bytes = java.net.URL(url).openStream().use { it.readBytes() }
            if (bytes.isNotEmpty()) {
                if (diskFile != null) {
                    try { diskFile.writeBytes(bytes) } catch (_: Exception) {}
                }
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) return@withContext bitmap.asImageBitmap()
            }
        } catch (_: Exception) {}
        null
    }

    actual suspend fun loadExerciseAnimation(exerciseId: String, url: String): List<ExerciseAnimationFrame>? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val context = PlatformStorage.appContext
        val baseDir = context?.cacheDir ?: PlatformStorage.storageDir ?: context?.filesDir
        val cacheDir = if (baseDir != null) File(baseDir, "exercise_gifs").apply { mkdirs() } else null
        val diskFile = if (cacheDir != null) File(cacheDir, "$exerciseId.gif") else null

        val bytes = if (diskFile != null && diskFile.exists() && diskFile.length() > 0) {
            try { diskFile.readBytes() } catch (_: Exception) { null }
        } else {
            try {
                val downloaded = java.net.URL(url).openStream().use { it.readBytes() }
                if (diskFile != null && downloaded.isNotEmpty()) {
                    try { diskFile.writeBytes(downloaded) } catch (_: Exception) {}
                }
                downloaded
            } catch (_: Exception) { null }
        } ?: return@withContext null

        try {
            @Suppress("DEPRECATION")
            val movie = android.graphics.Movie.decodeByteArray(bytes, 0, bytes.size)
            if (movie != null && movie.duration() > 0) {
                val duration = movie.duration()
                val stepMs = 100
                val frameCount = (duration / stepMs).coerceIn(4, 30)
                val width = movie.width().coerceAtLeast(1)
                val height = movie.height().coerceAtLeast(1)
                val result = mutableListOf<ExerciseAnimationFrame>()
                for (i in 0 until frameCount) {
                    val t = i * stepMs
                    movie.setTime(t)
                    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    movie.draw(canvas, 0f, 0f)
                    result.add(ExerciseAnimationFrame(bitmap.asImageBitmap(), stepMs.toLong()))
                }
                if (result.isNotEmpty()) return@withContext result
            }
        } catch (_: Exception) {}
        null
    }
}

object PlatformCameraBridge {
    var onRenderCameraPreview: (@Composable (
        modifier: androidx.compose.ui.Modifier,
        isFrontCamera: Boolean,
        onPermissionGranted: (Boolean) -> Unit,
        onPoseDetected: (List<com.adhils.fitness.core.Joint>) -> Unit
    ) -> Unit)? = null
}

@Composable
actual fun PlatformCameraPreview(
    modifier: androidx.compose.ui.Modifier,
    isFrontCamera: Boolean,
    onPermissionGranted: (Boolean) -> Unit,
    onPoseDetected: (List<com.adhils.fitness.core.Joint>) -> Unit
) {
    val render = PlatformCameraBridge.onRenderCameraPreview
    if (render != null) {
        render(modifier, isFrontCamera, onPermissionGranted, onPoseDetected)
    } else {
        androidx.compose.foundation.layout.Box(
            modifier = modifier.background(androidx.compose.ui.graphics.Color(0xFF141416)),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = "● CAMERA LIVE",
                color = androidx.compose.ui.graphics.Color(0xFF34C759)
            )
        }
    }
}

actual val isNativeCameraOverlaySupported: Boolean = false

actual object PlatformScreen {
    var onSetKeepScreenOn: ((Boolean) -> Unit)? = null
    actual fun setKeepScreenOn(enabled: Boolean) {
        onSetKeepScreenOn?.invoke(enabled)
    }
}

@android.annotation.SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun PlatformWebView(
    url: String,
    modifier: androidx.compose.ui.Modifier,
    onUrlChange: ((String) -> Unit)?
) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webChromeClient = android.webkit.WebChromeClient()
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                        val newUrl = request?.url?.toString() ?: return false
                        if (newUrl.startsWith("http://") || newUrl.startsWith("https://")) {
                            onUrlChange?.invoke(newUrl)
                            return false
                        }
                        return true
                    }
                    override fun onPageFinished(view: android.webkit.WebView?, finishedUrl: String?) {
                        if (!finishedUrl.isNullOrBlank()) {
                            onUrlChange?.invoke(finishedUrl)
                        }
                    }
                }
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url && !webView.url.isNullOrBlank() == false) {
                webView.loadUrl(url)
            }
        },
        onRelease = { it.destroy() },
        modifier = modifier
    )
}
