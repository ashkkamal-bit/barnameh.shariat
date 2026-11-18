package com.example.domain

import com.example.data.model.ScheduleConflict
import com.example.data.model.TaskItem
import kotlin.math.max
import kotlin.math.min

object ConflictDetector {

    /**
     * Finds all pairwise overlapping scheduled tasks for the same day.
     * Unscheduled or completed tasks are excluded from blocking conflicts.
     */
    fun findConflicts(tasksForDay: List<TaskItem>, includeCompleted: Boolean = false): List<ScheduleConflict> {
        val scheduled = tasksForDay
            .filter { it.isScheduled && (includeCompleted || !it.isCompleted) }
            .sortedWith(compareBy({ it.timeHour ?: 0 }, { it.timeMinute ?: 0 }, { it.id }))

        val conflicts = mutableListOf<ScheduleConflict>()
        for (i in scheduled.indices) {
            val a = scheduled[i]
            val aStart = a.startMinutesOfDay ?: continue
            val aEnd = a.endMinutesOfDay ?: continue
            for (j in i + 1 until scheduled.size) {
                val b = scheduled[j]
                val bStart = b.startMinutesOfDay ?: continue
                val bEnd = b.endMinutesOfDay ?: continue
                val overlapStart = max(aStart, bStart)
                val overlapEnd = min(aEnd, bEnd)
                if (overlapEnd > overlapStart) {
                    val slot = findNextFreeSlot(
                        tasksForDay = scheduled.filter { it.id != b.id },
                        durationMinutes = b.durationMinutes,
                        preferredStartMinute = aEnd
                    )
                    conflicts.add(
                        ScheduleConflict(
                            firstTask = a,
                            secondTask = b,
                            overlapMinutes = overlapEnd - overlapStart,
                            suggestedFreeSlotHour = slot?.first,
                            suggestedFreeSlotMinute = slot?.second
                        )
                    )
                }
            }
        }
        return conflicts
    }

    /**
     * Checks if a candidate task conflicts with existing scheduled tasks on its date.
     */
    fun findConflictsForCandidate(
        candidate: TaskItem,
        existingTasksForDay: List<TaskItem>
    ): List<ScheduleConflict> {
        if (!candidate.isScheduled) return emptyList()
        val cStart = candidate.startMinutesOfDay ?: return emptyList()
        val cEnd = candidate.endMinutesOfDay ?: return emptyList()

        val others = existingTasksForDay.filter {
            it.isScheduled && !it.isCompleted && (candidate.id == 0L || it.id != candidate.id)
        }
        val conflicts = mutableListOf<ScheduleConflict>()
        for (other in others) {
            val oStart = other.startMinutesOfDay ?: continue
            val oEnd = other.endMinutesOfDay ?: continue
            val overlap = min(cEnd, oEnd) - max(cStart, oStart)
            if (overlap > 0) {
                val slot = findNextFreeSlot(
                    tasksForDay = others,
                    durationMinutes = candidate.durationMinutes,
                    preferredStartMinute = oEnd
                )
                conflicts.add(
                    ScheduleConflict(
                        firstTask = other,
                        secondTask = candidate,
                        overlapMinutes = overlap,
                        suggestedFreeSlotHour = slot?.first,
                        suggestedFreeSlotMinute = slot?.second
                    )
                )
            }
        }
        return conflicts
    }

    /**
     * Suggests the nearest conflict-free slot on the day (from preferredStartMinute onward, or earlier in 06:00..23:30).
     */
    fun findNextFreeSlot(
        tasksForDay: List<TaskItem>,
        durationMinutes: Int,
        preferredStartMinute: Int = 9 * 60
    ): Pair<Int, Int>? {
        val duration = durationMinutes.coerceIn(5, 480)
        val occupied = tasksForDay
            .mapNotNull { t ->
                val s = t.startMinutesOfDay
                val e = t.endMinutesOfDay
                if (s != null && e != null) s to e else null
            }
            .sortedBy { it.first }

        fun isFree(start: Int, end: Int): Boolean {
            if (end > 23 * 60 + 55) return false
            return occupied.none { (os, oe) -> min(end, oe) > max(start, os) }
        }

        val alignedPref = ((preferredStartMinute + 14) / 15) * 15
        var cursor = alignedPref.coerceIn(6 * 60, 23 * 60)
        while (cursor + duration <= 23 * 60 + 55) {
            if (isFree(cursor, cursor + duration)) {
                return (cursor / 60) to (cursor % 60)
            }
            cursor += 15
        }
        cursor = 7 * 60
        while (cursor < alignedPref && cursor + duration <= 23 * 60 + 55) {
            if (isFree(cursor, cursor + duration)) {
                return (cursor / 60) to (cursor % 60)
            }
            cursor += 15
        }
        return null
    }
}
