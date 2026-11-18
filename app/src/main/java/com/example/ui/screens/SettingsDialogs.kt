package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.BackupPreview
import com.example.ui.theme.LocalUsePersianDigits
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.SignalCoralCrimson
import com.example.ui.theme.SoftEmeraldJade
import com.example.ui.theme.WarmSolarAmber
import com.example.util.PersianCalendarUtil

@Composable
fun FirebaseSetupGuideDialog(
    isConfigured: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isConfigured) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                contentDescription = null,
                tint = if (isConfigured) SoftEmeraldJade else WarmSolarAmber
            )
        },
        title = {
            Text(
                text = if (isConfigured) "وضعیت هوش مصنوعی ابری Gemini" else "راهنمای فعال‌سازی هوش مصنوعی Gemini",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isConfigured) {
                    Text(
                        text = "پیکربندی Firebase در این نسخه تشخیص داده شد و پردازش هوشمند Gemini از طریق Firebase AI Logic فعال است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftEmeraldJade
                    )
                } else {
                    Text(
                        text = "در حال حاضر فایل google-services.json در پروژه پیکربندی نشده است؛ بنابراین تمام تحلیل‌ها و فرمان‌های صوتی با موتور آفلاین فارسی انجام می‌شوند.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "مراحل فعال‌سازی کامل در Firebase / AI Studio:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "۱. پروژه را به Firebase متصل کنید تا فایل app/google-services.json تولید شود.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "۲. سرویس Firebase AI Logic (Gemini API) و Firebase App Check را در کنسول Firebase فعال کنید.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "۳. در نسخه‌های دیباگ از DebugAppCheckProviderFactory و در نسخه انتشار از Play Integrity / reCAPTCHA Enterprise با کلید معتبر در منابع استفاده می‌شود.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("firebase_guide_close_btn")
            ) {
                Text("متوجه شدم")
            }
        }
    )
}

@Composable
fun XiaomiNotificationGuideDialog(
    onOpenBatterySettings: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.BatteryAlert,
                contentDescription = null,
                tint = WarmSolarAmber
            )
        },
        title = {
            Text(
                text = "رفع مشکل اعلان در شیائومی و گوشی‌های بهینه‌ساز باتری",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "در گوشی‌های Xiaomi / HyperOS / MIUI و برخی برندهای دیگر، محدودیت‌های سیستم‌عامل می‌تواند آلارم‌ها را در پس‌زمینه متوقف کند. برنامه به‌تنهایی اجازه دور زدن این تنظیمات را ندارد و باید مراحل زیر را در تنظیمات گوشی بررسی کنید:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                GuideStepCard(
                    step = "۱. مجوز اعلان و آلارم دقیق",
                    detail = "اطمینان حاصل کنید اعلان‌ها (POST_NOTIFICATIONS) و دسترسی «آلارم‌ها و یادآورها» فعال هستند."
                )
                GuideStepCard(
                    step = "۲. صرفه‌جویی باتری: بدون محدودیت",
                    detail = "در تنظیمات باتری «برنامه ریز شریعت»، گزینه «بدون محدودیت» (No restrictions / Unrestricted) را انتخاب کنید."
                )
                GuideStepCard(
                    step = "۳. شروع خودکار (Autostart) و پس‌زمینه",
                    detail = "در MIUI/HyperOS گزینه Autostart و فعالیت پس‌زمینه را فعال کنید و از Force Stop دستی برنامه پرهیز کنید."
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenBatterySettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تنظیمات باتری")
                    }
                    OutlinedButton(
                        onClick = onOpenAppDetails,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("صفحه برنامه")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("xiaomi_guide_close_btn")
            ) {
                Text("بستن")
            }
        }
    )
}

