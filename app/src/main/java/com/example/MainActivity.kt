package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.ai.AppCheckInitializer
import com.example.data.model.TaskItem
import com.example.notifications.AlarmReceiver
import com.example.ui.AiSynthesisUiState
import com.example.ui.NocturneViewModel
import com.example.ui.components.NocturneDockedBottomBar
import com.example.ui.components.ROUTE_CALENDAR
import com.example.ui.components.ROUTE_INSIGHTS
import com.example.ui.components.ROUTE_SETTINGS
import com.example.ui.components.ROUTE_TODAY
import com.example.ui.components.TaskEditorSheet
import com.example.ui.components.VoiceAssistantSheet
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.LightBackground
import com.example.ui.theme.NocturneTheme
import com.example.ui.theme.VoidBlack
import com.example.widgets.NocturneWidgetActions

class MainActivity : ComponentActivity() {

    private var pendingLaunchIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must configure App Check immediately before Firebase AI calls
        AppCheckInitializer.configureAppCheck(this, intent)
        pendingLaunchIntent = intent

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(VoidBlack.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        )
        setContent {
            NocturneRoot(
                launchIntent = pendingLaunchIntent,
                onIntentHandled = { pendingLaunchIntent = null },
                onThemeChanged = { isDark ->
                    if (isDark) {
                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.dark(VoidBlack.toArgb()),
                            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
                        )
                    } else {
                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.light(LightBackground.toArgb(), LightBackground.toArgb()),
                            navigationBarStyle = SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
                        )
                    }
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingLaunchIntent = intent
    }
}

@Composable
private fun NocturneRoot(
    launchIntent: Intent?,
    onIntentHandled: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: NocturneViewModel = viewModel()
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    LaunchedEffect(preferences.isDarkTheme) {
        onThemeChanged(preferences.isDarkTheme)
    }

    NocturneTheme(
        darkTheme = preferences.isDarkTheme,
        usePersianDigits = preferences.usePersianDigits
    ) {
        NocturneApp(
            viewModel = viewModel,
            launchIntent = launchIntent,
            onIntentHandled = onIntentHandled
        )
    }
}

