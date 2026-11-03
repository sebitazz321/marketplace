package com.optativa.marketplace.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F8B7D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEFE9),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFFF2762E),
    onSecondary = Color.White,
    tertiary = Color(0xFF6B4EFF),
    background = Color(0xFFF6F8F8),
    surface = Color.White,
    surfaceVariant = Color(0xFFE6EEEC)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5CD6C4),
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFCDEFE9),
    secondary = Color(0xFFFFB68A),
    onSecondary = Color(0xFF4A1F00),
    tertiary = Color(0xFFC6BFFF),
    background = Color(0xFF101514),
    surface = Color(0xFF1A2120),
    surfaceVariant = Color(0xFF2A3331)
)

@Composable
fun MarketplaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
