package com.example

import com.example.data.ai.AiIntentKind
import com.example.data.ai.AiResponseValidator
import com.example.data.ai.OfflineAiRepository
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.util.PersianCalendarUtil
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiResponseValidatorTest {

    @Test
    fun validator_parsesMultiTaskJson_andLeavesMissingTimeUnscheduled() {
        val sampleJson = """
            {
              "intent": "CREATE_TASKS",
              "requiresClarification": false,
              "clarificationQuestion": null,
              "assistantReply": "دو کار برای فردا آماده شد.",
              "tasks": [
                {
                  "title": "جلسه طراحی محصول",
                  "date": "2026-11-17",
                  "time": "10:00",
                  "durationMinutes": 60,
                  "priority": "HIGH",
                  "category": "MEETING",
                  "description": "بررسی طرح نهایی",
                  "subtasks": ["آماده‌سازی فایل", "ارسال لینک"],
                  "reminderOffsetsMinutes": [15, 0]
                },
                {
                  "title": "خرید لوازم تحریر",
                  "date": "2026-11-17",
                  "time": null,
                  "durationMinutes": null,
                  "priority": "LOW",
                  "category": "PERSONAL",
                  "description": "",
                  "subtasks": [],
                  "reminderOffsetsMinutes": []
                }
              ],
              "recommendations": ["صبح انرژی بیشتری دارید"]
            }
        """.trimIndent()

        val result = AiResponseValidator.validateStructuredPlanJson(
            rawJson = sampleJson,
            originalTranscript = "فردا ساعت ۱۰ جلسه طراحی و خرید لوازم تحریر",
            fallbackJalaliDateKey = "1405-08-25",
            defaultDurationMinutes = 45,
            defaultReminderOffsetMinutes = 10,
            existingTasks = emptyList(),
            isOfflineFallback = false
        )

        assertTrue(
            "Validation failed: ${result.exceptionOrNull()?.stackTraceToString()}",
            result.isSuccess
        )
        val plan = result.getOrThrow()
        assertEquals(AiIntentKind.CREATE_TASKS, plan.intentKind)
        assertEquals(2, plan.proposedTasks.size)

        val first = plan.proposedTasks[0]
        assertEquals("1405-08-26", first.task.jalaliDateKey)
        assertEquals(10, first.task.timeHour)
        assertEquals(0, first.task.timeMinute)
        assertEquals(60, first.task.durationMinutes)
        assertFalse(first.isDurationInferred)
        assertEquals(2, first.task.subtasks.size)
        assertEquals(listOf(15, 0), first.task.reminderOffsetsMinutes)

        val second = plan.proposedTasks[1]
        assertNull(second.task.timeHour)
        assertNull(second.task.timeMinute)
        assertEquals(45, second.task.durationMinutes)
        assertTrue(second.isDurationInferred)
    }

    @Test
    fun validator_rejectsMalformedIsoDateAndOutOfRangeDuration() {
        val badDateJson = """
            {
              "intent": "CREATE_TASKS",
              "requiresClarification": false,
              "assistantReply": "پاسخ",
              "tasks": [{"title": "تست", "date": "1405-99-99", "time": "10:00", "durationMinutes": 30}]
            }
        """.trimIndent()
        val badDateRes = AiResponseValidator.validateStructuredPlanJson(
            rawJson = badDateJson,
            originalTranscript = "تست",
            fallbackJalaliDateKey = "1405-08-25",
            defaultDurationMinutes = 45,
            defaultReminderOffsetMinutes = 10,
            existingTasks = emptyList()
        )
        assertTrue(badDateRes.isFailure)

        val badDurJson = """
            {
              "intent": "CREATE_TASKS",
              "requiresClarification": false,
              "assistantReply": "پاسخ",
              "tasks": [{"title": "تست", "date": "2026-11-17", "time": "10:00", "durationMinutes": 9999}]
            }
        """.trimIndent()
        val badDurRes = AiResponseValidator.validateStructuredPlanJson(
            rawJson = badDurJson,
            originalTranscript = "تست",
            fallbackJalaliDateKey = "1405-08-25",
            defaultDurationMinutes = 45,
            defaultReminderOffsetMinutes = 10,
            existingTasks = emptyList()
        )
        assertTrue(badDurRes.isFailure)
    }

    @Test
    fun offlineAiRepository_handlesMultiTask_conversationalFollowUp_andVoiceQueries() = runTest {
        val repo = OfflineAiRepository()
        assertFalse(repo.isCloudAiAvailable)

        val todayKey = PersianCalendarUtil.today().dateKey
        // Multi-task split
        val multi = repo.processPlanningMessage(
            transcript = "فردا ساعت ۹ جلسه تیم و بعد ساعت ۱۴ تمرین ورزشی",
            fallbackJalaliDateKey = todayKey,
            defaultDurationMinutes = 45,
            defaultReminderOffsetMinutes = 10,
            existingTasks = emptyList(),
            conversationHistory = emptyList(),
            previousDrafts = emptyList(),
            activeGoals = emptyList()
        )
        assertTrue(multi.isOfflineFallback)
        assertNotNull(multi.noticeMessage)
        assertEquals(2, multi.proposedTasks.size)

        // Follow-up modification on previous drafts
        val followUp = repo.processPlanningMessage(
            transcript = "ساعتشو بذار ۱۶",
            fallbackJalaliDateKey = todayKey,
            defaultDurationMinutes = 45,
            defaultReminderOffsetMinutes = 10,
            existingTasks = emptyList(),
            conversationHistory = emptyList(),
            previousDrafts = multi.proposedTasks,
            activeGoals = emptyList()
        )
        assertEquals(16, followUp.proposedTasks.last().task.timeHour)

        // Voice query & complete command
        val sampleExisting = listOf(
            TaskItem(
                id = 7L,
                title = "ارسال گزارش ماهانه",
                jalaliDateKey = todayKey,
                timeHour = 18,
                timeMinute = 0,
                priority = TaskPriority.HIGH
            )
        )
        val query = repo.processPlanningMessage(
            transcript = "برنامه امروزمو بگو",
            fallbackJalaliDateKey = todayKey,
            defaultDurationMinutes = 45,
            defaultReminderOffsetMinutes = 10,
            existingTasks = sampleExisting,
            conversationHistory = emptyList(),
            previousDrafts = emptyList(),
            activeGoals = emptyList()
        )
        assertEquals(AiIntentKind.QUERY_SCHEDULE, query.intentKind)
        assertTrue(query.assistantReply.contains("ارسال گزارش ماهانه"))

        val completeCmd = repo.processPlanningMessage(
            transcript = "کار ارسال گزارش ماهانه انجام شد",
            fallbackJalaliDateKey = todayKey,
            defaultDurationMinutes = 45,
            defaultReminderOffsetMinutes = 10,
            existingTasks = sampleExisting,
            conversationHistory = emptyList(),
            previousDrafts = emptyList(),
            activeGoals = emptyList()
        )
        assertEquals(AiIntentKind.COMPLETE_TASK, completeCmd.intentKind)
        assertEquals(7L, completeCmd.targetExistingTaskToComplete?.id)
    }
}
