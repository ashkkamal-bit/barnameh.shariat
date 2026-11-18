package com.example.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.local.NocturneDatabase
import com.example.data.local.toDomain
import com.example.data.repository.UserPreferencesRepository
import com.example.domain.InsightsCalculator
import com.example.util.PersianCalendarUtil
import com.example.widgets.NocturneWidgetUpdater
import java.util.concurrent.TimeUnit

class ReminderReconciliationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            ReminderReconciliationManager.reconcileAll(applicationContext, notifyOverdue = true)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

object ReminderReconciliationManager {

    private const val UNIQUE_PERIODIC_WORK = "nocturne_periodic_reconciliation"
    private const val UNIQUE_IMMEDIATE_WORK = "nocturne_immediate_reconciliation"
    private const val OVERDUE_NOTIF_ID = 7205
    private const val OVERDUE_PREF = "nocturne_overdue_guard"

    fun schedulePeriodicReconciliation(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderReconciliationWorker>(6, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun enqueueImmediateReconciliation(context: Context) {
        val request = OneTimeWorkRequestBuilder<ReminderReconciliationWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    suspend fun reconcileAll(
        context: Context,
        notifyOverdue: Boolean = false
    ): AlarmScheduleReport {
        NotificationChannels.ensureCreated(context)
        val db = NocturneDatabase.getInstance(context)
        val prefs = UserPreferencesRepository(context).getPreferencesOnce()
        val tasks = db.taskDao().getAllTasksOnce().map { it.toDomain() }
        val goals = db.goalDao().getAllGoalsOnce().map { it.toDomain() }

        val report = AlarmScheduler.scheduleAll(
            context = context,
            tasks = tasks,
            goals = goals,
            preferences = prefs
        )

        if (notifyOverdue && prefs.overdueAlertsEnabled && AlarmScheduler.hasPostNotificationPermission(context)) {
            maybeNotifyOverdueTasks(
                context = context,
                tasks = tasks,
                usePersianDigits = prefs.usePersianDigits,
                timeZone = AlarmScheduler.resolveTimeZone(prefs)
            )
        }

        NocturneWidgetUpdater.updateAll(context)
        return report
    }

    private fun maybeNotifyOverdueTasks(
        context: Context,
        tasks: List<com.example.data.model.TaskItem>,
        usePersianDigits: Boolean,
        timeZone: java.util.TimeZone
    ) {
        val nowCal = java.util.Calendar.getInstance(timeZone)
        val overdue = InsightsCalculator.findOverdueTasks(tasks, nowCal)
        if (overdue.isEmpty()) return

        val guard = context.getSharedPreferences(OVERDUE_PREF, Context.MODE_PRIVATE)
        val todayKey = PersianCalendarUtil.fromCalendar(nowCal).dateKey
        val lastNotified = guard.getString("last_date", "")
        if (lastNotified == todayKey) return

        val countStr = PersianCalendarUtil.formatNumber(overdue.size, usePersianDigits)
        val firstTitle = overdue.first().title
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            OVERDUE_NOTIF_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_IMPORTANT_ACTIVITIES)
            .setSmallIcon(R.drawable.ic_notification_nocturne)
            .setContentTitle("$countStr کار عقب‌افتاده در انتظار بررسی")
            .setContentText("اولین مورد: $firstTitle")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(OVERDUE_NOTIF_ID, notif)
            guard.edit().putString("last_date", todayKey).apply()
        } catch (_: SecurityException) {
        }
    }
}
