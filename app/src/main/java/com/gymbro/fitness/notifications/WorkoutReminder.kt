package com.gymbro.fitness.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gymbro.fitness.MainActivity
import com.gymbro.fitness.R
import java.util.Calendar

/** Daily "time to train" reminder. Settings live in SharedPreferences so the boot receiver can read them. */
object WorkoutReminder {
    private const val PREFS = "gymbro_settings"
    private const val KEY_ENABLED = "reminder_enabled"
    private const val KEY_HOUR = "reminder_hour"
    private const val KEY_MINUTE = "reminder_minute"
    private const val CHANNEL_ID = "workout_reminder"
    private const val NOTIFICATION_ID = 1002
    private const val REQUEST_ALARM = 2003
    private const val REQUEST_OPEN_APP = 2004
    private const val WINDOW_MILLIS = 10 * 60 * 1000L

    data class Settings(val enabled: Boolean, val hour: Int, val minute: Int)

    fun settings(context: Context): Settings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Settings(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            hour = prefs.getInt(KEY_HOUR, 18),
            minute = prefs.getInt(KEY_MINUTE, 0)
        )
    }

    fun update(context: Context, settings: Settings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putInt(KEY_HOUR, settings.hour)
            .putInt(KEY_MINUTE, settings.minute)
            .apply()
        reschedule(context)
    }

    /** Schedules the next reminder if enabled, otherwise cancels it. */
    fun reschedule(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = alarmIntent(context)
        val current = settings(context)
        if (!current.enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, current.hour)
            set(Calendar.MINUTE, current.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        // A 10 minute window is fine for a reminder and needs no exact-alarm permission.
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, next.timeInMillis, WINDOW_MILLIS, pendingIntent)
    }

    @SuppressLint("MissingPermission") // guarded by RestTimerNotifier.canPostNotifications
    internal fun showReminder(context: Context) {
        if (!RestTimerNotifier.canPostNotifications(context)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Workout reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Daily reminder to train"
                }
            )
        }
        val openApp = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Time to train 💪")
            .setContentText("Your streak is waiting. Let's get a session in.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_ALARM,
        Intent(context, WorkoutReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

/** Fires the reminder and schedules tomorrow's; also restores the schedule after a reboot. */
class WorkoutReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isSystemEvent = intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (!isSystemEvent) {
            WorkoutReminder.showReminder(context)
        }
        WorkoutReminder.reschedule(context)
    }
}
