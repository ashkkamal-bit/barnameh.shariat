package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.HabitLogEntity
import com.example.data.local.NocturneDatabase
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.GoalPeriod
import com.example.data.model.HabitLog
import com.example.data.model.PersonalGoal
import com.example.data.model.RecurrenceType
import com.example.data.model.SubtaskItem
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.util.PersianCalendarUtil
import org.json.JSONArray
import org.json.JSONObject

data class BackupPreview(
    val isValid: Boolean,
    val schemaVersion: Int,
    val exportedAtMillis: Long,
    val validTasksCount: Int,
    val invalidTasksCount: Int,
    val duplicateTasksCount: Int,
    val goalsCount: Int,
    val habitLogsCount: Int,
    val includesPreferences: Boolean,
    val errorMessage: String?,
    val parsedTasks: List<TaskItem> = emptyList(),
    val parsedGoals: List<PersonalGoal> = emptyList(),
    val parsedHabitLogs: List<HabitLog> = emptyList(),
    val parsedPreferences: NocturnePreferences? = null
)

data class RestoreSummary(
    val restoredTasks: Int,
    val skippedDuplicates: Int,
    val restoredGoals: Int,
    val restoredHabitLogs: Int,
    val restoredPreferences: Boolean
)

class BackupManager(
    private val database: NocturneDatabase,
    private val prefsRepository: UserPreferencesRepository
) {
    companion object {
        const val CURRENT_BACKUP_VERSION = 2
        const val APP_SIGNATURE = "nocturne-planner-backup"
    }

    suspend fun exportToJson(): String {
        val tasks = database.taskDao().getAllTasksOnce().map { it.toDomain() }
        val goals = database.goalDao().getAllGoalsOnce().map { it.toDomain() }
        val logs = database.goalDao().getAllHabitLogsOnce().map { it.toDomain() }
        val prefs = prefsRepository.getPreferencesOnce()

        val root = JSONObject().apply {
            put("app", APP_SIGNATURE)
            put("schemaVersion", CURRENT_BACKUP_VERSION)
            put("exportedAtMillis", System.currentTimeMillis())
            put("tasks", JSONArray().apply {
                tasks.forEach { put(taskToJson(it)) }
            })
            put("goals", JSONArray().apply {
                goals.forEach { put(goalToJson(it)) }
            })
            put("habitLogs", JSONArray().apply {
                logs.forEach { put(habitLogToJson(it)) }
            })
            put("preferences", preferencesToJson(prefs))
        }
        return root.toString(2)
    }

    suspend fun inspectBackup(rawJson: String): BackupPreview {
        return try {
            val root = JSONObject(rawJson)
            val appTag = root.optString("app", "")
            val schemaVersion = root.optInt("schemaVersion", 0)
            if (appTag != APP_SIGNATURE || schemaVersion !in 1..CURRENT_BACKUP_VERSION) {
                return BackupPreview(
                    isValid = false,
                    schemaVersion = schemaVersion,
                    exportedAtMillis = 0L,
                    validTasksCount = 0,
                    invalidTasksCount = 0,
                    duplicateTasksCount = 0,
                    goalsCount = 0,
                    habitLogsCount = 0,
                    includesPreferences = false,
                    errorMessage = "ساختار فایل پشتیبان با فرمت Nocturne مطابقت ندارد."
                )
            }

            val existingSignatures = database.taskDao().getAllTasksOnce()
                .map { taskSignature(it.toDomain()) }
                .toHashSet()
            val seenInFile = HashSet<String>()

            val tasksArr = root.optJSONArray("tasks") ?: JSONArray()
            val validTasks = mutableListOf<TaskItem>()
            var invalidCount = 0
            var duplicateCount = 0

            for (i in 0 until tasksArr.length()) {
                val obj = tasksArr.optJSONObject(i)
                val parsed = obj?.let { jsonToTaskOrNull(it) }
                if (parsed == null) {
                    invalidCount++
                } else {
                    val sig = taskSignature(parsed)
                    if (!seenInFile.add(sig) || sig in existingSignatures) {
                        duplicateCount++
                    }
                    validTasks.add(parsed)
                }
            }

            val goalsArr = root.optJSONArray("goals") ?: JSONArray()
            val validGoals = (0 until goalsArr.length()).mapNotNull { idx ->
                goalsArr.optJSONObject(idx)?.let { jsonToGoalOrNull(it) }
            }

            val logsArr = root.optJSONArray("habitLogs") ?: JSONArray()
            val validLogs = (0 until logsArr.length()).mapNotNull { idx ->
                logsArr.optJSONObject(idx)?.let { jsonToHabitLogOrNull(it) }
            }

            val prefsObj = root.optJSONObject("preferences")
            val parsedPrefs = prefsObj?.let { jsonToPreferences(it) }

            BackupPreview(
                isValid = true,
                schemaVersion = schemaVersion,
                exportedAtMillis = root.optLong("exportedAtMillis", 0L),
                validTasksCount = validTasks.size,
                invalidTasksCount = invalidCount,
                duplicateTasksCount = duplicateCount,
                goalsCount = validGoals.size,
                habitLogsCount = validLogs.size,
                includesPreferences = parsedPrefs != null,
                errorMessage = null,
                parsedTasks = validTasks,
                parsedGoals = validGoals,
                parsedHabitLogs = validLogs,
                parsedPreferences = parsedPrefs
            )
        } catch (e: Exception) {
            BackupPreview(
                isValid = false,
                schemaVersion = 0,
                exportedAtMillis = 0L,
                validTasksCount = 0,
                invalidTasksCount = 0,
                duplicateTasksCount = 0,
                goalsCount = 0,
                habitLogsCount = 0,
                includesPreferences = false,
                errorMessage = "فایل انتخاب‌شده JSON معتبر نیست."
            )
        }
    }

    suspend fun restoreBackup(
        preview: BackupPreview,
        replaceExisting: Boolean,
        restorePreferences: Boolean = true
    ): RestoreSummary {
        require(preview.isValid) { preview.errorMessage ?: "Invalid backup" }

        var insertedTasks = 0
        var skippedDuplicates = 0
        var restoredGoals = 0
        var restoredLogs = 0

        database.withTransaction {
            val taskDao = database.taskDao()
            val goalDao = database.goalDao()

            if (replaceExisting) {
                taskDao.deleteAll()
                goalDao.deleteAllLogs()
                goalDao.deleteAllGoals()
                val seen = HashSet<String>()
                val uniqueTasks = preview.parsedTasks.filter { seen.add(taskSignature(it)) }
                skippedDuplicates = preview.parsedTasks.size - uniqueTasks.size
                if (uniqueTasks.isNotEmpty()) {
                    taskDao.insertTasks(uniqueTasks.map { it.copy(id = 0L).toEntity() })
                    insertedTasks = uniqueTasks.size
                }
            } else {
                val existingSigs = taskDao.getAllTasksOnce().map { taskSignature(it.toDomain()) }.toHashSet()
                val toInsert = mutableListOf<TaskItem>()
                for (t in preview.parsedTasks) {
                    val sig = taskSignature(t)
                    if (existingSigs.add(sig)) {
                        toInsert.add(t.copy(id = 0L))
                    } else {
                        skippedDuplicates++
                    }
                }
                if (toInsert.isNotEmpty()) {
                    taskDao.insertTasks(toInsert.map { it.toEntity() })
                    insertedTasks = toInsert.size
                }
            }

            // Map old goalIds -> new goalIds cleanly so habit logs stay linked
            val existingGoalTitles = if (replaceExisting) {
                emptyMap()
            } else {
                goalDao.getAllGoalsOnce().associate { it.title.trim() to it.id }
            }
            val goalIdMap = mutableMapOf<Long, Long>()
            for (g in preview.parsedGoals) {
                val existingId = existingGoalTitles[g.title.trim()]
                if (existingId != null) {
                    goalIdMap[g.id] = existingId
                } else {
                    val newId = goalDao.insertGoal(g.copy(id = 0L).toEntity())
                    goalIdMap[g.id] = newId
                    restoredGoals++
                }
            }

            for (log in preview.parsedHabitLogs) {
                val mappedGoalId = goalIdMap[log.goalId] ?: continue
                val existing = goalDao.getLogForDate(mappedGoalId, log.jalaliDateKey)
                if (existing == null) {
                    goalDao.upsertHabitLog(
                        HabitLogEntity(
                            id = 0L,
                            goalId = mappedGoalId,
                            jalaliDateKey = log.jalaliDateKey,
                            count = log.count,
                            loggedAtMillis = log.loggedAtMillis
                        )
                    )
                    restoredLogs++
                }
            }
        }

        val prefsApplied = if (restorePreferences && preview.parsedPreferences != null) {
            prefsRepository.applyFromBackup(preview.parsedPreferences)
            true
        } else {
            false
        }

        return RestoreSummary(
            restoredTasks = insertedTasks,
            skippedDuplicates = skippedDuplicates,
            restoredGoals = restoredGoals,
            restoredHabitLogs = restoredLogs,
            restoredPreferences = prefsApplied
        )
    }

    private fun taskSignature(t: TaskItem): String =
        "${t.title.trim().lowercase()}|${t.jalaliDateKey}|${t.timeHour ?: -1}|${t.timeMinute ?: -1}"

    private fun taskToJson(task: TaskItem): JSONObject = JSONObject().apply {
        put("title", task.title)
        put("aiNotes", task.aiNotes)
        put("jalaliDateKey", task.jalaliDateKey)
        if (task.timeHour != null) put("timeHour", task.timeHour) else put("timeHour", JSONObject.NULL)
        if (task.timeMinute != null) put("timeMinute", task.timeMinute) else put("timeMinute", JSONObject.NULL)
        put("durationMinutes", task.durationMinutes)
        put("isDurationUserSpecified", task.isDurationUserSpecified)
        put("priorityLevel", task.priority.level)
        put("category", task.category)
        put("isCompleted", task.isCompleted)
        put("isAiGenerated", task.isAiGenerated)
        put("remindersEnabled", task.remindersEnabled)
        put("reminderOffsetsMinutes", JSONArray(task.reminderOffsetsMinutes))
        put("recurrenceType", task.recurrenceType.code)
        put("recurrenceInterval", task.recurrenceInterval)
        if (task.recurrenceEndDateKey != null) put("recurrenceEndDateKey", task.recurrenceEndDateKey)
        put("recurrenceExceptions", JSONArray(task.recurrenceExceptions.toList()))
        put("timezoneId", task.timezoneId)
        put("isPrivate", task.isPrivate)
        put("createdAtMillis", task.createdAtMillis)
        put("updatedAtMillis", task.updatedAtMillis)
        put("subtasks", JSONArray().apply {
            task.subtasks.forEach { sub ->
                put(JSONObject().apply {
                    put("title", sub.title)
                    put("isDone", sub.isDone)
                })
            }
        })
    }

    private fun jsonToTaskOrNull(obj: JSONObject): TaskItem? {
        val title = obj.optString("title", "").trim()
        val dateKey = obj.optString("jalaliDateKey", "").trim()
        if (title.isEmpty() || !PersianCalendarUtil.isValidJalaliDateKey(dateKey)) return null

        val hour = if (obj.isNull("timeHour")) null else obj.optInt("timeHour", -1).takeIf { it in 0..23 }
        val minute = if (obj.isNull("timeMinute")) null else obj.optInt("timeMinute", -1).takeIf { it in 0..59 }
        val (validHour, validMinute) = if (hour != null && minute != null) hour to minute else null to null

        val subtasksArr = obj.optJSONArray("subtasks") ?: JSONArray()
        val subtasks = (0 until subtasksArr.length()).mapNotNull { idx ->
            val subObj = subtasksArr.optJSONObject(idx) ?: return@mapNotNull null
            val subTitle = subObj.optString("title", "").trim()
            if (subTitle.isEmpty()) null
            else SubtaskItem(id = "b_$idx", title = subTitle, isDone = subObj.optBoolean("isDone", false))
        }

        val offsetsArr = obj.optJSONArray("reminderOffsetsMinutes") ?: JSONArray()
        val offsets = (0 until offsetsArr.length()).mapNotNull { idx ->
            offsetsArr.optInt(idx, -1).takeIf { it in 0..1440 }
        }

        val excArr = obj.optJSONArray("recurrenceExceptions") ?: JSONArray()
        val exceptions = (0 until excArr.length()).mapNotNull { idx ->
            excArr.optString(idx, "").trim().takeIf { PersianCalendarUtil.isValidJalaliDateKey(it) }
        }.toSet()

        return TaskItem(
            title = title,
            aiNotes = obj.optString("aiNotes", ""),
            subtasks = subtasks,
            jalaliDateKey = dateKey,
            timeHour = validHour,
            timeMinute = validMinute,
            durationMinutes = obj.optInt("durationMinutes", 45).coerceIn(5, 720),
            isDurationUserSpecified = obj.optBoolean("isDurationUserSpecified", true),
            priority = TaskPriority.fromLevel(obj.optInt("priorityLevel", 0)),
            category = obj.optString("category", "برنامه‌ریزی").ifBlank { "برنامه‌ریزی" },
            isCompleted = obj.optBoolean("isCompleted", false),
            isAiGenerated = obj.optBoolean("isAiGenerated", false),
            remindersEnabled = obj.optBoolean("remindersEnabled", true),
            reminderOffsetsMinutes = offsets.ifEmpty { if (obj.has("reminderOffsetsMinutes")) emptyList() else listOf(10, 0) },
            recurrenceType = RecurrenceType.fromCode(obj.optString("recurrenceType", "NONE")),
            recurrenceInterval = obj.optInt("recurrenceInterval", 1).coerceAtLeast(1),
            recurrenceEndDateKey = obj.optString("recurrenceEndDateKey", "").takeIf { PersianCalendarUtil.isValidJalaliDateKey(it) },
            recurrenceExceptions = exceptions,
            timezoneId = obj.optString("timezoneId", ""),
            isPrivate = obj.optBoolean("isPrivate", false),
            createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
            updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
        )
    }

    private fun goalToJson(goal: PersonalGoal): JSONObject = JSONObject().apply {
        put("id", goal.id)
        put("title", goal.title)
        put("description", goal.description)
        put("period", goal.period.code)
        put("targetPerPeriod", goal.targetPerPeriod)
        put("category", goal.category)
        put("reminderEnabled", goal.reminderEnabled)
        put("reminderHour", goal.reminderHour)
        put("reminderMinute", goal.reminderMinute)
        put("isActive", goal.isActive)
        put("createdAtMillis", goal.createdAtMillis)
    }

    private fun jsonToGoalOrNull(obj: JSONObject): PersonalGoal? {
        val title = obj.optString("title", "").trim()
        if (title.isEmpty()) return null
        return PersonalGoal(
            id = obj.optLong("id", 0L),
            title = title,
            description = obj.optString("description", ""),
            period = GoalPeriod.fromCode(obj.optString("period", "DAILY")),
            targetPerPeriod = obj.optInt("targetPerPeriod", 1).coerceIn(1, 365),
            category = obj.optString("category", "رشد شخصی"),
            reminderEnabled = obj.optBoolean("reminderEnabled", false),
            reminderHour = obj.optInt("reminderHour", 20).coerceIn(0, 23),
            reminderMinute = obj.optInt("reminderMinute", 0).coerceIn(0, 59),
            isActive = obj.optBoolean("isActive", true),
            createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
        )
    }

    private fun habitLogToJson(log: HabitLog): JSONObject = JSONObject().apply {
        put("goalId", log.goalId)
        put("jalaliDateKey", log.jalaliDateKey)
        put("count", log.count)
        put("loggedAtMillis", log.loggedAtMillis)
    }

    private fun jsonToHabitLogOrNull(obj: JSONObject): HabitLog? {
        val dateKey = obj.optString("jalaliDateKey", "").trim()
        if (!PersianCalendarUtil.isValidJalaliDateKey(dateKey)) return null
        return HabitLog(
            goalId = obj.optLong("goalId", 0L),
            jalaliDateKey = dateKey,
            count = obj.optInt("count", 1).coerceAtLeast(1),
            loggedAtMillis = obj.optLong("loggedAtMillis", System.currentTimeMillis())
        )
    }

    private fun preferencesToJson(p: NocturnePreferences): JSONObject = JSONObject().apply {
        put("hapticsEnabled", p.hapticsEnabled)
        put("speechLanguageTag", p.speechLanguageTag)
        put("ttsEnabled", p.ttsEnabled)
        put("ttsSpeechRate", p.ttsSpeechRate.toDouble())
        put("autoListenOnVoiceTap", p.autoListenOnVoiceTap)
        put("defaultBlockMinutes", p.defaultBlockMinutes)
        put("defaultReminderOffsetMinutes", p.defaultReminderOffsetMinutes)
        put("taskRemindersEnabled", p.taskRemindersEnabled)
        put("overdueAlertsEnabled", p.overdueAlertsEnabled)
        put("morningSummaryEnabled", p.morningSummaryEnabled)
        put("morningSummaryHour", p.morningSummaryHour)
        put("eveningSummaryEnabled", p.eveningSummaryEnabled)
        put("eveningSummaryHour", p.eveningSummaryHour)
        put("habitRemindersEnabled", p.habitRemindersEnabled)
        put("isDarkTheme", p.isDarkTheme)
        put("usePersianDigits", p.usePersianDigits)
        put("useSystemTimezone", p.useSystemTimezone)
        put("customTimezoneId", p.customTimezoneId)
        put("widgetVisibleCount", p.widgetVisibleCount)
        put("widgetShowCompleted", p.widgetShowCompleted)
        put("widgetShowDescriptions", p.widgetShowDescriptions)
        put("widgetShowPriorities", p.widgetShowPriorities)
        put("widgetDarkTheme", p.widgetDarkTheme)
        put("widgetHidePrivateTitles", p.widgetHidePrivateTitles)
    }

    private fun jsonToPreferences(obj: JSONObject): NocturnePreferences = NocturnePreferences(
        hapticsEnabled = obj.optBoolean("hapticsEnabled", true),
        speechLanguageTag = obj.optString("speechLanguageTag", "fa-IR"),
        ttsEnabled = obj.optBoolean("ttsEnabled", false),
        ttsSpeechRate = obj.optDouble("ttsSpeechRate", 1.0).toFloat().coerceIn(0.6f, 1.6f),
        autoListenOnVoiceTap = obj.optBoolean("autoListenOnVoiceTap", false),
        defaultBlockMinutes = obj.optInt("defaultBlockMinutes", 45).coerceIn(10, 240),
        defaultReminderOffsetMinutes = obj.optInt("defaultReminderOffsetMinutes", 10).coerceIn(0, 120),
        taskRemindersEnabled = obj.optBoolean("taskRemindersEnabled", true),
        overdueAlertsEnabled = obj.optBoolean("overdueAlertsEnabled", true),
        morningSummaryEnabled = obj.optBoolean("morningSummaryEnabled", false),
        morningSummaryHour = obj.optInt("morningSummaryHour", 8).coerceIn(5, 11),
        eveningSummaryEnabled = obj.optBoolean("eveningSummaryEnabled", false),
        eveningSummaryHour = obj.optInt("eveningSummaryHour", 21).coerceIn(17, 23),
        habitRemindersEnabled = obj.optBoolean("habitRemindersEnabled", true),
        isDarkTheme = obj.optBoolean("isDarkTheme", true),
        usePersianDigits = obj.optBoolean("usePersianDigits", true),
        useSystemTimezone = obj.optBoolean("useSystemTimezone", true),
        customTimezoneId = obj.optString("customTimezoneId", ""),
        widgetVisibleCount = obj.optInt("widgetVisibleCount", 3).coerceIn(1, 8),
        widgetShowCompleted = obj.optBoolean("widgetShowCompleted", false),
        widgetShowDescriptions = obj.optBoolean("widgetShowDescriptions", false),
        widgetShowPriorities = obj.optBoolean("widgetShowPriorities", true),
        widgetDarkTheme = obj.optBoolean("widgetDarkTheme", true),
        widgetHidePrivateTitles = obj.optBoolean("widgetHidePrivateTitles", false)
    )
}
