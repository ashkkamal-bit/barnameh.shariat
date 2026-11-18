package com.example.data.model

enum class GoalPeriod(val code: String, val labelFa: String) {
    DAILY("DAILY", "روزانه"),
    WEEKLY("WEEKLY", "هفتگی"),
    MONTHLY("MONTHLY", "ماهانه"),
    YEARLY("YEARLY", "سالانه");

    companion object {
        fun fromCode(code: String?): GoalPeriod =
            entries.find { it.code.equals(code?.trim(), ignoreCase = true) } ?: DAILY
    }
}

data class PersonalGoal(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val period: GoalPeriod = GoalPeriod.DAILY,
    val targetPerPeriod: Int = 1,
    val category: String = "رشد شخصی",
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val isActive: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class HabitLog(
    val id: Long = 0L,
    val goalId: Long,
    val jalaliDateKey: String,
    val count: Int = 1,
    val loggedAtMillis: Long = System.currentTimeMillis()
)

data class GoalProgressSummary(
    val goal: PersonalGoal,
    val completedInCurrentPeriod: Int,
    val currentStreakDays: Int,
    val totalCompletions: Int,
    val completionRatio: Float,
    val isDoneToday: Boolean
)
