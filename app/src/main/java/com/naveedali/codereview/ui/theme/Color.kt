package com.naveedali.codereview.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand / Primary  (derived from the app-icon crimson palette) ───────────────
//
//  Icon gradient:  #8B1A1A (centre) → #5C0A0A (mid) → #0B0000 (edge)
//
//  Light-theme primary — deep crimson, legible on white surfaces
val BrandPrimary              = Color(0xFF8B1A1A)
//  Dark-theme primary — lighter rose-red so it pops on dark backgrounds
val BrandPrimaryDark          = Color(0xFFD07070)
//  Container tints
val BrandPrimaryContainer     = Color(0xFF6B1212)          // dark-mode container
val BrandPrimaryContainerLight = Color(0xFFFFE8EA)         // light-mode container
//  Text/icon colour on top of a primary-coloured surface (always white)
val BrandOnPrimary            = Color(0xFFFFFFFF)
//  Text on primaryContainer
val BrandOnPrimaryContainer   = Color(0xFFFFD6D6)          // dark-mode
val BrandOnPrimaryContainerLight = Color(0xFF5C0A0A)       // light-mode

// ── App Surfaces  (warm near-black — echoes the icon's dark background) ───────
val AppBgDark                 = Color(0xFF130808)   // deepest background
val AppBgLight                = Color(0xFFF7F2F2)   // very faint warm tint

val SurfaceDark               = Color(0xFF1E0E0E)   // card / sheet surface
val SurfaceLight              = Color(0xFFFFFFFF)

val SurfaceVariantDark        = Color(0xFF2A1515)   // input fields, chips
val SurfaceVariantLight       = Color(0xFFF5EAEA)

val DividerDark               = Color(0xFF3D1A1A)
val DividerLight              = Color(0xFFEEDDDD)

// ── Code Editor ───────────────────────────────────────────────────────────────
//   Dark theme: near-black with warm undertone; Light: clean neutral
val EditorBgDark              = Color(0xFF180E0E)
val EditorBgLight             = Color(0xFFFAFAFA)

val LineNumberBgDark          = Color(0xFF130808)
val LineNumberBgLight         = Color(0xFFEFF0F0)

val LineNumberTextDark        = Color(0xFF8B5A5A)
val LineNumberTextLight       = Color(0xFFAAAAAA)

val CodeTextDark              = Color(0xFFE8D5D5)   // warm off-white
val CodeTextLight             = Color(0xFF2D2D2D)

val CursorColorDark           = Color(0xFFD07070)   // matches primary dark
val CursorColorLight          = Color(0xFF8B1A1A)

val EditorSelectionDark       = Color(0xFF4A2020)
val EditorSelectionLight      = Color(0xFFFFD6D6)

// ── Issue Severity ────────────────────────────────────────────────────────────
val ErrorRed                  = Color(0xFFFF5555)
val ErrorContainerDark        = Color(0xFF3D1515)
val ErrorContainerLight       = Color(0xFFFFEBEB)
val OnErrorDark               = Color(0xFFFFADAD)
val OnErrorLight              = Color(0xFF8B0000)

val WarningOrange             = Color(0xFFFFB86C)
val WarningContainerDark      = Color(0xFF3D2800)
val WarningContainerLight     = Color(0xFFFFF3E0)
val OnWarningDark             = Color(0xFFFFD49A)
val OnWarningLight            = Color(0xFF7A4000)

val InfoCyan                  = Color(0xFF8BE9FD)
val InfoContainerDark         = Color(0xFF0D2F36)
val InfoContainerLight        = Color(0xFFE3F4FB)
val OnInfoDark                = Color(0xFFB8F2FF)
val OnInfoLight               = Color(0xFF0A4F5E)

// ── Optimization Type Chips ───────────────────────────────────────────────────
val OptPerfPink               = Color(0xFFFF79C6)   // Performance
val OptReadCyan               = Color(0xFF8BE9FD)   // Readability
val OptMemGreen               = Color(0xFF50FA7B)   // Memory
val OptBestPurple             = Color(0xFFBD93F9)   // Best Practice

// ── General Status ────────────────────────────────────────────────────────────
val SuccessGreen              = Color(0xFF50FA7B)
val SuccessContainerDark      = Color(0xFF0A2E18)
val SuccessContainerLight     = Color(0xFFE6F9EE)

// ── Top Bar ───────────────────────────────────────────────────────────────────
val TopBarDark                = Color(0xFF0D0505)   // deepest crimson-black
val TopBarLight               = Color(0xFF8B1A1A)   // primary crimson (tinted bar)
