package com.zackwhye.secondbrain.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** DESIGN.md's token set: Color, Type, Shape, Motion — applied together. */
@Composable
fun SecondBrainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) SecondBrainDarkColorScheme else SecondBrainLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SecondBrainTypography,
        shapes = SecondBrainShapes,
        content = content,
    )
}
