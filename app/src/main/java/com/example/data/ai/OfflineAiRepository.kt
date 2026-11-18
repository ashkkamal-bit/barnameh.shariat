package com.example.data.ai

import com.example.data.model.GoalProgressSummary
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.domain.ConflictDetector
import com.example.domain.InsightsCalculator
import com.example.domain.MeasuredDayMetrics
import com.example.domain.MeasuredWeekMetrics
import com.example.domain.RecurrenceEngine
import com.example.util.PersianCalendarUtil
import com.example.util.PersianScheduleParser
import kotlinx.coroutines.delay

/**
 * Honest, deterministic local Persian planner & voice command engine.
 * Never pretends to be Gemini; always labels offline status transparently.
 */
class OfflineAiRepository(
    private val customNotice: String = OFFLINE_NOTICE
) : AiRepository {

    companion object {
        const val OFFLINE_NOTICE =
            "حالت آفلاین فعال است (تحلیلگر محلی فارسی) — برای پردازش ابری با Gemini، پیکربندی Firebase لازم است."
    }

    override val isCloudAiAvailable: Boolean = false

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
        delay(120L)
        val raw = transcript.trim()
        val normalized = PersianScheduleParser.normalizePersian(raw)
        val today = PersianCalendarUtil.today()

        // 1. Check conversational follow-up / correction against previousDrafts
        if (previousDrafts.isNotEmpty() && isConversationalFollowUp(normalized)) {
            return applyFollowUpToDrafts(
                raw = raw,
                normalized = normalized,
                previousDrafts = previousDrafts,
                existingTasks = existingTasks,
                fallbackDateKey = fallbackJalaliDateKey
            )
        }

        // 2. Check voice queries and schedule actions ("برنامه امروزمو بگو", "کار بعدی من چیه", "این کار انجام شد", ...)
        handleVoiceQueryOrAction(
            raw = raw,
            normalized = normalized,
            existingTasks = existingTasks,
            today = today
        )?.let { return it }

        // 3. Split multi-task requests cleanly (e.g. "... و بعد ...", "... سپس ...", "،")
        val segments = splitMultiTaskSegments(raw)
        val drafts = mutableListOf<ProposedTaskDraft>()
        var anyClarification = false
        var clarificationQ: String? = null
        var inheritedDate = PersianCalendarUtil.parseDateKey(fallbackJalaliDateKey)

        for ((index, segment) in segments.withIndex()) {
            val parsed = PersianScheduleParser.parse(
                rawInput = segment,
                today = today,
                fallbackDate = if (index == 0) inheritedDate else inheritedDate
            )
            if (parsed.dateDetectedExplicitly) {
                inheritedDate = parsed.targetDate
            }
            if (parsed.isAmbiguous && clarificationQ == null) {
                anyClarification = true
                clarificationQ = parsed.clarificationQuestion
            }

            val segNorm = PersianScheduleParser.normalizePersian(segment)
            val priority = detectPriority(segNorm)
            val category = detectCategory(segNorm)
            val duration = parsed.durationMinutes ?: defaultDurationMinutes.coerceIn(10, 240)
            val durationInferred = parsed.durationMinutes == null

            val offsets = if (parsed.hour != null && parsed.minute != null) {
                listOfNotNull(parsed.reminderOffsetMinutes ?: defaultReminderOffsetMinutes, 0).distinct()
            } else {
                emptyList()
            }

            val task = TaskItem(
                title = parsed.cleanedTitle,
                aiNotes = "ثبت آفلاین بر اساس فرمان: «$segment»",
                subtasks = suggestSubtasksOffline(segNorm),
                jalaliDateKey = parsed.targetDate.dateKey,
                timeHour = parsed.hour,
                timeMinute = parsed.minute,
                durationMinutes = duration,
                isDurationUserSpecified = !durationInferred,
                priority = priority,
                category = category,
                isCompleted = false,
                isAiGenerated = false,
                remindersEnabled = parsed.hour != null,
                reminderOffsetsMinutes = offsets
            )

            val dayTasks = RecurrenceEngine.tasksForDate(existingTasks, parsed.targetDate.dateKey)
            val conflicts = ConflictDetector.findConflictsForCandidate(task, dayTasks)

            drafts.add(
                ProposedTaskDraft(
                    task = task,
                    isTimeExplicit = parsed.isTimeExplicit,
                    isDurationInferred = durationInferred,
                    targetDate = parsed.targetDate,
                    relativeDateHint = parsed.relativeDateHint,
                    conflictsWithExisting = conflicts
                )
            )
        }

        val reply = buildDraftReply(drafts, anyClarification, clarificationQ)
        val recommendations = buildList {
            val totalConflicts = drafts.sumOf { it.conflictsWithExisting.size }
            if (totalConflicts > 0) {
                add("تداخل زمانی با برنامه فعلی شناسایی شد؛ می‌توانید پیش از ذخیره زمان را تغییر دهید.")
            }
            if (drafts.any { !it.isTimeExplicit } && !anyClarification) {
                add("چون ساعتی ذکر نشد، کار به صورت «بدون ساعت ثابت» آماده شده تا در وقت آزاد انجام دهید.")
            }
        }

        return AiPlanResponse(
            intentKind = if (anyClarification) AiIntentKind.CLARIFY else AiIntentKind.CREATE_TASKS,
            originalTranscript = raw,
            proposedTasks = drafts,
            requiresClarification = anyClarification,
            clarificationQuestion = clarificationQ,
            assistantReply = reply,
            recommendations = recommendations,
            isOfflineFallback = true,
            noticeMessage = customNotice
        )
    }

    override suspend fun generateInsights(
        dayMetrics: MeasuredDayMetrics,
        weekMetrics: MeasuredWeekMetrics,
        goalSummaries: List<GoalProgressSummary>
    ): AiInsightResult {
        delay(100L)
        val morningPlan = if (dayMetrics.totalTasks == 0) {
            "امروز هیچ فعالیتی ثبت نشده است. با افزودن ۲ تا ۳ کار اصلی روز خود را شروع کنید."
        } else {
            "امروز ${PersianCalendarUtil.toPersianDigits(dayMetrics.totalTasks)} کار ثبت شده که ${
                PersianCalendarUtil.toPersianDigits(dayMetrics.highPriorityPendingCount)
            } مورد فوری در انتظار انجام است."
        }

        val eveningSummary = if (dayMetrics.totalTasks == 0) {
            "داده‌ای برای جمع‌بندی پایان روز وجود ندارد."
        } else {
            "${PersianCalendarUtil.toPersianDigits(dayMetrics.completedCount)} از ${
                PersianCalendarUtil.toPersianDigits(dayMetrics.totalTasks)
            } کار امروز تکمیل شده (${PersianCalendarUtil.toPersianDigits(dayMetrics.completionPercent)}٪)."
        }

        val weeklyReview = "در هفته جاری ${PersianCalendarUtil.toPersianDigits(weekMetrics.completedTasks)} از ${
            PersianCalendarUtil.toPersianDigits(weekMetrics.totalTasks)
        } فعالیت تکمیل شده است."

        return AiInsightResult(
            morningPlan = morningPlan,
            eveningSummary = eveningSummary,
            weeklyReview = weeklyReview,
            tomorrowSuggestions = listOf("پیشنهادهای تحلیلی هوش مصنوعی ابری در حالت آفلاین در دسترس نیستند؛ آمار بالا بر اساس داده‌های واقعی شما محاسبه شده است."),
            workloadBalanceTip = "مدیریت تعادل بار کاری به صورت محلی: از تداخل زمانی بپرهیزید.",
            goalTips = emptyList(),
            isOfflineFallback = true,
            noticeMessage = "تحلیلگر هوشمند ابری (Gemini) در دسترس نیست؛ آمار بالا دقیق و محاسبه‌شده از پایگاه داده است."
        )
    }

    private fun isConversationalFollowUp(normalized: String): Boolean {
        val startsWithCorrection = normalized.startsWith("نه") ||
            normalized.startsWith("بله") ||
            normalized.startsWith("آره") ||
            normalized.startsWith("اوکی") ||
            normalized.startsWith("تایید") ||
            normalized.contains("طول میکشه") ||
            normalized.contains("طول می کشه") ||
            normalized.contains("مدتش") ||
            normalized.contains("عوض کن") ||
            normalized.contains("تغییر بده") ||
            normalized.contains("ساعتش") ||
            normalized.contains("ساعت رو") ||
            normalized.contains("زمانش") ||
            Regex("""^(صبح|بامداد|ظهر|بعد\s*از\s*ظهر|بعدازظهر|عصر|شب)$""").matches(normalized) ||
            Regex("""^(?:نه\s+)?(?:ساعت|زمان)(?:ش|شو|ش\s+رو)?(?:\s+(?:رو|را))?(?:\s+(?:بذار|بزار|بکن|کن))?\s+\S+.*$""").matches(normalized)
        return startsWithCorrection
    }

    private fun applyFollowUpToDrafts(
        raw: String,
        normalized: String,
        previousDrafts: List<ProposedTaskDraft>,
        existingTasks: List<TaskItem>,
        fallbackDateKey: String
    ): AiPlanResponse {
        val last = previousDrafts.last()
        val rewrittenForParser = normalized.replace(
            Regex("""(?:ساعتش|ساعتشو|ساعت\s+رو|زمانش|زمانشو|ساعت)(?:\s+(?:رو|را))?(?:\s+(?:بذار|بزار|بکن|کن|تغییر\s+بده(?:\s+به)?|عوض\s*کن(?:\s+به)?))?\s+"""),
            "ساعت "
        )
        val parsed = PersianScheduleParser.parse(
            rawInput = rewrittenForParser,
            today = PersianCalendarUtil.today(),
            fallbackDate = last.targetDate
        )

        var updatedTask = last.task
        var updatedDate = last.targetDate
        var isTimeExplicit = last.isTimeExplicit
        var isDurationInferred = last.isDurationInferred
        var resolvedAmbiguityViaPeriod = false

        val normalizedPeriodWord = when {
            normalized == "صبح" || normalized == "بامداد" -> "صبح"
            normalized == "ظهر" -> "ظهر"
            normalized == "عصر" || normalized == "بعد از ظهر" || normalized == "بعدازظهر" -> "عصر"
            normalized == "شب" -> "شب"
            else -> null
        }

        // Handle "صبح" / "عصر" / "شب" reply to ambiguous hour question
        if (parsed.hour != null && parsed.minute != null && !parsed.isAmbiguous) {
            updatedTask = updatedTask.copy(
                timeHour = parsed.hour,
                timeMinute = parsed.minute,
                remindersEnabled = true,
                reminderOffsetsMinutes = updatedTask.reminderOffsetsMinutes.ifEmpty { listOf(10, 0) }
            )
            isTimeExplicit = true
        } else if (normalizedPeriodWord != null && last.task.timeHour != null) {
            val resolvedHour = PersianScheduleParser.adjustHourWithPeriod(
                last.task.timeHour,
                normalizedPeriodWord
            ).coerceIn(0, 23)
            updatedTask = updatedTask.copy(
                timeHour = resolvedHour,
                timeMinute = last.task.timeMinute ?: 0,
                remindersEnabled = true,
                reminderOffsetsMinutes = updatedTask.reminderOffsetsMinutes.ifEmpty { listOf(10, 0) }
            )
            isTimeExplicit = true
            resolvedAmbiguityViaPeriod = true
        } else if (normalizedPeriodWord != null && last.task.timeHour == null) {
            // Keep clarification if no hour existed yet
            return AiPlanResponse(
                intentKind = AiIntentKind.CLARIFY,
                originalTranscript = raw,
                proposedTasks = previousDrafts,
                requiresClarification = true,
                clarificationQuestion = "لطفاً ساعت دقیق را در بازه «$normalized» بفرمایید (مثلاً ساعت ۹ $normalized).",
                assistantReply = "لطفاً ساعت دقیق را در بازه «$normalized» بفرمایید.",
                isOfflineFallback = true,
                noticeMessage = customNotice
            )
        } else if (parsed.hour != null && parsed.minute != null) {
            updatedTask = updatedTask.copy(
                timeHour = parsed.hour,
                timeMinute = parsed.minute,
                remindersEnabled = true,
                reminderOffsetsMinutes = updatedTask.reminderOffsetsMinutes.ifEmpty { listOf(10, 0) }
            )
        }

        if (parsed.dateDetectedExplicitly) {
            updatedDate = parsed.targetDate
            updatedTask = updatedTask.copy(jalaliDateKey = parsed.targetDate.dateKey)
        }
        if (parsed.durationMinutes != null) {
            updatedTask = updatedTask.copy(
                durationMinutes = parsed.durationMinutes,
                isDurationUserSpecified = true
            )
            isDurationInferred = false
        }
        if (parsed.isAmbiguous && !resolvedAmbiguityViaPeriod) {
            return AiPlanResponse(
                intentKind = AiIntentKind.CLARIFY,
                originalTranscript = raw,
                proposedTasks = previousDrafts,
                requiresClarification = true,
                clarificationQuestion = parsed.clarificationQuestion,
                assistantReply = parsed.clarificationQuestion ?: "لطفاً زمان را دقیق‌تر بفرمایید.",
                isOfflineFallback = true,
                noticeMessage = customNotice
            )
        }

        val dayTasks = RecurrenceEngine.tasksForDate(existingTasks, updatedDate.dateKey)
        val conflicts = ConflictDetector.findConflictsForCandidate(updatedTask, dayTasks)
        val updatedDraft = last.copy(
            task = updatedTask,
            targetDate = updatedDate,
            isTimeExplicit = isTimeExplicit,
            isDurationInferred = isDurationInferred,
            conflictsWithExisting = conflicts
        )
        val updatedList = previousDrafts.dropLast(1) + updatedDraft

        val timeText = updatedTask.formatTime()
        val durText = updatedTask.formatDuration()
        val reply = "برنامه «${updatedTask.title}» برای ${updatedDate.formattedDayAndMonth()} ($timeText، مدت $durText) به‌روزرسانی شد. تأیید می‌کنید؟"

        return AiPlanResponse(
            intentKind = AiIntentKind.CREATE_TASKS,
            originalTranscript = raw,
            proposedTasks = updatedList,
            requiresClarification = false,
            assistantReply = reply,
            isOfflineFallback = true,
            noticeMessage = customNotice
        )
    }

    private fun handleVoiceQueryOrAction(
        raw: String,
        normalized: String,
        existingTasks: List<TaskItem>,
        today: com.example.util.JalaliDate
    ): AiPlanResponse? {
        val tomorrow = PersianCalendarUtil.offsetFromToday(1)

        // Overdue query
        if (normalized.contains("عقب افتاده") || normalized.contains("عقب‌افتاده")) {
            val overdue = InsightsCalculator.findOverdueTasks(existingTasks)
            val reply = if (overdue.isEmpty()) {
                "هیچ کار عقب‌افتاده‌ای ندارید؛ همه‌چیز به‌روز است."
            } else {
                val listText = overdue.take(4).joinToString("، ") { "${it.title} (${PersianCalendarUtil.parseDateKey(it.jalaliDateKey).formattedDayAndMonth()})" }
                "${PersianCalendarUtil.toPersianDigits(overdue.size)} کار عقب‌افتاده دارید: $listText"
            }
            return AiPlanResponse(
                intentKind = AiIntentKind.QUERY_SCHEDULE,
                originalTranscript = raw,
                assistantReply = reply,
                isOfflineFallback = true,
                noticeMessage = customNotice
            )
        }

        // Next task query
        if (normalized.contains("کار بعدی") || normalized.contains("بعدی من چیه") || normalized.contains("برنامه بعدی")) {
            val todayPending = RecurrenceEngine.tasksForDate(existingTasks, today.dateKey).filter { !it.isCompleted }
            val next = todayPending.firstOrNull { it.isScheduled } ?: todayPending.firstOrNull()
            val reply = if (next == null) {
                "امروز کار انجام‌نشده‌ای در صف ندارید."
            } else {
                val mins = PersianCalendarUtil.minutesUntil(next.effectiveDateKey, next.timeHour, next.timeMinute)
                val rem = if (mins != null) " (${PersianCalendarUtil.formatRemainingPersian(mins)})" else ""
                "کار بعدی شما: «${next.title}» ساعت ${next.formatTime()}$rem"
            }
            return AiPlanResponse(
                intentKind = AiIntentKind.QUERY_SCHEDULE,
                originalTranscript = raw,
                assistantReply = reply,
                isOfflineFallback = true,
                noticeMessage = customNotice
            )
        }

        // Complete current task command: "این کار انجام شد"
        if (normalized.contains("انجام شد") && !normalized.contains("چی")) {
            val candidate = RecurrenceEngine.tasksForDate(existingTasks, today.dateKey)
                .firstOrNull { !it.isCompleted }
            val reply = if (candidate == null) {
                "کار در حال انتظاری برای امروز پیدا نشد."
            } else {
                "آیا کار «${candidate.title}» به عنوان انجام‌شده علامت بخورد؟"
            }
            return AiPlanResponse(
                intentKind = AiIntentKind.COMPLETE_TASK,
                originalTranscript = raw,
                targetExistingTaskToComplete = candidate,
                assistantReply = reply,
                isOfflineFallback = true,
                noticeMessage = customNotice
            )
        }

        // Summary query: "برنامه هامو خلاصه کن"
        if (normalized.contains("خلاصه")) {
            val metrics = InsightsCalculator.calculateDayMetrics(existingTasks, today.dateKey)
            val reply = if (metrics.totalTasks == 0) {
                "امروز هنوز برنامه‌ای ثبت نشده است."
            } else {
                "امروز ${PersianCalendarUtil.toPersianDigits(metrics.totalTasks)} کار دارید (${
                    PersianCalendarUtil.toPersianDigits(metrics.completedCount)
                } تکمیل‌شده و ${PersianCalendarUtil.toPersianDigits(metrics.pendingCount)} باقیمانده؛ مجموع زمان ${
                    PersianCalendarUtil.formatDurationMinutes(metrics.totalWorkloadMinutes)
                })."
            }
            return AiPlanResponse(
                intentKind = AiIntentKind.SUMMARIZE,
                originalTranscript = raw,
                assistantReply = reply,
                isOfflineFallback = true,
                noticeMessage = customNotice
            )
        }

        // Today / Tomorrow schedule query
        val isQueryWords = normalized.contains("بگو") || normalized.contains("چیه") ||
            normalized.contains("چی دارم") || normalized.contains("چه کارهایی") ||
            normalized.contains("نشان بده") || normalized.contains("نشون بده")
        if (isQueryWords && (normalized.contains("امروز") || normalized.contains("فردا"))) {
            val target = if (normalized.contains("فردا")) tomorrow else today
            val dayLabel = if (normalized.contains("فردا")) "فردا" else "امروز"
            val tasks = RecurrenceEngine.tasksForDate(existingTasks, target.dateKey).filter { !it.isCompleted }
            val reply = if (tasks.isEmpty()) {
                "برای $dayLabel (${target.formattedDayAndMonth()}) کاری ثبت نشده است."
            } else {
                val listText = tasks.take(5).joinToString("؛ ") { "${it.title} (${it.formatTime()})" }
                "برنامه $dayLabel (${PersianCalendarUtil.toPersianDigits(tasks.size)} کار): $listText"
            }
            return AiPlanResponse(
                intentKind = AiIntentKind.QUERY_SCHEDULE,
                originalTranscript = raw,
                assistantReply = reply,
                isOfflineFallback = true,
                noticeMessage = customNotice
            )
        }

        // Move / Reschedule explicit request without deletion: "جلسه فردا رو حذف نکن، فقط جابه‌جاش کن"
        if (normalized.contains("جابه‌جا") || normalized.contains("جابه جا") || normalized.contains("منتقل")) {
            val candidate = existingTasks.firstOrNull { !it.isCompleted && (normalized.contains("فردا") && it.jalaliDateKey == tomorrow.dateKey) }
                ?: existingTasks.firstOrNull { !it.isCompleted }
            if (candidate != null) {
                val parsed = PersianScheduleParser.parse(raw, today = today, fallbackDate = PersianCalendarUtil.parseDateKey(candidate.jalaliDateKey))
                val updated = candidate.copy(
                    jalaliDateKey = parsed.targetDate.dateKey,
                    timeHour = parsed.hour ?: candidate.timeHour,
                    timeMinute = parsed.minute ?: candidate.timeMinute
                )
                return AiPlanResponse(
                    intentKind = AiIntentKind.MODIFY_TASK,
                    originalTranscript = raw,
                    targetExistingTaskToUpdate = candidate,
                    proposedTasks = listOf(
                        ProposedTaskDraft(
                            task = updated,
                            isTimeExplicit = parsed.isTimeExplicit,
                            isDurationInferred = false,
                            targetDate = parsed.targetDate,
                            relativeDateHint = parsed.relativeDateHint
                        )
                    ),
                    requiresClarification = parsed.hour == null && !parsed.dateDetectedExplicitly,
                    clarificationQuestion = if (parsed.hour == null) "زمان جدید برای «${candidate.title}» چه ساعتی باشد؟" else null,
                    assistantReply = "تغییر زمان «${candidate.title}» (بدون حذف کار) آماده بررسی است.",
                    isOfflineFallback = true,
                    noticeMessage = customNotice
                )
            }
        }

        return null
    }

    private fun splitMultiTaskSegments(raw: String): List<String> {
        val splitRegex = Regex(
            """\s*(?:،|,|؛|(?<=^|\s)(?:و\s+بعد|بعدش|سپس|و\s+همینطور)(?=\s|$)|(?<=^|\s)و\s+(?=ساعت\s))\s*"""
        )
        val parts = raw.split(splitRegex)
            .map { it.trim() }
            .filter { it.length >= 2 }
        return if (parts.isEmpty()) listOf(raw) else parts
    }

    private fun detectPriority(normalized: String): TaskPriority = when {
        normalized.contains("فوری") || normalized.contains("مهم") || normalized.contains("ضروری") || normalized.contains("اورژانسی") ->
            TaskPriority.HIGH
        normalized.contains("جلسه") || normalized.contains("تماس") || normalized.contains("ارائه") ->
            TaskPriority.MEDIUM
        else -> TaskPriority.LOW
    }

    private fun detectCategory(normalized: String): String = when {
        normalized.contains("جلسه") || normalized.contains("تیم") || normalized.contains("قرار") -> "جلسه"
        normalized.contains("تمرکز") || normalized.contains("کد") || normalized.contains("مطالعه") || normalized.contains("پروژه") -> "تمرکز عمیق"
        normalized.contains("ورزش") || normalized.contains("باشگاه") || normalized.contains("پیاده") -> "سلامتی"
        normalized.contains("خرید") || normalized.contains("شخصی") || normalized.contains("خانه") || normalized.contains("منزل") -> "شخصی"
        else -> "کار"
    }

    private fun suggestSubtasksOffline(normalized: String): List<SubtaskItem> {
        return when {
            normalized.contains("جلسه") || normalized.contains("ارائه") -> listOf(
                SubtaskItem("s_1", "مرور دستور جلسه و نکات کلیدی", false),
                SubtaskItem("s_2", "ثبت مصوبات و اقدامات بعدی", false)
            )
            else -> emptyList()
        }
    }

    private fun buildDraftReply(
        drafts: List<ProposedTaskDraft>,
        requiresClarification: Boolean,
        clarificationQ: String?
    ): String {
        if (requiresClarification && clarificationQ != null) {
            return clarificationQ
        }
        if (drafts.isEmpty()) {
            return "متأسفانه مورد قابل ثبتی شناسایی نشد؛ لطفاً واضح‌تر بنویسید."
        }
        if (drafts.size == 1) {
            val d = drafts.first()
            val dateLabel = d.relativeDateHint ?: d.targetDate.formattedDayAndMonth()
            val timeLabel = if (d.isTimeExplicit) "ساعت ${d.task.formatTime()}" else "بدون ساعت ثابت"
            return "«${d.task.title}» برای $dateLabel ($timeLabel) آماده ثبت است. تأیید می‌کنید؟"
        }
        return "${PersianCalendarUtil.toPersianDigits(drafts.size)} کار از پیام شما استخراج شد. پس از بررسی می‌توانید همه را ثبت کنید."
    }
}
