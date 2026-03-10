package com.naveedali.claudecodereview.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ── Dark Color Scheme ─────────────────────────────────────────────────────────
private val AppDarkColorScheme = darkColorScheme(
    primary               = BrandPrimaryDark,
    onPrimary             = BrandOnPrimary,
    primaryContainer      = BrandPrimaryContainer,
    onPrimaryContainer    = BrandOnPrimary,

    background            = AppBgDark,
    onBackground          = CodeTextDark,

    surface               = SurfaceDark,
    onSurface             = CodeTextDark,
    surfaceVariant        = SurfaceVariantDark,
    onSurfaceVariant      = LineNumberTextDark,

    outline               = DividerDark,
    error                 = ErrorRed,
    onError               = OnErrorDark,
)

// ── Light Color Scheme ────────────────────────────────────────────────────────
private val AppLightColorScheme = lightColorScheme(
    primary               = BrandPrimary,
    onPrimary             = BrandOnPrimary,
    primaryContainer      = BrandPrimaryContainerLight,
    onPrimaryContainer    = BrandPrimary,

    background            = AppBgLight,
    onBackground          = CodeTextLight,

    surface               = SurfaceLight,
    onSurface             = CodeTextLight,
    surfaceVariant        = SurfaceVariantLight,
    onSurfaceVariant      = LineNumberTextLight,

    outline               = DividerLight,
    error                 = ErrorRed,
    onError               = OnErrorLight,
)

/**
 * App-wide theme wrapper.
 *
 * Usage:
 *   ClaudeCodeReviewTheme(darkTheme = isDark) { ... }
 *
 * [darkTheme] defaults to the system setting so previews and the real device
 * both behave correctly without extra plumbing.
 */
@Composable
fun ClaudeCodeReviewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
