package com.example.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.R
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.util.PersianCalendarUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NocturneWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NocturneAppWidget()
}

object NocturneWidgetUpdater {
    suspend fun updateAll(context: Context) {
        try {
            NocturneAppWidget().updateAll(context)
        } catch (_: Exception) {
            // Safe guard during unit tests or unattached states
        }
    }

    fun updateAllAsync(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            updateAll(appContext)
        }
    }

    suspend fun getInstalledCount(context: Context): Int {
        return try {
            GlanceAppWidgetManager(context).getGlanceIds(NocturneAppWidget::class.java).size
        } catch (_: Exception) {
            0
        }
    }
}

class NocturneAppWidget : GlanceAppWidget() {

    companion object {
        val SMALL_SIZE = DpSize(130.dp, 100.dp)
        val MEDIUM_SIZE = DpSize(240.dp, 140.dp)
        val LARGE_SIZE = DpSize(260.dp, 240.dp)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL_SIZE, MEDIUM_SIZE, LARGE_SIZE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetDataProvider.loadSnapshot(context)
        provideContent {
            val size = LocalSize.current
            NocturneWidgetContent(
                context = context,
                snapshot = snapshot,
                size = size
            )
        }
    }
}

private data class WidgetPalette(
    val bg: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentViolet: Color,
    val accentCyan: Color,
    val emerald: Color,
    val coral: Color,
    val amber: Color
)

private fun paletteFor(darkTheme: Boolean): WidgetPalette = if (darkTheme) {
    WidgetPalette(
        bg = Color(0xFF0C1322),
        card = Color(0xFF1E2235),
        textPrimary = Color(0xFFF9FAFB),
        textSecondary = Color(0xFF9CA3AF),
        accentViolet = Color(0xFF8B5CF6),
        accentCyan = Color(0xFF06B6D4),
        emerald = Color(0xFF10B981),
        coral = Color(0xFFEF4444),
        amber = Color(0xFFF59E0B)
    )
} else {
    WidgetPalette(
        bg = Color(0xFFF4F6FB),
        card = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        accentViolet = Color(0xFF6D28D9),
        accentCyan = Color(0xFF0891B2),
        emerald = Color(0xFF059669),
        coral = Color(0xFFDC2626),
        amber = Color(0xFFD97706)
    )
}

@Composable
internal fun NocturneWidgetContent(
    context: Context,
    snapshot: WidgetSnapshot,
    size: DpSize
) {
    val palette = paletteFor(snapshot.preferences.widgetDarkTheme)
    val usePersianDigits = snapshot.preferences.usePersianDigits

    when {
        size.height < 135.dp || size.width < 200.dp ->
            SmallNocturneWidget(context, snapshot, palette, usePersianDigits)
        size.height < 225.dp ->
            MediumNocturneWidget(context, snapshot, palette, usePersianDigits)
        else ->
            LargeNocturneWidget(context, snapshot, palette, usePersianDigits)
    }
}

@Composable
private fun SmallNocturneWidget(
    context: Context,
    snapshot: WidgetSnapshot,
    p: WidgetPalette,
    usePersianDigits: Boolean
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(p.bg)
            .cornerRadius(20.dp)
            .padding(12.dp)
            .clickable(
                actionStartActivity(
                    NocturneWidgetActions.launchMainIntent(context, NocturneWidgetActions.ACTION_OPEN_APP)
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.app_name),
                maxLines = 1,
                style = TextStyle(color = ColorProvider(p.accentCyan), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = snapshot.currentTimeFormatted,
                style = TextStyle(color = ColorProvider(p.textPrimary), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        Text(
            text = snapshot.todayDate.formattedDayAndMonth(usePersianDigits),
            style = TextStyle(color = ColorProvider(p.textSecondary), fontSize = 11.sp)
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        val next = snapshot.nextScheduledTask
        if (next != null) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(p.card)
                    .cornerRadius(12.dp)
                    .padding(8.dp)
                    .clickable(
                        actionStartActivity(
                            NocturneWidgetActions.launchMainIntent(
                                context,
                                NocturneWidgetActions.ACTION_OPEN_TASK,
                                next.id
                            )
                        )
                    )
            ) {
                Text(
                    text = snapshot.displayTitle(next),
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(p.textPrimary), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = "${next.formatTime(usePersianDigits)} • ${snapshot.remainingLabel}",
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(p.accentViolet), fontSize = 10.sp)
                )
            }
        } else {
            Text(
                text = "برنامه‌ای در صف نیست",
                maxLines = 1,
                style = TextStyle(color = ColorProvider(p.textSecondary), fontSize = 11.sp)
            )
        }
    }
}

