package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.notifications.AlarmScheduleReport
import com.example.ui.TodayUiState
import com.example.ui.components.ConflictWarningBanner
import com.example.ui.components.DailyTimelineView
import com.example.ui.components.JalaliDateStrip
import com.example.ui.components.NocturneSearchBar
import com.example.ui.components.TimelineTaskCard
import com.example.ui.components.VoiceQuickActionChip
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LocalUsePersianDigits
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.SignalCoralCrimson
import com.example.ui.theme.SoftEmeraldJade
import com.example.ui.theme.TabularTimeStyle
import com.example.ui.theme.WarmSolarAmber
import com.example.util.JalaliDate
import com.example.util.PersianCalendarUtil
import java.util.Calendar

@Composable
fun TodayScreen(
    uiState: TodayUiState,
    alarmStatus: AlarmScheduleReport,
    hapticsEnabled: Boolean,
    onSelectDate: (JalaliDate) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectPriorityFilter: (TaskPriority?) -> Unit,
    onToggleTaskCompleted: (TaskItem) -> Unit,
    onToggleTaskExpanded: (Long) -> Unit,
    onExpandTask: (Long) -> Unit,
    onToggleSubtask: (TaskItem, String) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onRescheduleTask: (TaskItem, Int, Int) -> Unit,
    onAddManualTask: () -> Unit,
    onAddTaskAtHour: (Int, Int) -> Unit,
    onQuickVoiceCommand: (String?) -> Unit,
    onRefreshPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val usePersianDigits = LocalUsePersianDigits.current
    var showTimelineMode by remember { mutableStateOf(false) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshPermissions()
    }

    val isToday = uiState.selectedDate.dateKey == uiState.todayDate.dateKey
    val conflictingIds = remember(uiState.conflictsForSelectedDate) {
        uiState.conflictsForSelectedDate.flatMap { listOf(it.firstTask.id, it.secondTask.id) }.toSet()
    }
    val nowCal = remember(uiState.currentTimeMinutes) { Calendar.getInstance() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 128.dp)
    ) {
        // 1. Header with live Jalali date & progress ring
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "۲.۰",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.selectedDate.formattedFull(usePersianDigits),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (isToday) {
                        val nowH = uiState.currentTimeMinutes / 60
                        val nowM = uiState.currentTimeMinutes % 60
                        Text(
                            text = "ساعت اکنون: ${PersianCalendarUtil.formatTime(nowH, nowM, usePersianDigits)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val progress = if (uiState.totalCount == 0) 0f else uiState.completedCount.toFloat() / uiState.totalCount
                    Box(
                        modifier = Modifier.size(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(46.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = "${
                                PersianCalendarUtil.formatNumber(uiState.completedCount, usePersianDigits)
                            }/${
                                PersianCalendarUtil.formatNumber(uiState.totalCount, usePersianDigits)
                            }",
                            style = TabularTimeStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onAddManualTask,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                CircleShape
                            )
                            .testTag("add_task_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.cd_add_task),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Notification or Exact Alarm Permission Warning (Android 13+ / 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !alarmStatus.postNotificationPermitted) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = WarmSolarAmber.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, WarmSolarAmber.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = WarmSolarAmber)
                            Text(
                                text = "برای دریافت یادآوری کارها، مجوز اعلان سیستم را فعال کنید.",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        TextButton(onClick = {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }) {
                            Text("فعال‌سازی", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmStatus.exactAlarmPermitted && alarmStatus.scheduledTaskAlarms > 0) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "دسترسی هشدار دقیق غیرفعال است؛ یادآورها ممکن است با تاخیر ارسال شوند.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching { context.startActivity(intent) }
                        }) {
                            Text("تنظیم", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }

        // 2. Search Bar
        item {
            NocturneSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                onVoiceTriggerClick = { onQuickVoiceCommand(null) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 3. Jalali Date Strip
        item {
            JalaliDateStrip(
                dates = uiState.weekDates,
                selectedDate = uiState.selectedDate,
                todayDate = uiState.todayDate,
                onSelectDate = onSelectDate
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 4. Quick Voice Command Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoiceQuickActionChip(
                    label = "برنامه فردا چیه؟",
                    icon = Icons.Filled.AutoAwesome,
                    hapticsEnabled = hapticsEnabled,
                    onClick = { onQuickVoiceCommand("فردا چه کارهایی دارم؟") }
                )
                VoiceQuickActionChip(
                    label = "ثبت جلسه فردا ۹ صبح",
                    icon = Icons.Filled.Mic,
                    hapticsEnabled = hapticsEnabled,
                    onClick = { onQuickVoiceCommand("فردا ساعت ۹ صبح جلسه تیم") }
                )
                VoiceQuickActionChip(
                    label = "کار بعدی من چیه؟",
                    icon = Icons.Filled.Schedule,
                    hapticsEnabled = hapticsEnabled,
                    onClick = { onQuickVoiceCommand("کار بعدی من چیه؟") }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 5. Overdue Banner
        if (uiState.overdueTasks.isNotEmpty() && isToday) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onEditTask(uiState.overdueTasks.first()) },
                    shape = RoundedCornerShape(14.dp),
                    color = SignalCoralCrimson.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, SignalCoralCrimson.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = SignalCoralCrimson)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${
                                    PersianCalendarUtil.formatNumber(uiState.overdueTasks.size, usePersianDigits)
                                } کار عقب‌افتاده",
                                style = MaterialTheme.typography.labelLarge,
                                color = SignalCoralCrimson
                            )
                            Text(
                                text = "اولین مورد: ${uiState.overdueTasks.first().title} (برای زمان‌بندی مجدد لمس کنید)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // 6. Conflicts Banner for Selected Date
        if (uiState.conflictsForSelectedDate.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConflictWarningBanner(
                        conflict = uiState.conflictsForSelectedDate.first(),
                        onMoveSecondToSlot = onRescheduleTask,
                        onEditSecond = onEditTask
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // 7. Priority Filters & List/Timeline Mode Switch
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriorityFilterChip(
                        label = "همه",
                        selected = uiState.selectedPriorityFilter == null,
                        accent = MaterialTheme.colorScheme.secondary,
                        onClick = { onSelectPriorityFilter(null) }
                    )
                    PriorityFilterChip(
                        label = "فوری",
                        selected = uiState.selectedPriorityFilter == TaskPriority.HIGH,
                        accent = SignalCoralCrimson,
                        onClick = { onSelectPriorityFilter(TaskPriority.HIGH) }
                    )
                    PriorityFilterChip(
                        label = "متوسط",
                        selected = uiState.selectedPriorityFilter == TaskPriority.MEDIUM,
                        accent = WarmSolarAmber,
                        onClick = { onSelectPriorityFilter(TaskPriority.MEDIUM) }
                    )
                    PriorityFilterChip(
                        label = "عادی",
                        selected = uiState.selectedPriorityFilter == TaskPriority.LOW,
                        accent = SoftEmeraldJade,
                        onClick = { onSelectPriorityFilter(TaskPriority.LOW) }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ViewModeIconButton(
                        selected = !showTimelineMode,
                        icon = Icons.AutoMirrored.Filled.ViewList,
                        cd = "نمای فهرست",
                        onClick = { showTimelineMode = false }
                    )
                    ViewModeIconButton(
                        selected = showTimelineMode,
                        icon = Icons.Filled.ViewTimeline,
                        cd = "نمای خط زمانی",
                        onClick = { showTimelineMode = true }
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 8. Main Content: Empty / Timeline / Task Cards
        if (uiState.tasksForSelectedDate.isEmpty() && !showTimelineMode) {
            item {
                EmptyDayCard(
                    date = uiState.selectedDate,
                    onAddManualTask = onAddManualTask,
                    onVoiceClick = { onQuickVoiceCommand(null) }
                )
            }
        } else if (showTimelineMode) {
            item {
                DailyTimelineView(
                    tasks = uiState.tasksForSelectedDate,
                    conflicts = uiState.conflictsForSelectedDate,
                    isToday = isToday,
                    currentTimeMinutes = uiState.currentTimeMinutes,
                    onToggleComplete = onToggleTaskCompleted,
                    onTaskClick = { task ->
                        onExpandTask(task.id)
                        onEditTask(task)
                    },
                    onEmptyHourClick = { hour -> onAddTaskAtHour(hour, 0) }
                )
            }
        } else {
            val activeId = uiState.activeTaskId
            itemsIndexed(
                items = uiState.tasksForSelectedDate,
                key = { idx, t -> "${t.id}_${t.effectiveDateKey}_$idx" }
            ) { index, task ->
                val overdue = !task.isCompleted && PersianCalendarUtil.isOverdue(
                    task.effectiveDateKey,
                    task.timeHour,
                    task.timeMinute,
                    nowCal
                )
                TimelineTaskCard(
                    task = task,
                    isActiveNode = task.id == activeId,
                    isExpanded = task.id == uiState.expandedTaskId,
                    isLastItem = index == uiState.tasksForSelectedDate.lastIndex,
                    hapticsEnabled = hapticsEnabled,
                    hasConflict = task.id in conflictingIds,
                    isOverdue = overdue,
                    onToggleComplete = { onToggleTaskCompleted(task) },
                    onToggleExpand = { onToggleTaskExpanded(task.id) },
                    onToggleSubtask = { subId -> onToggleSubtask(task, subId) },
                    onEdit = { onEditTask(task) },
                    onDelete = { onDeleteTask(task.id) }
                )
            }
        }
    }
}

@Composable
private fun ViewModeIconButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cd: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = cd,
            tint = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun PriorityFilterChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                1.dp,
                if (selected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyDayCard(
    date: JalaliDate,
    onAddManualTask: () -> Unit,
    onVoiceClick: () -> Unit
) {
    val usePersianDigits = LocalUsePersianDigits.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(ElectricViolet.copy(alpha = 0.22f), LuminousCyan.copy(alpha = 0.22f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = "برای ${date.formattedDayAndMonth(usePersianDigits)} کاری ثبت نشده است",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "فهرست شما خالی است؛ می‌توانید با دستیار صوتی یا به صورت دستی برنامه خود را بسازید.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onVoiceClick) {
                    Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فرمان صوتی")
                }
                OutlinedButton(onClick = onAddManualTask) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("افزودن کار")
                }
            }
        }
    }
}
