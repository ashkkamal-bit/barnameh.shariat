package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.TaskItem
import com.example.domain.ConflictDetector
import com.example.domain.RecurrenceEngine
import com.example.ui.TodayUiState
import com.example.ui.components.ConflictWarningBanner
import com.example.ui.components.DailyTimelineView
import com.example.ui.components.TimelineTaskCard
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LocalUsePersianDigits
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.SoftEmeraldJade
import com.example.ui.theme.TabularTimeStyle
import com.example.ui.theme.WarmSolarAmber
import com.example.util.JalaliDate
import com.example.util.PersianCalendarUtil
import java.util.Calendar

private enum class CalendarViewMode(val labelFa: String) {
    MONTHLY("ماهانه"),
    WEEKLY("هفتگی"),
    DAILY("روزانه")
}

@Composable
fun CalendarScreen(
    uiState: TodayUiState,
    hapticsEnabled: Boolean,
    onSelectDate: (JalaliDate) -> Unit,
    onToggleTaskCompleted: (TaskItem) -> Unit,
    onToggleTaskExpanded: (Long) -> Unit,
    onExpandTask: (Long) -> Unit,
    onToggleSubtask: (TaskItem, String) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onRescheduleTask: (TaskItem, Int, Int) -> Unit,
    onAddManualTask: () -> Unit,
    onAddTaskAtHour: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val usePersianDigits = LocalUsePersianDigits.current
    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTHLY) }
    var displayYear by remember(uiState.selectedDate.year) { mutableIntStateOf(uiState.selectedDate.year) }
    var displayMonth by remember(uiState.selectedDate.month) { mutableIntStateOf(uiState.selectedDate.month) }

    val daysInMonth = PersianCalendarUtil.daysInJalaliMonth(displayYear, displayMonth)
    val firstDowOffset = PersianCalendarUtil.firstDayOfWeekInMonth(displayYear, displayMonth)
    val weekDates = remember(uiState.selectedDate) {
        PersianCalendarUtil.weekDatesStartingSaturday(uiState.selectedDate)
    }
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
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "تقویم و برنامه‌ریزی",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = uiState.selectedDate.formattedFull(usePersianDigits),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                IconButton(
                    onClick = onAddManualTask,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), CircleShape)
                        .testTag("calendar_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.cd_add_task),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Daily / Weekly / Monthly Switcher
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CalendarViewMode.entries.forEach { mode ->
                    val selected = viewMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) {
                                    Brush.linearGradient(listOf(ElectricViolet, LuminousCyan))
                                } else {
                                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                }
                            )
                            .clickable { viewMode = mode }
                            .padding(vertical = 8.dp)
                            .testTag("calendar_mode_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.labelFa,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        when (viewMode) {
            CalendarViewMode.MONTHLY -> {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    if (displayMonth == 1) {
                                        displayMonth = 12
                                        displayYear -= 1
                                    } else {
                                        displayMonth -= 1
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = stringResource(R.string.cd_prev_month),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Text(
                                    text = "${PersianCalendarUtil.monthName(displayMonth)} ${
                                        PersianCalendarUtil.formatNumber(displayYear, usePersianDigits)
                                    }",
                                    style = MaterialTheme.typography.titleLarge
                                )

                                IconButton(onClick = {
                                    if (displayMonth == 12) {
                                        displayMonth = 1
                                        displayYear += 1
                                    } else {
                                        displayMonth += 1
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = stringResource(R.string.cd_next_month),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                PersianCalendarUtil.allWeekDayShorts().forEach { shortDay ->
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = shortDay,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val totalCells = firstDowOffset + daysInMonth
                            val rows = (totalCells + 6) / 7
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (row in 0 until rows) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        for (col in 0 until 7) {
                                            val cellIndex = row * 7 + col
                                            val dayNum = cellIndex - firstDowOffset + 1
                                            if (dayNum !in 1..daysInMonth) {
                                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                            } else {
                                                val cellDate = PersianCalendarUtil.dateForJalali(displayYear, displayMonth, dayNum)
                                                val dayTasks = RecurrenceEngine.tasksForDate(uiState.allTasks, cellDate.dateKey)
                                                val isSelected = cellDate.dateKey == uiState.selectedDate.dateKey
                                                val isToday = cellDate.dateKey == uiState.todayDate.dateKey
                                                val hasConflicts = ConflictDetector.findConflicts(dayTasks).isNotEmpty()
                                                val allDone = dayTasks.isNotEmpty() && dayTasks.all { it.isCompleted }

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .then(
                                                            when {
                                                                isSelected -> Modifier.background(
                                                                    Brush.linearGradient(listOf(ElectricViolet, LuminousCyan))
                                                                )
                                                                isToday -> Modifier
                                                                    .background(MaterialTheme.colorScheme.surface)
                                                                    .border(1.5.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
                                                                else -> Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                                            }
                                                        )
                                                        .clickable { onSelectDate(cellDate) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = PersianCalendarUtil.formatNumber(dayNum, usePersianDigits),
                                                            style = TabularTimeStyle,
                                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (dayTasks.isNotEmpty()) {
                                                            Spacer(modifier = Modifier.height(3.dp))
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(5.dp)
                                                                    .clip(CircleShape)
                                                                    .background(
                                                                        when {
                                                                            isSelected -> Color.White
                                                                            hasConflicts -> WarmSolarAmber
                                                                            allDone -> SoftEmeraldJade
                                                                            else -> LuminousCyan
                                                                        }
                                                                    )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CalendarViewMode.WEEKLY -> {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "برنامه ۷ روزه و توزیع بار کاری",
                                style = MaterialTheme.typography.titleMedium
                            )
                            weekDates.forEach { d ->
                                val dayTasks = RecurrenceEngine.tasksForDate(uiState.allTasks, d.dateKey)
                                val totalMins = dayTasks.sumOf { it.durationMinutes }
                                val doneCount = dayTasks.count { it.isCompleted }
                                val hasConflict = ConflictDetector.findConflicts(dayTasks).isNotEmpty()
                                val isSel = d.dateKey == uiState.selectedDate.dateKey

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            1.dp,
                                            if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onSelectDate(d) }
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${d.weekDayName} ${d.formattedDayAndMonth(usePersianDigits)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = if (dayTasks.isEmpty()) {
                                                "آزاد"
                                            } else {
                                                "${
                                                    PersianCalendarUtil.formatNumber(doneCount, usePersianDigits)
                                                }/${
                                                    PersianCalendarUtil.formatNumber(dayTasks.size, usePersianDigits)
                                                } کار • ${
                                                    PersianCalendarUtil.formatDurationMinutes(totalMins, usePersianDigits)
                                                }${if (hasConflict) " • تداخل" else ""}"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (hasConflict) WarmSolarAmber else MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (totalMins / 480f).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)),
                                        color = if (hasConflict) WarmSolarAmber else MaterialTheme.colorScheme.secondary,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                }
                            }
                        }
                    }
                }
            }

            CalendarViewMode.DAILY -> {
                item {
                    DailyTimelineView(
                        tasks = uiState.tasksForSelectedDate,
                        conflicts = uiState.conflictsForSelectedDate,
                        isToday = uiState.selectedDate.dateKey == uiState.todayDate.dateKey,
                        currentTimeMinutes = uiState.currentTimeMinutes,
                        onToggleComplete = onToggleTaskCompleted,
                        onTaskClick = { task ->
                            onExpandTask(task.id)
                            onEditTask(task)
                        },
                        onEmptyHourClick = { hour -> onAddTaskAtHour(hour, 0) }
                    )
                }
            }
        }

        if (uiState.conflictsForSelectedDate.isNotEmpty() && viewMode != CalendarViewMode.DAILY) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ConflictWarningBanner(
                        conflict = uiState.conflictsForSelectedDate.first(),
                        onMoveSecondToSlot = onRescheduleTask,
                        onEditSecond = onEditTask
                    )
                }
            }
        }

        // Day Details below Monthly / Weekly views
        if (viewMode != CalendarViewMode.DAILY) {
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "برنامه ${uiState.selectedDate.weekDayName} ${uiState.selectedDate.formattedDayAndMonth(usePersianDigits)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${PersianCalendarUtil.formatNumber(uiState.tasksForSelectedDate.size, usePersianDigits)} کار",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (uiState.tasksForSelectedDate.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "برای این روز هنوز برنامه‌ای ثبت نشده است.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val activeId = uiState.activeTaskId
                itemsIndexed(
                    items = uiState.tasksForSelectedDate,
                    key = { idx, t -> "cal_${t.id}_${t.effectiveDateKey}_$idx" }
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
}
