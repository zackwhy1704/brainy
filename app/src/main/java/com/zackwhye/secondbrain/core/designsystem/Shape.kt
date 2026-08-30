package com.zackwhye.secondbrain.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// DESIGN.md → shape. Named per component so feature code never inlines a
// corner radius; Shapes below covers the generic Material 3 slots.

val CardShape = RoundedCornerShape(20.dp)
val ChipShape = RoundedCornerShape(percent = 50) // pill
val ButtonShape = RoundedCornerShape(14.dp)
val BottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val TextInputShape = RoundedCornerShape(16.dp)

val SecondBrainShapes = Shapes(
    extraSmall = ChipShape,
    small = TextInputShape,
    medium = CardShape,
    large = ButtonShape,
    extraLarge = BottomSheetShape,
)
