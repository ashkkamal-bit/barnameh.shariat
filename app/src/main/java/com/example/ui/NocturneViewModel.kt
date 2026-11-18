package com.example.ui

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiChatMessage
import com.example.data.ai.AiInsightResult
import com.example.data.ai.AiPlanResponse
import com.example.data.ai.AiRepository
import com.example.data.ai.AiRepositoryRouter
import com.example.data.ai.AppCheckInitializer
import com.example.data.ai.ProposedTaskDraft
import com.example.data.local.NocturneDatabase
import com.example.data.model.GoalProgressSummary
import com.example.data.model.PersonalGoal
import com.example.data.model.RecurrenceEditScope
import com.example.data.model.RecurrenceType
import com.example.data.model.ScheduleConflict
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.repository.BackupManager
import com.example.data.repository.BackupPreview
import com.example.data.repository.GoalRepository
import com.example.data.repository.NocturnePreferences
import com.example.data.repository.TaskRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.domain.ConflictDetector
import com.example.domain.InsightsCalculator
import com.example.domain.MeasuredDayMetrics
import com.example.domain.MeasuredWeekMetrics
import com.example.domain.RecurrenceEngine
import com.example.notifications.AlarmScheduleReport
import com.example.notifications.AlarmScheduler
import com.example.notifications.NotificationChannels
import com.example.notifications.ReminderReconciliationManager
import com.example.util.JalaliDate
import com.example.util.PersianCalendarUtil
import com.example.voice.SpeechRecognitionManager
import com.example.voice.SpeechState
import com.example.voice.TextToSpeechManager
import com.example.voice.TtsStatus
import com.example.widgets.NocturneWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

sealed interface AiSynthesisUiState {
    data object Idle : AiSynthesisUiState
    data object Listening : AiSynthesisUiState
    data object Processing : AiSynthesisUiState
    data class PreviewReady(val response: AiPlanResponse) : AiSynthesisUiState
    data class Error(val message: String) : AiSynthesisUiState
}

sealed interface AiInsightUiState {
    data object Idle : AiInsightUiState
    data object Loading : AiInsightUiState
    data class Ready(val result: AiInsightResult) : AiInsightUiState
    data class Error(val message: String) : AiInsightUiState
}

data class TodayUiState(
    val todayDate: JalaliDate = PersianCalendarUtil.today(),
    val selectedDate: JalaliDate = PersianCalendarUtil.today(),
    val weekDates: List<JalaliDate> = (-1..13).map { PersianCalendarUtil.offsetFromToday(it) },
    val searchQuery: String = "",
    val selectedPriorityFilter: TaskPriority? = null,
    val tasksForSelectedDate: List<TaskItem> = emptyList(),
    val scheduledTasksForSelectedDate: List<TaskItem> = emptyList(),
    val unscheduledTasksForSelectedDate: List<TaskItem> = emptyList(),
    val overdueTasks: List<TaskItem> = emptyList(),
    val conflictsForSelectedDate: List<ScheduleConflict> = emptyList(),
    val allTasks: List<TaskItem> = emptyList(),
    val expandedTaskId: Long? = null,
    val currentTimeMinutes: Int = Calendar.getInstance().let {
        it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE)
    },
    val dayMetrics: MeasuredDayMetrics = InsightsCalculator.calculateDayMetrics(emptyList(), PersianCalendarUtil.today().dateKey),
    val weekMetrics: MeasuredWeekMetrics = InsightsCalculator.calculateWeekMetrics(emptyList(), PersianCalendarUtil.today())
) {
    val completedCount: Int get() = tasksForSelectedDate.count { it.isCompleted }
    val totalCount: Int get() = tasksForSelectedDate.size
    val activeTaskId: Long?
        get() = scheduledTasksForSelectedDate.firstOrNull { !it.isCompleted }?.id
            ?: tasksForSelectedDate.firstOrNull { !it.isCompleted }?.id
}

class NocturneViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val database = NocturneDatabase.getInstance(appContext)
    private val taskRepository = TaskRepository(database.taskDao())
    private val goalRepository = GoalRepository(database.goalDao())
    private val prefsRepository = UserPreferencesRepository(appContext)
    private val backupManager = BackupManager(database, prefsRepository)
    private val aiRepository: AiRepository = AiRepositoryRouter(appContext)

    val speechManager = SpeechRecognitionManager(appContext)
    val ttsManager = TextToSpeechManager(appContext)

    val speechState: StateFlow<SpeechState> = speechManager.state
    val ttsStatus: StateFlow<TtsStatus> = ttsManager.status
    val isCloudAiConfigured: Boolean
        get() = AppCheckInitializer.isFirebaseConfigured(appContext)

    private val _clockTickMillis = MutableStateFlow(System.currentTimeMillis())
    private val _selectedDate = MutableStateFlow(PersianCalendarUtil.today())
    private val _searchQuery = MutableStateFlow("")
    private val _priorityFilter = MutableStateFlow<TaskPriority?>(null)
    private val _expandedTaskId = MutableStateFlow<Long?>(null)

    private val _aiSynthesisState = MutableStateFlow<AiSynthesisUiState>(AiSynthesisUiState.Idle)
    val aiSynthesisState: StateFlow<AiSynthesisUiState> = _aiSynthesisState.asStateFlow()

    private val _aiInsightState = MutableStateFlow<AiInsightUiState>(AiInsightUiState.Idle)
    val aiInsightState: StateFlow<AiInsightUiState> = _aiInsightState.asStateFlow()

    private val _voiceSheetVisible = MutableStateFlow(false)
    val voiceSheetVisible: StateFlow<Boolean> = _voiceSheetVisible.asStateFlow()

    private val _voiceDraftInput = MutableStateFlow("")
    val voiceDraftInput: StateFlow<String> = _voiceDraftInput.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<AiChatMessage>> = _chatHistory.asStateFlow()

    private val _pendingSessionDrafts = MutableStateFlow<List<ProposedTaskDraft>>(emptyList())

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _backupPreview = MutableStateFlow<BackupPreview?>(null)
    val backupPreview: StateFlow<BackupPreview?> = _backupPreview.asStateFlow()

    private val _alarmStatus = MutableStateFlow(
        AlarmScheduleReport(
            scheduledTaskAlarms = 0,
            usedInexactFallback = false,
            exactAlarmPermitted = AlarmScheduler.canScheduleExactAlarms(appContext),
            postNotificationPermitted = AlarmScheduler.hasPostNotificationPermission(appContext)
        )
    )
    val alarmStatus: StateFlow<AlarmScheduleReport> = _alarmStatus.asStateFlow()

    val preferences: StateFlow<NocturnePreferences> = prefsRepository.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = NocturnePreferences()
        )

    val goalSummaries: StateFlow<List<GoalProgressSummary>> = combine(
        goalRepository.observeAllGoals(),
        goalRepository.observeAllHabitLogs(),
        _clockTickMillis
    ) { goals, logs, _ ->
        val today = PersianCalendarUtil.today(AlarmScheduler.resolveTimeZone(preferences.value))
        InsightsCalculator.calculateGoalProgress(goals, logs, today)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val todayUiState: StateFlow<TodayUiState> = combine(
        taskRepository.observeAllTasks(),
        _selectedDate,
        _searchQuery,
        _priorityFilter,
        _expandedTaskId,
        _clockTickMillis,
        preferences
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allTasks = values[0] as List<TaskItem>
        var selectedDate = values[1] as JalaliDate
        val query = values[2] as String
        val priorityFilter = values[3] as TaskPriority?
        val expandedId = values[4] as Long?
        val nowMillis = values[5] as Long
        val prefs = values[6] as NocturnePreferences

        val timeZone = AlarmScheduler.resolveTimeZone(prefs)
        val nowCal = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val today = PersianCalendarUtil.fromCalendar(nowCal)
        val baseWeekDates = (-1..13).map { PersianCalendarUtil.addDays(today, it) }
        val visibleDates = if (baseWeekDates.any { it.dateKey == selectedDate.dateKey }) {
            baseWeekDates
        } else {
            (listOf(selectedDate) + baseWeekDates).sortedBy { it.dateKey }
        }

        val expandedDateTasks = RecurrenceEngine.tasksForDate(allTasks, selectedDate.dateKey)
        val queryFiltered = if (query.isBlank()) {
            expandedDateTasks
        } else {
            val q = query.trim()
            expandedDateTasks.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.aiNotes.contains(q, ignoreCase = true) ||
                    it.category.contains(q, ignoreCase = true)
            }
        }
        val finalTasks = if (priorityFilter == null) {
            queryFiltered
        } else {
            queryFiltered.filter { it.priority == priorityFilter }
        }

        val scheduled = finalTasks.filter { it.isScheduled }
        val unscheduled = finalTasks.filter { !it.isScheduled }
        val overdue = InsightsCalculator.findOverdueTasks(allTasks, nowCal)
        val conflicts = ConflictDetector.findConflicts(expandedDateTasks)
        val currentMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
        val dayMetrics = InsightsCalculator.calculateDayMetrics(allTasks, selectedDate.dateKey, nowCal)
        val weekMetrics = InsightsCalculator.calculateWeekMetrics(allTasks, selectedDate, nowCal)

        TodayUiState(
            todayDate = today,
            selectedDate = selectedDate,
            weekDates = visibleDates,
            searchQuery = query,
            selectedPriorityFilter = priorityFilter,
            tasksForSelectedDate = finalTasks,
            scheduledTasksForSelectedDate = scheduled,
            unscheduledTasksForSelectedDate = unscheduled,
            overdueTasks = overdue,
            conflictsForSelectedDate = conflicts,
            allTasks = allTasks,
            expandedTaskId = expandedId,
            currentTimeMinutes = currentMinutes,
            dayMetrics = dayMetrics,
            weekMetrics = weekMetrics
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TodayUiState()
    )

    init {
        NotificationChannels.ensureCreated(appContext)
        ReminderReconciliationManager.schedulePeriodicReconciliation(appContext)
        refreshClockAndSyncAlarms()

        // Observe speech manager state transitions
        viewModelScope.launch {
            speechManager.state.collect { state ->
                when (state) {
                    is SpeechState.Listening -> {
                        _aiSynthesisState.value = AiSynthesisUiState.Listening
                    }
                    is SpeechState.Result -> {
                        _voiceDraftInput.value = state.transcript
                        processVoiceInput(state.transcript)
                    }
                    is SpeechState.Error -> {
                        _aiSynthesisState.value = AiSynthesisUiState.Error(state.message)
                    }
                    is SpeechState.Idle -> Unit
                }
            }
        }

        // Foreground-safe 30s clock tick (stops automatically when ViewModel clears; zero WakeLock battery drain)
        viewModelScope.launch {
            var lastDateKey = PersianCalendarUtil.today().dateKey
            while (isActive) {
                delay(30_000L)
                val prefs = preferences.value
                val tz = AlarmScheduler.resolveTimeZone(prefs)
                val nowToday = PersianCalendarUtil.today(tz)
                if (nowToday.dateKey != lastDateKey && _selectedDate.value.dateKey == lastDateKey) {
                    _selectedDate.value = nowToday
                }
                lastDateKey = nowToday.dateKey
                _clockTickMillis.value = System.currentTimeMillis()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        ttsManager.shutdown()
    }

    fun refreshClockAndSyncAlarms() {
        val prevToday = PersianCalendarUtil.today(AlarmScheduler.resolveTimeZone(preferences.value))
        _clockTickMillis.value = System.currentTimeMillis()
        if (_selectedDate.value.dateKey < prevToday.dateKey) {
            // Leave manual past selection intact, only refresh permissions and alarms
        }
        viewModelScope.launch {
            val report = ReminderReconciliationManager.reconcileAll(appContext, notifyOverdue = false)
            _alarmStatus.value = report
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun postMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun selectDate(date: JalaliDate) {
        _selectedDate.value = date
        _expandedTaskId.value = null
        NocturneWidgetUpdater.updateAllAsync(appContext)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectPriorityFilter(priority: TaskPriority?) {
        _priorityFilter.value = if (_priorityFilter.value == priority) null else priority
    }

    fun toggleTaskExpanded(taskId: Long) {
        _expandedTaskId.value = if (_expandedTaskId.value == taskId) null else taskId
    }

    fun expandTask(taskId: Long) {
        _expandedTaskId.value = taskId
    }

    fun focusTaskFromDeepLink(taskId: Long) {
        viewModelScope.launch {
            val task = taskRepository.getAllTasksOnce().firstOrNull { it.id == taskId }
            if (task != null) {
                PersianCalendarUtil.parseDateKey(task.effectiveDateKey)?.let {
                    _selectedDate.value = it
                }
            }
            _expandedTaskId.value = taskId
        }
    }

    fun toggleTaskCompleted(task: TaskItem) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompleted(task.id)
            syncAfterMutation()
        }
    }

    fun toggleSubtask(task: TaskItem, subtaskId: String) {
        viewModelScope.launch {
            taskRepository.toggleSubtask(task.id, subtaskId)
            syncAfterMutation()
        }
    }

    fun saveTask(task: TaskItem) {
        viewModelScope.launch {
            val prefs = preferences.value
            val prepared = task.copy(
                timezoneId = task.timezoneId.ifBlank { AlarmScheduler.resolveTimeZone(prefs).id }
            )
            if (prepared.id == 0L) {
                val newId = taskRepository.addTask(prepared)
                _expandedTaskId.value = newId
            } else {
                taskRepository.updateTask(prepared)
            }
            syncAfterMutation()
        }
    }

    fun saveRecurringEdit(
        original: TaskItem,
        updated: TaskItem,
        occurrenceDateKey: String,
        scope: RecurrenceEditScope
    ) {
        viewModelScope.launch {
            val id = taskRepository.updateRecurringTask(original, updated, occurrenceDateKey, scope)
            _expandedTaskId.value = id
            syncAfterMutation()
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
            AlarmScheduler.cancelTaskAlarms(appContext, taskId)
            syncAfterMutation()
        }
    }

    fun deleteRecurringOccurrence(seriesId: Long, occurrenceDateKey: String) {
        viewModelScope.launch {
            taskRepository.excludeRecurringOccurrence(seriesId, occurrenceDateKey)
            syncAfterMutation()
        }
    }

    fun rescheduleTaskToSlot(task: TaskItem, newHour: Int, newMinute: Int) {
        viewModelScope.launch {
            taskRepository.updateTask(
                task.copy(
                    timeHour = newHour,
                    timeMinute = newMinute,
                    remindersEnabled = true,
                    reminderOffsetsMinutes = task.reminderOffsetsMinutes.ifEmpty {
                        listOf(preferences.value.defaultReminderOffsetMinutes, 0).distinct()
                    }
                )
            )
            syncAfterMutation()
            _snackbarMessage.value = "زمان «${task.title}» به ${
                PersianCalendarUtil.formatTime(newHour, newMinute, preferences.value.usePersianDigits)
            } تغییر یافت."
        }
    }

    // --- Voice Assistant & Conversational AI ---

    fun openVoiceSheet(prefillCommand: String? = null) {
        _voiceSheetVisible.value = true
        if (!prefillCommand.isNullOrBlank()) {
            _voiceDraftInput.value = prefillCommand
            processVoiceInput(prefillCommand)
        } else if (preferences.value.autoListenOnVoiceTap && speechManager.hasAudioPermission()) {
            startVoiceListening()
        } else {
            _aiSynthesisState.value = AiSynthesisUiState.Idle
        }
    }

    fun closeVoiceSheet() {
        speechManager.resetState()
        ttsManager.stop()
        _voiceSheetVisible.value = false
        _aiSynthesisState.value = AiSynthesisUiState.Idle
        _chatHistory.value = emptyList()
        _pendingSessionDrafts.value = emptyList()
    }

    fun startVoiceListening() {
        ttsManager.stop()
        speechManager.startListening(preferences.value.speechLanguageTag)
    }

    fun stopVoiceListening() {
        speechManager.stopListening()
    }

    fun updateVoiceDraft(text: String) {
        _voiceDraftInput.value = text
    }

    fun processVoiceInput(transcript: String) {
        val cleaned = transcript.trim()
        if (cleaned.isEmpty()) {
            _aiSynthesisState.value = AiSynthesisUiState.Error("لطفاً متن یا فرمان صوتی خود را وارد کنید.")
            return
        }
        _voiceDraftInput.value = ""
        val updatedHistory = _chatHistory.value + AiChatMessage(isUser = true, text = cleaned)
        _chatHistory.value = updatedHistory

        viewModelScope.launch {
            _aiSynthesisState.value = AiSynthesisUiState.Processing
            try {
                val prefs = preferences.value
                val response = aiRepository.processPlanningMessage(
                    transcript = cleaned,
                    fallbackJalaliDateKey = _selectedDate.value.dateKey,
                    defaultDurationMinutes = prefs.defaultBlockMinutes,
                    defaultReminderOffsetMinutes = prefs.defaultReminderOffsetMinutes,
                    existingTasks = todayUiState.value.allTasks,
                    conversationHistory = updatedHistory,
                    previousDrafts = _pendingSessionDrafts.value,
                    activeGoals = goalSummaries.value
                )
                _pendingSessionDrafts.value = response.proposedTasks
                _chatHistory.value = _chatHistory.value + AiChatMessage(isUser = false, text = response.assistantReply)
                _aiSynthesisState.value = AiSynthesisUiState.PreviewReady(response)
                ttsManager.setSpeechRate(prefs.ttsSpeechRate)
                ttsManager.speakIfAllowed(response.assistantReply, prefs.ttsEnabled)
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "خطا در پردازش فرمان"
                _aiSynthesisState.value = AiSynthesisUiState.Error(err)
            }
        }
    }

    fun confirmDraftTasks(drafts: List<ProposedTaskDraft>) {
        if (drafts.isEmpty()) return
        viewModelScope.launch {
            val prefs = preferences.value
            val tzId = AlarmScheduler.resolveTimeZone(prefs).id
            val toInsert = drafts.filter { it.task.id == 0L }.map {
                it.task.copy(timezoneId = tzId)
            }
            val toUpdate = drafts.filter { it.task.id > 0L }.map {
                it.task.copy(timezoneId = tzId)
            }
            if (toInsert.isNotEmpty()) {
                val ids = taskRepository.addTasks(toInsert)
                _expandedTaskId.value = ids.firstOrNull()
            }
            for (upd in toUpdate) {
                taskRepository.updateTask(upd)
                _expandedTaskId.value = upd.id
            }
            val focusDate = drafts.first().targetDate
            _selectedDate.value = focusDate
            syncAfterMutation()

            _pendingSessionDrafts.value = emptyList()
            _chatHistory.value = emptyList()
            _voiceDraftInput.value = ""
            _aiSynthesisState.value = AiSynthesisUiState.Idle
            _voiceSheetVisible.value = false
            _snackbarMessage.value = "${
                PersianCalendarUtil.formatNumber(drafts.size, prefs.usePersianDigits)
            } مورد در برنامه شما ثبت شد."
        }
    }

    fun confirmCompleteExistingTask(task: TaskItem) {
        viewModelScope.launch {
            taskRepository.setTaskCompleted(task.id, true)
            syncAfterMutation()
            _pendingSessionDrafts.value = emptyList()
            _chatHistory.value = emptyList()
            _aiSynthesisState.value = AiSynthesisUiState.Idle
            _voiceSheetVisible.value = false
            _snackbarMessage.value = "کار «${task.title}» تکمیل شد."
        }
    }

    // --- Goals & Habits ---

    fun saveGoal(goal: PersonalGoal) {
        viewModelScope.launch {
            goalRepository.saveGoal(goal)
            syncAfterMutation()
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            goalRepository.deleteGoal(goalId)
            AlarmScheduler.cancelGoalAlarm(appContext, goalId)
            syncAfterMutation()
        }
    }

    fun toggleGoalHabitToday(goalId: Long) {
        viewModelScope.launch {
            val todayKey = PersianCalendarUtil.today(AlarmScheduler.resolveTimeZone(preferences.value)).dateKey
            goalRepository.toggleHabitForDate(goalId, todayKey)
        }
    }

    // --- Insights ---

    fun refreshDailyInsights() {
        val state = todayUiState.value
        val goals = goalSummaries.value
        viewModelScope.launch {
            _aiInsightState.value = AiInsightUiState.Loading
            try {
                val result = aiRepository.generateInsights(
                    dayMetrics = state.dayMetrics,
                    weekMetrics = state.weekMetrics,
                    goalSummaries = goals
                )
                _aiInsightState.value = AiInsightUiState.Ready(result)
            } catch (e: Exception) {
                _aiInsightState.value = AiInsightUiState.Error(
                    e.localizedMessage ?: "خطا در تحلیل بهره‌وری"
                )
            }
        }
    }

    // --- Backup & Restore (SAF) ---

    fun exportBackupToUri(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) { backupManager.exportToJson() }
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("دسترسی نوشتن فایل در دسترس نیست")
                }
                _snackbarMessage.value = "فایل پشتیبان با موفقیت ذخیره شد."
            } catch (e: Exception) {
                _snackbarMessage.value = "خطا در تهیه نسخه پشتیبان: ${e.localizedMessage ?: "نامشخص"}"
            }
        }
    }

    fun inspectBackupFromUri(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            try {
                val raw = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: error("امکان خواندن فایل وجود ندارد")
                }
                val preview = withContext(Dispatchers.IO) { backupManager.inspectBackup(raw) }
                _backupPreview.value = preview
            } catch (e: Exception) {
                _snackbarMessage.value = "خطا در بررسی فایل پشتیبان: ${e.localizedMessage ?: "نامشخص"}"
            }
        }
    }

    fun dismissBackupPreview() {
        _backupPreview.value = null
    }

    fun confirmRestoreBackup(replaceExisting: Boolean) {
        val preview = _backupPreview.value ?: return
        viewModelScope.launch {
            try {
                val summary = withContext(Dispatchers.IO) {
                    backupManager.restoreBackup(preview, replaceExisting = replaceExisting, restorePreferences = true)
                }
                _backupPreview.value = null
                syncAfterMutation()
                _snackbarMessage.value = "بازیابی انجام شد: ${
                    PersianCalendarUtil.formatNumber(summary.restoredTasks, preferences.value.usePersianDigits)
                } کار و ${
                    PersianCalendarUtil.formatNumber(summary.restoredGoals, preferences.value.usePersianDigits)
                } هدف."
            } catch (e: Exception) {
                _backupPreview.value = null
                _snackbarMessage.value = "خطا در بازیابی اطلاعات: ${e.localizedMessage ?: "نامشخص"}"
            }
        }
    }

    // --- Explicit Demo Data & Clear All ---

    fun importDemoTasks(replaceExisting: Boolean) {
        viewModelScope.launch {
            val count = taskRepository.importDemoTasks(replaceExisting = replaceExisting)
            syncAfterMutation()
            _snackbarMessage.value = "${
                PersianCalendarUtil.formatNumber(count, preferences.value.usePersianDigits)
            } کار نمونه با موفقیت وارد شد."
        }
    }

    fun clearAllUserTasks() {
        viewModelScope.launch {
            val all = taskRepository.getAllTasksOnce()
            all.forEach { AlarmScheduler.cancelTaskAlarms(appContext, it.id) }
            taskRepository.clearAllUserTasks()
            syncAfterMutation()
            _snackbarMessage.value = "تمام کارهای شما با موفقیت پاک شدند."
        }
    }

    // --- Preferences ---

    fun setHapticsEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepository.setHapticsEnabled(enabled)
    }

    fun setAutoListenEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepository.setAutoListenOnVoiceTap(enabled)
    }

    fun setSpeechLanguageTag(tag: String) = viewModelScope.launch {
        prefsRepository.setSpeechLanguageTag(tag)
    }

    fun setTtsEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepository.setTtsEnabled(enabled)
    }

    fun setTtsSpeechRate(rate: Float) = viewModelScope.launch {
        ttsManager.setSpeechRate(rate)
        prefsRepository.setTtsSpeechRate(rate)
    }

    fun setDefaultBlockMinutes(minutes: Int) = viewModelScope.launch {
        prefsRepository.setDefaultBlockMinutes(minutes)
    }

    fun setDefaultReminderOffset(minutes: Int) = viewModelScope.launch {
        prefsRepository.setDefaultReminderOffsetMinutes(minutes)
    }

    fun setTaskRemindersEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepository.setTaskRemindersEnabled(enabled)
        syncAfterMutation()
    }

    fun setOverdueAlertsEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepository.setOverdueAlertsEnabled(enabled)
    }

    fun setMorningSummary(enabled: Boolean, hour: Int) = viewModelScope.launch {
        prefsRepository.setMorningSummary(enabled, hour)
        syncAfterMutation()
    }

    fun setEveningSummary(enabled: Boolean, hour: Int) = viewModelScope.launch {
        prefsRepository.setEveningSummary(enabled, hour)
        syncAfterMutation()
    }

    fun setHabitRemindersEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepository.setHabitRemindersEnabled(enabled)
        syncAfterMutation()
    }

    fun setDarkTheme(isDark: Boolean) = viewModelScope.launch {
        prefsRepository.setDarkTheme(isDark)
    }

    fun setUsePersianDigits(usePersian: Boolean) = viewModelScope.launch {
        prefsRepository.setUsePersianDigits(usePersian)
        NocturneWidgetUpdater.updateAll(appContext)
    }

    fun setTimezoneConfig(useSystem: Boolean, customId: String) = viewModelScope.launch {
        prefsRepository.setTimezoneConfig(useSystem, customId)
        _clockTickMillis.value = System.currentTimeMillis()
        syncAfterMutation()
    }

    fun updateWidgetSettings(
        visibleCount: Int? = null,
        showCompleted: Boolean? = null,
        showDescriptions: Boolean? = null,
        showPriorities: Boolean? = null,
        darkTheme: Boolean? = null,
        hidePrivate: Boolean? = null
    ) = viewModelScope.launch {
        prefsRepository.updateWidgetSettings(
            visibleCount = visibleCount,
            showCompleted = showCompleted,
            showDescriptions = showDescriptions,
            showPriorities = showPriorities,
            darkTheme = darkTheme,
            hidePrivate = hidePrivate
        )
        NocturneWidgetUpdater.updateAll(appContext)
    }

    private suspend fun syncAfterMutation() {
        val report = ReminderReconciliationManager.reconcileAll(appContext, notifyOverdue = false)
        _alarmStatus.value = report
    }

    fun getActiveTimeZone(): TimeZone = AlarmScheduler.resolveTimeZone(preferences.value)
}
