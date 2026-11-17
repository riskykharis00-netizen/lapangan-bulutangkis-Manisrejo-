package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = CourtEmerald,
    onPrimary = Color.White,
    primaryContainer = CourtEmeraldContainer,
    onPrimaryContainer = OnCourtEmeraldContainer,
    secondary = ShuttleLime,
    onSecondary = OnShuttleLimeContainer,
    secondaryContainer = ShuttleLimeContainer,
    onSecondaryContainer = OnShuttleLimeContainer,
    tertiary = SlateNavy,
    onTertiary = Color.White,
    tertiaryContainer = SlateNavyContainer,
    onTertiaryContainer = SlateNavy,
    background = ArenaBackgroundLight,
    onBackground = ArenaOnSurfaceLight,
    surface = ArenaSurfaceLight,
    onSurface = ArenaOnSurfaceLight,
    surfaceVariant = ArenaSurfaceVariantLight,
    onSurfaceVariant = ArenaOnSurfaceVariantLight,
    outline = ArenaOutlineLight,
    error = StatusOverdueFg,
    onError = Color.White,
    errorContainer = StatusOverdueBg,
    onErrorContainer = StatusOverdueFg
)

private val DarkColorScheme = darkColorScheme(
    primary = CourtEmeraldLight,
    onPrimary = Color(0xFF002116),
    primaryContainer = CourtEmeraldDark,
    onPrimaryContainer = CourtEmeraldContainer,
    secondary = ShuttleLimeBright,
    onSecondary = OnShuttleLimeContainer,
    secondaryContainer = Color(0xFF2E4709),
    onSecondaryContainer = ShuttleLimeContainer,
    tertiary = Color(0xFF8AB4A8),
    onTertiary = Color(0xFF0A201A),
    background = ArenaBackgroundDark,
    onBackground = ArenaOnSurfaceDark,
    surface = ArenaSurfaceDark,
    onSurface = ArenaOnSurfaceDark,
    surfaceVariant = ArenaSurfaceVariantDark,
    onSurfaceVariant = ArenaOnSurfaceVariantDark,
    outline = ArenaOutlineDark,
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

val ShuttleShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ShuttleCourtTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ShuttleTypography,
        shapes = ShuttleShapes,
        content = content
    )
}
