package com.adhils.fitness

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WorkoutReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        android.util.Log.d("WorkoutReminderReceiver", "Received action: $action")

        val previewTime = intent.getStringExtra("PREVIEW_TIME") ?: "9:00 AM"

        if (action == AndroidWorkoutNotificationManager.ACTION_WORKOUT_PREVIEW) {
            // Post the notification
            AndroidWorkoutNotificationManager.sendWorkoutPreviewNotification(
                context = context,
                previewTime = previewTime,
                title = "TODAY'S WORKOUT IS READY 🏋️",
                message = "Push Day: Barbell Bench Press, Dumbbell Shoulder Press, Lateral Raise & 3 more."
            )
            // Reschedule for next day
            AndroidWorkoutNotificationManager.scheduleWorkoutPreview(context, previewTime)
        } else if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-register channel & reschedule on reboot
            AndroidWorkoutNotificationManager.createNotificationChannel(context)
            AndroidWorkoutNotificationManager.scheduleWorkoutPreview(context, previewTime)
        }
    }
}
