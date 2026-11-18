package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.RecurrenceType
import com.example.data.model.ScheduleConflict
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.IndigoGlow
import com.example.ui.theme.LocalUsePersianDigits
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.SignalCoralCrimson
import com.example.ui.theme.SoftEmeraldJade
import com.example.ui.theme.TabularTimeStyle
import com.example.ui.theme.WarmSolarAmber
import com.example.util.JalaliDate
import com.example.util.PersianCalendarUtil

@Composable
fun NocturneSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onVoiceTriggerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val secondary = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) secondary else outline,
        animationSpec = tween(180),
        label = "searchBorder"
    )
    val bgColor = MaterialTheme.colorScheme.surfaceContainerLow

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = stringResource(R.string.cd_search_tasks),
            tint = if (isFocused) secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text(
                    text = "جستجو در برنامه‌ها، یادداشت‌ها یا برچسب...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
                    .testTag("search_input")
            )
        }

        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("clear_search_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_clear_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        IconButton(
            onClick = onVoiceTriggerClick,
            modifier = Modifier
                .size(40.dp)
                .testTag("search_mic_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = stringResource(R.string.cd_voice_fab),
                tint = secondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun JalaliDateStrip(
    dates: List<JalaliDate>,
    selectedDate: JalaliDate,
    todayDate: JalaliDate,
    onSelectDate: (JalaliDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val usePersianDigits = LocalUsePersianDigits.current
    val listState = rememberLazyListState()
    LaunchedEffect(selectedDate.dateKey, dates) {
        val index = dates.indexOfFirst { it.dateKey == selectedDate.dateKey }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(dates, key = { it.dateKey }) { date ->
            val isSelected = date.dateKey == selectedDate.dateKey
            val isToday = date.dateKey == todayDate.dateKey
            val interactionSource = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .then(
                        if (isSelected) {
                            Modifier
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(999.dp),
                                    ambientColor = LuminousCyan,
                                    spotColor = ElectricViolet
                                )
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(ElectricViolet, LuminousCyan)
                                    )
                                )
                        } else {
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(999.dp)
                                )
                        }
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true, color = Color.White),
                        onClick = { onSelectDate(date) }
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("jalali_day_chip_${date.dateKey}"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isToday) "امروز" else date.weekDayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = PersianCalendarUtil.formatNumber(date.day, usePersianDigits),
                        style = TabularTimeStyle,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceQuickActionChip(
    label: String,
    icon: ImageVector,
    hapticsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val primary = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, primary.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = primary),
                onClick = {
                    if (hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onClick()
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
fun TimelineTaskCard(
    task: TaskItem,
    isActiveNode: Boolean,
    isExpanded: Boolean,
    isLastItem: Boolean,
    hapticsEnabled: Boolean,
    hasConflict: Boolean = false,
    isOverdue: Boolean = false,
    onToggleComplete: () -> Unit,
    onToggleExpand: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usePersianDigits = LocalUsePersianDigits.current
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "nodePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> SignalCoralCrimson
        TaskPriority.MEDIUM -> WarmSolarAmber
        TaskPriority.LOW -> SoftEmeraldJade
    }

    val cardBackground = if (isActiveNode) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier.size(18.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isActiveNode && !task.isCompleted) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = ElectricViolet.copy(alpha = pulseAlpha),
                            radius = size.minDimension / 1.6f
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                task.isCompleted -> SoftEmeraldJade
                                hasConflict -> WarmSolarAmber
                                isActiveNode -> ElectricViolet
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                        .border(
                            width = 1.5.dp,
                            color = when {
                                task.isCompleted -> SoftEmeraldJade
                                hasConflict -> WarmSolarAmber
                                isActiveNode -> LuminousCyan
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                            shape = CircleShape
                        )
                )
            }
            if (!isLastItem) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .weight(1f)
                        .padding(top = 4.dp)
                        .background(
                            if (task.isCompleted) {
                                SoftEmeraldJade.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        val cardInteraction = remember { MutableInteractionSource() }
        Surface(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
                .then(
                    if (isActiveNode && !task.isCompleted) {
                        Modifier.shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = IndigoGlow.copy(alpha = 0.45f),
                            spotColor = IndigoGlow.copy(alpha = 0.45f)
                        )
                    } else {
                        Modifier
                    }
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = cardInteraction,
                    indication = ripple(bounded = true, color = ElectricViolet),
                    onClick = onToggleExpand
                )
                .testTag("task_card_${task.id}"),
            shape = RoundedCornerShape(16.dp),
            color = cardBackground,
            border = BorderStroke(
                width = 1.dp,
                color = when {
                    hasConflict && !task.isCompleted -> WarmSolarAmber.copy(alpha = 0.6f)
                    isOverdue && !task.isCompleted -> SignalCoralCrimson.copy(alpha = 0.55f)
                    isActiveNode && !task.isCompleted -> ElectricViolet.copy(alpha = 0.45f)
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                }
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onToggleComplete()
                            }
                            .testTag("task_checkbox_${task.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (task.isCompleted) SoftEmeraldJade else Color.Transparent
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (task.isCompleted) SoftEmeraldJade else priorityColor,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (task.isCompleted) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.cd_toggle_complete),
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (task.isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = if (isExpanded) 4 else 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = task.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (task.recurrenceType != RecurrenceType.NONE) {
                                Icon(
                                    imageVector = Icons.Filled.Repeat,
                                    contentDescription = task.recurrenceType.labelFa,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            if (task.remindersEnabled && task.isScheduled && task.reminderOffsetsMinutes.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = "یادآوری فعال",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            if (task.isPrivate) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "کار خصوصی",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            if (task.isAiGenerated) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = "پیشنهاد هوش مصنوعی",
                                    tint = ElectricViolet,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            if (task.subtasks.isNotEmpty()) {
                                Text(
                                    text = "${
                                        PersianCalendarUtil.formatNumber(task.completedSubtasksCount, usePersianDigits)
                                    }/${
                                        PersianCalendarUtil.formatNumber(task.subtasks.size, usePersianDigits)
                                    } زیرکار",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        if (hasConflict || isOverdue) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = if (hasConflict) WarmSolarAmber else SignalCoralCrimson,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (hasConflict) "تداخل زمانی" else "عقب‌افتاده",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasConflict) WarmSolarAmber else SignalCoralCrimson
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                                    RoundedCornerShape(999.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = task.formatTime(usePersianDigits),
                                style = TabularTimeStyle,
                                color = if (task.isScheduled) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = task.priority.labelFa,
                                style = MaterialTheme.typography.labelSmall,
                                color = priorityColor
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(priorityColor)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (task.aiNotes.isNotBlank()) {
                            Text(
                                text = task.aiNotes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (task.subtasks.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                task.subtasks.forEach { sub ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onToggleSubtask(sub.id) }
                                            .padding(vertical = 4.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(
                                                    if (sub.isDone) SoftEmeraldJade else Color.Transparent
                                                )
                                                .border(
                                                    1.dp,
                                                    if (sub.isDone) SoftEmeraldJade else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    RoundedCornerShape(5.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (sub.isDone) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = sub.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                textDecoration = if (sub.isDone) TextDecoration.LineThrough else TextDecoration.None
                                            ),
                                            color = if (sub.isDone) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "مدت: ${task.formatDuration(usePersianDigits)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = onEdit,
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "ویرایش کار",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = onDelete,
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "حذف کار",
                                        tint = SignalCoralCrimson,
                                        modifier = Modifier.size(18.dp)
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

@Composable
fun FirebaseRequiredNoticeBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ConflictWarningBanner(
    conflict: ScheduleConflict,
    onMoveSecondToSlot: (TaskItem, Int, Int) -> Unit,
    onEditSecond: (TaskItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val usePersianDigits = LocalUsePersianDigits.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WarmSolarAmber.copy(alpha = 0.12f))
            .border(1.dp, WarmSolarAmber.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = WarmSolarAmber,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "تداخل زمانی (${
                    PersianCalendarUtil.formatNumber(conflict.overlapMinutes, usePersianDigits)
                } دقیقه هم‌پوشانی)",
                style = MaterialTheme.typography.labelLarge,
                color = WarmSolarAmber
            )
        }
        Text(
            text = "«${conflict.firstTask.title}» (${conflict.firstTask.formatTime(usePersianDigits)}) با «${conflict.secondTask.title}» (${conflict.secondTask.formatTime(usePersianDigits)}) هم‌پوشانی دارد.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (conflict.suggestedFreeSlotHour != null && conflict.suggestedFreeSlotMinute != null) {
                val slotText = PersianCalendarUtil.formatTime(
                    conflict.suggestedFreeSlotHour,
                    conflict.suggestedFreeSlotMinute,
                    usePersianDigits
                )
                TextButton(onClick = {
                    onMoveSecondToSlot(
                        conflict.secondTask,
                        conflict.suggestedFreeSlotHour,
                        conflict.suggestedFreeSlotMinute
                    )
                }) {
                    Text("انتقال دومی به $slotText", color = MaterialTheme.colorScheme.secondary)
                }
            }
            TextButton(onClick = { onEditSecond(conflict.secondTask) }) {
                Text("ویرایش دستی", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
