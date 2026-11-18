package com.example.data.repository

import com.example.data.local.GoalDao
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.HabitLog
import com.example.data.model.PersonalGoal
import com.example.util.PersianCalendarUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalRepository(private val goalDao: GoalDao) {

    fun observeAllGoals(): Flow<List<PersonalGoal>> =
        goalDao.observeAllGoals().map { list -> list.map { it.toDomain() } }

    fun observeAllHabitLogs(): Flow<List<HabitLog>> =
        goalDao.observeAllHabitLogs().map { list -> list.map { it.toDomain() } }

    suspend fun getAllGoalsOnce(): List<PersonalGoal> =
        goalDao.getAllGoalsOnce().map { it.toDomain() }

    suspend fun getAllHabitLogsOnce(): List<HabitLog> =
        goalDao.getAllHabitLogsOnce().map { it.toDomain() }

    suspend fun saveGoal(goal: PersonalGoal): Long {
        return if (goal.id == 0L) {
            goalDao.insertGoal(goal.toEntity())
        } else {
            goalDao.updateGoal(goal.toEntity())
            goal.id
        }
    }

    suspend fun deleteGoal(goalId: Long) {
        goalDao.deleteGoalById(goalId)
    }

    suspend fun toggleHabitForDate(
        goalId: Long,
        dateKey: String = PersianCalendarUtil.today().dateKey
    ): Boolean {
        return goalDao.toggleHabitForDateAtomic(goalId, dateKey)
    }
}
