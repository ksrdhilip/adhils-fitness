package com.adhils.fitness

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ConnectedWearable(
    val name: String = PlatformHealth.wearableName,
    val model: String = PlatformHealth.defaultWearableModel,
    val liveHeartRateBpm: Int = 138,
    val caloriesBurnedToday: Int = 420,
    val stepsToday: Int = 8450,
    val batteryPct: Int = 88
)

object HealthWearableManager {
    val connectedDevice = MutableStateFlow<ConnectedWearable?>(
        ConnectedWearable(
            name = PlatformHealth.wearableName,
            model = PlatformHealth.defaultWearableModel
        )
    )
    val isSyncing = MutableStateFlow(false)

    fun setHealthSyncEnabled(enabled: Boolean) {
        if (enabled) {
            connectedDevice.value = ConnectedWearable(
                name = PlatformHealth.wearableName,
                model = PlatformHealth.defaultWearableModel
            )
        } else {
            connectedDevice.value = null
        }
    }

    fun syncFromHealthConnect(context: Any? = null) {}

    fun syncWorkoutSession(context: Any? = null, session: Any? = null, calories: Int = 0) {
        PlatformHealth.syncWorkout(session?.toString() ?: "", calories)
    }
}

object HealthConnectManager {
    fun isSamsungHealthInstalled(context: Any? = null): Boolean = false
    fun createSettingsIntent(context: Any? = null): Any? = null
}

object WorkoutNotificationManager {
    fun hasNotificationPermission(context: Any? = null, callback: ((Boolean) -> Unit)? = null): Boolean {
        PlatformNotification.checkPermission { granted ->
            callback?.invoke(granted)
        }
        return true
    }

    fun requestNotificationPermission(callback: ((Boolean) -> Unit)? = null) {
        PlatformNotification.requestPermission { granted ->
            callback?.invoke(granted)
        }
    }

    fun createNotificationChannel(context: Any? = null) {}

    fun scheduleWorkoutPreview(context: Any? = null, time: String) {
        val (h, m) = parseTimeString(time)
        PlatformNotification.scheduleReminder(h, m, setOf(1, 2, 3, 4, 5, 6, 7))
    }

    fun sendWorkoutPreviewNotification(
        context: Any? = null,
        previewTime: String = "",
        title: String = "",
        message: String = ""
    ) {
        PlatformNotification.sendTestNotification(
            title = title.ifEmpty { "TODAY'S WORKOUT IS READY 🏋️" },
            message = message.ifEmpty { "Push Day: Barbell Bench Press, Shoulder Press & more." }
        )
    }

    fun cancelWorkoutPreview(context: Any? = null) {}

    fun openNotificationSettings(context: Any? = null) {
        PlatformNotification.openNotificationSettings()
    }
}

fun parseTimeString(timeStr: String): Pair<Int, Int> {
    val isPm = timeStr.contains("PM", ignoreCase = true)
    val isAm = timeStr.contains("AM", ignoreCase = true)
    val clean = timeStr.replace("AM", "", ignoreCase = true).replace("PM", "", ignoreCase = true).trim()
    val parts = clean.split(":")
    var hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 9
    val minute = parts.getOrNull(1)?.trim()?.take(2)?.toIntOrNull() ?: 0
    if (isPm && hour < 12) hour += 12
    if (isAm && hour == 12) hour = 0
    return Pair(hour, minute)
}