@Composable
private fun GuideStepCard(step: String, detail: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = step,
                style = MaterialTheme.typography.labelLarge,
                color = LuminousCyan,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun WidgetGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Widgets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("راهنمای افزودن ویجت صفحه اصلی", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "۱. روی فضای خالی صفحه اصلی (Home Screen) گوشی انگشت خود را نگه دارید.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "۲. گزینه ویجت‌ها (Widgets) را بزنید و نام «برنامه ریز شریعت» را پیدا کنید.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "۳. ویجت را روی صفحه بکشید. با تغییر اندازه ویجت، نمای کوچک، متوسط (لیست کارها + پیشرفت) یا بزرگ (افزودن سریع، دستور صوتی و کارهای بیشتر) به‌صورت خودکار نمایش داده می‌شود.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("فهمیدم")
            }
        }
    )
}

@Composable
fun BackupPreviewConfirmDialog(
    preview: BackupPreview,
    onConfirmMerge: () -> Unit,
    onConfirmReplace: () -> Unit,
    onDismiss: () -> Unit
) {
    val usePersian = LocalUsePersianDigits.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (preview.isValid) Icons.Filled.Restore else Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = if (preview.isValid) LuminousCyan else SignalCoralCrimson
            )
        },
        title = {
            Text(
                text = if (preview.isValid) "پیش‌نمایش فایل پشتیبان" else "فایل پشتیبان نامعتبر",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!preview.isValid) {
                    Text(
                        text = preview.errorMessage ?: "ساختار فایل پشتیبان قابل خواندن نیست.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SignalCoralCrimson
                    )
                } else {
                    Text(
                        text = "نسخه ساختار: ${PersianCalendarUtil.formatNumber(preview.schemaVersion, usePersian)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• کارهای معتبر: ${PersianCalendarUtil.formatNumber(preview.validTasksCount, usePersian)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (preview.invalidTasksCount > 0) {
                        Text(
                            text = "• موارد نامعتبر نادیده‌گرفته: ${PersianCalendarUtil.formatNumber(preview.invalidTasksCount, usePersian)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmSolarAmber
                        )
                    }
                    if (preview.duplicateTasksCount > 0) {
                        Text(
                            text = "• کارهای تکراری در فایل: ${PersianCalendarUtil.formatNumber(preview.duplicateTasksCount, usePersian)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmSolarAmber
                        )
                    }
                    Text(
                        text = "• اهداف و عادت‌ها: ${PersianCalendarUtil.formatNumber(preview.goalsCount, usePersian)} هدف (${PersianCalendarUtil.formatNumber(preview.habitLogsCount, usePersian)} ثبت روزانه)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (preview.includesPreferences) "• تنظیمات برنامه نیز بازیابی می‌شود." else "• بدون تنظیمات ذخیره شده.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "آیا می‌خواهید داده‌ها به برنامه فعلی اضافه (ادغام) شوند یا جایگزین کل داده‌های فعلی گردند؟",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmSolarAmber
                    )
                }
            }
        },
        confirmButton = {
            if (preview.isValid) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onConfirmMerge,
                        modifier = Modifier.testTag("backup_merge_btn")
                    ) {
                        Text("ادغام با فعلی")
                    }
                    Button(
                        onClick = onConfirmReplace,
                        colors = ButtonDefaults.buttonColors(containerColor = SignalCoralCrimson),
                        modifier = Modifier.testTag("backup_replace_btn")
                    ) {
                        Text("جایگزینی کامل")
                    }
                }
            } else {
                Button(onClick = onDismiss) { Text("بستن") }
            }
        },
        dismissButton = {
            if (preview.isValid) {
                TextButton(onClick = onDismiss) { Text("انصراف") }
            }
        }
    )
}

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    isDestructive: Boolean,
    secondaryConfirmLabel: String? = null,
    onSecondaryConfirm: (() -> Unit)? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = if (isDestructive) SignalCoralCrimson else WarmSolarAmber
            )
        },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (secondaryConfirmLabel != null && onSecondaryConfirm != null) {
                    OutlinedButton(onClick = onSecondaryConfirm) {
                        Text(secondaryConfirmLabel)
                    }
                }
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDestructive) SignalCoralCrimson else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("confirm_dialog_primary_btn")
                ) {
                    Text(confirmLabel)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
