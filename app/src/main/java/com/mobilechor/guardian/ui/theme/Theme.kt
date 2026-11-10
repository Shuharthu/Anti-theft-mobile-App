package com.mobilechor.guardian.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = CleanSurface,
    primaryContainer = RoyalBlueLight,
    onPrimaryContainer = RoyalBlueDark,
    secondary = IconOrangeTint,
    onSecondary = CleanSurface,
    secondaryContainer = IconOrangeBg,
    onSecondaryContainer = IconOrangeTint,
    background = CleanBg,
    onBackground = TextPrimary,
    surface = CleanSurface,
    onSurface = TextPrimary,
    surfaceVariant = CleanSurface,
    onSurfaceVariant = TextSecondary,
    error = CyberRed,
    onError = CleanSurface
)

@Composable
fun MobileChorTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.statusBarColor = CleanBg.toArgb()
            window?.navigationBarColor = CleanSurface.toArgb()
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun cleanTextFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    disabledTextColor = TextPrimary,
    errorTextColor = CyberRed,
    focusedContainerColor = CleanSurface,
    unfocusedContainerColor = CleanSurface,
    disabledContainerColor = CleanSurface,
    errorContainerColor = CleanSurface,
    cursorColor = RoyalBlue,
    errorCursorColor = CyberRed,
    focusedBorderColor = RoyalBlue,
    unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFF94A3B8),
    disabledBorderColor = androidx.compose.ui.graphics.Color(0xFFCBD5E1),
    errorBorderColor = CyberRed,
    focusedLabelColor = RoyalBlue,
    unfocusedLabelColor = androidx.compose.ui.graphics.Color(0xFF334155),
    disabledLabelColor = androidx.compose.ui.graphics.Color(0xFF334155),
    errorLabelColor = CyberRed,
    focusedPlaceholderColor = androidx.compose.ui.graphics.Color(0xFF64748B),
    unfocusedPlaceholderColor = androidx.compose.ui.graphics.Color(0xFF64748B),
    disabledPlaceholderColor = androidx.compose.ui.graphics.Color(0xFF64748B),
    errorPlaceholderColor = androidx.compose.ui.graphics.Color(0xFF64748B)
)
