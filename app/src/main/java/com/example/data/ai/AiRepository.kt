package com.example.data.ai

import com.example.data.model.GoalProgressSummary
import com.example.data.model.TaskItem
import com.example.domain.MeasuredDayMetrics
import com.example.domain.MeasuredWeekMetrics

interface AiRepository {
    val isCloudAiAvailable: Boolean

    suspend fun processPlanningMessage(
        transcript: String,
        fallbackJalaliDateKey: String,
        defaultDurationMinutes: Int,
        defaultReminderOffsetMinutes: Int,
        existingTasks: List<TaskItem>,
        conversationHistory: List<AiChatMessage>,
        previousDrafts: List<ProposedTaskDraft>,
        activeGoals: List<GoalProgressSummary>
    ): AiPlanResponse

    suspend fun generateInsights(
        dayMetrics: MeasuredDayMetrics,
        weekMetrics: MeasuredWeekMetrics,
        goalSummaries: List<GoalProgressSummary>
    ): AiInsightResult
}
