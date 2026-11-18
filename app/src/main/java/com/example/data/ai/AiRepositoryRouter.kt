package com.example.data.ai

import android.content.Context
import android.util.Log
import com.example.data.model.GoalProgressSummary
import com.example.data.model.TaskItem
import com.example.domain.MeasuredDayMetrics
import com.example.domain.MeasuredWeekMetrics
import kotlinx.coroutines.CancellationException

class AiRepositoryRouter(
    private val context: Context,
    private val cloudRepo: AiRepository = GeminiAiRepository(context),
    private val offlineRepo: AiRepository = OfflineAiRepository()
) : AiRepository {

    companion object {
        private const val TAG = "AiRepositoryRouter"
    }

    override val isCloudAiAvailable: Boolean
        get() = cloudRepo.isCloudAiAvailable

    override suspend fun processPlanningMessage(
        transcript: String,
        fallbackJalaliDateKey: String,
        defaultDurationMinutes: Int,
        defaultReminderOffsetMinutes: Int,
        existingTasks: List<TaskItem>,
        conversationHistory: List<AiChatMessage>,
        previousDrafts: List<ProposedTaskDraft>,
        activeGoals: List<GoalProgressSummary>
    ): AiPlanResponse {
        if (!cloudRepo.isCloudAiAvailable) {
            return offlineRepo.processPlanningMessage(
                transcript = transcript,
                fallbackJalaliDateKey = fallbackJalaliDateKey,
                defaultDurationMinutes = defaultDurationMinutes,
                defaultReminderOffsetMinutes = defaultReminderOffsetMinutes,
                existingTasks = existingTasks,
                conversationHistory = conversationHistory,
                previousDrafts = previousDrafts,
                activeGoals = activeGoals
            )
        }

        return try {
            cloudRepo.processPlanningMessage(
                transcript = transcript,
                fallbackJalaliDateKey = fallbackJalaliDateKey,
                defaultDurationMinutes = defaultDurationMinutes,
                defaultReminderOffsetMinutes = defaultReminderOffsetMinutes,
                existingTasks = existingTasks,
                conversationHistory = conversationHistory,
                previousDrafts = previousDrafts,
                activeGoals = activeGoals
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Cloud Gemini failed, switching to transparent offline fallback", e)
            val fallbackNotice = "اتصال به Gemini برقرار نشد (${e.localizedMessage ?: "خطای شبکه"})؛ پردازش با تحلیلگر محلی انجام شد."
            OfflineAiRepository(customNotice = fallbackNotice).processPlanningMessage(
                transcript = transcript,
                fallbackJalaliDateKey = fallbackJalaliDateKey,
                defaultDurationMinutes = defaultDurationMinutes,
                defaultReminderOffsetMinutes = defaultReminderOffsetMinutes,
                existingTasks = existingTasks,
                conversationHistory = conversationHistory,
                previousDrafts = previousDrafts,
                activeGoals = activeGoals
            )
        }
    }

    override suspend fun generateInsights(
        dayMetrics: MeasuredDayMetrics,
        weekMetrics: MeasuredWeekMetrics,
        goalSummaries: List<GoalProgressSummary>
    ): AiInsightResult {
        if (!cloudRepo.isCloudAiAvailable) {
            return offlineRepo.generateInsights(dayMetrics, weekMetrics, goalSummaries)
        }

        return try {
            cloudRepo.generateInsights(dayMetrics, weekMetrics, goalSummaries)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Cloud Gemini insights failed, using offline fallback", e)
            val notice = "امکان دریافت تحلیل ابری Gemini وجود نداشت؛ آمار واقعی محاسبه‌شده نمایش داده می‌شود."
            OfflineAiRepository(customNotice = notice).generateInsights(dayMetrics, weekMetrics, goalSummaries)
        }
    }
}
