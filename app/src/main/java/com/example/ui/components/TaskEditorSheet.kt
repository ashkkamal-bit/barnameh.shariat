package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.RecurrenceEditScope
import com.example.data.model.RecurrenceType
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.domain.ConflictDetector
import com.example.domain.RecurrenceEngine
import com.example.ui.theme.LocalUsePersianDigits
import com.example.ui.theme.SignalCoralCrimson
import com.example.ui.theme.SoftEmeraldJade
import com.example.ui.theme.TabularTimeStyle
import com.example.ui.theme.WarmSolarAmber
import com.example.util.PersianCalendarUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    initialTask: TaskItem?,
    selectedDateKey: String,
    defaultDurationMinutes: Int,
    defaultReminderOffsetMinutes: Int,
    allExistingTasks: List<TaskItem>,
    prefillHour: Int? = null,
    prefillMinute: Int? = null,
    onSave: (TaskItem) -> Unit,
    onSaveRecurring: (original: TaskItem, updated: TaskItem, occurrenceDateKey: String, scope: RecurrenceEditScope) -> Unit,
    onDismiss: () -> Unit
) {
    val usePersianDigits = LocalUsePersianDigits.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var notes by remember { mutableStateOf(initialTask?.aiNotes ?: "") }
    var dateKey by remember { mutableStateOf(initialTask?.effectiveDateKey ?: selectedDateKey) }

    val initialScheduled = when {
        initialTask != null -> initialTask.isScheduled
        prefillHour != null -> true
        else -> false
    }
    var hasScheduledTime by remember { mutableStateOf(initialScheduled) }
    var hour by remember { mutableIntStateOf(initialTask?.timeHour ?: prefillHour ?: 9) }
    var minute by remember { mutableIntStateOf(initialTask?.timeMinute ?: prefillMinute ?: 0) }
    var duration by remember { mutableIntStateOf(initialTask?.durationMinutes ?: defaultDurationMinutes) }
    var priority by remember { mutableStateOf(initialTask?.priority ?: TaskPriority.LOW) }
    var category by remember { mutableStateOf(initialTask?.category ?: "برنامه‌ریزی") }

    var remindersEnabled by remember { mutableStateOf(initialTask?.remindersEnabled ?: true) }
    var selectedOffsets by remember {
        mutableStateOf(
            (initialTask?.reminderOffsetsMinutes ?: listOf(defaultReminderOffsetMinutes, 0))
                .distinct()
                .toSet()
        )
    }
    var customOffsetText by remember { mutableStateOf("") }
    var recurrence by remember { mutableStateOf(initialTask?.recurrenceType ?: RecurrenceType.NONE) }
    var isPrivate by remember { mutableStateOf(initialTask?.isPrivate ?: false) }

    var titleError by remember { mutableStateOf(false) }
    var pendingSaveTask by remember { mutableStateOf<TaskItem?>(null) }
    var showConflictDialog by remember { mutableStateOf(false) }
    var showRecurringScopeDialog by remember { mutableStateOf(false) }

    val categories = listOf("برنامه‌ریزی", "جلسه", "تمرکز عمیق", "کار", "شخصی", "سلامتی", "یادگیری")
    val quickDates = listOf(
        "امروز" to PersianCalendarUtil.today(),
        "فردا" to PersianCalendarUtil.offsetFromToday(1),
        "پس‌فردا" to PersianCalendarUtil.offsetFromToday(2)
    )
    val presetOffsets = listOf(
        0 to "در زمان شروع",
        5 to "۵ دقیقه قبل",
        10 to "۱۰ دقیقه قبل",
        15 to "۱۵ دقیقه قبل",
        30 to "۳۰ دقیقه قبل"
    )

    val candidatePreview = TaskItem(
        id = initialTask?.id ?: 0L,
        title = title.trim().ifEmpty { "پیش‌نویس" },
        aiNotes = notes.trim(),
        subtasks = initialTask?.subtasks ?: emptyList(),
        jalaliDateKey = dateKey,
        timeHour = if (hasScheduledTime) hour else null,
        timeMinute = if (hasScheduledTime) minute else null,
        durationMinutes = duration,
        isDurationUserSpecified = true,
        priority = priority,
        category = category,
        isCompleted = initialTask?.isCompleted ?: false,
        isAiGenerated = initialTask?.isAiGenerated ?: false,
        remindersEnabled = remindersEnabled && hasScheduledTime,
        reminderOffsetsMinutes = selectedOffsets.sortedDescending(),
        recurrenceType = recurrence,
        recurrenceInterval = initialTask?.recurrenceInterval ?: 1,
        recurrenceEndDateKey = initialTask?.recurrenceEndDateKey,
        recurrenceExceptions = initialTask?.recurrenceExceptions ?: emptySet(),
        seriesParentId = initialTask?.seriesParentId,
        isPrivate = isPrivate,
        createdAtMillis = initialTask?.createdAtMillis ?: System.currentTimeMillis()
    )

    val dayTasks = remember(allExistingTasks, dateKey) {
        RecurrenceEngine.tasksForDate(allExistingTasks, dateKey)
    }
    val liveConflicts = remember(candidatePreview, dayTasks) {
        ConflictDetector.findConflictsForCandidate(candidatePreview, dayTasks)
    }

    fun finalizeSave(taskToCommit: TaskItem) {
        if (initialTask != null && initialTask.recurrenceType != RecurrenceType.NONE) {
            pendingSaveTask = taskToCommit
            showRecurringScopeDialog = true
        } else {
            onSave(taskToCommit)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialTask == null) "کار جدید" else "ویرایش فعالیت",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close_sheet))
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = false
                },
                label = { Text("عنوان فعالیت") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("لطفاً عنوان کار را وارد کنید.") }
                } else null,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("editor_title_input")
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("توضیحات یا یادداشت (اختیاری)") },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Quick Date Selector
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickDates.forEach { (label, jDate) ->
                    val selected = dateKey == jDate.dateKey
                    ChoiceChipPill(
                        label = "$label (${jDate.formattedDayAndMonth(usePersianDigits)})",
                        selected = selected,
                        onClick = { dateKey = jDate.dateKey }
                    )
                }
            }

            // Scheduled vs Unscheduled Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("تعیین ساعت شروع مشخص", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (hasScheduledTime) {
                            "در تایم‌لاین و با قابلیت یادآوری نمایش داده می‌شود"
                        } else {
                            "بدون ساعت ثابت (در لیست آزاد روز)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = hasScheduledTime,
                    onCheckedChange = { hasScheduledTime = it },
                    modifier = Modifier.testTag("editor_has_time_switch")
                )
            }

            if (hasScheduledTime) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StepperFieldCard(
                        label = "ساعت",
                        valueText = PersianCalendarUtil.formatTime(hour, minute, usePersianDigits),
                        onMinus = {
                            val total = (hour * 60 + minute - 15 + 1440) % 1440
                            hour = total / 60
                            minute = total % 60
                        },
                        onPlus = {
                            val total = (hour * 60 + minute + 15) % 1440
                            hour = total / 60
                            minute = total % 60
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StepperFieldCard(
                        label = "مدت",
                        valueText = PersianCalendarUtil.formatDurationMinutes(duration, usePersianDigits),
                        onMinus = { duration = (duration - 15).coerceAtLeast(15) },
                        onPlus = { duration = (duration + 15).coerceAtMost(360) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Live conflict indicator inside editor
                if (liveConflicts.isNotEmpty()) {
                    val firstC = liveConflicts.first()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarmSolarAmber.copy(alpha = 0.12f))
                            .border(1.dp, WarmSolarAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = WarmSolarAmber, modifier = Modifier.size(16.dp))
                            Text(
                                text = "تداخل با «${firstC.firstTask.title}» (${firstC.firstTask.formatTime(usePersianDigits)})",
                                style = MaterialTheme.typography.labelMedium,
                                color = WarmSolarAmber
                            )
                        }
                        if (firstC.suggestedFreeSlotHour != null && firstC.suggestedFreeSlotMinute != null) {
                            val slotStr = PersianCalendarUtil.formatTime(
                                firstC.suggestedFreeSlotHour,
                                firstC.suggestedFreeSlotMinute,
                                usePersianDigits
                            )
                            TextButton(onClick = {
                                hour = firstC.suggestedFreeSlotHour
                                minute = firstC.suggestedFreeSlotMinute
                            }) {
                                Text("انتخاب وقت آزاد پیشنهادی ($slotStr)", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }

                // Per-task reminder toggle & multiple offsets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("یادآوری برای این کار", style = MaterialTheme.typography.titleMedium)
                    Switch(checked = remindersEnabled, onCheckedChange = { remindersEnabled = it })
                }

                if (remindersEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetOffsets.forEach { (off, label) ->
                            val active = off in selectedOffsets
                            ChoiceChipPill(
                                label = label,
                                selected = active,
                                onClick = {
                                    selectedOffsets = if (active) selectedOffsets - off else selectedOffsets + off
                                }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customOffsetText,
                            onValueChange = { txt -> customOffsetText = txt.filter { it.isDigit() }.take(3) },
                            label = { Text("دقیقه دلخواه قبل از شروع") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = {
                            val v = customOffsetText.toIntOrNull()
                            if (v != null && v in 0..240) {
                                selectedOffsets = selectedOffsets + v
                                customOffsetText = ""
                            }
                        }) {
                            Text("افزودن")
                        }
                    }
                }
            } else {
                StepperFieldCard(
                    label = "مدت تخمینی",
                    valueText = PersianCalendarUtil.formatDurationMinutes(duration, usePersianDigits),
                    onMinus = { duration = (duration - 15).coerceAtLeast(15) },
                    onPlus = { duration = (duration + 15).coerceAtMost(360) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Priority
            Text("اولویت", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskPriority.entries.forEach { p ->
                    val selected = priority == p
                    val pColor = when (p) {
                        TaskPriority.HIGH -> SignalCoralCrimson
                        TaskPriority.MEDIUM -> WarmSolarAmber
                        TaskPriority.LOW -> SoftEmeraldJade
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) pColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(
                                1.dp,
                                if (selected) pColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { priority = p }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = p.labelFa,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) pColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Category
            Text("دسته‌بندی", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    ChoiceChipPill(label = cat, selected = category == cat, onClick = { category = cat })
                }
            }

            // Recurrence
            Text("تکرار فعالیت", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecurrenceType.entries.forEach { rec ->
                    ChoiceChipPill(label = rec.labelFa, selected = recurrence == rec, onClick = { recurrence = rec })
                }
            }

            // Privacy toggle for home screen widget
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("مخفی‌سازی عنوان در ویجت صفحه اصلی (حریم خصوصی)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
            }

            Button(
                onClick = {
                    val trimmed = title.trim()
                    if (trimmed.isEmpty()) {
                        titleError = true
                        return@Button
                    }
                    val finalTask = candidatePreview.copy(title = trimmed)
                    if (liveConflicts.isNotEmpty()) {
                        pendingSaveTask = finalTask
                        showConflictDialog = true
                    } else {
                        finalizeSave(finalTask)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_task_button")
            ) {
                Text(
                    text = if (initialTask == null) "افزودن به برنامه" else "ذخیره تغییرات",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showConflictDialog && pendingSaveTask != null) {
        val c = liveConflicts.firstOrNull()
        AlertDialog(
            onDismissRequest = { showConflictDialog = false },
            title = { Text("تداخل زمانی شناسایی شد") },
            text = {
                Text(
                    if (c != null) {
                        "این کار با «${c.firstTask.title}» (${c.firstTask.formatTime(usePersianDigits)}) تداخل دارد. چه تصمیمی می‌گیرید؟"
                    } else {
                        "تداخل زمانی در این ساعت وجود دارد."
                    }
                )
            },
            confirmButton = {
                Button(onClick = {
                    showConflictDialog = false
                    pendingSaveTask?.let { finalizeSave(it) }
                }) {
                    Text("ذخیره با همین زمان")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (c?.suggestedFreeSlotHour != null && c.suggestedFreeSlotMinute != null) {
                        val slotTxt = PersianCalendarUtil.formatTime(c.suggestedFreeSlotHour, c.suggestedFreeSlotMinute, usePersianDigits)
                        TextButton(onClick = {
                            showConflictDialog = false
                            hour = c.suggestedFreeSlotHour
                            minute = c.suggestedFreeSlotMinute
                            pendingSaveTask?.copy(timeHour = c.suggestedFreeSlotHour, timeMinute = c.suggestedFreeSlotMinute)
                                ?.let { finalizeSave(it) }
                        }) {
                            Text("زمان آزاد ($slotTxt)")
                        }
                    }
                    TextButton(onClick = { showConflictDialog = false }) {
                        Text("اصلاح دستی")
                    }
                }
            }
        )
    }

    if (showRecurringScopeDialog && initialTask != null && pendingSaveTask != null) {
        AlertDialog(
            onDismissRequest = { showRecurringScopeDialog = false },
            title = { Text("ویرایش فعالیت تکرارشونده") },
            text = { Text("تغییرات روی کدام نوبت‌ها اعمال شود؟") },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecurrenceEditScope.entries.forEach { scope ->
                        OutlinedButton(
                            onClick = {
                                showRecurringScopeDialog = false
                                onSaveRecurring(
                                    initialTask,
                                    pendingSaveTask!!,
                                    initialTask.effectiveDateKey,
                                    scope
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(scope.labelFa)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecurringScopeDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun ChoiceChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepperFieldCard(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(onClick = onMinus, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "کاهش $label", tint = MaterialTheme.colorScheme.secondary)
            }
            Text(valueText, style = TabularTimeStyle, color = MaterialTheme.colorScheme.onSurface)
            IconButton(onClick = onPlus, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "افزایش $label", tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
