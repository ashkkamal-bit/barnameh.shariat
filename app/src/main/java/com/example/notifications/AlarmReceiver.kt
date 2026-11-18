package com.example.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.NocturneDatabase
import com.example.data.local.toDomain
import com.example.data.model.TaskPriority
import com.example.data.repository.UserPreferencesRepository
import com.example.domain.InsightsCalculator
import com.example.domain.RecurrenceEngine
import com.example.util.PersianCalendarUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_OPEN_TASK_ID = "extra_open_task_id"
        private const val PREF_DEDUP = "nocturne_notif_dedup"
    }

    override fun onReceive(context: Context, intent: Intent) {
        NotificationChannels.ensureCreated(context)
        if (!AlarmScheduler.hasPostNotificationPermission(context)) return

        val pendingResult: PendingResult? = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    AlarmScheduler.ACTION_TASK_REMINDER -> handleTaskReminder(context, intent)
                    AlarmScheduler.ACTION_MORNING_SUMMARY -> handleDailySummary(context, isMorning = true)
                    AlarmScheduler.ACTION_EVENING_SUMMARY -> handleDailySummary(context, isMorning = false)
                    AlarmScheduler.ACTION_GOAL_REMINDER -> handleGoalReminder(context, intent)
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    private suspend fun handleTaskReminder(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return
        val offsetMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_OFFSET_MINUTES, 0)
        val occurrenceDate = intent.getStringExtra(AlarmScheduler.EXTRA_OCCURRENCE_DATE)
            ?: PersianCalendarUtil.today().dateKey

        val db = NocturneDatabase.getInstance(context)
        val task = db.taskDao().getTaskById(taskId)?.toDomain() ?: return
        if (task.isCompleted || !task.remindersEnabled) return

        // Duplicate prevention within same minute
        val dedupKey = "task_${taskId}_${offsetMinutes}_${occurrenceDate}_${System.currentTimeMillis() / 60_000L}"
        if (isDuplicateAndMark(context, dedupKey)) return

        val prefs = UserPreferencesRepository(context).getPreferencesOnce()
        val channelId = if (task.priority == TaskPriority.HIGH) {
            NotificationChannels.CHANNEL_IMPORTANT_ACTIVITIES
        } else {
            NotificationChannels.CHANNEL_TASK_REMINDERS
        }

        val subtitle = when {
            offsetMinutes < 0 -> "یادآوری مجدد (تعویق‌یافته)"
            offsetMinutes == 0 -> "زمان شروع فعالیت فرا رسیده است"
            else -> "${PersianCalendarUtil.formatNumber(offsetMinutes, prefs.usePersianDigits)} دقیقه تا شروع فعالیت"
        }
        val timeText = task.formatTime(prefs.usePersianDigits)
        val bodyText = if (task.aiNotes.isNotBlank()) {
            "$subtitle • ساعت $timeText\n${task.aiNotes}"
        } else {
            "$subtitle • ساعت $timeText (${task.category})"
        }

        val notifId = (taskId.toInt() * 31 + offsetMinutes).coerceAtLeast(100)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TASK_ID, taskId)
        }
        val openPi = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeAction = buildAction(
            context = context,
            requestCode = notifId * 10 + 1,
            action = NotificationActionReceiver.ACTION_COMPLETE_TASK,
            taskId = taskId,
            notifId = notifId,
            label = "انجام شد"
        )
        val snooze5 = buildAction(
            context = context,
            requestCode = notifId * 10 + 2,
            action = NotificationActionReceiver.ACTION_SNOOZE_TASK,
            taskId = taskId,
            notifId = notifId,
            snoozeMinutes = 5,
            label = "۵ دقیقه"
        )
        val snooze10 = buildAction(
            context = context,
            requestCode = notifId * 10 + 3,
            action = NotificationActionReceiver.ACTION_SNOOZE_TASK,
            taskId = taskId,
            notifId = notifId,
            snoozeMinutes = 10,
            label = "۱۰ دقیقه"
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_nocturne)
            .setContentTitle(task.title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(
                if (task.priority == TaskPriority.HIGH) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .addAction(0, "انجام شد", completeAction)
            .addAction(0, "تعویق ۵د", snooze5)
            .addAction(0, "تعویق ۱۰د", snooze10)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {
        }

        // If recurring, schedule next occurrence cleanly
        if (task.recurrenceType != com.example.data.model.RecurrenceType.NONE && offsetMinutes == 0) {
            val tomorrow = PersianCalendarUtil.offsetFromToday(1).dateKey
            val nextOcc = RecurrenceEngine.nextOccurrenceOnOrAfter(task, tomorrow)
            if (nextOcc != null) {
                AlarmScheduler.scheduleTaskAlarms(context, task, prefs)
            }
        }
    }

    private suspend fun handleDailySummary(context: Context, isMorning: Boolean) {
        val db = NocturneDatabase.getInstance(context)
        val prefs = UserPreferencesRepository(context).getPreferencesOnce()
        val allTasks = db.taskDao().getAllTasksOnce().map { it.toDomain() }
        val today = PersianCalendarUtil.today()
        val metrics = InsightsCalculator.calculateDayMetrics(allTasks, today.dateKey)

        val dedupKey = "summary_${if (isMorning) "m" else "e"}_${today.dateKey}"
        if (isDuplicateAndMark(context, dedupKey)) return

        val title = if (isMorning) {
            "برنامه صبحگاهی • ${today.formattedDayAndMonth(prefs.usePersianDigits)}"
        } else {
            "جمع‌بندی روزانه برنامه ریز شریعت"
        }
        val body = if (isMorning) {
            if (metrics.totalTasks == 0) {
                "امروز هنوز کاری ثبت نشده است. با دستیار صوتی برنامه‌ریزی کنید."
            } else {
                "${PersianCalendarUtil.formatNumber(metrics.totalTasks, prefs.usePersianDigits)} کار برنامه‌ریزی شده (${
                    PersianCalendarUtil.formatNumber(metrics.highPriorityPendingCount, prefs.usePersianDigits)
                } فوری) • مجموع زمان: ${PersianCalendarUtil.formatDurationMinutes(metrics.totalWorkloadMinutes, prefs.usePersianDigits)}"
            }
        } else {
            "از ${PersianCalendarUtil.formatNumber(metrics.totalTasks, prefs.usePersianDigits)} فعالیت امروز، ${
                PersianCalendarUtil.formatNumber(metrics.completedCount, prefs.usePersianDigits)
            } مورد تکمیل شد (${PersianCalendarUtil.formatNumber(metrics.completionPercent, prefs.usePersianDigits)}٪)."
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            if (isMorning) 7101 else 7102,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_DAILY_SUMMARIES)
            .setSmallIcon(R.drawable.ic_notification_nocturne)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(if (isMorning) 7101 else 7102, notification)
        } catch (_: SecurityException) {
        }

        // Schedule next day's summary
        val goals = db.goalDao().getAllGoalsOnce().map { it.toDomain() }
        AlarmScheduler.scheduleAll(context, allTasks, goals, prefs)
    }

    private suspend fun handleGoalReminder(context: Context, intent: Intent) {
        val goalId = intent.getLongExtra(AlarmScheduler.EXTRA_GOAL_ID, -1L)
        if (goalId <= 0L) return
        val db = NocturneDatabase.getInstance(context)
        val goal = db.goalDao().getAllGoalsOnce().firstOrNull { it.id == goalId }?.toDomain() ?: return
        if (!goal.isActive || !goal.reminderEnabled) return

        val todayKey = PersianCalendarUtil.today().dateKey
        val alreadyLogged = db.goalDao().getLogForDate(goalId, todayKey) != null
        if (alreadyLogged) return

        val dedupKey = "goal_${goalId}_$todayKey"
        if (isDuplicateAndMark(context, dedupKey)) return

        val notifId = 8000 + goalId.toInt()
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_HABIT_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_nocturne)
            .setContentTitle("یادآوری هدف: ${goal.title}")
            .setContentText("ثبت پیشرفت امروز (${goal.period.labelFa}) را فراموش نکنید.")
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun buildAction(
        context: Context,
        requestCode: Int,
        action: String,
        taskId: Long,
        notifId: Int,
        snoozeMinutes: Int = 0,
        label: String
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIF_ID, notifId)
            putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun isDuplicateAndMark(context: Context, key: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_DEDUP, Context.MODE_PRIVATE)
        if (prefs.getBoolean(key, false)) return true
        val editor = prefs.edit()
        if (prefs.all.size > 60) editor.clear()
        editor.putBoolean(key, true).apply()
        return false
    }
}
