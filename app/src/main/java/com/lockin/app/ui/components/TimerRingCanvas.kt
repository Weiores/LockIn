package com.lockin.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockin.app.ui.theme.AccentBlue
import com.lockin.app.ui.theme.AccentCyan
import com.lockin.app.ui.theme.AccentRed
import com.lockin.app.ui.theme.TextPrimary
import com.lockin.app.ui.theme.TextSecondary

@Composable
fun TimerRingCanvas(
    modifier: Modifier = Modifier,
    remainingMs: Long,
    totalMs: Long,
    isPaused: Boolean = false,
    isWarning: Boolean = false,
    size: Dp = 260.dp,
    strokeWidth: Dp = 12.dp,
) {
    val progress = if (totalMs > 0) {
        (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400),
        label = "timerProgress",
    )

    val minutes = (remainingMs / 1000) / 60
    val seconds = (remainingMs / 1000) % 60
    val formattedTime = "%02d:%02d".format(minutes, seconds)

    val activeColor = if (isWarning) AccentRed else AccentBlue
    val activeSecondaryColor = if (isWarning) AccentRed.copy(alpha = 0.7f) else AccentCyan

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.toPx() - strokePx
            val topLeft = Offset(strokePx / 2, strokePx / 2)
            val arcSize = Size(diameter, diameter)

            // Background track (subtle translucent ring)
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            // Active progress arc with vertical/sweep gradient
            val sweepAngle = animatedProgress * 360f
            if (sweepAngle > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            activeColor,
                            activeSecondaryColor,
                            activeColor,
                        ),
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }

        // Center display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = formattedTime,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
                color = TextPrimary,
                letterSpacing = (-0.5).sp,
            )

            Spacer(modifier = Modifier.height(4.dp))

            val statusLabel = when {
                isWarning -> "LIFT WARNING • 10s"
                isPaused -> "PAUSED"
                else -> "LOCKED IN"
            }

            val statusColor = when {
                isWarning -> AccentRed
                isPaused -> TextSecondary
                else -> AccentCyan
            }

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                color = statusColor,
            )
        }
    }
}
