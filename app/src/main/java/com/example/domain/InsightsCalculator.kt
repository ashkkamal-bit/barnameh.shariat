package com.example.domain

import com.example.data.model.GoalPeriod
import com.example.data.model.GoalProgressSummary
import com.example.data.model.HabitLog
import com.example.data.model.PersonalGoal
import com.example.data.model.ScheduleConflict
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.util.JalaliDate
import com.example.util.PersianCalendarUtil
import java.util.Calendar

data class MeasuredDayMetrics(
    val dateKey: String,
    val totalScheduled: Int,
    val totalUnscheduled: Int,
    val totalTasks: Int,
    val completedCount: Int,
    val pendingCount: Int,
    val overdueCount: Int,
    val highPriorityPendingCount: Int,
    val totalWorkloadMinutes: Int,
    val completedWorkloadMinutes: Int,
    val completionPercent: Int,
    val conflictCount: Int,
    val conflicts: List<ScheduleConflict>,
    val categoryDistributionMinutes: Map<String, Int>,
    val morningMinutes: Int,
    val afternoonMinutes: Int,
    val eveningMinutes: Int
)

data class MeasuredWeekMetrics(
    val weekDates: List<JalaliDate>,
    val totalTasks: Int,
    val completedTasks: Int,
    val overdueTasks: Int,
    val totalWorkloadMinutes: Int,
    val dailyTaskCounts: List<Pair<JalaliDate, Int>>,
    val dailyCompletedCounts: List<Pair<JalaliDate, Int>>,
    val completionPercent: Int
)

object InsightsCalculator {

    fun calculateDayMetrics(
        allTasks: List<TaskItem>,
        dateKey: String,
        nowCal: Calendar = Calendar.getInstance()
    ): MeasuredDayMetrics {
        val dayTasks = RecurrenceEngine.tasksForDate(allTasks, dateKey)
        val scheduled = dayTasks.filter { it.isScheduled }
        val unscheduled = dayTasks.filter { !it.isScheduled }
        val completed = dayTasks.filter { it.isCompleted }
        val pending = dayTasks.filter { !it.isCompleted }
        val overdue = pending.filter {
            PersianCalendarUtil.isOverdue(it.effectiveDateKey, it.timeHour, it.timeMinute, nowCal)
        }
        val highPending = pending.count { it.priority == TaskPriority.HIGH }
        val totalWorkload = dayTasks.sumOf { it.durationMinutes }
        val completedWorkload = completed.sumOf { it.durationMinutes }
        val pct = if (dayTasks.isEmpty()) 0 else ((completed.size * 100f) / dayTasks.size).toInt()
        val conflicts = ConflictDetector.findConflicts(dayTasks)

        val categoryDist = dayTasks
            .groupBy { it.category.ifBlank { "عمومی" } }
            .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }

        var morning = 0
        var afternoon = 0
        var evening = 0
        for (t in scheduled) {
            val h = t.timeHour ?: continue
            when {
                h < 12 -> morning += t.durationMinutes
                h < 17 -> afternoon += t.durationMinutes
                else -> evening += t.durationMinutes
            }
        }

