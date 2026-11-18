package com.example.domain

import com.example.data.model.RecurrenceType
import com.example.data.model.TaskItem
import com.example.util.PersianCalendarUtil

object RecurrenceEngine {

    /**
     * Expands recurring tasks for [dateKey] without duplicating database rows.
     * Respects recurrenceExceptions and recurrenceEndDateKey.
     */
    fun tasksForDate(allTasks: List<TaskItem>, dateKey: String): List<TaskItem> {
        val result = mutableListOf<TaskItem>()
        for (task in allTasks) {
            if (task.jalaliDateKey == dateKey) {
                if (task.recurrenceType != RecurrenceType.NONE && dateKey in task.recurrenceExceptions) {
                    continue
                }
                result.add(task)
            } else if (task.recurrenceType != RecurrenceType.NONE && occursOnDate(task, dateKey)) {
                result.add(task.copy(virtualOccurrenceDateKey = dateKey))
            }
        }
        return result.sortedWith(
            compareBy<TaskItem>(
                { if (it.isScheduled) 0 else 1 },
                { it.timeHour ?: Int.MAX_VALUE },
                { it.timeMinute ?: Int.MAX_VALUE },
                { it.id }
            )
        )
    }

    fun occursOnDate(task: TaskItem, targetDateKey: String): Boolean {
        if (task.recurrenceType == RecurrenceType.NONE) {
            return task.jalaliDateKey == targetDateKey
        }
        if (targetDateKey < task.jalaliDateKey) return false
        val endKey = task.recurrenceEndDateKey
        if (!endKey.isNullOrBlank() && targetDateKey > endKey) return false
        if (targetDateKey in task.recurrenceExceptions) return false

        if (targetDateKey == task.jalaliDateKey) return true

        val interval = task.recurrenceInterval.coerceAtLeast(1)
        val start = PersianCalendarUtil.parseDateKey(task.jalaliDateKey)
        val target = PersianCalendarUtil.parseDateKey(targetDateKey)

        return when (task.recurrenceType) {
            RecurrenceType.NONE -> false
            RecurrenceType.DAILY -> {
                val daysDiff = PersianCalendarUtil.daysBetween(task.jalaliDateKey, targetDateKey)
                daysDiff >= 0 && daysDiff % interval == 0
            }
            RecurrenceType.WEEKLY -> {
                val daysDiff = PersianCalendarUtil.daysBetween(task.jalaliDateKey, targetDateKey)
                daysDiff >= 0 && daysDiff % (7 * interval) == 0
            }
            RecurrenceType.MONTHLY -> {
                val monthsDiff = (target.year - start.year) * 12 + (target.month - start.month)
                if (monthsDiff < 0 || monthsDiff % interval != 0) return false
                val maxDayInTarget = PersianCalendarUtil.daysInJalaliMonth(target.year, target.month)
                val expectedDay = start.day.coerceAtMost(maxDayInTarget)
                target.day == expectedDay
            }
        }
    }

    /**
     * Returns the next upcoming occurrence dateKey >= [fromDateKey] for reminder rescheduling.
     */
    fun nextOccurrenceOnOrAfter(task: TaskItem, fromDateKey: String, maxDaysScan: Int = 90): String? {
        if (task.recurrenceType == RecurrenceType.NONE) {
            return if (task.jalaliDateKey >= fromDateKey) task.jalaliDateKey else null
        }
        val startSearch = if (fromDateKey > task.jalaliDateKey) fromDateKey else task.jalaliDateKey
        var cursor = PersianCalendarUtil.parseDateKey(startSearch)
        for (i in 0..maxDaysScan) {
            if (occursOnDate(task, cursor.dateKey)) {
                return cursor.dateKey
            }
            cursor = PersianCalendarUtil.addDays(cursor, 1)
        }
        return null
    }
}
