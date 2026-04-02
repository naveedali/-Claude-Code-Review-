package com.naveedali.claudecodereview.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ── Dark Color Scheme ─────────────────────────────────────────────────────────
private val AppDarkColorScheme = darkColorScheme(
    primary               = BrandPrimaryDark,           // rose-red #D07070
    onPrimary             = BrandOnPrimary,             // white
    primaryContainer      = BrandPrimaryContainer,      // deep-red #6B1212
    onPrimaryContainer    = BrandOnPrimaryContainer,    // warm #FFD6D6

    background            = AppBgDark,                  // #130808
    onBackground          = CodeTextDark,               // warm off-white

    surface               = SurfaceDark,                // #1E0E0E
    onSurface             = CodeTextDark,
    surfaceVariant        = SurfaceVariantDark,         // #2A1515
    onSurfaceVariant      = LineNumberTextDark,         // muted #8B5A5A

    outline               = DividerDark,                // #3D1A1A

    error                 = ErrorRed,
    onError               = OnErrorDark,
)

// ── Light Color Scheme ────────────────────────────────────────────────────────
private val AppLightColorScheme = lightColorScheme(
    primary               = BrandPrimary,               // crimson #8B1A1A
    onPrimary             = BrandOnPrimary,             // white
    primaryContainer      = BrandPrimaryContainerLight, // #FFE8EA
    onPrimaryContainer    = BrandOnPrimaryContainerLight, // dark crimson #5C0A0A

    background            = AppBgLight,                 // faint warm #F7F2F2
    onBackground          = CodeTextLight,

    surface               = SurfaceLight,               // white
    onSurface             = CodeTextLight,
    surfaceVariant        = SurfaceVariantLight,        // #F5EAEA
    onSurfaceVariant      = LineNumberTextLight,

    outline               = DividerLight,               // #EEDDDD

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
