package com.gymbro.fitness.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            RestTimerNotifier.ACTION_ADD_TIME ->
                RestTimerNotifier.addTime(context, intent.getIntExtra(RestTimerNotifier.EXTRA_SECONDS, 15))
            RestTimerNotifier.ACTION_SKIP -> RestTimerNotifier.skip(context)
            else -> RestTimerNotifier.showRestOver(context)
        }
    }
}
