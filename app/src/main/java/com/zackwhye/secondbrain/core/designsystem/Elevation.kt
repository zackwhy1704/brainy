package com.zackwhye.secondbrain.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// DESIGN.md → elevation. Material 3 tonal elevation, 3 tiers only (values
// verified against androidx.compose.material3.tokens.ElevationTokens: Level0
// = 0dp, Level1 = 1dp, Level3 = 6dp — Level2/4/5 exist in Material 3 but are
// not used by this design system).

val ElevationLevel0: Dp = 0.dp // screen background, list rows resting state
val ElevationLevel1: Dp = 1.dp // resting cards — surface + tonal overlay, no drop shadow in dark theme
val ElevationLevel3: Dp = 6.dp // bottom sheet / modal

val ScrimLight: Color = Color.Black.copy(alpha = 0.48f)
val ScrimDark: Color = Color.Black.copy(alpha = 0.60f)
