package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

val LocalUsePersianDigits = compositionLocalOf { true }

private val NocturneDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = TextPrimary,
    primaryContainer = IndigoGlow,
    onPrimaryContainer = PrimaryLavender,
    secondary = LuminousCyan,
    onSecondary = OnSecondaryDeep,
    secondaryContainer = SecondaryContainerCyan,
    onSecondaryContainer = TextPrimary,
    tertiary = WarmSolarAmber,
    onTertiary = OnTertiaryDeep,
    tertiaryContainer = TertiaryAmber,
    background = VoidBlack,
    onBackground = OnSurfaceLight,
    surface = SurfaceDim,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerHighest,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = SurfaceLowest,
    surfaceContainerLow = CardSlate,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = OutlineMuted,
    outlineVariant = OutlineVariantDark,
    error = SignalCoralCrimson,
    onError = TextPrimary,
    errorContainer = ErrorContainerDeep,
    onErrorContainer = ErrorSoft
)

private val NocturneLightColorScheme = lightColorScheme(
    primary = LightElectricViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF3B0764),
    secondary = LightCyanAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF0E7490),
    tertiary = WarmSolarAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurfaceLow,
    surfaceContainerHigh = LightSurfaceContainer,
    surfaceContainerHighest = LightSurfaceHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val NocturneShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun NocturneTheme(
    darkTheme: Boolean = true,
    usePersianDigits: Boolean = true,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalUsePersianDigits provides usePersianDigits
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) NocturneDarkColorScheme else NocturneLightColorScheme,
            typography = NocturneTypography,
            shapes = NocturneShapes,
            content = content
        )
    }
}
