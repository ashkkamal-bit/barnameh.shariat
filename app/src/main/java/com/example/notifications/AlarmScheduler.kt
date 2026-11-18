package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.PersonalGoal
import com.example.data.model.TaskItem
import com.example.data.repository.NocturnePreferences
import com.example.domain.RecurrenceEngine
import com.example.util.PersianCalendarUtil
import java.util.Calendar
import java.util.TimeZone

data class AlarmScheduleReport(
    val scheduledTaskAlarms: Int,
    val usedInexactFallback: Boolean,
    val exactAlarmPermitted: Boolean,
    val postNotificationPermitted: Boolean
)

object AlarmScheduler {

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_OFFSET_MINUTES = "extra_offset_minutes"
    const val EXTRA_OCCURRENCE_DATE = "extra_occurrence_date"
    const val EXTRA_GOAL_ID = "extra_goal_id"
    const val EXTRA_SUMMARY_KIND = "extra_summary_kind"

    const val ACTION_TASK_REMINDER = "com.example.nocturne.ACTION_TASK_REMINDER"
    const val ACTION_MORNING_SUMMARY = "com.example.nocturne.ACTION_MORNING_SUMMARY"
    const val ACTION_EVENING_SUMMARY = "com.example.nocturne.ACTION_EVENING_SUMMARY"
    const val ACTION_GOAL_REMINDER = "com.example.nocturne.ACTION_GOAL_REMINDER"

    private const val MORNING_REQUEST_CODE = 900_001
    private const val EVENING_REQUEST_CODE = 900_002
    private const val MAX_OFFSETS_PER_TASK = 8

    fun hasPostNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun taskRequestCode(taskId: Long, slotIndex: Int): Int {
        val base = ((taskId and 0xFFFFFFL).toInt()) * 10
        return base + slotIndex.coerceIn(0, 9)
    }

    fun snoozeRequestCode(taskId: Long): Int =
        ((taskId and 0xFFFFFFL).toInt()) * 10 + 9

    fun goalRequestCode(goalId: Long): Int =
        800_000 + (goalId.toInt() and 0xFFFF)

    fun scheduleAll(
        context: Context,
        tasks: List<TaskItem>,
        goals: List<PersonalGoal>,
        preferences: NocturnePreferences,
        nowMillis: Long = System.currentTimeMillis()
    ): AlarmScheduleReport {
        val exactPermitted = canScheduleExactAlarms(context)
        val notifPermitted = hasPostNotificationPermission(context)
        var scheduledCount = 0
        var usedInexact = false

        val timeZone = resolveTimeZone(preferences)
        val today = PersianCalendarUtil.today(timeZone)

        for (task in tasks) {
            cancelTaskAlarms(context, task.id)
            if (!preferences.taskRemindersEnabled || !task.remindersEnabled || task.isCompleted || !task.isScheduled) {
                continue
            }
            val nextDateKey = RecurrenceEngine.nextOccurrenceOnOrAfter(task, today.dateKey) ?: continue
            val hour = task.timeHour ?: continue
            val minute = task.timeMinute ?: continue
            val eventMillis = PersianCalendarUtil.jalaliToTimestampMillis(nextDateKey, hour, minute, timeZone)

            val offsets = task.reminderOffsetsMinutes.distinct().take(MAX_OFFSETS_PER_TASK)
            offsets.forEachIndexed { idx, offsetMin ->
                val triggerAt = eventMillis - (offsetMin * 60_000L)
                if (triggerAt > nowMillis - 10_000L) {
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        action = ACTION_TASK_REMINDER
                        putExtra(EXTRA_TASK_ID, task.id)
                        putExtra(EXTRA_OFFSET_MINUTES, offsetMin)
                        putExtra(EXTRA_OCCURRENCE_DATE, nextDateKey)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context,
                        taskRequestCode(task.id, idx),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val inexact = setAlarmTrigger(context, triggerAt.coerceAtLeast(nowMillis + 1_000L), pi, exactPermitted)
                    if (inexact) usedInexact = true
                    scheduledCount++
                }
            }
        }

        // Daily summaries
        cancelSummaryAlarm(context, isMorning = true)
        if (preferences.morningSummaryEnabled) {
            val nextMorning = nextDailyTriggerMillis(preferences.morningSummaryHour, 0, timeZone, nowMillis)
            val pi = summaryPendingIntent(context, isMorning = true)
            if (setAlarmTrigger(context, nextMorning, pi, exactPermitted)) usedInexact = true
        }

        cancelSummaryAlarm(context, isMorning = false)
        if (preferences.eveningSummaryEnabled) {
            val nextEvening = nextDailyTriggerMillis(preferences.eveningSummaryHour, 0, timeZone, nowMillis)
            val pi = summaryPendingIntent(context, isMorning = false)
            if (setAlarmTrigger(context, nextEvening, pi, exactPermitted)) usedInexact = true
        }

        // Goal / Habit reminders
        for (goal in goals) {
            cancelGoalAlarm(context, goal.id)
            if (preferences.habitRemindersEnabled && goal.isActive && goal.reminderEnabled) {
                val triggerAt = nextDailyTriggerMillis(goal.reminderHour, goal.reminderMinute, timeZone, nowMillis)
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_GOAL_REMINDER
                    putExtra(EXTRA_GOAL_ID, goal.id)
                }
                val pi = PendingIntent.getBroadcast(
                    context,
                    goalRequestCode(goal.id),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                if (setAlarmTrigger(context, triggerAt, pi, exactPermitted)) usedInexact = true
            }
        }

        return AlarmScheduleReport(
            scheduledTaskAlarms = scheduledCount,
            usedInexactFallback = usedInexact,
            exactAlarmPermitted = exactPermitted,
            postNotificationPermitted = notifPermitted
        )
    }

