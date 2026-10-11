package com.gymbro.fitness.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gymbro.fitness.MainActivity
import com.gymbro.fitness.R
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Rest timer outside the app: an ongoing countdown notification while resting, and an alarm
 * that replaces it with a "rest over" alert. The alarm fires even if the process was killed.
 */
object RestTimerNotifier {
    /** When the current rest ends (null when not resting). Shared by the app UI and notification actions. */
    val endAtMillis = MutableStateFlow<Long?>(null)

    const val ACTION_ADD_TIME = "com.gymbro.fitness.REST_ADD_TIME"
    const val ACTION_SKIP = "com.gymbro.fitness.REST_SKIP"
    const val EXTRA_SECONDS = "seconds"
    private const val REQUEST_ADD_TIME = 2005
    private const val REQUEST_SKIP = 2006

    private const val CHANNEL_COUNTDOWN = "rest_timer_countdown"
    private const val CHANNEL_DONE = "rest_timer_done"
    private const val NOTIFICATION_ID = 1001
    private const val REQUEST_ALARM = 2001
    private const val REQUEST_OPEN_APP = 2002
    private const val ALARM_WINDOW_MILLIS = 1_000L

    @SuppressLint("MissingPermission") // guarded by canPostNotifications
    fun start(context: Context, seconds: Int) {
        schedule(context, System.currentTimeMillis() + seconds * 1000L)
    }

    /** Extends (or shortens, with a negative value) the current rest. */
    fun addTime(context: Context, seconds: Int) {
        val current = endAtMillis.value ?: return
        val updated = current + seconds * 1000L
        if (updated <= System.currentTimeMillis()) skip(context) else schedule(context, updated)
    }

    fun skip(context: Context) {
        cancel(context)
    }

    @SuppressLint("MissingPermission") // guarded by canPostNotifications
    private fun schedule(context: Context, endAtMillis: Long) {
        this.endAtMillis.value = endAtMillis
        ensureChannels(context)
        if (canPostNotifications(context)) {
            val notification = NotificationCompat.Builder(context, CHANNEL_COUNTDOWN)
                .setSmallIcon(R.drawable.ic_stat_timer)
                .setContentTitle("Resting")
                .setContentText("Next set when the timer hits zero")
                .setWhen(endAtMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
                .setContentIntent(openAppIntent(context))
                .addAction(0, "+15s", actionIntent(context, ACTION_ADD_TIME, REQUEST_ADD_TIME))
                .addAction(0, "Skip", actionIntent(context, ACTION_SKIP, REQUEST_SKIP))
                .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        // setWindow needs no exact-alarm permission and still fires within about a second while the phone is awake.
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, endAtMillis, ALARM_WINDOW_MILLIS, alarmIntent(context))
    }

    fun cancel(context: Context) {
        endAtMillis.value = null
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context))
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /** Called by [RestTimerReceiver] when the rest period ends. */
    @SuppressLint("MissingPermission") // guarded by canPostNotifications
    fun showRestOver(context: Context) {
        endAtMillis.value = null
        ensureChannels(context)
        if (!canPostNotifications(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Rest over 💪")
            .setContentText("Time for your next set")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setTimeoutAfter(60_000)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_COUNTDOWN, "Rest timer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Countdown between sets"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "Rest finished", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alert when it's time for the next set"
                enableVibration(true)
            }
        )
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_ALARM,
        Intent(context, RestTimerReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun actionIntent(context: Context, action: String, requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, RestTimerReceiver::class.java).setAction(action).putExtra(EXTRA_SECONDS, 15),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN_APP,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
