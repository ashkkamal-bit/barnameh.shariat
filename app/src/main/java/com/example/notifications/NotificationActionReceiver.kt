package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.NocturneDatabase
import com.example.widgets.NocturneWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE_TASK = "com.example.nocturne.ACTION_COMPLETE_TASK"
        const val ACTION_SNOOZE_TASK = "com.example.nocturne.ACTION_SNOOZE_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        if (taskId <= 0L) return

        val pendingResult: PendingResult? = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = NocturneDatabase.getInstance(context)
                when (intent.action) {
                    ACTION_COMPLETE_TASK -> {
                        db.taskDao().setTaskCompletedAtomic(taskId, true)
                        AlarmScheduler.cancelTaskAlarms(context, taskId)
                        NocturneWidgetUpdater.updateAll(context)
                        if (notifId > 0) {
                            NotificationManagerCompat.from(context).cancel(notifId)
                        }
                    }
                    ACTION_SNOOZE_TASK -> {
                        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10).coerceIn(1, 60)
                        AlarmScheduler.scheduleSnooze(context, taskId, snoozeMinutes)
                        if (notifId > 0) {
                            NotificationManagerCompat.from(context).cancel(notifId)
                        }
                    }
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
