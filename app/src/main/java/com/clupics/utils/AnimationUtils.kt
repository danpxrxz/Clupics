package com.clupics.utils

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * MD3 motion tokens
 * https://m3.material.io/styles/motion/easing-and-duration/tokens-specs
 */
object Md3Motion {

    // MD3 Easing values
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerateEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerateEasing: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val StandardEasing: Easing = FastOutSlowInEasing
    val StandardDecelerateEasing: Easing = LinearOutSlowInEasing
    val StandardAccelerateEasing: Easing = FastOutLinearInEasing

    // MD3 Duration tokens
    const val Short1 = 50
    const val Short2 = 100
    const val Short3 = 150
    const val Short4 = 200
    const val Medium1 = 250
    const val Medium2 = 300
    const val Medium3 = 350
    const val Medium4 = 400
    const val Long1 = 450
    const val Long2 = 500
    const val Long3 = 550
    const val Long4 = 600
    const val ExtraLong1 = 700
    const val ExtraLong2 = 800
    const val ExtraLong3 = 900
    const val ExtraLong4 = 1000

    // Reusable tween specs
    fun <T> emphasizedTween(durationMs: Int = Medium2) = tween<T>(
        durationMillis = durationMs,
        easing = EmphasizedEasing
    )

    fun <T> standardTween(durationMs: Int = Medium1) = tween<T>(
        durationMillis = durationMs,
        easing = StandardEasing
    )

    fun <T> enterTween(durationMs: Int = Long1) = tween<T>(
        durationMillis = durationMs,
        easing = EmphasizedDecelerateEasing
    )

    fun <T> exitTween(durationMs: Int = Medium2) = tween<T>(
        durationMillis = durationMs,
        easing = EmphasizedAccelerateEasing
    )
}
