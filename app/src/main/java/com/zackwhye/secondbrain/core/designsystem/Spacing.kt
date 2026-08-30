package com.zackwhye.secondbrain.core.designsystem

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// DESIGN.md → spacing. 4dp base unit, 4/8/12/16/24/32 increments. Named by
// relative size (the scale is generic, reused across not-yet-known layouts)
// plus the three specific semantic values DESIGN.md calls out by use.

val SpacingXs: Dp = 4.dp
val SpacingSm: Dp = 8.dp
val SpacingMd: Dp = 12.dp
val SpacingLg: Dp = 16.dp
val SpacingXl: Dp = 24.dp
val SpacingXxl: Dp = 32.dp

val CardPadding: Dp = SpacingLg // 16dp
val CardGap: Dp = SpacingMd // 12dp
val ScreenHorizontalMargin: Dp = SpacingLg // 16dp
