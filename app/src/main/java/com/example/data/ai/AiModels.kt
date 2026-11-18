package com.example.data.ai

import com.example.data.model.ScheduleConflict
import com.example.data.model.TaskItem
import com.example.util.JalaliDate

enum class AiIntentKind {
    CREATE_TASKS,
    MODIFY_TASK,
    COMPLETE_TASK,
    QUERY_SCHEDULE,
    SUMMARIZE,
    CLARIFY
}

data class ProposedTaskDraft(
    val task: TaskItem,
    val isTimeExplicit: Boolean,
    val isDurationInferred: Boolean,
    val targetDate: JalaliDate,
    val relativeDateHint: String?,
    val conflictsWithExisting: List<ScheduleConflict> = emptyList()
)

data class AiChatMessage(
    val isUser: Boolean,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class AiPlanResponse(
    val intentKind: AiIntentKind,
    val originalTranscript: String,
    val proposedTasks: List<ProposedTaskDraft> = emptyList(),
    val targetExistingTaskToComplete: TaskItem? = null,
    val targetExistingTaskToUpdate: TaskItem? = null,
    val requiresClarification: Boolean = false,
    val clarificationQuestion: String? = null,
    val assistantReply: String,
    val recommendations: List<String> = emptyList(),
    val isOfflineFallback: Boolean,
    val noticeMessage: String? = null
)

data class AiInsightResult(
    val morningPlan: String,
    val eveningSummary: String,
    val weeklyReview: String,
    val tomorrowSuggestions: List<String>,
    val workloadBalanceTip: String,
    val goalTips: List<String>,
    val isOfflineFallback: Boolean,
    val noticeMessage: String? = null
)
