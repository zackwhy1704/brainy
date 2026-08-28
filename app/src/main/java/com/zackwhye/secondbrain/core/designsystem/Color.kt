package com.zackwhye.secondbrain.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// DESIGN.md → Color roles (Material 3 mapping). Named per-role, not inlined,
// so feature code never reaches for a raw hex.

private val PrimaryLight = Color(0xFF6366F1)
private val OnPrimaryLight = Color(0xFFFFFFFF)
private val PrimaryContainerLight = Color(0xFFE0E7FF)
private val OnPrimaryContainerLight = Color(0xFF1E1B4B)
private val SecondaryLight = Color(0xFF059669)
private val OnSecondaryLight = Color(0xFFFFFFFF)
private val BackgroundLight = Color(0xFFFAFAFC)
private val OnBackgroundLight = Color(0xFF1E1B4B)
private val SurfaceLight = Color(0xFFFFFFFF)
private val OnSurfaceLight = Color(0xFF1E1B4B)
private val SurfaceVariantLight = Color(0xFFEBEFF9)
private val OnSurfaceVariantLight = Color(0xFF64748B)
private val OutlineLight = Color(0xFFE0E7FF)
private val ErrorLight = Color(0xFFDC2626)
private val OnErrorLight = Color(0xFFFFFFFF)

private val PrimaryDark = Color(0xFFB4B8FF)
private val OnPrimaryDark = Color(0xFF1E1B4B)
private val PrimaryContainerDark = Color(0xFF3730A3)
private val OnPrimaryContainerDark = Color(0xFFE0E7FF)
private val SecondaryDark = Color(0xFF4ADE80)
private val OnSecondaryDark = Color(0xFF0B2A1C)
private val BackgroundDark = Color(0xFF131320)
private val OnBackgroundDark = Color(0xFFE7E7F5)
private val SurfaceDark = Color(0xFF1C1B2E)
private val OnSurfaceDark = Color(0xFFE7E7F5)
private val SurfaceVariantDark = Color(0xFF272538)
private val OnSurfaceVariantDark = Color(0xFF9A99B8)
private val OutlineDark = Color(0xFF38364F)
private val ErrorDark = Color(0xFFF87171)
private val OnErrorDark = Color(0xFF450A0A)

/** Brief-importance accent — chips/badges only, always paired with text/icon, never the sole signal. */
val AmberImportanceLight = Color(0xFFD97706)
val AmberImportanceDark = Color(0xFFFBBF24)

val SecondBrainLightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    error = ErrorLight,
    onError = OnErrorLight,
)

val SecondBrainDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    error = ErrorDark,
    onError = OnErrorDark,
)
