package com.lockin.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.lockin.app.ui.theme.LockInPalette

/**
 * One 2dp accent stroke that travels across the top of the screen whenever the
 * destination changes. It is a load cue, not a spinner: it starts with the
 * transition, finishes in ~320ms, and never blocks a tap.
 */
@Composable
fun ScreenLoadSweep(trigger: Any?, modifier: Modifier = Modifier) {
    val travel = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val accent = LockInPalette.Cyan

    LaunchedEffect(trigger) {
        travel.snapTo(0f)
        alpha.snapTo(0.85f)
        travel.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        alpha.animateTo(0f, tween(140, easing = LinearOutSlowInEasing))
    }

    Canvas(modifier = modifier.fillMaxWidth().height(2.dp)) {
        val a = alpha.value
        if (a <= 0.01f) return@Canvas

        val segment = size.width * 0.34f
        val x = -segment + travel.value * (size.width + segment)

        drawRoundRect(
            color = accent.copy(alpha = a),
            topLeft = Offset(x, 0f),
            size = Size(segment, size.height),
            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
        )
    }
}
