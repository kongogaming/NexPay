// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package com.nexpay.app.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nexpay.app.constants.AppConstants
import com.nexpay.app.utils.findComponentActivity

data class NexPayColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color
)

val LocalNexPayColors = staticCompositionLocalOf {
    NexPayColors(
        isDark = true,
        background = NexPaySurfaceDim,
        surface = NexPayDarkGray,
        surfaceElevated = NexPayMediumGray,
        border = NexPayLightGray,
        textPrimary = NexPayTextWhite,
        textSecondary = NexPayTextLightGray,
        textTertiary = NexPayTextGray
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = NexPayTextWhite,
    secondary = NexPayAccentBlue,
    tertiary = NexPayAccentGreen,
    background = NexPayBlack,
    surface = NexPayDarkGray,
    onBackground = NexPayTextWhite,
    onSurface = NexPayTextWhite
)

private val LightColorScheme = lightColorScheme(
    primary = NexPayAccentBlue,
    secondary = NexPayAccentBlue,
    tertiary = NexPayAccentGreen,
    background = NexPayLightBackground,
    surface = NexPayLightSurface,
    onPrimary = NexPayTextWhite,
    onSecondary = NexPayTextWhite,
    onTertiary = NexPayTextWhite,
    onBackground = NexPayLightTextPrimary,
    onSurface = NexPayLightTextPrimary
)

fun getSavedThemeMode(context: Context): String {
    val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(AppConstants.KEY_THEME_MODE, AppConstants.THEME_MODE_SYSTEM)
        ?: AppConstants.THEME_MODE_SYSTEM
}

fun saveThemeMode(context: Context, mode: String) {
    val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(AppConstants.KEY_THEME_MODE, mode).apply()
}

@Composable
fun isAppInDarkTheme(themeMode: String = getSavedThemeMode(LocalContext.current)): Boolean {
    return when (themeMode) {
        AppConstants.THEME_MODE_LIGHT -> false
        AppConstants.THEME_MODE_DARK -> true
        else -> isSystemInDarkTheme()
    }
}

fun isAppInDarkTheme(themeMode: String, configuration: Configuration): Boolean {
    return when (themeMode) {
        AppConstants.THEME_MODE_LIGHT -> false
        AppConstants.THEME_MODE_DARK -> true
        else -> (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}

/**
 * NexPay theme supporting System Default, Light, and Dark modes.
 * Dynamic (Material You) color is intentionally NOT used — on Android 12+ it silently
 * replaced the brand palette with wallpaper-derived colors.
 */
@Composable
fun NexPayTheme(
    themeMode: String = getSavedThemeMode(LocalContext.current),
    content: @Composable () -> Unit
) {
    val darkTheme = isAppInDarkTheme(themeMode)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findComponentActivity()?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            if (darkTheme) {
                window.statusBarColor = android.graphics.Color.BLACK
                window.navigationBarColor = android.graphics.Color.BLACK
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            } else {
                window.statusBarColor = android.graphics.Color.WHITE
                window.navigationBarColor = android.graphics.Color.WHITE
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    val colors = if (darkTheme) {
        NexPayColors(
            isDark = true,
            background = NexPaySurfaceDim,
            surface = NexPayDarkGray,
            surfaceElevated = NexPayMediumGray,
            border = NexPayLightGray,
            textPrimary = NexPayTextWhite,
            textSecondary = NexPayTextLightGray,
            textTertiary = NexPayTextGray
        )
    } else {
        NexPayColors(
            isDark = false,
            background = NexPayLightBackground,
            surface = NexPayLightSurface,
            surfaceElevated = NexPayLightSurfaceElevated,
            border = NexPayLightBorder,
            textPrimary = NexPayLightTextPrimary,
            textSecondary = NexPayLightTextSecondary,
            textTertiary = NexPayLightTextTertiary
        )
    }

    CompositionLocalProvider(LocalNexPayColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
