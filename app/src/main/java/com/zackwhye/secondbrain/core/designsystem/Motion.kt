package com.zackwhye.secondbrain.core.designsystem

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

// DESIGN.md → motion. 150-250ms, ease-out on enter / ease-in on exit, exit at
// ~65% of enter duration. Named here so feature code never hand-rolls a
// duration or easing curve.

const val MotionEnterDurationMs: Int = 200
val MotionExitDurationMs: Int = (MotionEnterDurationMs * 0.65f).toInt() // ~130ms

val MotionEnterEasing: Easing = LinearOutSlowInEasing
val MotionExitEasing: Easing = FastOutLinearInEasing

/** Card/row press feedback — restore to 1f on release. */
const val PressScale: Float = 0.98f
