package com.example.widgets

import android.content.Context
import com.example.data.local.NocturneDatabase
import com.example.data.local.toDomain
import com.example.data.model.TaskItem
import com.example.data.repository.NocturnePreferences
import com.example.data.repository.UserPreferencesRepository
import com.example.domain.InsightsCalculator
import com.example.domain.RecurrenceEngine
import com.example.notifications.AlarmScheduler
import com.example.util.JalaliDate
import com.example.util.PersianCalendarUtil
import java.util.Calendar

data class WidgetSnapshot(
    val todayDate: JalaliDate,
    val currentTimeFormatted: String,
    val nextScheduledTask: TaskItem?,
    val remainingLabel: String,
    val nextReminderLabel: String?,
    val visibleTasks: List<TaskItem>,
    val pendingTodayCount: Int,
    val completedTodayCount: Int,
    val overdueTasks: List<TaskItem>,
    val totalTodayCount: Int,
    val progressPercent: Int,
    val preferences: NocturnePreferences
) {
    fun displayTitle(task: TaskItem): String {
        return if (task.isPrivate && preferences.widgetHidePrivateTitles) {
            "فعالیت خصوصی"
        } else {
            task.title
        }
    }
}

object WidgetDataProvider {

    suspend fun loadSnapshot(
        context: Context,
        nowCal: Calendar? = null
    ): WidgetSnapshot {
        val prefs = UserPreferencesRepository(context).getPreferencesOnce()
        val timeZone = AlarmScheduler.resolveTimeZone(prefs)
        val cal = nowCal ?: Calendar.getInstance(timeZone)
        val today = PersianCalendarUtil.fromCalendar(cal)

        val db = NocturneDatabase.getInstance(context)
        val allTasks = db.taskDao().getAllTasksOnce().map { it.toDomain() }
        val todayTasks = RecurrenceEngine.tasksForDate(allTasks, today.dateKey)

        val pending = todayTasks.filter { !it.isCompleted }
        val completed = todayTasks.filter { it.isCompleted }
        val overdue = InsightsCalculator.findOverdueTasks(allTasks, cal)

        val nextScheduled = pending
            .filter { it.isScheduled }
            .firstOrNull { task ->
                val mins = PersianCalendarUtil.minutesUntil(task.effectiveDateKey, task.timeHour, task.timeMinute, cal)
                mins != null && mins >= -15L
            } ?: pending.firstOrNull { it.isScheduled }

        val remainingMins = nextScheduled?.let {
            PersianCalendarUtil.minutesUntil(it.effectiveDateKey, it.timeHour, it.timeMinute, cal)
        }
        val remainingLabel = if (remainingMins != null) {
            if (remainingMins < 0L) "در حال اجرا / گذشته"
            else PersianCalendarUtil.formatRemainingPersian(remainingMins, prefs.usePersianDigits)
        } else {
            "بدون زمان مشخص"
        }

        val nextReminderLabel = nextScheduled?.let { task ->
            val offset = task.reminderOffsetsMinutes.maxOrNull()
            if (task.remindersEnabled && offset != null && task.timeHour != null && task.timeMinute != null) {
                val totalMin = (task.timeHour * 60 + task.timeMinute - offset).coerceAtLeast(0)
                val rh = totalMin / 60
                val rm = totalMin % 60
                "یادآور ${PersianCalendarUtil.formatTime(rh, rm, prefs.usePersianDigits)}"
            } else {
                null
            }
        }

        val filteredTasks = if (prefs.widgetShowCompleted) todayTasks else pending
        val total = todayTasks.size
        val pct = if (total == 0) 0 else ((completed.size * 100f) / total).toInt()

        val timeFormatted = PersianCalendarUtil.formatTime(
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            prefs.usePersianDigits
        )

        return WidgetSnapshot(
            todayDate = today,
            currentTimeFormatted = timeFormatted,
            nextScheduledTask = nextScheduled,
            remainingLabel = remainingLabel,
            nextReminderLabel = nextReminderLabel,
            visibleTasks = filteredTasks,
            pendingTodayCount = pending.size,
            completedTodayCount = completed.size,
            overdueTasks = overdue,
            totalTodayCount = total,
            progressPercent = pct,
            preferences = prefs
        )
    }
}