@Composable
private fun MediumNocturneWidget(
    context: Context,
    snapshot: WidgetSnapshot,
    p: WidgetPalette,
    usePersianDigits: Boolean
) {
    val maxItems = snapshot.preferences.widgetVisibleCount.coerceIn(1, 4)
    val itemsToShow = snapshot.visibleTasks.take(maxItems)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(p.bg)
            .cornerRadius(20.dp)
            .padding(12.dp)
    ) {
        // Header Row
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(
                        actionStartActivity(
                            NocturneWidgetActions.launchMainIntent(context, NocturneWidgetActions.ACTION_OPEN_APP)
                        )
                    )
            ) {
                Text(
                    text = "امروز • ${snapshot.todayDate.formattedDayAndMonth(usePersianDigits)}",
                    style = TextStyle(color = ColorProvider(p.textPrimary), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
                val pctStr = PersianCalendarUtil.formatNumber(snapshot.progressPercent, usePersianDigits)
                val sub = buildString {
                    append("پیشرفت: $pctStr٪")
                    snapshot.nextReminderLabel?.let { append(" • $it") }
                }
                Text(
                    text = sub,
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(p.accentCyan), fontSize = 10.sp)
                )
            }

            // Quick Voice Shortcut
            Box(
                modifier = GlanceModifier
                    .background(p.accentViolet)
                    .cornerRadius(10.dp)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .clickable(
                        actionStartActivity(
                            NocturneWidgetActions.launchMainIntent(context, NocturneWidgetActions.ACTION_OPEN_VOICE)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "دستیار",
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            // Quick Add Shortcut
            Box(
                modifier = GlanceModifier
                    .background(p.card)
                    .cornerRadius(10.dp)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .clickable(
                        actionStartActivity(
                            NocturneWidgetActions.launchMainIntent(context, NocturneWidgetActions.ACTION_ADD_TASK)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ جدید",
                    style = TextStyle(color = ColorProvider(p.accentCyan), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (itemsToShow.isEmpty()) {
            WidgetEmptyState(context, p)
        } else {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                itemsToShow.forEachIndexed { idx, task ->
                    WidgetTaskRow(
                        context = context,
                        task = task,
                        displayTitle = snapshot.displayTitle(task),
                        showPriority = snapshot.preferences.widgetShowPriorities,
                        showDescription = snapshot.preferences.widgetShowDescriptions,
                        usePersianDigits = usePersianDigits,
                        p = p
                    )
                    if (idx < itemsToShow.lastIndex) {
                        Spacer(modifier = GlanceModifier.height(5.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LargeNocturneWidget(
    context: Context,
    snapshot: WidgetSnapshot,
    p: WidgetPalette,
    usePersianDigits: Boolean
) {
    val maxItems = snapshot.preferences.widgetVisibleCount.coerceIn(2, 8)
    val itemsToShow = snapshot.visibleTasks.take(maxItems)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(p.bg)
            .cornerRadius(22.dp)
            .padding(14.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(
                        actionStartActivity(
                            NocturneWidgetActions.launchMainIntent(context, NocturneWidgetActions.ACTION_OPEN_APP)
                        )
                    )
            ) {
                Text(
                    text = snapshot.todayDate.formattedFull(usePersianDigits),
                    style = TextStyle(color = ColorProvider(p.textPrimary), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
                val doneStr = PersianCalendarUtil.formatNumber(snapshot.completedTodayCount, usePersianDigits)
                val totalStr = PersianCalendarUtil.formatNumber(snapshot.totalTodayCount, usePersianDigits)
                val pctStr = PersianCalendarUtil.formatNumber(snapshot.progressPercent, usePersianDigits)
                Text(
                    text = "تکمیل‌شده: $doneStr از $totalStr ($pctStr٪) • ساعت ${snapshot.currentTimeFormatted}",
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(p.accentCyan), fontSize = 11.sp)
                )
            }

            Box(
                modifier = GlanceModifier
                    .background(p.accentViolet)
                    .cornerRadius(10.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .clickable(
                        actionStartActivity(
                            NocturneWidgetActions.launchMainIntent(context, NocturneWidgetActions.ACTION_OPEN_VOICE)
                        )
                    )
            ) {
                Text(
                    text = "صوتی",
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = GlanceModifier.width(6.dp))
            Box(
                modifier = GlanceModifier
                    .background(p.card)
                    .cornerRadius(10.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .clickable(
                        actionStartActivity(
                            NocturneWidgetActions.launchMainIntent(context, NocturneWidgetActions.ACTION_ADD_TASK)
                        )
                    )
            ) {
                Text(
                    text = "+ کار",
                    style = TextStyle(color = ColorProvider(p.accentCyan), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        if (snapshot.overdueTasks.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            val overdueCount = PersianCalendarUtil.formatNumber(snapshot.overdueTasks.size, usePersianDigits)
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(p.coral.copy(alpha = 0.16f))
                    .cornerRadius(10.dp)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .clickable(
                        actionStartActivity(
                            NocturneWidgetActions.launchMainIntent(context, NocturneWidgetActions.ACTION_OPEN_APP)
                        )
                    )
            ) {
                Text(
                    text = "$overdueCount کار عقب‌افتاده • ${snapshot.displayTitle(snapshot.overdueTasks.first())}",
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(p.coral), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                )
            }
        }

        val next = snapshot.nextScheduledTask
        if (next != null) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(p.accentViolet.copy(alpha = 0.14f))
                    .cornerRadius(10.dp)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "کار بعدی: ${snapshot.displayTitle(next)} (${next.formatTime(usePersianDigits)})",
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(color = ColorProvider(p.textPrimary), fontSize = 11.sp)
                )
                Text(
                    text = snapshot.remainingLabel,
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(p.accentCyan), fontSize = 10.sp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (itemsToShow.isEmpty()) {
            WidgetEmptyState(context, p)
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(itemsToShow, itemId = { it.id }) { task ->
                    Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 5.dp)) {
                        WidgetTaskRow(
                            context = context,
                            task = task,
                            displayTitle = snapshot.displayTitle(task),
                            showPriority = snapshot.preferences.widgetShowPriorities,
                            showDescription = snapshot.preferences.widgetShowDescriptions,
                            usePersianDigits = usePersianDigits,
                            p = p
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetTaskRow(
    context: Context,
    task: TaskItem,
    displayTitle: String,
    showPriority: Boolean,
    showDescription: Boolean,
    usePersianDigits: Boolean,
    p: WidgetPalette
) {
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> p.coral
        TaskPriority.MEDIUM -> p.amber
        TaskPriority.LOW -> p.emerald
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(p.card)
            .cornerRadius(12.dp)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Completion button via real Glance ActionCallback
        Box(
            modifier = GlanceModifier
                .size(26.dp)
                .background(if (task.isCompleted) p.emerald else p.bg)
                .cornerRadius(8.dp)
                .clickable(
                    actionRunCallback<ToggleTaskCompleteAction>(
                        NocturneWidgetActions.taskToggleParams(task.id)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (task.isCompleted) "✓" else "○",
                style = TextStyle(
                    color = ColorProvider(if (task.isCompleted) Color.White else priorityColor),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(
                    actionStartActivity(
                        NocturneWidgetActions.launchMainIntent(
                            context,
                            NocturneWidgetActions.ACTION_OPEN_TASK,
                            task.id
                        )
                    )
                )
        ) {
            Text(
                text = displayTitle,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(if (task.isCompleted) p.textSecondary else p.textPrimary),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            if (showDescription && task.aiNotes.isNotBlank()) {
                Text(
                    text = task.aiNotes,
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(p.textSecondary), fontSize = 10.sp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(6.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = task.formatTime(usePersianDigits),
                style = TextStyle(color = ColorProvider(p.accentCyan), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
            if (showPriority) {
                Text(
                    text = task.priority.labelFa,
                    style = TextStyle(color = ColorProvider(priorityColor), fontSize = 9.sp)
                )
            }
        }
    }
}

@Composable
private fun WidgetEmptyState(
    context: Context,
    p: WidgetPalette
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(p.card)
            .cornerRadius(14.dp)
            .padding(12.dp)
            .clickable(
                actionStartActivity(
                    NocturneWidgetActions.launchMainIntent(context, NocturneWidgetActions.ACTION_ADD_TASK)
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "برنامه‌ای برای امروز ثبت نشده است",
            style = TextStyle(
                color = ColorProvider(p.textPrimary),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "برای افزودن کار جدید یا فرمان صوتی لمس کنید",
            style = TextStyle(
                color = ColorProvider(p.accentCyan),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        )
    }
}
