// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package com.nexpay.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

data class NexPayAccentTheme(
    val primary: Color,
    val primaryDark: Color,
    val headerGradientStart: Color,
    val headerGradientEnd: Color,
    val accent: Color,
    val accentLight: Color
)

val BlueAccentTheme = NexPayAccentTheme(
    primary = Color(0xFF5B8DEF),
    primaryDark = Color(0xFF1976D2),
    headerGradientStart = Color(0xFF7BA8F5),
    headerGradientEnd = Color(0xFF6A96EE),
    accent = Color(0xFF1E88E5),
    accentLight = Color(0xFF42A5F5)
)

val LocalNexPayAccentTheme = compositionLocalOf { BlueAccentTheme }
