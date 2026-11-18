package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.GoalPeriod
import com.example.data.model.HabitLog
import com.example.data.model.PersonalGoal
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    val periodCode: String,
    val targetPerPeriod: Int,
    val category: String,
    val reminderEnabled: Boolean,
    val reminderHour: Int,
    val reminderMinute: Int,
    val isActive: Boolean,
    val createdAtMillis: Long
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("goalId"), Index(value = ["goalId", "jalaliDateKey"], unique = true)]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val goalId: Long,
    val jalaliDateKey: String,
    val count: Int,
    val loggedAtMillis: Long
)

fun GoalEntity.toDomain(): PersonalGoal = PersonalGoal(
    id = id,
    title = title,
    description = description,
    period = GoalPeriod.fromCode(periodCode),
    targetPerPeriod = targetPerPeriod,
    category = category,
    reminderEnabled = reminderEnabled,
    reminderHour = reminderHour,
    reminderMinute = reminderMinute,
    isActive = isActive,
    createdAtMillis = createdAtMillis
)

fun PersonalGoal.toEntity(): GoalEntity = GoalEntity(
    id = id,
    title = title,
    description = description,
    periodCode = period.code,
    targetPerPeriod = targetPerPeriod.coerceAtLeast(1),
    category = category,
    reminderEnabled = reminderEnabled,
    reminderHour = reminderHour.coerceIn(0, 23),
    reminderMinute = reminderMinute.coerceIn(0, 59),
    isActive = isActive,
    createdAtMillis = createdAtMillis
)

fun HabitLogEntity.toDomain(): HabitLog = HabitLog(
    id = id,
    goalId = goalId,
    jalaliDateKey = jalaliDateKey,
    count = count,
    loggedAtMillis = loggedAtMillis
)

fun HabitLog.toEntity(): HabitLogEntity = HabitLogEntity(
    id = id,
    goalId = goalId,
    jalaliDateKey = jalaliDateKey,
    count = count,
    loggedAtMillis = loggedAtMillis
)

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY isActive DESC, createdAtMillis DESC")
    fun observeAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals")
    suspend fun getAllGoalsOnce(): List<GoalEntity>

    @Query("SELECT * FROM habit_logs ORDER BY jalaliDateKey DESC")
    fun observeAllHabitLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs")
    suspend fun getAllHabitLogsOnce(): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE goalId = :goalId AND jalaliDateKey = :dateKey LIMIT 1")
    suspend fun getLogForDate(goalId: Long, dateKey: String): HabitLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoalById(goalId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitLog(log: HabitLogEntity): Long

    @Query("DELETE FROM habit_logs WHERE goalId = :goalId AND jalaliDateKey = :dateKey")
    suspend fun deleteHabitLogForDate(goalId: Long, dateKey: String)

    @Transaction
    suspend fun toggleHabitForDateAtomic(
        goalId: Long,
        dateKey: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val existing = getLogForDate(goalId, dateKey)
        return if (existing != null) {
            deleteHabitLogForDate(goalId, dateKey)
            false
        } else {
            upsertHabitLog(
                HabitLogEntity(
                    goalId = goalId,
                    jalaliDateKey = dateKey,
                    count = 1,
                    loggedAtMillis = nowMillis
                )
            )
            true
        }
    }

    @Transaction
    suspend fun replaceAllGoalsAndLogsAtomic(
        goals: List<GoalEntity>,
        logs: List<HabitLogEntity>
    ) {
        deleteAllLogs()
        deleteAllGoals()
        if (goals.isNotEmpty()) {
            goals.forEach { insertGoal(it) }
        }
        if (logs.isNotEmpty()) {
            logs.forEach { upsertHabitLog(it) }
        }
    }

    @Query("DELETE FROM habit_logs")
    suspend fun deleteAllLogs()

    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()
}
