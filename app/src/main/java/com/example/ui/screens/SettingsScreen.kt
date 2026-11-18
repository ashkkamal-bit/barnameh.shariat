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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.repository.BackupPreview
import com.example.data.repository.NocturnePreferences
import com.example.notifications.AlarmScheduleReport
import com.example.ui.components.FirebaseRequiredNoticeBanner
import com.example.ui.theme.LocalUsePersianDigits
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.SignalCoralCrimson
import com.example.ui.theme.SoftEmeraldJade
import com.example.ui.theme.TabularTimeStyle
import com.example.ui.theme.WarmSolarAmber
import com.example.util.PersianCalendarUtil
import com.example.voice.TtsStatus
import java.util.TimeZone

@Composable
fun SettingsScreen(
    preferences: NocturnePreferences,
    alarmStatus: AlarmScheduleReport,
    ttsStatus: TtsStatus,
    isCloudAiConfigured: Boolean,
    backupPreview: BackupPreview?,
    onSetDarkTheme: (Boolean) -> Unit,
    onSetUsePersianDigits: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onChangeDefaultDuration: (Int) -> Unit,
    onSetTimezoneConfig: (useSystem: Boolean, customId: String) -> Unit,
    onToggleAutoListen: (Boolean) -> Unit,
    onSetSpeechLanguageTag: (String) -> Unit,
    onSetTtsEnabled: (Boolean) -> Unit,
    onSetTtsSpeechRate: (Float) -> Unit,
    onSetDefaultReminderOffset: (Int) -> Unit,
    onSetTaskRemindersEnabled: (Boolean) -> Unit,
    onSetOverdueAlertsEnabled: (Boolean) -> Unit,
    onSetMorningSummary: (Boolean, Int) -> Unit,
    onSetEveningSummary: (Boolean, Int) -> Unit,
    onSetHabitRemindersEnabled: (Boolean) -> Unit,
    onUpdateWidgetSettings: (
        visibleCount: Int?,
        showCompleted: Boolean?,
        showDescriptions: Boolean?,
        showPriorities: Boolean?,
        darkTheme: Boolean?,
        hidePrivate: Boolean?
    ) -> Unit,
    onExportBackupUri: (Uri) -> Unit,
    onInspectBackupUri: (Uri) -> Unit,
    onConfirmBackupRestore: (replaceExisting: Boolean) -> Unit,
    onDismissBackupPreview: () -> Unit,
    onImportDemoTasks: (replaceExisting: Boolean) -> Unit,
    onClearAllTasks: () -> Unit,
    onRefreshPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val usePersian = LocalUsePersianDigits.current

    var showFirebaseGuide by remember { mutableStateOf(false) }
    var showXiaomiGuide by remember { mutableStateOf(false) }
    var showWidgetGuide by remember { mutableStateOf(false) }
    var showDemoImportDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshPermissions()
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackupUri(uri)
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onInspectBackupUri(uri)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "تنظیمات و مدیریت Nocturne",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "شخصی‌سازی ظاهر، دستیار صوتی، یادآورها، ویجت و پشتیبان‌گیری",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // AI & Firebase Status Banner
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isCloudAiConfigured) {
                        FirebaseRequiredNoticeBanner(
                            message = stringResource(R.string.ai_offline_notice)
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SoftEmeraldJade.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, SoftEmeraldJade.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = SoftEmeraldJade
                                )
                                Text(
                                    text = "هوش مصنوعی ابری Gemini و محافظت App Check متصل است.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showFirebaseGuide = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_firebase_guide_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("وضعیت و راهنمای پیکربندی هوش مصنوعی ابری")
                    }
                }
            }

            // 1. General & Appearance
            item {
                SettingsSectionCard(title = "ظاهر، تقویم و عمومی", accent = MaterialTheme.colorScheme.primary) {
                    SettingsToggleRow(
                        icon = Icons.Filled.DarkMode,
                        title = "تم شب (تیره)",
                        subtitle = "خاموش کردن این گزینه ظاهر روشن Material 3 را فعال می‌کند",
                        checked = preferences.isDarkTheme,
                        onCheckedChange = onSetDarkTheme,
                        testTag = "switch_dark_theme"
                    )
                    SettingsDivider()

                    SettingsToggleRow(
                        icon = Icons.Filled.Numbers,
                        title = "نمایش اعداد فارسی",
                        subtitle = "استفاده از ارقام فارسی (۱۲:۳۰) در سراسر برنامه و ویجت",
                        checked = preferences.usePersianDigits,
                        onCheckedChange = onSetUsePersianDigits,
                        testTag = "switch_persian_digits"
                    )
                    SettingsDivider()

                    SettingsToggleRow(
                        icon = Icons.Filled.Vibration,
                        title = "بازخورد لمسی (Haptics)",
                        subtitle = "لرزش ظریف هنگام تیک زدن کارها و لمس کنترل‌ها",
                        checked = preferences.hapticsEnabled,
                        onCheckedChange = onToggleHaptics,
                        testTag = "switch_haptics"
                    )
                    SettingsDivider()

                    SettingsToggleRow(
                        icon = Icons.Filled.Schedule,
                        title = "استفاده از منطقه زمانی سیستم",
                        subtitle = if (preferences.useSystemTimezone) {
                            "فعال: ${TimeZone.getDefault().id}"
                        } else {
                            "منطقه دستی: ${preferences.customTimezoneId.ifBlank { "Asia/Tehran" }}"
                        },
                        checked = preferences.useSystemTimezone,
                        onCheckedChange = { useSys ->
                            onSetTimezoneConfig(useSys, if (useSys) "" else "Asia/Tehran")
                        },
                        testTag = "switch_system_tz"
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "مدت پیش‌فرض کارهای جدید",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(25, 45, 60, 90).forEach { mins ->
                            val selected = preferences.defaultBlockMinutes == mins
                            SelectableChip(
                                label = PersianCalendarUtil.formatDurationMinutes(mins, usePersian),
                                selected = selected,
                                onClick = { onChangeDefaultDuration(mins) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 2. Voice Assistant & TTS
            item {
                SettingsSectionCard(title = "دستیار صوتی و گفتار دوطرفه", accent = LuminousCyan) {
                    SettingsToggleRow(
                        icon = Icons.Filled.GraphicEq,
                        title = "شروع خودکار شنیدن با باز کردن دستیار",
                        subtitle = "در صورت داشتن مجوز میکروفون، بلافاصله ضبط صدا آغاز شود",
                        checked = preferences.autoListenOnVoiceTap,
                        onCheckedChange = onToggleAutoListen,
                        testTag = "switch_auto_listen"
                    )
                    SettingsDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("زبان تشخیص گفتار", style = MaterialTheme.typography.titleMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SelectableChip(
                                label = "فارسی",
                                selected = preferences.speechLanguageTag == "fa-IR",
                                onClick = { onSetSpeechLanguageTag("fa-IR") }
                            )
                            SelectableChip(
                                label = "English",
                                selected = preferences.speechLanguageTag == "en-US",
                                onClick = { onSetSpeechLanguageTag("en-US") }
                            )
                        }
                    }
                    SettingsDivider()

                    SettingsToggleRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "پاسخ صوتی دستیار (TTS)",
                        subtitle = ttsStatus.statusMessage,
                        checked = preferences.ttsEnabled,
                        onCheckedChange = onSetTtsEnabled,
                        testTag = "switch_tts_enabled"
                    )

                    if (preferences.ttsEnabled) {
                        Column {
                            Text(
                                text = "سرعت خواندن صوتی: ${
                                    PersianCalendarUtil.formatDigits(
                                        String.format("%.1fx", preferences.ttsSpeechRate),
                                        usePersian
                                    )
                                }",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = preferences.ttsSpeechRate,
                                onValueChange = onSetTtsSpeechRate,
                                valueRange = 0.7f..1.5f
                            )
                        }
                    }
                }
            }

            // 3. Notifications & Smart Alarms
            item {
                SettingsSectionCard(title = "موتور یادآور و اعلان‌های هوشمند", accent = WarmSolarAmber) {
                    // Permission & Exact Alarm status banner inside settings
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            1.dp,
                            if (alarmStatus.postNotificationPermitted && alarmStatus.exactAlarmPermitted)
                                SoftEmeraldJade.copy(alpha = 0.45f)
                            else WarmSolarAmber.copy(alpha = 0.55f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "وضعیت دسترسی اعلان: ${if (alarmStatus.postNotificationPermitted) "فعال" else "نیازمند مجوز"} | آلارم دقیق: ${if (alarmStatus.exactAlarmPermitted) "فعال" else "حالت جایگزین"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (alarmStatus.postNotificationPermitted && alarmStatus.exactAlarmPermitted)
                                    SoftEmeraldJade else WarmSolarAmber
                            )
                            Text(
                                text = "تعداد یادآورهای زمان‌بندی‌شده فعال: ${PersianCalendarUtil.formatNumber(alarmStatus.scheduledTaskAlarms, usePersian)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!alarmStatus.postNotificationPermitted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    Button(
                                        onClick = { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("مجوز اعلان")
                                    }
                                }
                                if (!alarmStatus.exactAlarmPermitted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    OutlinedButton(
                                        onClick = {
                                            context.startActivity(
                                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                    data = Uri.parse("package:${context.packageName}")
                                                }
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("مجوز آلارم دقیق")
                                    }
                                }
                            }
                        }
                    }

                    SettingsToggleRow(
                        icon = Icons.Filled.NotificationsActive,
                        title = "یادآور زمان‌بندی کارها",
                        subtitle = "ارسال اعلان با دکمه «انجام شد» و «به تعویق انداختن»",
                        checked = preferences.taskRemindersEnabled,
                        onCheckedChange = onSetTaskRemindersEnabled,
                        testTag = "switch_task_reminders"
                    )
                    SettingsDivider()

                    SettingsToggleRow(
                        icon = Icons.Filled.Alarm,
                        title = "هشدار کارهای عقب‌افتاده",
                        subtitle = "بررسی دوره‌ای کارهای گذشته و اعلام در اعلان‌ها",
                        checked = preferences.overdueAlertsEnabled,
                        onCheckedChange = onSetOverdueAlertsEnabled,
                        testTag = "switch_overdue_alerts"
                    )
                    SettingsDivider()

                    SettingsToggleRow(
                        icon = Icons.Filled.NotificationsActive,
                        title = "یادآور عادت‌ها و اهداف",
                        subtitle = "اطلاع‌رسانی روزانه برای ثبت تداوم اهداف شخصی",
                        checked = preferences.habitRemindersEnabled,
                        onCheckedChange = onSetHabitRemindersEnabled,
                        testTag = "switch_habit_reminders"
                    )
                    SettingsDivider()

                    Text(
                        text = "فاصله پیش‌فرض یادآور قبل از شروع",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 5, 10, 15, 30).forEach { off ->
                            SelectableChip(
                                label = if (off == 0) "سر ساعت" else "${PersianCalendarUtil.formatNumber(off, usePersian)} دقیقه قبل",
                                selected = preferences.defaultReminderOffsetMinutes == off,
                                onClick = { onSetDefaultReminderOffset(off) }
                            )
                        }
                    }
                    SettingsDivider()

                    // Morning & Evening summaries
                    SummaryHourRow(
                        title = "خلاصه صبحگاهی",
                        enabled = preferences.morningSummaryEnabled,
                        hour = preferences.morningSummaryHour,
                        minHour = 5,
                        maxHour = 11,
                        usePersian = usePersian,
                        onToggle = { onSetMorningSummary(it, preferences.morningSummaryHour) },
                        onChangeHour = { onSetMorningSummary(preferences.morningSummaryEnabled, it) }
                    )
                    SummaryHourRow(
                        title = "مرور و جمع‌بندی شبانه",
                        enabled = preferences.eveningSummaryEnabled,
                        hour = preferences.eveningSummaryHour,
                        minHour = 17,
                        maxHour = 23,
                        usePersian = usePersian,
                        onToggle = { onSetEveningSummary(it, preferences.eveningSummaryHour) },
                        onChangeHour = { onSetEveningSummary(preferences.eveningSummaryEnabled, it) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("کانال‌های سیستم")
                        }
                        OutlinedButton(
                            onClick = { showXiaomiGuide = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("xiaomi_guide_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.BatteryAlert, contentDescription = null, modifier = Modifier.size(16.dp), tint = WarmSolarAmber)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("راهنمای شیائومی/MIUI")
                        }
                    }
                }
            }

            // 4. Home Screen Widget Settings
            item {
                SettingsSectionCard(title = "ویجت صفحه اصلی (Home Screen)", accent = SoftEmeraldJade) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تعداد کارهای قابل نمایش در ویجت", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (preferences.widgetVisibleCount > 2) {
                                        onUpdateWidgetSettings(preferences.widgetVisibleCount - 1, null, null, null, null, null)
                                    }
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "کاهش تعداد کار ویجت")
                            }
                            Text(
                                text = PersianCalendarUtil.formatNumber(preferences.widgetVisibleCount, usePersian),
                                style = TabularTimeStyle.copy(fontWeight = FontWeight.Bold)
                            )
                            IconButton(
                                onClick = {
                                    if (preferences.widgetVisibleCount < 6) {
                                        onUpdateWidgetSettings(preferences.widgetVisibleCount + 1, null, null, null, null, null)
                                    }
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "افزایش تعداد کار ویجت")
                            }
                        }
                    }
                    SettingsDivider()

                    SettingsToggleRow(
                        icon = Icons.Filled.Widgets,
                        title = "نمایش کارهای انجام‌شده در ویجت",
                        subtitle = "در صورت خاموش بودن فقط کارهای باقی‌مانده نمایش داده می‌شوند",
                        checked = preferences.widgetShowCompleted,
                        onCheckedChange = { onUpdateWidgetSettings(null, it, null, null, null, null) }
                    )
                    SettingsToggleRow(
                        icon = Icons.Filled.Widgets,
                        title = "نمایش یادداشت و توضیحات کوتاه",
                        subtitle = "نمایش خط اول یادداشت کار در حالت ویجت بزرگ",
                        checked = preferences.widgetShowDescriptions,
                        onCheckedChange = { onUpdateWidgetSettings(null, null, it, null, null, null) }
                    )
                    SettingsToggleRow(
                        icon = Icons.Filled.Widgets,
                        title = "نمایش نشانگر اولویت",
                        subtitle = "برچسب یا خط رنگی اولویت کارها روی ویجت",
                        checked = preferences.widgetShowPriorities,
                        onCheckedChange = { onUpdateWidgetSettings(null, null, null, it, null, null) }
                    )
                    SettingsToggleRow(
                        icon = Icons.Filled.DarkMode,
                        title = "تم تیره ویجت",
                        subtitle = "هماهنگ با پس‌زمینه تیره یا روشن صفحه اصلی شما",
                        checked = preferences.widgetDarkTheme,
                        onCheckedChange = { onUpdateWidgetSettings(null, null, null, null, it, null) }
                    )
                    SettingsToggleRow(
                        icon = Icons.Filled.PrivacyTip,
                        title = "مخفی‌سازی عنوان کارهای خصوصی",
                        subtitle = "جایگزینی عنوان کارهای علامت‌گذاری‌شده به‌عنوان «خصوصی» در ویجت",
                        checked = preferences.widgetHidePrivateTitles,
                        onCheckedChange = { onUpdateWidgetSettings(null, null, null, null, null, it) }
                    )

                    OutlinedButton(
                        onClick = { showWidgetGuide = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("نحوه افزودن ویجت به صفحه اصلی")
                    }
                }
            }

            // 5. Backup, Restore & Data Management
            item {
                SettingsSectionCard(title = "پشتیبان‌گیری و مدیریت داده‌ها", accent = MaterialTheme.colorScheme.secondary) {
                    Text(
                        text = "تمام کارها، زیرکارها، یادآورها، اهداف، عادت‌ها و تنظیمات شما به‌صورت محلی در Room و DataStore نگهداری می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val fileName = "nocturne_backup_${
                                    PersianCalendarUtil.today().dateKey
                                }.json"
                                exportBackupLauncher.launch(fileName)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_backup_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("خروجی JSON")
                        }
                        OutlinedButton(
                            onClick = {
                                importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_backup_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بازیابی فایل")
                        }
                    }

                    SettingsDivider()

                    OutlinedButton(
                        onClick = { showDemoImportDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_demo_data_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = LuminousCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("وارد کردن برنامه نمونه فارسی (اختیاری)")
                    }

                    OutlinedButton(
                        onClick = { showClearAllDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SignalCoralCrimson),
                        border = BorderStroke(1.dp, SignalCoralCrimson.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_all_tasks_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("پاک کردن تمام کارها")
                    }
                }
            }

            // 6. Privacy & About
            item {
                SettingsSectionCard(title = "حریم خصوصی و درباره برنامه ریز شریعت", accent = MaterialTheme.colorScheme.onSurfaceVariant) {
                    Text(
                        text = "• ذخیره‌سازی پیش‌فرض کاملاً محلی و خصوصی روی دستگاه است.\n" +
                            "• تنها در صورت اتصال Firebase و درخواست مستقیم شما برای تحلیل هوشمند، متن درخواست برای پردازش به سرویس Firebase AI Logic ارسال می‌شود.\n" +
                            "• نسخه ۲.۰ — مجهز به موتور اعلان زمان‌بندی‌شده، ویجت Glance، تقویم شمسی و دستیار صوتی دوطرفه.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showFirebaseGuide) {
        FirebaseSetupGuideDialog(
            isConfigured = isCloudAiConfigured,
            onDismiss = { showFirebaseGuide = false }
        )
    }

    if (showXiaomiGuide) {
        XiaomiNotificationGuideDialog(
            onOpenBatterySettings = {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    )
                } catch (_: Exception) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                }
            },
            onOpenAppDetails = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            },
            onDismiss = { showXiaomiGuide = false }
        )
    }

    if (showWidgetGuide) {
        WidgetGuideDialog(onDismiss = { showWidgetGuide = false })
    }

    if (backupPreview != null) {
        BackupPreviewConfirmDialog(
            preview = backupPreview,
            onConfirmMerge = { onConfirmBackupRestore(false) },
            onConfirmReplace = { onConfirmBackupRestore(true) },
            onDismiss = onDismissBackupPreview
        )
    }

    if (showDemoImportDialog) {
        ConfirmActionDialog(
            title = "وارد کردن برنامه نمونه",
            message = "می‌توانید چند کار نمونه فارسی را به برنامه فعلی اضافه کنید یا جایگزین کارهای فعلی نمایید.",
            confirmLabel = "افزودن به کارهای فعلی",
            isDestructive = false,
            secondaryConfirmLabel = "جایگزینی کل لیست",
            onSecondaryConfirm = {
                showDemoImportDialog = false
                onImportDemoTasks(true)
            },
            onConfirm = {
                showDemoImportDialog = false
                onImportDemoTasks(false)
            },
            onDismiss = { showDemoImportDialog = false }
        )
    }

    if (showClearAllDialog) {
        ConfirmActionDialog(
            title = "پاک کردن تمام کارها",
            message = "آیا مطمئن هستید؟ تمامی کارها و یادآورهای زمان‌بندی‌شده شما حذف خواهند شد و قابل بازگشت نیستند (مگر از قبل نسخه پشتیبان تهیه کرده باشید).",
            confirmLabel = "بله، همه پاک شوند",
            isDestructive = true,
            onConfirm = {
                showClearAllDialog = false
                onClearAllTasks()
            },
            onDismiss = { showClearAllDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    accent: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryHourRow(
    title: String,
    enabled: Boolean,
    hour: Int,
    minHour: Int,
    maxHour: Int,
    usePersian: Boolean,
    onToggle: (Boolean) -> Unit,
    onChangeHour: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "ساعت ارسال: ${PersianCalendarUtil.formatTime(hour, 0, usePersian)}",
                style = TabularTimeStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (enabled) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (hour > minHour) onChangeHour(hour - 1) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "کاهش ساعت")
                }
                IconButton(
                    onClick = { if (hour < maxHour) onChangeHour(hour + 1) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "افزایش ساعت")
                }
            }
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}
