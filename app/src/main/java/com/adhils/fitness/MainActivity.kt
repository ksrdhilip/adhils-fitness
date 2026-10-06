package com.adhils.fitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlatformStorage.appContext = applicationContext
        AndroidWorkoutNotificationManager.createNotificationChannel(applicationContext)

        PlatformNotification.onCheckPermission = { callback ->
            callback(AndroidWorkoutNotificationManager.hasNotificationPermission(applicationContext))
        }
        PlatformNotification.onRequestPermission = { callback ->
            callback(AndroidWorkoutNotificationManager.hasNotificationPermission(applicationContext))
        }
        PlatformNotification.onScheduleReminder = { hour, minute, _ ->
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val timeStr = "$displayHour:${minute.toString().padStart(2, '0')} $amPm"
            AndroidWorkoutNotificationManager.scheduleWorkoutPreview(applicationContext, timeStr)
        }
        PlatformNotification.onSendTestNotification = { title, message ->
            AndroidWorkoutNotificationManager.sendWorkoutPreviewNotification(
                context = applicationContext,
                title = title,
                message = message
            )
        }
        PlatformNotification.onOpenSettings = {
            AndroidWorkoutNotificationManager.openNotificationSettings(applicationContext)
        }

        PlatformScreen.onSetKeepScreenOn = { enabled ->
            runOnUiThread {
                if (enabled) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }

        PlatformCameraBridge.onRenderCameraPreview = { modifier, isFront, onPerm, onPose ->
            AndroidCameraPreview(modifier, isFront, onPerm, onPose)
        }

        enableEdgeToEdge()
        setContent {
            FitnessApp()
        }
    }
}
