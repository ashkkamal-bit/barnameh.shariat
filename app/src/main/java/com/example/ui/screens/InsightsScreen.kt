package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GoalPeriod
import com.example.data.model.GoalProgressSummary
import com.example.data.model.PersonalGoal
import com.example.ui.AiInsightUiState
import com.example.ui.TodayUiState
import com.example.ui.components.FirebaseRequiredNoticeBanner
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LocalUsePersianDigits
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.SignalCoralCrimson
import com.example.ui.theme.SoftEmeraldJade
import com.example.ui.theme.TabularTimeStyle
import com.example.ui.theme.WarmSolarAmber
import com.example.util.PersianCalendarUtil

private enum class InsightsTab(val labelFa: String) {
    ANALYTICS("آمار و تحلیل"),
    GOALS("اهداف و عادت‌ها")
}

@Composable
fun InsightsScreen(
    uiState: TodayUiState,
    insightState: AiInsightUiState,
    goalSummaries: List<GoalProgressSummary>,
    isCloudAiConfigured: Boolean,
    onRefreshInsights: () -> Unit,
    onSaveGoal: (PersonalGoal) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onToggleGoalToday: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val usePersianDigits = LocalUsePersianDigits.current
    var activeTab by remember { mutableStateOf(InsightsTab.ANALYTICS) }
    var showGoalDialog by remember { mutableStateOf(false) }

    val dayMetrics = uiState.dayMetrics
    val weekMetrics = uiState.weekMetrics

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 128.dp, start = 16.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "تحلیل بهره‌وری و اهداف",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = uiState.selectedDate.formattedFull(usePersianDigits),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                if (activeTab == InsightsTab.GOALS) {
                    IconButton(
                        onClick = { showGoalDialog = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), CircleShape)
                            .testTag("add_goal_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "افزودن هدف شخصی", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        // Tab selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                InsightsTab.entries.forEach { tab ->
                    val selected = activeTab == tab
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
                            .clickable { activeTab = tab }
                            .padding(vertical = 9.dp)
                            .testTag("insights_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.labelFa,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (activeTab == InsightsTab.ANALYTICS) {
            // Section 1: 100% Measured Local Statistics (clearly separated from AI suggestions)
            item {
                Text(
                    text = "آمار اندازه‌گیری‌شده واقعی (از پایگاه داده)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "پیشرفت روز",
                        value = "${PersianCalendarUtil.formatNumber(dayMetrics.completionPercent, usePersianDigits)}٪",
                        subtitle = "${PersianCalendarUtil.formatNumber(dayMetrics.completedCount, usePersianDigits)} از ${
                            PersianCalendarUtil.formatNumber(dayMetrics.totalTasks, usePersianDigits)
                        } کار",
                        accent = SoftEmeraldJade,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "حجم کار کل",
                        value = PersianCalendarUtil.formatDurationMinutes(dayMetrics.totalWorkloadMinutes, usePersianDigits),
                        subtitle = "${PersianCalendarUtil.formatNumber(dayMetrics.totalScheduled, usePersianDigits)} زمان‌بندی • ${
                            PersianCalendarUtil.formatNumber(dayMetrics.totalUnscheduled, usePersianDigits)
                        } آزاد",
                        accent = LuminousCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "عقب‌افتاده و فوری",
                        value = PersianCalendarUtil.formatNumber(dayMetrics.overdueCount, usePersianDigits),
                        subtitle = "${PersianCalendarUtil.formatNumber(dayMetrics.highPriorityPendingCount, usePersianDigits)} کار فوری باقیمانده",
                        accent = SignalCoralCrimson,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "عملکرد هفته",
                        value = "${PersianCalendarUtil.formatNumber(weekMetrics.completionPercent, usePersianDigits)}٪",
                        subtitle = "${PersianCalendarUtil.formatNumber(weekMetrics.completedTasks, usePersianDigits)} از ${
                            PersianCalendarUtil.formatNumber(weekMetrics.totalTasks, usePersianDigits)
                        } کار هفته",
                        accent = if (dayMetrics.conflictCount > 0) WarmSolarAmber else ElectricViolet,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Time-of-day distribution measured from DB
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("توزیع زمانی فعالیت‌ها", style = MaterialTheme.typography.titleMedium)
                        val maxDist = maxOf(
                            dayMetrics.morningMinutes,
                            dayMetrics.afternoonMinutes,
                            dayMetrics.eveningMinutes,
                            60
                        ).toFloat()
                        DistributionRow("صبح (تا ۱۲)", dayMetrics.morningMinutes, maxDist, SoftEmeraldJade, usePersianDigits)
                        DistributionRow("بعدازظهر (۱۲ تا ۱۷)", dayMetrics.afternoonMinutes, maxDist, LuminousCyan, usePersianDigits)
                        DistributionRow("عصر و شب (۱۷+)", dayMetrics.eveningMinutes, maxDist, ElectricViolet, usePersianDigits)

                        if (dayMetrics.conflictCount > 0) {
                            Text(
                                text = "تداخل‌های زمانی شناسایی‌شده: ${
                                    PersianCalendarUtil.formatNumber(dayMetrics.conflictCount, usePersianDigits)
                                } مورد",
                                style = MaterialTheme.typography.labelMedium,
                                color = WarmSolarAmber
                            )
                        }
                    }
                }
            }

            // Section 2: AI Coaching / Suggestions (clearly distinguished from measured metrics)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (isCloudAiConfigured) "پیشنهادهای هوش مصنوعی Gemini" else "تحلیل پیشنهادی (آفلاین)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    TextButton(onClick = onRefreshInsights) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("به‌روزرسانی تحلیل")
                    }
                }
            }

            item {
                when (insightState) {
                    is AiInsightUiState.Idle -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "برای دریافت برنامه صبحگاهی، جمع‌بندی شبانه و مرور هفتگی دکمه زیر را لمس کنید.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(onClick = onRefreshInsights) {
                                    Text("تولید تحلیل و پیشنهادها")
                                }
                            }
                        }
                    }
                    is AiInsightUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    is AiInsightUiState.Error -> {
                        FirebaseRequiredNoticeBanner(message = insightState.message)
                    }
                    is AiInsightUiState.Ready -> {
                        val res = insightState.result
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!res.noticeMessage.isNullOrBlank()) {
                                FirebaseRequiredNoticeBanner(message = res.noticeMessage)
                            }
                            InsightParagraphCard("برنامه پیشنهادی صبح", res.morningPlan)
                            InsightParagraphCard("جمع‌بندی پایان روز", res.eveningSummary)
                            InsightParagraphCard("مرور عملکرد هفته", res.weeklyReview)
                            InsightParagraphCard("تعادل بار کاری", res.workloadBalanceTip)
                            if (res.tomorrowSuggestions.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("نکات و پیشنهادها", style = MaterialTheme.typography.titleMedium)
                                        res.tomorrowSuggestions.forEach { s ->
                                            Text("• $s", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Goals & Habit Tracking Section
            item {
                Text(
                    text = "مدیریت اختیاری اهداف شخصی و پیگیری عادت‌ها (بدون اجبار برای استفاده از برنامه‌ریز روزانه)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (goalSummaries.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("هنوز هدفی تعریف نکرده‌اید", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "می‌توانید اهدافی مانند یادگیری زبان، مطالعه روزانه، هوش مصنوعی یا ورزش منظم را ثبت کنید.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = { showGoalDialog = true }) {
                                Text("ساخت اولین هدف")
                            }
                        }
                    }
                }
            } else {
                items(goalSummaries, key = { it.goal.id }) { summary ->
                    GoalProgressCard(
                        summary = summary,
                        usePersianDigits = usePersianDigits,
                        onToggleToday = { onToggleGoalToday(summary.goal.id) },
                        onDelete = { onDeleteGoal(summary.goal.id) }
                    )
                }
            }
        }
    }

    if (showGoalDialog) {
        AddGoalDialog(
            onSave = { goal ->
                onSaveGoal(goal)
                showGoalDialog = false
            },
            onDismiss = { showGoalDialog = false }
        )
    }
}

