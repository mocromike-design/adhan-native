package com.astronomy.quranicprayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MinimalistDarkColorScheme = darkColorScheme(
    primary = AccentSolar,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = BackgroundDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun QuranicPrayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MinimalistDarkColorScheme,
        content = content
    )
}
