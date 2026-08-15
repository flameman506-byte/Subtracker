package com.subtracker.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SubTrackerPurple = Color(0xFF6C5CE7)
val SubTrackerBlue = Color(0xFF4C6FFF)
val SubTrackerBackground = Color(0xFF0B0F1A)
val SubTrackerSurface = Color(0xFF151B2C)
val SubTrackerSurfaceVariant = Color(0xFF1E2740)
val SubTrackerGreen = Color(0xFF1DB954)
val SubTrackerTextSecondary = Color(0xFF9CA3C0)

private val SubTrackerColorScheme = darkColorScheme(
    primary = SubTrackerPurple,
    secondary = SubTrackerBlue,
    background = SubTrackerBackground,
    surface = SubTrackerSurface,
    surfaceVariant = SubTrackerSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = SubTrackerTextSecondary,
    onPrimary = Color.White,
    onSecondary = Color.White
)

@Composable
fun SubTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SubTrackerColorScheme, content = content)
}
