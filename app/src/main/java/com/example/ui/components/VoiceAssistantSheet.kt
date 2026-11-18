package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.ai.AiChatMessage
import com.example.data.ai.AiIntentKind
import com.example.data.ai.ProposedTaskDraft
import com.example.data.model.TaskItem
import com.example.ui.AiSynthesisUiState
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LocalUsePersianDigits
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.SignalCoralCrimson
import com.example.ui.theme.SoftEmeraldJade
import com.example.ui.theme.TabularTimeStyle
import com.example.ui.theme.WarmSolarAmber
import com.example.util.PersianCalendarUtil
import com.example.voice.SpeechState
import com.example.voice.TtsStatus
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantSheet(
    synthesisState: AiSynthesisUiState,
    speechState: SpeechState,
    ttsStatus: TtsStatus,
    ttsEnabled: Boolean,
    isCloudAiConfigured: Boolean,
    chatHistory: List<AiChatMessage>,
    draftInput: String,
    onDraftChange: (String) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onToggleTts: (Boolean) -> Unit,
    onSubmitCommand: (String) -> Unit,
    onConfirmDrafts: (List<ProposedTaskDraft>) -> Unit,
    onConfirmCompleteTask: (TaskItem) -> Unit,
    onEditDraftManually: (TaskItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val usePersianDigits = LocalUsePersianDigits.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var permissionDeniedNote by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNote = false
            onStartListening()
        } else {
            permissionDeniedNote = true
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
                .padding(bottom = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(ElectricViolet, LuminousCyan))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "دستیار گفتاری Nocturne",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isCloudAiConfigured) {
                                "متصل به Gemini (پاسخ هوشمند)"
                            } else {
                                "حالت آفلاین فارسی • تایپ یا فرمان صوتی"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onToggleTts(!ttsEnabled) },
                        modifier = Modifier.testTag("toggle_tts_button")
                    ) {
                        Icon(
                            imageVector = if (ttsEnabled) {
                                Icons.AutoMirrored.Filled.VolumeUp
                            } else {
                                Icons.AutoMirrored.Filled.VolumeOff
                            },
                            contentDescription = if (ttsEnabled) "خاموش کردن پاسخ صوتی" else "روشن کردن پاسخ صوتی",
                            tint = if (ttsEnabled && ttsStatus.isPersianAvailable) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_close_sheet),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (ttsEnabled && !ttsStatus.isPersianAvailable) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = ttsStatus.statusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmSolarAmber
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick example commands (planning & queries)
            val quickSuggestions = listOf(
                "برنامه امروزمو بگو",
                "کار بعدی من چیه؟",
                "فردا ساعت ۹ صبح جلسه تیم و بعد ساعت ۱۷ ورزش",
                "سه‌شنبه ساعت ۱۰:۳۰ ارائه پروژه",
                "برنامه‌هامو خلاصه کن"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickSuggestions.forEach { sample ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(999.dp)
                            )
                            .clickable {
                                onDraftChange(sample)
                                onSubmitCommand(sample)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = sample,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Conversation history within current planning session
            if (chatHistory.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chatHistory.takeLast(6).forEach { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (msg.isUser) Arrangement.Start else Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (msg.isUser) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Live Speech Listening / Partial Results
            if (speechState is SpeechState.Listening) {
                VoiceWaveformIndicator(modifier = Modifier.fillMaxWidth().height(48.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = speechState.partialTranscript.ifBlank { "در حال شنیدن فرمان فارسی شما..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (permissionDeniedNote) {
                FirebaseRequiredNoticeBanner(
                    message = "برای ضبط صدا، دسترسی میکروفون لازم است. می‌توانید از کادر متنی زیر برای تایپ فرمان استفاده کنید."
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Input Row + Mic & Send Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isListening = speechState is SpeechState.Listening
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) SignalCoralCrimson else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .border(
                            1.dp,
                            if (isListening) SignalCoralCrimson else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .clickable {
                            if (isListening) {
                                onStopListening()
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    permissionDeniedNote = false
                                    onStartListening()
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                        .testTag("sheet_mic_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isListening) "توقف ضبط" else "شروع ضبط صوتی فارسی",
                        tint = if (isListening) Color.White else MaterialTheme.colorScheme.secondary
                    )
                }

                OutlinedTextField(
                    value = draftInput,
                    onValueChange = onDraftChange,
                    placeholder = {
                        Text(
                            text = "مثلاً: فردا ساعت ۹ جلسه تیم و ساعت ۱۶ باشگاه",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voice_text_input")
                )

                IconButton(
                    onClick = { onSubmitCommand(draftInput) },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("voice_submit_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "ارسال فرمان",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Processing / Error / Structured Confirmation Preview
            when (synthesisState) {
                is AiSynthesisUiState.Processing -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "در حال تحلیل هوشمند زمان و برنامه...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                is AiSynthesisUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SignalCoralCrimson.copy(alpha = 0.12f))
                            .border(1.dp, SignalCoralCrimson.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = synthesisState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = { onStartListening() },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تلاش مجدد با میکروفون")
                        }
                    }
                }
                is AiSynthesisUiState.PreviewReady -> {
                    val response = synthesisState.response

                    if (!response.noticeMessage.isNullOrBlank()) {
                        FirebaseRequiredNoticeBanner(message = response.noticeMessage)
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Action confirmation for existing task completion ("این کار انجام شد")
                    if (response.intentKind == AiIntentKind.COMPLETE_TASK && response.targetExistingTaskToComplete != null) {
                        val target = response.targetExistingTaskToComplete
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, SoftEmeraldJade.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "تأیید تکمیل کار",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SoftEmeraldJade
                                )
                                Text(
                                    text = "«${target.title}» (${target.formatTime(usePersianDigits)})",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Button(
                                    onClick = { onConfirmCompleteTask(target) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftEmeraldJade),
                                    modifier = Modifier.fillMaxWidth().testTag("confirm_complete_existing_button")
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("بله، انجام شد", color = Color.White)
                                }
                            }
                        }
                    }

                    // Clarification banner if ambiguous
                    if (response.requiresClarification && !response.clarificationQuestion.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = WarmSolarAmber.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, WarmSolarAmber.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "نیاز به شفاف‌سازی",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = WarmSolarAmber
                                )
                                Text(
                                    text = response.clarificationQuestion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Proposed Tasks Review & Confirmation Cards
                    if (response.proposedTasks.isNotEmpty()) {
                        Text(
                            text = "پیش‌نمایش و تأیید پیش از ذخیره (${
                                PersianCalendarUtil.formatNumber(response.proposedTasks.size, usePersianDigits)
                            } مورد)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        response.proposedTasks.forEach { draft ->
                            ProposedTaskReviewCard(
                                draft = draft,
                                usePersianDigits = usePersianDigits,
                                onEditManually = { onEditDraftManually(draft.task) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = { onConfirmDrafts(response.proposedTasks) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_ai_tasks_button")
                        ) {
                            Text(
                                text = if (response.proposedTasks.size > 1) {
                                    "تأیید و ثبت همه (${PersianCalendarUtil.formatNumber(response.proposedTasks.size, usePersianDigits)} کار)"
                                } else if (response.intentKind == AiIntentKind.MODIFY_TASK) {
                                    "تأیید و اعمال تغییر زمان"
                                } else {
                                    "تأیید و ثبت در برنامه"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun ProposedTaskReviewCard(
    draft: ProposedTaskDraft,
    usePersianDigits: Boolean,
    onEditManually: () -> Unit
) {
    val task = draft.task
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            if (draft.conflictsWithExisting.isNotEmpty()) {
                WarmSolarAmber.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = draft.targetDate.formattedFull(usePersianDigits),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (draft.isTimeExplicit) task.formatTime(usePersianDigits) else "بدون ساعت ثابت",
                        style = TabularTimeStyle,
                        color = if (draft.isTimeExplicit) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Explicit vs inferred transparency badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مدت: ${task.formatDuration(usePersianDigits)}${if (draft.isDurationInferred) " (پیش‌فرض)" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "اولویت: ${task.priority.labelFa}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "ویرایش دستی",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onEditManually() }
                )
            }

            if (draft.conflictsWithExisting.isNotEmpty()) {
                val c = draft.conflictsWithExisting.first()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = WarmSolarAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "تداخل با «${c.firstTask.title}» (${c.firstTask.formatTime(usePersianDigits)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmSolarAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceWaveformIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "voiceWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )
    Canvas(modifier = modifier) {
        val barCount = 24
        val spacing = size.width / (barCount + 1)
        val centerY = size.height / 2f
        for (i in 1..barCount) {
            val x = spacing * i
            val amp = (0.28f + 0.62f * ((sin(phase + i * 0.6f) + 1f) / 2f)) * (size.height / 2f)
            drawLine(
                color = if (i % 2 == 0) LuminousCyan else ElectricViolet,
                start = Offset(x, centerY - amp),
                end = Offset(x, centerY + amp),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
