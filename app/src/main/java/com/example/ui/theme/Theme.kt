package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ApexDarkColorScheme = darkColorScheme(
    primary = RacingCrimson,
    onPrimary = TextPrimary,
    primaryContainer = RacingCrimsonDark,
    onPrimaryContainer = TextPrimary,
    secondary = NitroCyan,
    onSecondary = CarbonBlack,
    secondaryContainer = PitSurfaceElevated,
    onSecondaryContainer = NitroCyanBright,
    tertiary = ChampionshipAmber,
    onTertiary = CarbonBlack,
    background = CarbonBlack,
    onBackground = TextPrimary,
    surface = AsphaltNavy,
    onSurface = TextPrimary,
    surfaceVariant = PitSurface,
    onSurfaceVariant = TextSecondary,
    outline = SubtleBorder,
    error = DangerRed,
    onError = TextPrimary
)

val ApexShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun ApexCityRushTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ApexDarkColorScheme,
        typography = ApexTypography,
        shapes = ApexShapes,
        content = content
    )
}
