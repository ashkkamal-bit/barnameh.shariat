package com.example.data.repository

import com.example.data.local.TaskDao
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.RecurrenceEditScope
import com.example.data.model.RecurrenceType
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.util.PersianCalendarUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository(private val taskDao: TaskDao) {

    fun observeAllTasks(): Flow<List<TaskItem>> =
        taskDao.observeAllTasks().map { list -> list.map { it.toDomain() } }

    fun observeTasksForDate(dateKey: String): Flow<List<TaskItem>> =
        taskDao.observeTasksForDate(dateKey).map { list -> list.map { it.toDomain() } }

    suspend fun getAllTasksOnce(): List<TaskItem> =
        taskDao.getAllTasksOnce().map { it.toDomain() }

    suspend fun getTaskById(id: Long): TaskItem? =
        taskDao.getTaskById(id)?.toDomain()

    suspend fun getTaskCount(): Int = taskDao.getTaskCount()

    suspend fun addTask(task: TaskItem): Long {
        val now = System.currentTimeMillis()
        val prepared = task.copy(
            createdAtMillis = if (task.createdAtMillis == 0L) now else task.createdAtMillis,
            updatedAtMillis = now
        )
        return taskDao.insertTask(prepared.toEntity())
    }

    suspend fun addTasks(tasks: List<TaskItem>): List<Long> {
        val now = System.currentTimeMillis()
        val entities = tasks.map {
            it.copy(
                createdAtMillis = if (it.createdAtMillis == 0L) now else it.createdAtMillis,
                updatedAtMillis = now
            ).toEntity()
        }
        return taskDao.insertTasks(entities)
    }

    suspend fun updateTask(task: TaskItem) {
        taskDao.updateTask(task.copy(updatedAtMillis = System.currentTimeMillis()).toEntity())
    }

    /**
     * Edits a recurring task according to user-selected scope without duplicating the whole series.
     */
    suspend fun updateRecurringTask(
        original: TaskItem,
        updated: TaskItem,
        occurrenceDateKey: String,
        scope: RecurrenceEditScope
    ): Long {
        val now = System.currentTimeMillis()
        return when (scope) {
            RecurrenceEditScope.ONLY_THIS -> {
                taskDao.addRecurrenceExceptionAtomic(original.id, occurrenceDateKey, now)
                val detached = updated.copy(
                    id = 0L,
                    jalaliDateKey = if (updated.jalaliDateKey.isBlank() || updated.jalaliDateKey == original.jalaliDateKey) {
                        occurrenceDateKey
                    } else {
                        updated.jalaliDateKey
                    },
                    recurrenceType = RecurrenceType.NONE,
                    recurrenceEndDateKey = null,
                    recurrenceExceptions = emptySet(),
                    seriesParentId = original.id,
                    createdAtMillis = now,
                    updatedAtMillis = now,
                    virtualOccurrenceDateKey = null
                )
                taskDao.insertTask(detached.toEntity())
            }
            RecurrenceEditScope.THIS_AND_FUTURE -> {
                if (occurrenceDateKey <= original.jalaliDateKey) {
                    taskDao.updateTask(updated.copy(id = original.id, updatedAtMillis = now).toEntity())
                    original.id
                } else {
                    val prevDayKey = PersianCalendarUtil.addDays(
                        PersianCalendarUtil.parseDateKey(occurrenceDateKey),
                        -1
                    ).dateKey
                    taskDao.updateTask(
                        original.copy(recurrenceEndDateKey = prevDayKey, updatedAtMillis = now).toEntity()
                    )
                    val newSeries = updated.copy(
                        id = 0L,
                        jalaliDateKey = occurrenceDateKey,
                        seriesParentId = original.id,
                        createdAtMillis = now,
                        updatedAtMillis = now,
                        virtualOccurrenceDateKey = null
                    )
                    taskDao.insertTask(newSeries.toEntity())
                }
            }
            RecurrenceEditScope.ENTIRE_SERIES -> {
                taskDao.updateTask(
                    updated.copy(
                        id = original.id,
                        jalaliDateKey = original.jalaliDateKey,
                        updatedAtMillis = now,
                        virtualOccurrenceDateKey = null
                    ).toEntity()
                )
                original.id
            }
        }
    }

    suspend fun toggleTaskCompleted(taskId: Long) {
        taskDao.toggleTaskCompletionAtomic(taskId)
    }

    suspend fun setTaskCompleted(taskId: Long, completed: Boolean) {
        taskDao.setTaskCompletedAtomic(taskId, completed)
    }

    suspend fun toggleSubtask(taskId: Long, subtaskId: String) {
        taskDao.toggleSubtaskAtomic(taskId, subtaskId)
    }

    suspend fun deleteTask(taskId: Long) {
        taskDao.deleteTaskById(taskId)
    }

    suspend fun excludeRecurringOccurrence(seriesId: Long, occurrenceDateKey: String) {
        taskDao.addRecurrenceExceptionAtomic(seriesId, occurrenceDateKey)
    }

    /**
     * Explicit user-triggered clear of all tasks. Never called automatically.
     */
    suspend fun clearAllUserTasks() {
        taskDao.deleteAll()
    }

    /**
     * Explicit user-triggered demo import. Never called automatically on empty DB.
     */
    suspend fun importDemoTasks(replaceExisting: Boolean = false): Int {
        val sample = buildDemoTasks()
        val entities = sample.map { it.toEntity() }
        if (replaceExisting) {
            taskDao.replaceAllTasksAtomic(entities)
        } else {
            taskDao.insertTasks(entities)
        }
        return sample.size
    }

    private fun buildDemoTasks(): List<TaskItem> {
        val todayKey = PersianCalendarUtil.today().dateKey
        val tomorrowKey = PersianCalendarUtil.offsetFromToday(1).dateKey
        val dayAfterKey = PersianCalendarUtil.offsetFromToday(2).dateKey

        return listOf(
            TaskItem(
                title = "مرور اولویت‌های صبحگاهی و ایمیل‌ها",
                aiNotes = "شروع روز با پاک‌سازی پیام‌های ضروری و تنظیم ریتم کاری",
                subtasks = listOf(
                    SubtaskItem("d1", "پاسخ به پیام‌های تیم محصول", true),
                    SubtaskItem("d2", "بررسی تقویم جلسات امروز", true)
                ),
                jalaliDateKey = todayKey,
                timeHour = 8,
                timeMinute = 30,
                durationMinutes = 30,
                priority = TaskPriority.LOW,
                category = "برنامه‌ریزی",
                isCompleted = true,
                isAiGenerated = false,
                reminderOffsetsMinutes = listOf(10, 0)
            ),
            TaskItem(
                title = "جلسه هم‌راستایی طراحی و زمان‌بندی اسپرینت",
                aiNotes = "مرور بلوک‌های زمانی، اولویت‌های هفته و تقسیم وظایف",
                subtasks = listOf(
                    SubtaskItem("d3", "بررسی اهداف کلیدی هفته", true),
                    SubtaskItem("d4", "تعیین زمان تحویل نسخه جدید", false)
                ),
                jalaliDateKey = todayKey,
                timeHour = 10,
                timeMinute = 30,
                durationMinutes = 60,
                priority = TaskPriority.HIGH,
                category = "جلسه",
                isCompleted = false,
                isAiGenerated = false,
                reminderOffsetsMinutes = listOf(15, 0)
            ),
            TaskItem(
                title = "کار عمیق روی ماژول گزارش‌گیری",
                aiNotes = "بازه تمرکز عمیق بدون اعلان‌های مزاحم",
                subtasks = emptyList(),
                jalaliDateKey = todayKey,
                timeHour = 13,
                timeMinute = 0,
                durationMinutes = 90,
                priority = TaskPriority.MEDIUM,
                category = "تمرکز عمیق",
                isCompleted = false,
                isAiGenerated = false,
                reminderOffsetsMinutes = listOf(10, 0)
            ),
            TaskItem(
                title = "خرید اقلام ضروری منزل (بدون ساعت ثابت)",
                aiNotes = "کار بدون ساعت ثابت برای انجام در وقت آزاد",
                subtasks = emptyList(),
                jalaliDateKey = todayKey,
                timeHour = null,
                timeMinute = null,
                durationMinutes = 40,
                priority = TaskPriority.LOW,
                category = "شخصی",
                isCompleted = false,
                isAiGenerated = false,
                remindersEnabled = false,
                reminderOffsetsMinutes = emptyList()
            ),
            TaskItem(
                title = "بازنگری گزارش عملکرد هفتگی",
                aiNotes = "آماده‌سازی نکات کلیدی ارائه فردا",
                subtasks = emptyList(),
                jalaliDateKey = tomorrowKey,
                timeHour = 9,
                timeMinute = 30,
                durationMinutes = 60,
                priority = TaskPriority.HIGH,
                category = "کار",
                isCompleted = false,
                isAiGenerated = false,
                reminderOffsetsMinutes = listOf(15, 0)
            ),
            TaskItem(
                title = "ورزش و پیاده‌روی عصرگاهی",
                aiNotes = "تکرار سه‌روز در هفته یا روزانه برای حفظ انرژی",
                subtasks = emptyList(),
                jalaliDateKey = dayAfterKey,
                timeHour = 18,
                timeMinute = 0,
                durationMinutes = 45,
                priority = TaskPriority.MEDIUM,
                category = "سلامتی",
                isCompleted = false,
                isAiGenerated = false,
                recurrenceType = RecurrenceType.DAILY,
                reminderOffsetsMinutes = listOf(10)
            )
        )
    }
}
