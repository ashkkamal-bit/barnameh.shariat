package com.example.data.model

import com.example.util.PersianCalendarUtil

enum class TaskPriority(val level: Int, val labelFa: String) {
    LOW(0, "عادی"),
    MEDIUM(1, "متوسط"),
    HIGH(2, "فوری");

    companion object {
        fun fromLevel(level: Int): TaskPriority = entries.find { it.level == level } ?: LOW
        fun fromNameOrNull(raw: String?): TaskPriority? = when (raw?.trim()?.uppercase()) {
            "HIGH", "URGENT", "فوری", "مهم", "زیاد" -> HIGH
            "MEDIUM", "NORMAL", "متوسط" -> MEDIUM
            "LOW", "عادی", "کم" -> LOW
            else -> null
        }
    }
}

enum class RecurrenceType(val code: String, val labelFa: String) {
    NONE("NONE", "بدون تکرار"),
    DAILY("DAILY", "روزانه"),
    WEEKLY("WEEKLY", "هفتگی"),
    MONTHLY("MONTHLY", "ماهانه");

    companion object {
        fun fromCode(code: String?): RecurrenceType =
            entries.find { it.code.equals(code?.trim(), ignoreCase = true) } ?: NONE
    }
}

enum class RecurrenceEditScope(val labelFa: String) {
    ONLY_THIS("فقط همین نوبت"),
    THIS_AND_FUTURE("این نوبت و نوبت‌های بعدی"),
    ENTIRE_SERIES("کل مجموعه تکرارشونده")
}

data class SubtaskItem(
    val id: String,
    val title: String,
    val isDone: Boolean
)

data class TaskItem(
    val id: Long = 0L,
    val title: String,
    val aiNotes: String = "",
    val subtasks: List<SubtaskItem> = emptyList(),
    val jalaliDateKey: String,
    val timeHour: Int? = null,
    val timeMinute: Int? = null,
    val durationMinutes: Int = 45,
    val isDurationUserSpecified: Boolean = true,
    val priority: TaskPriority = TaskPriority.LOW,
    val category: String = "برنامه‌ریزی",
    val isCompleted: Boolean = false,
    val isAiGenerated: Boolean = false,
    val remindersEnabled: Boolean = true,
    val reminderOffsetsMinutes: List<Int> = listOf(10, 0),
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceInterval: Int = 1,
    val recurrenceEndDateKey: String? = null,
    val recurrenceExceptions: Set<String> = emptySet(),
    val seriesParentId: Long? = null,
    val timezoneId: String = "",
    val isPrivate: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = createdAtMillis,
    // Ephemeral UI field for expanded recurring instances on a specific date
    val virtualOccurrenceDateKey: String? = null
) {
    val isScheduled: Boolean
        get() = timeHour != null && timeMinute != null

    val effectiveDateKey: String
        get() = virtualOccurrenceDateKey ?: jalaliDateKey

    val isRecurringInstance: Boolean
        get() = virtualOccurrenceDateKey != null && virtualOccurrenceDateKey != jalaliDateKey

    fun formatTime(usePersianDigits: Boolean = true): String =
        PersianCalendarUtil.formatTime(timeHour, timeMinute, usePersianDigits)

    val formattedTime: String
        get() = formatTime(usePersianDigits = true)

    fun formatDuration(usePersianDigits: Boolean = true): String =
        PersianCalendarUtil.formatDurationMinutes(durationMinutes, usePersianDigits)

    val formattedDuration: String
        get() = formatDuration(usePersianDigits = true)

    val completedSubtasksCount: Int
        get() = subtasks.count { it.isDone }

    val startMinutesOfDay: Int?
        get() = if (timeHour != null && timeMinute != null) timeHour * 60 + timeMinute else null

    val endMinutesOfDay: Int?
        get() = startMinutesOfDay?.let { it + durationMinutes.coerceAtLeast(1) }
}

data class ScheduleConflict(
    val firstTask: TaskItem,
    val secondTask: TaskItem,
    val overlapMinutes: Int,
    val suggestedFreeSlotHour: Int? = null,
    val suggestedFreeSlotMinute: Int? = null
)
