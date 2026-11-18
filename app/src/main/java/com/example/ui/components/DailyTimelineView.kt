package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.ScheduleConflict
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.ui.theme.LocalUsePersianDigits
import com.example.ui.theme.SignalCoralCrimson
import com.example.ui.theme.SoftEmeraldJade
import com.example.ui.theme.TabularTimeStyle
import com.example.ui.theme.WarmSolarAmber
import com.example.util.PersianCalendarUtil

@Composable
fun DailyTimelineView(
    tasks: List<TaskItem>,
    conflicts: List<ScheduleConflict>,
    isToday: Boolean,
    currentTimeMinutes: Int,
    onToggleComplete: (TaskItem) -> Unit,
    onTaskClick: (TaskItem) -> Unit,
    onEmptyHourClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val usePersianDigits = LocalUsePersianDigits.current
    val hourHeight: Dp = 64.dp
    val startHour = 6
    val endHour = 23
    val totalHours = endHour - startHour + 1

    val scheduledTasks = tasks.filter { it.isScheduled }
    val unscheduledTasks = tasks.filter { !it.isScheduled }
    val conflictingIds = conflicts.flatMap { listOf(it.firstTask.id, it.secondTask.id) }.toSet()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (unscheduledTasks.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "کارهای بدون ساعت ثابت (${
                            PersianCalendarUtil.formatNumber(unscheduledTasks.size, usePersianDigits)
                        })",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                unscheduledTasks.forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onTaskClick(task) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تعیین ساعت",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(hourHeight * totalHours)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                for (hour in startHour..endHour) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(hourHeight)
                            .clickable { onEmptyHourClick(hour) },
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = PersianCalendarUtil.formatTime(hour, 0, usePersianDigits),
                            style = TabularTimeStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier
                                .width(48.dp)
                                .padding(top = 2.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            )
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "افزودن کار در ساعت $hour",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(top = 8.dp, end = 6.dp)
                                    .size(14.dp)
                            )
                        }
                    }
                }
            }

            if (isToday) {
                val clampedMins = currentTimeMinutes.coerceIn(startHour * 60, (endHour + 1) * 60)
                val offsetDp = ((clampedMins - startHour * 60) / 60f) * hourHeight.value
                val nowHour = currentTimeMinutes / 60
                val nowMin = currentTimeMinutes % 60
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = offsetDp.dp + 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .padding(end = 4.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = PersianCalendarUtil.formatTime(nowHour, nowMin, usePersianDigits),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                }
            }

            // Scheduled Task Blocks
            scheduledTasks.forEachIndexed { idx, task ->
                val hour = task.timeHour ?: return@forEachIndexed
                val minute = task.timeMinute ?: 0
                val startTotalMinutes = hour * 60 + minute
                val clampedStart = startTotalMinutes.coerceIn(startHour * 60, endHour * 60 + 45)
                val topOffsetDp = ((clampedStart - startHour * 60) / 60f) * hourHeight.value
                val blockHeightDp = ((task.durationMinutes.coerceIn(25, 240)) / 60f) * hourHeight.value
                val hasConflict = task.id in conflictingIds
                val staggerStartPadding = if (hasConflict && idx % 2 == 1) 76.dp else 56.dp

                val priorityColor = when (task.priority) {
                    TaskPriority.HIGH -> SignalCoralCrimson
                    TaskPriority.MEDIUM -> WarmSolarAmber
                    TaskPriority.LOW -> SoftEmeraldJade
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = staggerStartPadding, end = if (hasConflict && idx % 2 == 0) 20.dp else 4.dp)
                        .offset(y = topOffsetDp.dp + 8.dp)
                        .height(blockHeightDp.dp.coerceAtLeast(44.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTaskClick(task) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = when {
                            task.isCompleted -> SoftEmeraldJade.copy(alpha = 0.45f)
                            hasConflict -> WarmSolarAmber
                            else -> priorityColor.copy(alpha = 0.55f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(26.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (task.isCompleted) SoftEmeraldJade else priorityColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (task.isCompleted) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${task.formatTime(usePersianDigits)} • ${task.formatDuration(usePersianDigits)} • ${task.category}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (hasConflict && !task.isCompleted) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = "تداخل",
                                tint = WarmSolarAmber,
                                modifier = Modifier.size(16.dp).padding(end = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (task.isCompleted) SoftEmeraldJade else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (task.isCompleted) SoftEmeraldJade else priorityColor,
                                    RoundedCornerShape(7.dp)
                                )
                                .clickable { onToggleComplete(task) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (task.isCompleted) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "تکمیل",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
