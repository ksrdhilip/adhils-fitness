package com.adhils.fitness

import androidx.compose.runtime.Composable

expect object PlatformSound {
    fun playCountdownBeep()
    fun playTimerFinishedChime()
}

expect object PlatformStorage {
    fun loadSnapshot(): String?
    fun saveSnapshot(json: String)
}

@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)

expect object PlatformHealth {
    val platformName: String
    val wearableName: String
    val defaultWearableModel: String
    fun requestAuthorization(onResult: (Boolean) -> Unit)
    fun syncWorkout(sessionId: String, calories: Int)
}

expect object PlatformNotification {
    fun checkPermission(onResult: (Boolean) -> Unit)
    fun requestPermission(onResult: (Boolean) -> Unit)
    fun scheduleReminder(hour: Int, minute: Int, daysOfWeek: Set<Int>)
    fun sendTestNotification(title: String, message: String)
    fun openNotificationSettings()
}

data class ExerciseAnimationFrame(
    val bitmap: androidx.compose.ui.graphics.ImageBitmap,
    val durationMs: Long = 100L
)

expect object PlatformImageLoader {
    suspend fun loadExerciseImage(exerciseId: String, url: String): androidx.compose.ui.graphics.ImageBitmap?
    suspend fun loadExerciseAnimation(exerciseId: String, url: String): List<ExerciseAnimationFrame>?
}

@Composable
expect fun PlatformCameraPreview(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    isFrontCamera: Boolean = true,
    onPermissionGranted: (Boolean) -> Unit = {},
    onPoseDetected: (List<com.adhils.fitness.core.Joint>) -> Unit = {}
)

expect val isNativeCameraOverlaySupported: Boolean

expect object PlatformScreen {
    fun setKeepScreenOn(enabled: Boolean)
}

@Composable
expect fun PlatformWebView(
    url: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    onUrlChange: ((String) -> Unit)? = null
)

