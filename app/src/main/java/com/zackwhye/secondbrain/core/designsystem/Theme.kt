package com.zackwhye.secondbrain.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Placeholder only — default Material 3 color scheme. DESIGN.md's token set
 * (Color.kt, Type.kt, Shape.kt, Motion.kt) replaces this in Phase 1.
 */
@Composable
fun SecondBrainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colorScheme, content = content)
}
