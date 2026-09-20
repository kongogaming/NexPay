// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Flowpay

package com.nexpay.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────
// NexPay design tokens — dark theme.
//
// Every Compose screen draws from these; inline Color(0x…) literals outside
// this package are a CI failure (see the palette-gate step in build.yml).
// The ramp consolidates greys into one value per visual role, so "card grey"
// or "secondary text" can be changed in exactly one place.
// ─────────────────────────────────────────────────────────────────────────

// Surfaces (darkest → lightest)
val NexPayBlack = Color(0xFF000000)

/** Screen background behind cards/lists. */
val NexPaySurfaceDim = Color(0xFF0A0A0A)

/** Card / dialog surface. */
val NexPayDarkGray = Color(0xFF1A1A1A)

/** Elevated surface: input fields, chips, avatars. */
val NexPayMediumGray = Color(0xFF2A2A2A)

/** Borders, dividers, inactive track. */
val NexPayLightGray = Color(0xFF333333)

/** Stronger outline / disabled container. */
val NexPayOutlineGray = Color(0xFF4A4A4A)

/** Disabled content / faint hint. */
val NexPayDisabledGray = Color(0xFF555555)

// Text (dimmest → brightest)
/** Placeholder / hint text. */
val NexPayTextGray = Color(0xFF666666)

/** Secondary text: captions, labels, timestamps. */
val NexPayTextLightGray = Color(0xFF888888)

/** Long-form body text on dark dialogs. */
val NexPayTextPale = Color(0xFFCCCCCC)

val NexPayTextWhite = Color(0xFFFFFFFF)

// Card Colors (light card variant)
val NexPayCardBackground = Color(0xFFE8E8E8)
val NexPayCardText = Color(0xFF000000)
val NexPayCardSubtext = Color(0xFF4A4A4A)

// Light Theme Design Tokens
val NexPayLightBackground = Color(0xFFF6F8FA)
val NexPayLightSurface = Color(0xFFFFFFFF)
val NexPayLightSurfaceElevated = Color(0xFFF0F2F5)
val NexPayLightBorder = Color(0xFFE1E4E8)
val NexPayLightTextPrimary = Color(0xFF1F2328)
val NexPayLightTextSecondary = Color(0xFF656D76)
val NexPayLightTextTertiary = Color(0xFF8C959F)

// Accents
val NexPayAccentBlue = Color(0xFF1E88E5)
val NexPayAccentGreen = Color(0xFF00C853)

/** Bright green used as the light end of success gradients. */
val NexPayAccentGreenBright = Color(0xFF43E97B)

// ─────────────────────────────────────────────────────────────────────────
// Transaction status palette. One color per outcome, used identically in
// the history list, detail dialog and result screen so a status never
// changes meaning between screens.
// ─────────────────────────────────────────────────────────────────────────

/** SUCCESS — bank confirmed. */
val NexPayStatusSuccess = NexPayAccentGreen

/** FAILED / declined, and destructive actions (delete, clear). */
val NexPayStatusError = Color(0xFFF44336)

/** NEEDS_REVIEW / PENDING — user attention required. */
val NexPayStatusWarning = Color(0xFFFF9800)

/** UNVERIFIED / CANCELLED — outcome unknown or nothing happened. Neutral:
 *  deliberately neither success-green nor failure-red. */
val NexPayStatusNeutral = Color(0xFF9E9E9E)

/**
 * The single mapping from a [com.nexpay.app.data.TransactionStatus] string
 * to its display color.
 */
fun statusColor(status: String): Color = when (status.uppercase()) {
    "SUCCESS", "SUCCESSFUL", "COMPLETED" -> NexPayStatusSuccess
    "FAILED", "DECLINED" -> NexPayStatusError
    "PENDING", "NEEDS_REVIEW" -> NexPayStatusWarning
    else -> NexPayStatusNeutral // UNVERIFIED, CANCELLED, unknown
}
