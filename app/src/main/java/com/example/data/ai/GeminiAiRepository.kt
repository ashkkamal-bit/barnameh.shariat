package com.example.data.ai

import android.content.Context
import android.util.Log
import com.example.data.model.GoalProgressSummary
import com.example.data.model.TaskItem
import com.example.domain.MeasuredDayMetrics
import com.example.domain.MeasuredWeekMetrics
import com.example.util.PersianCalendarUtil
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class GeminiAiRepository(
    private val context: Context
) : AiRepository {

    companion object {
        private const val TAG = "GeminiAiRepository"
        private const val MODEL_NAME = "gemini-3.8-flash"
        private const val TIMEOUT_MILLIS = 45_000L
    }

    override val isCloudAiAvailable: Boolean
        get() = AppCheckInitializer.isFirebaseConfigured(context)

    private val planningSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "intent" to Schema.enumeration(
                    values = listOf("CREATE_TASKS", "MODIFY_TASK", "COMPLETE_TASK", "QUERY_SCHEDULE", "SUMMARIZE", "CLARIFY"),
                    description = "Detected user intent"
                ),
                "tasks" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "title" to Schema.string(description = "Clean Persian task title"),
                            "date" to Schema.string(description = "ISO Gregorian date YYYY-MM-DD", nullable = true),
                            "time" to Schema.string(description = "24-hour HH:mm or null if unscheduled", nullable = true),
                            "durationMinutes" to Schema.integer(description = "Duration in minutes (5..720)", nullable = true),
                            "priority" to Schema.enumeration(listOf("LOW", "MEDIUM", "HIGH")),
                            "category" to Schema.enumeration(listOf("WORK", "MEETING", "DEEP_FOCUS", "PERSONAL", "HEALTH", "PLANNING", "LEARNING")),
                            "description" to Schema.string(description = "Concise Persian notes"),
                            "subtasks" to Schema.array(items = Schema.string()),
                            "reminderOffsetsMinutes" to Schema.array(items = Schema.integer())
                        ),
                        optionalProperties = listOf("date", "time", "durationMinutes", "description", "subtasks", "reminderOffsetsMinutes")
                    ),
                    description = "Extracted or updated tasks"
                ),
                "targetTaskId" to Schema.integer(description = "Existing task ID when modifying or completing", nullable = true),
                "requiresClarification" to Schema.boolean(description = "True if time or intent is genuinely ambiguous"),
                "clarificationQuestion" to Schema.string(description = "Natural Persian clarification question", nullable = true),
                "assistantReply" to Schema.string(description = "Natural Persian conversational reply"),
                "recommendations" to Schema.array(items = Schema.string(), description = "Optional Persian scheduling tips")
            ),
            optionalProperties = listOf("targetTaskId", "clarificationQuestion", "recommendations")
        )
    }

    private val insightsSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "morningPlan" to Schema.string(),
                "eveningSummary" to Schema.string(),
                "weeklyReview" to Schema.string(),
                "tomorrowSuggestions" to Schema.array(items = Schema.string()),
                "workloadBalanceTip" to Schema.string(),
                "goalTips" to Schema.array(items = Schema.string())
            )
        )
    }

    private val planningModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = planningSchema
            }
        )
    }

    private val insightsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = insightsSchema
            }
        )
    }

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
        val todayJalali = PersianCalendarUtil.today()
        val todayIso = todayJalali.toIsoGregorian()
        val selectedIso = PersianCalendarUtil.jalaliToIsoGregorian(fallbackJalaliDateKey)

        // Minimize external context: send only essential scheduling fields
        val compactTasks = existingTasks.take(40).joinToString("\n") { t ->
            val timeStr = if (t.isScheduled) "%02d:%02d".format(t.timeHour, t.timeMinute) else "unscheduled"
            "- id=${t.id} | title=${t.title} | date=${PersianCalendarUtil.jalaliToIsoGregorian(t.jalaliDateKey)} | time=$timeStr | dur=${t.durationMinutes}m | done=${t.isCompleted} | priority=${t.priority.name}"
        }
        val compactHistory = conversationHistory.takeLast(6).joinToString("\n") { msg ->
            "${if (msg.isUser) "User" else "Assistant"}: ${msg.text}"
        }
        val compactDrafts = previousDrafts.joinToString("\n") { d ->
            "- title=${d.task.title} | date=${d.targetDate.toIsoGregorian()} | time=${d.task.timeHour?.let { "%02d:%02d".format(it, d.task.timeMinute) } ?: "null"} | dur=${d.task.durationMinutes}"
        }

        val prompt = buildString {
            appendLine("You are برنامه ریز شریعت, a polite Persian personal planner assistant.")
            appendLine("Today ISO date: $todayIso (Jalali: ${todayJalali.formattedFull()}).")
            appendLine("Selected calendar ISO date: $selectedIso.")
            appendLine("Default task duration if not specified by user: $defaultDurationMinutes minutes.")
            appendLine("Rules:")
            appendLine("1. Respond with valid JSON matching the schema. All human-readable strings must be natural Persian.")
            appendLine("2. Always use ISO Gregorian YYYY-MM-DD for 'date'. Never invent a clock time if the user did not specify one—set 'time' to null.")
            appendLine("3. Never convert 7 عصر/شب to 07:00 (must be 19:00), and 12 شب must be 00:00 while 12 ظهر must be 12:00.")
            appendLine("4. If the time or edit target is genuinely ambiguous, set requiresClarification=true and ask in Persian.")
            appendLine("5. If the user corrects a previous draft in this conversation (e.g. 'یک ساعت طول میکشه' or 'نه ساعت ۹ باشه'), update the draft accordingly.")
            appendLine("6. Support splitting multiple activities in one sentence into separate items in 'tasks'.")
            if (compactDrafts.isNotEmpty()) {
                appendLine("\nCurrent pending drafts in session:\n$compactDrafts")
            }
            if (compactHistory.isNotEmpty()) {
                appendLine("\nRecent conversation:\n$compactHistory")
            }
            if (compactTasks.isNotEmpty()) {
                appendLine("\nUser's existing tasks (for queries/conflicts/rescheduling):\n$compactTasks")
            }
            appendLine("\nUser message: $transcript")
        }

        return try {
            val response = withTimeout(TIMEOUT_MILLIS) {
                planningModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("پاسخ خالی از مدل هوش مصنوعی دریافت شد.")
            AiResponseValidator.validateStructuredPlanJson(
                rawJson = rawJson,
                originalTranscript = transcript,
                fallbackJalaliDateKey = fallbackJalaliDateKey,
                defaultDurationMinutes = defaultDurationMinutes,
                defaultReminderOffsetMinutes = defaultReminderOffsetMinutes,
                existingTasks = existingTasks,
                isOfflineFallback = false,
                noticeMessage = null
            ).getOrThrow()
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini request timed out", e)
            throw IllegalStateException("زمان پاسخگویی سرویس Gemini به پایان رسید. لطفاً دوباره تلاش کنید.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini planning failed", e)
            throw e
        }
    }

    override suspend fun generateInsights(
        dayMetrics: MeasuredDayMetrics,
        weekMetrics: MeasuredWeekMetrics,
        goalSummaries: List<GoalProgressSummary>
    ): AiInsightResult {
        val goalsSummaryText = goalSummaries.take(10).joinToString("\n") { g ->
            "- ${g.goal.title} (${g.goal.period.labelFa}): ${g.completedInCurrentPeriod}/${g.goal.targetPerPeriod}, streak=${g.currentStreakDays}d"
        }
        val prompt = buildString {
            appendLine("You are برنامه ریز شریعت productivity coach. Write concise, practical Persian coaching strictly grounded in these measured stats:")
            appendLine("Day: total=${dayMetrics.totalTasks}, scheduled=${dayMetrics.totalScheduled}, unscheduled=${dayMetrics.totalUnscheduled}, completed=${dayMetrics.completedCount}, overdue=${dayMetrics.overdueCount}, highPriorityPending=${dayMetrics.highPriorityPendingCount}, totalMinutes=${dayMetrics.totalWorkloadMinutes}, conflicts=${dayMetrics.conflictCount}")
            appendLine("Day distribution: morning=${dayMetrics.morningMinutes}m, afternoon=${dayMetrics.afternoonMinutes}m, evening=${dayMetrics.eveningMinutes}m")
            appendLine("Week: total=${weekMetrics.totalTasks}, completed=${weekMetrics.completedTasks}, overdue=${weekMetrics.overdueTasks}, completion=${weekMetrics.completionPercent}%")
            if (goalsSummaryText.isNotEmpty()) appendLine("Goals:\n$goalsSummaryText")
            appendLine("Never invent statistics that contradict the numbers above. Return JSON matching schema.")
        }

        return try {
            val response = withTimeout(TIMEOUT_MILLIS) {
                insightsModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("پاسخ خالی از تحلیل هوشمند")
            AiResponseValidator.validateInsightsJson(
                rawJson = rawJson,
                isOfflineFallback = false,
                noticeMessage = null
            ).getOrThrow()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini insights failed", e)
            throw e
        }
    }
}
