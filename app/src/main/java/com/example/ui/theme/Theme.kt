package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberColorScheme = darkColorScheme(
    primary = NeonX,
    secondary = NeonO,
    tertiary = NeonGold,
    background = CyberBg,
    surface = CyberPanel,
    onPrimary = Color.White,
    onSecondary = CyberBg,
    onBackground = CyberText,
    onSurface = CyberText
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}
