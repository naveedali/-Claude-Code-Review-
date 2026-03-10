package com.naveedali.claudecodereview.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Material 3 Typography ─────────────────────────────────────────────────────
val Typography = Typography(
    // App bar / screen titles
    titleLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 20.sp,
        lineHeight   = 28.sp,
        letterSpacing = 0.sp
    ),
    // Section headings inside panels
    titleMedium = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Medium,
        fontSize     = 15.sp,
        lineHeight   = 22.sp,
        letterSpacing = 0.sp
    ),
    // Card titles / issue titles
    titleSmall = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 13.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.sp
    ),
    // General body text (descriptions, messages)
    bodyLarge = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Normal,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Normal,
        fontSize     = 13.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.1.sp
    ),
    // Severity badge labels, chip text
    labelMedium = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Medium,
        fontSize     = 11.sp,
        lineHeight   = 14.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily   = FontFamily.Default,
        fontWeight   = FontWeight.Medium,
        fontSize     = 10.sp,
        lineHeight   = 12.sp,
        letterSpacing = 0.4.sp
    ),
)

// ── Code Editor Text Style ────────────────────────────────────────────────────
// Used by CodeEditorPanel – monospace, easy-to-read size for code
val CodeEditorTextStyle = TextStyle(
    fontFamily   = FontFamily.Monospace,
    fontWeight   = FontWeight.Normal,
    fontSize     = 13.sp,
    lineHeight   = 20.sp,
    letterSpacing = 0.sp
)

// Slightly dimmer monospace style for line numbers
val LineNumberTextStyle = TextStyle(
    fontFamily   = FontFamily.Monospace,
    fontWeight   = FontWeight.Normal,
    fontSize     = 11.sp,
    lineHeight   = 20.sp,   // Must match CodeEditorTextStyle.lineHeight
    letterSpacing = 0.sp
)

// Monospace style for before/after code snippets in optimization cards
val CodeSnippetTextStyle = TextStyle(
    fontFamily   = FontFamily.Monospace,
    fontWeight   = FontWeight.Normal,
    fontSize     = 11.sp,
    lineHeight   = 16.sp,
    letterSpacing = 0.sp
)