@Composable
private fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineMedium, color = accent, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DistributionRow(
    label: String,
    minutes: Int,
    maxMinutes: Float,
    color: Color,
    usePersianDigits: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                PersianCalendarUtil.formatDurationMinutes(minutes, usePersianDigits),
                style = TabularTimeStyle,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { (minutes / maxMinutes).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
private fun InsightParagraphCard(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun GoalProgressCard(
    summary: GoalProgressSummary,
    usePersianDigits: Boolean,
    onToggleToday: () -> Unit,
    onDelete: () -> Unit
) {
    val goal = summary.goal
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            if (summary.isDoneToday) SoftEmeraldJade.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (summary.isDoneToday) SoftEmeraldJade else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                1.5.dp,
                                if (summary.isDoneToday) SoftEmeraldJade else MaterialTheme.colorScheme.secondary,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(onClick = onToggleToday),
                        contentAlignment = Alignment.Center
                    ) {
                        if (summary.isDoneToday) {
                            Icon(Icons.Filled.Check, contentDescription = "ثبت امروز", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Column {
                        Text(goal.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${goal.period.labelFa} • پیشرفت: ${
                                PersianCalendarUtil.formatNumber(summary.completedInCurrentPeriod, usePersianDigits)
                            }/${
                                PersianCalendarUtil.formatNumber(goal.targetPerPeriod, usePersianDigits)
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (summary.currentStreakDays > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(WarmSolarAmber.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = WarmSolarAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${PersianCalendarUtil.formatNumber(summary.currentStreakDays, usePersianDigits)} روز",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmSolarAmber
                            )
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "حذف هدف",
                            tint = SignalCoralCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { summary.completionRatio },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                color = if (summary.isDoneToday) SoftEmeraldJade else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }
    }
}

@Composable
private fun AddGoalDialog(
    onSave: (PersonalGoal) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var period by remember { mutableStateOf(GoalPeriod.DAILY) }
    var target by remember { mutableIntStateOf(1) }
    var reminderEnabled by remember { mutableStateOf(false) }

    val presets = listOf("یادگیری زبان انگلیسی", "یادگیری هوش مصنوعی", "ورزش منظم", "مطالعه روزانه")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("هدف شخصی جدید") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { p ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .clickable { title = p }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(p, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان هدف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GoalPeriod.entries.forEach { gp ->
                        val sel = period == gp
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                )
                                .clickable {
                                    period = gp
                                    target = when (gp) {
                                        GoalPeriod.DAILY -> 1
                                        GoalPeriod.WEEKLY -> 4
                                        GoalPeriod.MONTHLY -> 15
                                        GoalPeriod.YEARLY -> 100
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(gp.labelFa, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("یادآور روزانه ساعت ۲۰:۰۰", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = title.trim()
                    if (clean.isNotEmpty()) {
                        onSave(
                            PersonalGoal(
                                title = clean,
                                period = period,
                                targetPerPeriod = target,
                                reminderEnabled = reminderEnabled,
                                reminderHour = 20,
                                reminderMinute = 0
                            )
                        )
                    }
                }
            ) {
                Text("ثبت هدف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
