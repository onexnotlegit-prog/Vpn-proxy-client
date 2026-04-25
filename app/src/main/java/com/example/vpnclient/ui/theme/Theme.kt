package com.example.vpnclient.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VpnColorScheme = darkColorScheme(
    background = TerminalBackground,
    surface = TerminalSurface,
    primary = TerminalAccent,
    onPrimary = TerminalBackground,
    onBackground = TerminalText,
    onSurface = TerminalText,
    error = TerminalError
)

@Composable
fun VpnProxyClientTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VpnColorScheme,
        content = content
    )
}