        return MeasuredDayMetrics(
            dateKey = dateKey,
            totalScheduled = scheduled.size,
            totalUnscheduled = unscheduled.size,
            totalTasks = dayTasks.size,
            completedCount = completed.size,
            pendingCount = pending.size,
            overdueCount = overdue.size,
            highPriorityPendingCount = highPending,
            totalWorkloadMinutes = totalWorkload,
            completedWorkloadMinutes = completedWorkload,
            completionPercent = pct,
            conflictCount = conflicts.size,
            conflicts = conflicts,
            categoryDistributionMinutes = categoryDist,
            morningMinutes = morning,
            afternoonMinutes = afternoon,
            eveningMinutes = evening
        )
    }

    fun calculateWeekMetrics(
        allTasks: List<TaskItem>,
        anchorDate: JalaliDate,
        nowCal: Calendar = Calendar.getInstance()
    ): MeasuredWeekMetrics {
        val weekDates = PersianCalendarUtil.weekDatesStartingSaturday(anchorDate)
        var total = 0
        var completed = 0
        var overdue = 0
        var workload = 0
        val dailyTotal = mutableListOf<Pair<JalaliDate, Int>>()
        val dailyDone = mutableListOf<Pair<JalaliDate, Int>>()

        for (d in weekDates) {
            val tasks = RecurrenceEngine.tasksForDate(allTasks, d.dateKey)
            val done = tasks.count { it.isCompleted }
            val od = tasks.count {
                !it.isCompleted && PersianCalendarUtil.isOverdue(it.effectiveDateKey, it.timeHour, it.timeMinute, nowCal)
            }
            total += tasks.size
            completed += done
            overdue += od
            workload += tasks.sumOf { it.durationMinutes }
            dailyTotal.add(d to tasks.size)
            dailyDone.add(d to done)
        }
        val pct = if (total == 0) 0 else ((completed * 100f) / total).toInt()
        return MeasuredWeekMetrics(
            weekDates = weekDates,
            totalTasks = total,
            completedTasks = completed,
            overdueTasks = overdue,
            totalWorkloadMinutes = workload,
            dailyTaskCounts = dailyTotal,
            dailyCompletedCounts = dailyDone,
            completionPercent = pct
        )
    }

    fun findOverdueTasks(
        allTasks: List<TaskItem>,
        nowCal: Calendar = Calendar.getInstance()
    ): List<TaskItem> {
        return allTasks
            .filter { !it.isCompleted }
            .filter { PersianCalendarUtil.isOverdue(it.jalaliDateKey, it.timeHour, it.timeMinute, nowCal) }
            .sortedWith(compareBy({ it.jalaliDateKey }, { it.timeHour ?: 24 }, { it.timeMinute ?: 0 }))
    }

    fun calculateGoalProgress(
        goals: List<PersonalGoal>,
        logs: List<HabitLog>,
        today: JalaliDate = PersianCalendarUtil.today()
    ): List<GoalProgressSummary> {
        val todayKey = today.dateKey
        val weekKeys = PersianCalendarUtil.weekDatesStartingSaturday(today).map { it.dateKey }.toSet()
        val monthPrefix = "%04d-%02d-".format(today.year, today.month)
        val yearPrefix = "%04d-".format(today.year)

        return goals.map { goal ->
            val goalLogs = logs.filter { it.goalId == goal.id }
            val dateSet = goalLogs.map { it.jalaliDateKey }.toSet()
            val isDoneToday = todayKey in dateSet
            val completedInPeriod = when (goal.period) {
                GoalPeriod.DAILY -> if (isDoneToday) 1 else 0
                GoalPeriod.WEEKLY -> goalLogs.count { it.jalaliDateKey in weekKeys }
                GoalPeriod.MONTHLY -> goalLogs.count { it.jalaliDateKey.startsWith(monthPrefix) }
                GoalPeriod.YEARLY -> goalLogs.count { it.jalaliDateKey.startsWith(yearPrefix) }
            }

            // Calculate consecutive daily streak ending today or yesterday
            var streak = 0
            var cursor = if (isDoneToday) today else PersianCalendarUtil.addDays(today, -1)
            while (cursor.dateKey in dateSet && streak < 365) {
                streak++
                cursor = PersianCalendarUtil.addDays(cursor, -1)
            }

            val ratio = (completedInPeriod.toFloat() / goal.targetPerPeriod.coerceAtLeast(1)).coerceIn(0f, 1f)
            GoalProgressSummary(
                goal = goal,
                completedInCurrentPeriod = completedInPeriod,
                currentStreakDays = streak,
                totalCompletions = goalLogs.size,
                completionRatio = ratio,
                isDoneToday = isDoneToday
            )
        }
    }
}