@Composable
fun NocturneApp(
    viewModel: NocturneViewModel,
    launchIntent: Intent? = null,
    onIntentHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_TODAY

    val todayUiState by viewModel.todayUiState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val voiceSheetVisible by viewModel.voiceSheetVisible.collectAsStateWithLifecycle()
    val synthesisState by viewModel.aiSynthesisState.collectAsStateWithLifecycle()
    val speechState by viewModel.speechState.collectAsStateWithLifecycle()
    val ttsStatus by viewModel.ttsStatus.collectAsStateWithLifecycle()
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val voiceDraft by viewModel.voiceDraftInput.collectAsStateWithLifecycle()
    val insightState by viewModel.aiInsightState.collectAsStateWithLifecycle()
    val goalSummaries by viewModel.goalSummaries.collectAsStateWithLifecycle()
    val alarmStatus by viewModel.alarmStatus.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val backupPreview by viewModel.backupPreview.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var editorSheetOpen by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var prefillHour by remember { mutableStateOf<Int?>(null) }
    var prefillMinute by remember { mutableStateOf<Int?>(null) }

    // Refresh clock and reconcile alarms/permissions every time activity returns to foreground
    LifecycleResumeEffect(Unit) {
        viewModel.refreshClockAndSyncAlarms()
        onPauseOrDispose { }
    }

    // Show transient snackbar messages
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    // Handle deep links from Widget & Notification actions
    LaunchedEffect(launchIntent) {
        val intent = launchIntent ?: return@LaunchedEffect
        val widgetAction = intent.getStringExtra(NocturneWidgetActions.EXTRA_WIDGET_ACTION)
        val widgetTaskId = intent.getLongExtra(NocturneWidgetActions.EXTRA_WIDGET_TASK_ID, -1L)
        val notifTaskId = intent.getLongExtra(AlarmReceiver.EXTRA_OPEN_TASK_ID, -1L)

        when {
            widgetAction == NocturneWidgetActions.ACTION_OPEN_VOICE -> {
                viewModel.openVoiceSheet(null)
            }
            widgetAction == NocturneWidgetActions.ACTION_ADD_TASK -> {
                editingTask = null
                prefillHour = null
                prefillMinute = null
                editorSheetOpen = true
            }
            widgetAction == NocturneWidgetActions.ACTION_OPEN_TASK && widgetTaskId > 0L -> {
                viewModel.focusTaskFromDeepLink(widgetTaskId)
                navController.navigate(ROUTE_TODAY) {
                    launchSingleTop = true
                }
            }
            notifTaskId > 0L -> {
                viewModel.focusTaskFromDeepLink(notifTaskId)
                navController.navigate(ROUTE_TODAY) {
                    launchSingleTop = true
                }
            }
        }
        onIntentHandled()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = ROUTE_TODAY,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            composable(ROUTE_TODAY) {
                TodayScreen(
                    uiState = todayUiState,
                    alarmStatus = alarmStatus,
                    hapticsEnabled = preferences.hapticsEnabled,
                    onSelectDate = viewModel::selectDate,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onSelectPriorityFilter = viewModel::selectPriorityFilter,
                    onToggleTaskCompleted = viewModel::toggleTaskCompleted,
                    onToggleTaskExpanded = viewModel::toggleTaskExpanded,
                    onExpandTask = viewModel::expandTask,
                    onToggleSubtask = viewModel::toggleSubtask,
                    onEditTask = { task ->
                        editingTask = task
                        prefillHour = null
                        prefillMinute = null
                        editorSheetOpen = true
                    },
                    onDeleteTask = viewModel::deleteTask,
                    onRescheduleTask = viewModel::rescheduleTaskToSlot,
                    onAddManualTask = {
                        editingTask = null
                        prefillHour = null
                        prefillMinute = null
                        editorSheetOpen = true
                    },
                    onAddTaskAtHour = { h, m ->
                        editingTask = null
                        prefillHour = h
                        prefillMinute = m
                        editorSheetOpen = true
                    },
                    onQuickVoiceCommand = { prompt ->
                        viewModel.openVoiceSheet(prompt)
                    },
                    onRefreshPermissions = viewModel::refreshClockAndSyncAlarms
                )
            }

            composable(ROUTE_CALENDAR) {
                CalendarScreen(
                    uiState = todayUiState,
                    hapticsEnabled = preferences.hapticsEnabled,
                    onSelectDate = viewModel::selectDate,
                    onToggleTaskCompleted = viewModel::toggleTaskCompleted,
                    onToggleTaskExpanded = viewModel::toggleTaskExpanded,
                    onExpandTask = viewModel::expandTask,
                    onToggleSubtask = viewModel::toggleSubtask,
                    onEditTask = { task ->
                        editingTask = task
                        prefillHour = null
                        prefillMinute = null
                        editorSheetOpen = true
                    },
                    onDeleteTask = viewModel::deleteTask,
                    onRescheduleTask = viewModel::rescheduleTaskToSlot,
                    onAddManualTask = {
                        editingTask = null
                        prefillHour = null
                        prefillMinute = null
                        editorSheetOpen = true
                    },
                    onAddTaskAtHour = { h, m ->
                        editingTask = null
                        prefillHour = h
                        prefillMinute = m
                        editorSheetOpen = true
                    }
                )
            }

            composable(ROUTE_INSIGHTS) {
                InsightsScreen(
                    uiState = todayUiState,
                    insightState = insightState,
                    goalSummaries = goalSummaries,
                    isCloudAiConfigured = viewModel.isCloudAiConfigured,
                    onRefreshInsights = viewModel::refreshDailyInsights,
                    onSaveGoal = viewModel::saveGoal,
                    onDeleteGoal = viewModel::deleteGoal,
                    onToggleGoalToday = viewModel::toggleGoalHabitToday
                )
            }

            composable(ROUTE_SETTINGS) {
                SettingsScreen(
                    preferences = preferences,
                    alarmStatus = alarmStatus,
                    ttsStatus = ttsStatus,
                    isCloudAiConfigured = viewModel.isCloudAiConfigured,
                    backupPreview = backupPreview,
                    onSetDarkTheme = viewModel::setDarkTheme,
                    onSetUsePersianDigits = viewModel::setUsePersianDigits,
                    onToggleHaptics = viewModel::setHapticsEnabled,
                    onChangeDefaultDuration = viewModel::setDefaultBlockMinutes,
                    onSetTimezoneConfig = viewModel::setTimezoneConfig,
                    onToggleAutoListen = viewModel::setAutoListenEnabled,
                    onSetSpeechLanguageTag = viewModel::setSpeechLanguageTag,
                    onSetTtsEnabled = viewModel::setTtsEnabled,
                    onSetTtsSpeechRate = viewModel::setTtsSpeechRate,
                    onSetDefaultReminderOffset = viewModel::setDefaultReminderOffset,
                    onSetTaskRemindersEnabled = viewModel::setTaskRemindersEnabled,
                    onSetOverdueAlertsEnabled = viewModel::setOverdueAlertsEnabled,
                    onSetMorningSummary = viewModel::setMorningSummary,
                    onSetEveningSummary = viewModel::setEveningSummary,
                    onSetHabitRemindersEnabled = viewModel::setHabitRemindersEnabled,
                    onUpdateWidgetSettings = viewModel::updateWidgetSettings,
                    onExportBackupUri = { uri ->
                        viewModel.exportBackupToUri(context.contentResolver, uri)
                    },
                    onInspectBackupUri = { uri ->
                        viewModel.inspectBackupFromUri(context.contentResolver, uri)
                    },
                    onConfirmBackupRestore = viewModel::confirmRestoreBackup,
                    onDismissBackupPreview = viewModel::dismissBackupPreview,
                    onImportDemoTasks = viewModel::importDemoTasks,
                    onClearAllTasks = viewModel::clearAllUserTasks,
                    onRefreshPermissions = viewModel::refreshClockAndSyncAlarms
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 104.dp, start = 16.dp, end = 16.dp)
        )

        NocturneDockedBottomBar(
            currentRoute = currentRoute,
            isVoiceActive = voiceSheetVisible || synthesisState is AiSynthesisUiState.Listening,
            hapticsEnabled = preferences.hapticsEnabled,
            onNavigate = { route ->
                if (route != currentRoute) {
                    navController.navigate(route) {
                        popUpTo(ROUTE_TODAY) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            onVoiceFabClick = {
                viewModel.openVoiceSheet(null)
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (voiceSheetVisible) {
        VoiceAssistantSheet(
            synthesisState = synthesisState,
            speechState = speechState,
            ttsStatus = ttsStatus,
            ttsEnabled = preferences.ttsEnabled,
            isCloudAiConfigured = viewModel.isCloudAiConfigured,
            chatHistory = chatHistory,
            draftInput = voiceDraft,
            onDraftChange = viewModel::updateVoiceDraft,
            onStartListening = viewModel::startVoiceListening,
            onStopListening = viewModel::stopVoiceListening,
            onToggleTts = viewModel::setTtsEnabled,
            onSubmitCommand = viewModel::processVoiceInput,
            onConfirmDrafts = viewModel::confirmDraftTasks,
            onConfirmCompleteTask = viewModel::confirmCompleteExistingTask,
            onEditDraftManually = { draftTask ->
                viewModel.closeVoiceSheet()
                editingTask = draftTask
                prefillHour = draftTask.timeHour
                prefillMinute = draftTask.timeMinute
                editorSheetOpen = true
            },
            onDismiss = viewModel::closeVoiceSheet
        )
    }

    if (editorSheetOpen) {
        TaskEditorSheet(
            initialTask = editingTask,
            selectedDateKey = todayUiState.selectedDate.dateKey,
            defaultDurationMinutes = preferences.defaultBlockMinutes,
            defaultReminderOffsetMinutes = preferences.defaultReminderOffsetMinutes,
            allExistingTasks = todayUiState.allTasks,
            prefillHour = prefillHour,
            prefillMinute = prefillMinute,
            onSave = { saved ->
                viewModel.saveTask(saved)
                editorSheetOpen = false
                editingTask = null
                prefillHour = null
                prefillMinute = null
            },
            onSaveRecurring = { original, updated, occKey, scope ->
                viewModel.saveRecurringEdit(original, updated, occKey, scope)
                editorSheetOpen = false
                editingTask = null
                prefillHour = null
                prefillMinute = null
            },
            onDismiss = {
                editorSheetOpen = false
                editingTask = null
                prefillHour = null
                prefillMinute = null
            }
        )
    }
}
