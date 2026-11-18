package com.example.data.ai

import com.example.data.model.SubtaskItem
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.domain.ConflictDetector
import com.example.domain.RecurrenceEngine
import com.example.util.PersianCalendarUtil
import org.json.JSONArray
import org.json.JSONObject

object AiResponseValidator {

    private val categoryMap = mapOf(
        "WORK" to "کار",
        "MEETING" to "جلسه",
        "DEEP_FOCUS" to "تمرکز عمیق",
        "PERSONAL" to "شخصی",
        "HEALTH" to "سلامتی",
        "PLANNING" to "برنامه‌ریزی",
        "LEARNING" to "یادگیری"
    )

    fun validateStructuredPlanJson(
        rawJson: String,
        originalTranscript: String,
        fallbackJalaliDateKey: String,
        defaultDurationMinutes: Int,
        defaultReminderOffsetMinutes: Int,
        existingTasks: List<TaskItem>,
        isOfflineFallback: Boolean = false,
        noticeMessage: String? = null
    ): Result<AiPlanResponse> {
        return try {
            val root = JSONObject(rawJson)
            val intentStr = root.optString("intent", "CREATE_TASKS").uppercase()
            val intentKind = when (intentStr) {
                "QUERY_SCHEDULE" -> AiIntentKind.QUERY_SCHEDULE
                "SUMMARIZE" -> AiIntentKind.SUMMARIZE
                "MODIFY_TASK" -> AiIntentKind.MODIFY_TASK
                "COMPLETE_TASK" -> AiIntentKind.COMPLETE_TASK
                "CLARIFY" -> AiIntentKind.CLARIFY
                else -> AiIntentKind.CREATE_TASKS
            }

            val requiresClarification = root.optBoolean("requiresClarification", false)
            val clarificationQuestion = optNonNullString(root, "clarificationQuestion")
            val assistantReply = optNonNullString(root, "assistantReply") ?: ""
            val targetTaskId = if (root.has("targetTaskId") && !root.isNull("targetTaskId")) {
                root.optLong("targetTaskId", -1L).takeIf { it > 0L }
            } else {
                null
            }

            val recommendationsArr = root.optJSONArray("recommendations") ?: JSONArray()
            val recommendations = (0 until recommendationsArr.length())
                .mapNotNull { idx -> recommendationsArr.optString(idx, "").trim().takeIf { it.isNotEmpty() } }

            val tasksArr = root.optJSONArray("tasks") ?: JSONArray()
            val drafts = mutableListOf<ProposedTaskDraft>()

            for (i in 0 until tasksArr.length()) {
                val obj = tasksArr.optJSONObject(i) ?: continue
                val title = optNonNullString(obj, "title") ?: continue

                val isoDateStr = optNonNullString(obj, "date")
                val jalaliDate = if (isoDateStr != null) {
                    PersianCalendarUtil.isoGregorianToJalali(isoDateStr)
                        ?: return Result.failure(IllegalArgumentException("Invalid ISO date from AI: $isoDateStr"))
                } else {
                    PersianCalendarUtil.parseDateKey(fallbackJalaliDateKey)
                }

                val rawTime = optNonNullString(obj, "time")
                val parsedTime: Pair<Int, Int>? = if (rawTime == null) {
                    null
                } else {
                    parseStrictTime(rawTime)
                        ?: return Result.failure(IllegalArgumentException("Invalid time format from AI: $rawTime"))
                }

                val hasExplicitDuration = obj.has("durationMinutes") && !obj.isNull("durationMinutes")
                val rawDur = if (hasExplicitDuration) obj.optInt("durationMinutes", -1) else -1
                if (hasExplicitDuration && (rawDur !in 5..720)) {
                    return Result.failure(IllegalArgumentException("Invalid duration from AI: $rawDur"))
                }
                val finalDuration = if (rawDur in 5..720) rawDur else defaultDurationMinutes.coerceIn(10, 240)
                val durationInferred = !hasExplicitDuration

                val priority = TaskPriority.fromNameOrNull(obj.optString("priority", "LOW")) ?: TaskPriority.LOW
                val rawCat = obj.optString("category", "PLANNING").trim()
                val category = categoryMap[rawCat.uppercase()] ?: rawCat.ifEmpty { "برنامه‌ریزی" }
                val description = obj.optString("description", "").trim()

                val subtasksArr = obj.optJSONArray("subtasks") ?: JSONArray()
                val subtasks = (0 until subtasksArr.length()).mapIndexedNotNull { sIdx, _ ->
                    val sTitle = subtasksArr.optString(sIdx, "").trim()
                    if (sTitle.isEmpty()) null
                    else SubtaskItem(id = "ai_${i}_$sIdx", title = sTitle, isDone = false)
                }

                val offsetsArr = obj.optJSONArray("reminderOffsetsMinutes") ?: JSONArray()
                val parsedOffsets = (0 until offsetsArr.length())
                    .mapNotNull { idx -> offsetsArr.optInt(idx, -1).takeIf { it in 0..240 } }
                    .distinct()
                val finalOffsets = if (parsedTime == null) {
                    emptyList()
                } else {
                    parsedOffsets.ifEmpty { listOf(defaultReminderOffsetMinutes, 0).distinct() }
                }

                val candidateTask = TaskItem(
                    id = if (intentKind == AiIntentKind.MODIFY_TASK && targetTaskId != null) targetTaskId else 0L,
                    title = title.take(90),
                    aiNotes = description,
                    subtasks = subtasks,
                    jalaliDateKey = jalaliDate.dateKey,
                    timeHour = parsedTime?.first,
                    timeMinute = parsedTime?.second,
                    durationMinutes = finalDuration,
                    isDurationUserSpecified = !durationInferred,
                    priority = priority,
                    category = category,
                    isCompleted = false,
                    isAiGenerated = !isOfflineFallback,
                    remindersEnabled = parsedTime != null,
                    reminderOffsetsMinutes = finalOffsets
                )

                val dayTasks = RecurrenceEngine.tasksForDate(existingTasks, jalaliDate.dateKey)
                val conflicts = ConflictDetector.findConflictsForCandidate(candidateTask, dayTasks)

                drafts.add(
                    ProposedTaskDraft(
                        task = candidateTask,
                        isTimeExplicit = parsedTime != null,
                        isDurationInferred = durationInferred,
                        targetDate = jalaliDate,
                        relativeDateHint = null,
                        conflictsWithExisting = conflicts
                    )
                )
            }

            val existingTarget = targetTaskId?.let { id -> existingTasks.firstOrNull { it.id == id } }

            val safeReply = assistantReply.ifEmpty {
                clarificationQuestion ?: when {
                    drafts.isNotEmpty() -> "${PersianCalendarUtil.toPersianDigits(drafts.size)} کار برای بررسی آماده شد."
                    else -> "درخواست شما بررسی شد."
                }
            }

            Result.success(
                AiPlanResponse(
                    intentKind = if (requiresClarification) AiIntentKind.CLARIFY else intentKind,
                    originalTranscript = originalTranscript,
                    proposedTasks = drafts,
                    targetExistingTaskToComplete = if (intentKind == AiIntentKind.COMPLETE_TASK) existingTarget else null,
                    targetExistingTaskToUpdate = if (intentKind == AiIntentKind.MODIFY_TASK) existingTarget else null,
                    requiresClarification = requiresClarification,
                    clarificationQuestion = clarificationQuestion,
                    assistantReply = safeReply,
                    recommendations = recommendations,
                    isOfflineFallback = isOfflineFallback,
                    noticeMessage = noticeMessage
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun validateInsightsJson(
        rawJson: String,
        isOfflineFallback: Boolean = false,
        noticeMessage: String? = null
    ): Result<AiInsightResult> {
        return try {
            val root = JSONObject(rawJson)
            val morningPlan = root.optString("morningPlan", "").trim()
            val eveningSummary = root.optString("eveningSummary", "").trim()
            val weeklyReview = root.optString("weeklyReview", "").trim()
            val workloadTip = root.optString("workloadBalanceTip", "").trim()

            if (morningPlan.isEmpty() && eveningSummary.isEmpty() && weeklyReview.isEmpty()) {
                return Result.failure(IllegalArgumentException("Empty AI insights response"))
            }

            val tomorrowArr = root.optJSONArray("tomorrowSuggestions") ?: JSONArray()
            val tomorrowSuggestions = (0 until tomorrowArr.length())
                .mapNotNull { tomorrowArr.optString(it, "").trim().takeIf { s -> s.isNotEmpty() } }

            val goalArr = root.optJSONArray("goalTips") ?: JSONArray()
            val goalTips = (0 until goalArr.length())
                .mapNotNull { goalArr.optString(it, "").trim().takeIf { s -> s.isNotEmpty() } }

            Result.success(
                AiInsightResult(
                    morningPlan = morningPlan.ifEmpty { "برنامه صبحگاهی بر اساس اولویت‌های امروز تنظیم شده است." },
                    eveningSummary = eveningSummary.ifEmpty { "جمع‌بندی پایان روز آماده بررسی است." },
                    weeklyReview = weeklyReview.ifEmpty { "مرور عملکرد هفتگی محاسبه شد." },
                    tomorrowSuggestions = tomorrowSuggestions,
                    workloadBalanceTip = workloadTip.ifEmpty { "بین بلوک‌های کاری فشرده وقفه کوتاه استراحت قرار دهید." },
                    goalTips = goalTips,
                    isOfflineFallback = isOfflineFallback,
                    noticeMessage = noticeMessage
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseStrictTime(raw: String): Pair<Int, Int>? {
        val clean = PersianCalendarUtil.toEnglishDigits(raw.trim())
        val match = Regex("""^([01]?\d|2[0-3]):([0-5]\d)$""").matchEntire(clean) ?: return null
        val h = match.groupValues[1].toIntOrNull() ?: return null
        val m = match.groupValues[2].toIntOrNull() ?: return null
        return if (h in 0..23 && m in 0..59) h to m else null
    }

    private fun optNonNullString(obj: JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val value = obj.optString(key, "").trim()
        if (value.isEmpty() || value.equals("null", ignoreCase = true)) return null
        return value
    }
}
