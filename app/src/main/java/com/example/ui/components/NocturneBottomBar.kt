package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.IndigoGlow
import com.example.ui.theme.LuminousCyan
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

const val ROUTE_TODAY = "today"
const val ROUTE_CALENDAR = "calendar"
const val ROUTE_INSIGHTS = "insights"
const val ROUTE_SETTINGS = "settings"

data class NocturneNavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

val nocturneNavItems = listOf(
    NocturneNavItem(ROUTE_TODAY, R.string.nav_today, Icons.Filled.Today, Icons.Outlined.Today, "nav_today"),
    NocturneNavItem(ROUTE_CALENDAR, R.string.nav_calendar, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_calendar"),
    NocturneNavItem(ROUTE_INSIGHTS, R.string.nav_insights, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_insights"),
    NocturneNavItem(ROUTE_SETTINGS, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun NocturneDockedBottomBar(
    currentRoute: String?,
    isVoiceActive: Boolean,
    hapticsEnabled: Boolean,
    onNavigate: (String) -> Unit,
    onVoiceFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val barSurfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    val selectedPillBg = MaterialTheme.colorScheme.surfaceContainerLow

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val center = w / 2f
                val cutoutRadius = 42.dp.toPx()
                val notchDepth = 26.dp.toPx()

                val barPath = Path().apply {
                    moveTo(0f, 16.dp.toPx())
                    quadraticTo(0f, 0f, 16.dp.toPx(), 0f)
                    lineTo(center - cutoutRadius - 14.dp.toPx(), 0f)
                    cubicTo(
                        center - cutoutRadius + 4.dp.toPx(), 0f,
                        center - cutoutRadius + 6.dp.toPx(), notchDepth,
                        center, notchDepth
                    )
                    cubicTo(
                        center + cutoutRadius - 6.dp.toPx(), notchDepth,
                        center + cutoutRadius - 4.dp.toPx(), 0f,
                        center + cutoutRadius + 14.dp.toPx(), 0f
                    )
                    lineTo(w - 16.dp.toPx(), 0f)
                    quadraticTo(w, 0f, w, 16.dp.toPx())
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }

                drawPath(path = barPath, color = barSurfaceColor)

                val borderPath = Path().apply {
                    moveTo(0f, 16.dp.toPx())
                    quadraticTo(0f, 0f, 16.dp.toPx(), 0f)
                    lineTo(center - cutoutRadius - 14.dp.toPx(), 0f)
                    cubicTo(
                        center - cutoutRadius + 4.dp.toPx(), 0f,
                        center - cutoutRadius + 6.dp.toPx(), notchDepth,
                        center, notchDepth
                    )
                    cubicTo(
                        center + cutoutRadius - 6.dp.toPx(), notchDepth,
                        center + cutoutRadius - 4.dp.toPx(), 0f,
                        center + cutoutRadius + 14.dp.toPx(), 0f
                    )
                    lineTo(w - 16.dp.toPx(), 0f)
                    quadraticTo(w, 0f, w, 16.dp.toPx())
                }

                drawPath(
                    path = borderPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ElectricViolet.copy(alpha = 0.35f),
                            LuminousCyan.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    ),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    nocturneNavItems.take(2).forEach { item ->
                        BottomNavTab(
                            item = item,
                            selected = currentRoute == item.route,
                            selectedPillBg = selectedPillBg,
                            onClick = {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onNavigate(item.route)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(84.dp))

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    nocturneNavItems.drop(2).forEach { item ->
                        BottomNavTab(
                            item = item,
                            selected = currentRoute == item.route,
                            selectedPillBg = selectedPillBg,
                            onClick = {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onNavigate(item.route)
                            }
                        )
                    }
                }
            }
        }

        FloatingVoiceFab(
            isActive = isVoiceActive,
            onClick = {
                if (hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onVoiceFabClick()
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-24).dp)
        )
    }
}

@Composable
private fun BottomNavTab(
    item: NocturneNavItem,
    selected: Boolean,
    selectedPillBg: Color,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tintColor by animateColorAsState(
        targetValue = if (selected) primaryColor else inactiveColor,
        animationSpec = tween(200),
        label = "navTint"
    )
    val label = stringResource(item.labelRes)
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = primaryColor),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(item.testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (selected) selectedPillBg else Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = label,
                tint = tintColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else inactiveColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FloatingVoiceFab(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voiceFabPulse")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isActive) 1200 else 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    val cd = stringResource(R.string.cd_voice_fab)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(88.dp)
            .semantics { contentDescription = cd },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseR = 34.dp.toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (isActive) LuminousCyan else IndigoGlow).copy(alpha = if (isActive) 0.35f else 0.22f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseR * pulseRadius * 1.2f
                ),
                radius = baseR * pulseRadius * 1.2f,
                center = center
            )

            if (isActive) {
                val wavePath = Path()
                val steps = 72
                for (i in 0..steps) {
                    val angle = (i.toFloat() / steps) * (2f * PI.toFloat())
                    val offsetR = baseR + 4.dp.toPx() + 3.dp.toPx() * sin(6f * angle + phase)
                    val x = center.x + offsetR * cos(angle)
                    val y = center.y + offsetR * sin(angle)
                    if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                }
                wavePath.close()
                drawPath(
                    path = wavePath,
                    color = LuminousCyan.copy(alpha = 0.85f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = IndigoGlow,
                    spotColor = if (isActive) LuminousCyan else ElectricViolet
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isActive) {
                            listOf(ElectricViolet, LuminousCyan)
                        } else {
                            listOf(ElectricViolet, IndigoGlow)
                        }
                    )
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = Color.White),
                    onClick = onClick
                )
                .testTag("voice_fab"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = cd,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