    fun scheduleTaskAlarms(
        context: Context,
        task: TaskItem,
        preferences: NocturnePreferences,
        nowMillis: Long = System.currentTimeMillis()
    ): AlarmScheduleReport {
        return scheduleAll(
            context = context,
            tasks = listOf(task),
            goals = emptyList(),
            preferences = preferences,
            nowMillis = nowMillis
        )
    }

    fun scheduleSnooze(
        context: Context,
        taskId: Long,
        snoozeMinutes: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val exactPermitted = canScheduleExactAlarms(context)
        val triggerAt = nowMillis + snoozeMinutes.coerceIn(1, 180) * 60_000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TASK_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_OFFSET_MINUTES, -snoozeMinutes) // negative marks snooze
            putExtra(EXTRA_OCCURRENCE_DATE, PersianCalendarUtil.today().dateKey)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return !setAlarmTrigger(context, triggerAt, pi, exactPermitted)
    }

    fun cancelTaskAlarms(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (slot in 0 until MAX_OFFSETS_PER_TASK) {
            val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_TASK_REMINDER }
            val pi = PendingIntent.getBroadcast(
                context,
                taskRequestCode(taskId, slot),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_TASK_REMINDER }
        val snoozePi = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode(taskId),
            snoozeIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (snoozePi != null) {
            alarmManager.cancel(snoozePi)
            snoozePi.cancel()
        }
    }

    fun cancelGoalAlarm(context: Context, goalId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_GOAL_REMINDER }
        val pi = PendingIntent.getBroadcast(
            context,
            goalRequestCode(goalId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    fun cancelSummaryAlarm(context: Context, isMorning: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = if (isMorning) ACTION_MORNING_SUMMARY else ACTION_EVENING_SUMMARY
        }
        val reqCode = if (isMorning) MORNING_REQUEST_CODE else EVENING_REQUEST_CODE
        val pi = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    private fun summaryPendingIntent(context: Context, isMorning: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = if (isMorning) ACTION_MORNING_SUMMARY else ACTION_EVENING_SUMMARY
            putExtra(EXTRA_SUMMARY_KIND, if (isMorning) "MORNING" else "EVENING")
        }
        return PendingIntent.getBroadcast(
            context,
            if (isMorning) MORNING_REQUEST_CODE else EVENING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Returns true if an inexact fallback was used.
     */
    internal fun setAlarmTrigger(
        context: Context,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
        exactPermitted: Boolean
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return true
        return try {
            if (exactPermitted) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                false
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                true
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            true
        }
    }

    fun resolveTimeZone(preferences: NocturnePreferences): TimeZone {
        return if (preferences.useSystemTimezone || preferences.customTimezoneId.isBlank()) {
            TimeZone.getDefault()
        } else {
            TimeZone.getTimeZone(preferences.customTimezoneId)
        }
    }

    private fun nextDailyTriggerMillis(
        hour: Int,
        minute: Int,
        timeZone: TimeZone,
        nowMillis: Long
    ): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis + 5_000L) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }
}
