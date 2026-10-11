package com.gymbro.fitness.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.gymbro.fitness.MainActivity
import com.gymbro.fitness.R
import com.gymbro.fitness.data.GymBroDatabase
import com.gymbro.fitness.domain.FitnessCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Home-screen widget: workout streak plus calories and protein left for today. */
class GymBroWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                val views = buildViews(context.applicationContext)
                appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Call after logging food or finishing a workout so the widget isn't stale. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, GymBroWidget::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, GymBroWidget::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            )
        }

        private suspend fun buildViews(context: Context): RemoteViews {
            val database = GymBroDatabase.getDatabase(context, scope)
            val sessions = database.workoutSessionDao().getAllSessions().first()
            val streak = FitnessCalculator.workoutStreak(sessions.map { it.dateMillis })
            val profile = database.userProfileDao().getUserProfileOneShot()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val foods = database.foodLogDao().getFoodLogsForDay(today).first()

            val streakText = when (streak) {
                0 -> "Start a streak today 💪"
                1 -> "🔥 1 day streak"
                else -> "🔥 $streak day streak"
            }
            val macrosText = if (profile == null) {
                "Open GymBro to set your goals"
            } else {
                val caloriesLeft = (profile.targetCalories - foods.sumOf { it.calories }).coerceAtLeast(0)
                val proteinLeft = (profile.targetProteinGrams - foods.sumOf { it.proteinGrams }).coerceAtLeast(0)
                String.format(Locale.US, "%,d kcal · %d g protein left", caloriesLeft, proteinLeft)
            }

            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return RemoteViews(context.packageName, R.layout.widget_gymbro).apply {
                setTextViewText(R.id.widget_streak, streakText)
                setTextViewText(R.id.widget_macros, macrosText)
                setOnClickPendingIntent(R.id.widget_root, openApp)
            }
        }
    }
}
