package com.adhils.fitness

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

object AndroidWorkoutNotificationManager {
    const val CHANNEL_ID = "adhils_workout_previews"
    const val NOTIFICATION_ID_PREVIEW = 1001
    const val ALARM_REQUEST_CODE = 2001
    const val ACTION_WORKOUT_PREVIEW = "com.adhils.fitness.ACTION_WORKOUT_PREVIEW"

    /**
     * Creates the high-importance Notification Channel for workout previews.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Workout Previews & Reminders"
            val descriptionText = "Daily preview notifications for your scheduled training days"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                lightColor = AndroidColor.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Checks if notification permissions are granted and notifications are enabled for the app.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) {
            return false
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Dispatches an immediate workout preview notification (e.g. confirmation or scheduled alarm trigger).
     */
    fun sendWorkoutPreviewNotification(
        context: Context,
        previewTime: String = "9:00 AM",
        title: String = "TODAY'S WORKOUT IS READY 🏋️",
        message: String = "Push Day: Barbell Bench Press, Dumbbell Shoulder Press, Lateral Raise & 3 more."
    ) {
        createNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            android.util.Log.w("WorkoutNotification", "Cannot post notification: permission not granted")
            return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAV_ROUTE", "today")
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setColor(0xFFFF2D55.toInt())
            .setContentTitle(title)
            .setContentText(message)
            .setSubText("Preview · $previewTime")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(message)
                    .setSummaryText("Workout preview for $previewTime")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_launcher, "Start Workout", contentPendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID_PREVIEW, builder.build())
        } catch (e: SecurityException) {
            android.util.Log.e("WorkoutNotification", "SecurityException posting notification", e)
        }
    }

    /**
     * Schedules the next daily workout preview using AlarmManager.
     */
    fun scheduleWorkoutPreview(context: Context, timeStr: String) {
        val (hour, minute) = parseHourAndMinute(timeStr)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WorkoutReminderReceiver::class.java).apply {
            action = ACTION_WORKOUT_PREVIEW
            putExtra("PREVIEW_TIME", timeStr)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            android.util.Log.d("WorkoutNotification", "Scheduled workout preview alarm for ${calendar.time}")
        } catch (e: SecurityException) {
            android.util.Log.e("WorkoutNotification", "SecurityException setting alarm", e)
        }
    }

    /**
     * Cancels any previously scheduled workout preview alarm.
     */
    fun cancelWorkoutPreview(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WorkoutReminderReceiver::class.java).apply {
            action = ACTION_WORKOUT_PREVIEW
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Opens system notification settings for this application.
     */
    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Helper to parse time strings like "9:00 AM", "09:00 AM", "6:30 PM", "18:00".
     */
    fun parseHourAndMinute(timeStr: String): Pair<Int, Int> {
        return try {
            val upper = timeStr.trim().uppercase()
            val isPm = upper.contains("PM")
            val isAm = upper.contains("AM")
            val clean = upper.replace("AM", "").replace("PM", "").trim()
            val parts = clean.split(":")
            var hour = parts[0].trim().toIntOrNull() ?: 9
            val minute = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 0 else 0

            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0

            Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        } catch (_: Exception) {
            Pair(9, 0)
        }
    }
}
