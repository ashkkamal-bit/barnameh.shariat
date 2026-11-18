package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.RecurrenceType
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val aiNotes: String,
    val subtasksSerialized: String,
    val jalaliDateKey: String,
    val timeHour: Int?,
    val timeMinute: Int?,
    val durationMinutes: Int,
    val isDurationUserSpecified: Boolean = true,
    val priorityLevel: Int,
    val category: String,
    val isCompleted: Boolean,
    val isAiGenerated: Boolean,
    val remindersEnabled: Boolean = true,
    val reminderOffsetsCsv: String = "10,0",
    val recurrenceType: String = "NONE",
    val recurrenceInterval: Int = 1,
    val recurrenceEndDateKey: String? = null,
    val recurrenceExceptionsCsv: String = "",
    val seriesParentId: Long? = null,
    val timezoneId: String = "",
    val isPrivate: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long = createdAtMillis
)

fun TaskEntity.toDomain(): TaskItem {
    val parsedSubtasks = parseSubtasksSerialized(id, subtasksSerialized)
    val offsets = reminderOffsetsCsv
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 0..1440 }
        .distinct()
    val exceptions = recurrenceExceptionsCsv
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()

    return TaskItem(
        id = id,
        title = title,
        aiNotes = aiNotes,
        subtasks = parsedSubtasks,
        jalaliDateKey = jalaliDateKey,
        timeHour = timeHour,
        timeMinute = timeMinute,
        durationMinutes = durationMinutes,
        isDurationUserSpecified = isDurationUserSpecified,
        priority = TaskPriority.fromLevel(priorityLevel),
        category = category,
        isCompleted = isCompleted,
        isAiGenerated = isAiGenerated,
        remindersEnabled = remindersEnabled,
        reminderOffsetsMinutes = offsets,
        recurrenceType = RecurrenceType.fromCode(recurrenceType),
        recurrenceInterval = recurrenceInterval.coerceAtLeast(1),
        recurrenceEndDateKey = recurrenceEndDateKey,
        recurrenceExceptions = exceptions,
        seriesParentId = seriesParentId,
        timezoneId = timezoneId,
        isPrivate = isPrivate,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

fun TaskItem.toEntity(): TaskEntity {
    val serialized = subtasks.joinToString("\n") { sub ->
        val flag = if (sub.isDone) "1" else "0"
        "$flag|${sub.title.replace("\n", " ")}"
    }
    return TaskEntity(
        id = id,
        title = title,
        aiNotes = aiNotes,
        subtasksSerialized = serialized,
        jalaliDateKey = jalaliDateKey,
        timeHour = timeHour,
        timeMinute = timeMinute,
        durationMinutes = durationMinutes,
        isDurationUserSpecified = isDurationUserSpecified,
        priorityLevel = priority.level,
        category = category,
        isCompleted = isCompleted,
        isAiGenerated = isAiGenerated,
        remindersEnabled = remindersEnabled,
        reminderOffsetsCsv = reminderOffsetsMinutes.distinct().joinToString(","),
        recurrenceType = recurrenceType.code,
        recurrenceInterval = recurrenceInterval.coerceAtLeast(1),
        recurrenceEndDateKey = recurrenceEndDateKey,
        recurrenceExceptionsCsv = recurrenceExceptions.sorted().joinToString(","),
        seriesParentId = seriesParentId,
        timezoneId = timezoneId,
        isPrivate = isPrivate,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

internal fun parseSubtasksSerialized(taskId: Long, rawSerialized: String): List<SubtaskItem> {
    if (rawSerialized.isBlank()) return emptyList()
    return rawSerialized.split("\n").mapIndexedNotNull { index, raw ->
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return@mapIndexedNotNull null
        val parts = trimmed.split("|", limit = 2)
        if (parts.size == 2) {
            SubtaskItem(
                id = "${taskId}_$index",
                title = parts[1],
                isDone = parts[0] == "1"
            )
        } else {
            SubtaskItem(id = "${taskId}_$index", title = trimmed, isDone = false)
        }
    }
}

@Dao
interface TaskDao {
    @Query(
        "SELECT * FROM tasks ORDER BY jalaliDateKey ASC, CASE WHEN timeHour IS NULL THEN 1 ELSE 0 END ASC, timeHour ASC, timeMinute ASC, id ASC"
    )
    fun observeAllTasks(): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM tasks WHERE jalaliDateKey = :dateKey ORDER BY CASE WHEN timeHour IS NULL THEN 1 ELSE 0 END ASC, timeHour ASC, timeMinute ASC, id ASC"
    )
    fun observeTasksForDate(dateKey: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE jalaliDateKey = :dateKey")
    suspend fun getTasksForDateOnce(dateKey: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>): List<Long>

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = NOT isCompleted, updatedAtMillis = :nowMillis WHERE id = :id")
    suspend fun toggleTaskCompletionAtomic(id: Long, nowMillis: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isCompleted = :completed, updatedAtMillis = :nowMillis WHERE id = :id")
    suspend fun setTaskCompletedAtomic(id: Long, completed: Boolean, nowMillis: Long = System.currentTimeMillis())

    @Transaction
    suspend fun toggleSubtaskAtomic(
        taskId: Long,
        subtaskId: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val current = getTaskById(taskId) ?: return
        val subtasks = parseSubtasksSerialized(taskId, current.subtasksSerialized)
        val updated = subtasks.map { sub ->
            if (sub.id == subtaskId) sub.copy(isDone = !sub.isDone) else sub
        }
        val serialized = updated.joinToString("\n") { sub ->
            val flag = if (sub.isDone) "1" else "0"
            "$flag|${sub.title.replace("\n", " ")}"
        }
        updateTask(current.copy(subtasksSerialized = serialized, updatedAtMillis = nowMillis))
    }

    @Transaction
    suspend fun addRecurrenceExceptionAtomic(
        seriesId: Long,
        exceptionDateKey: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val current = getTaskById(seriesId) ?: return
        val currentSet = current.recurrenceExceptionsCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableSet()
        currentSet.add(exceptionDateKey)
        updateTask(
            current.copy(
                recurrenceExceptionsCsv = currentSet.sorted().joinToString(","),
                updatedAtMillis = nowMillis
            )
        )
    }

    @Transaction
    suspend fun replaceAllTasksAtomic(tasks: List<TaskEntity>): List<Long> {
        deleteAll()
        return if (tasks.isNotEmpty()) insertTasks(tasks) else emptyList()
    }

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
