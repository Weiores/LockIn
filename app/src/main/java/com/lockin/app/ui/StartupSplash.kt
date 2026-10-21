package com.lockin.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockin.app.ui.components.LockInAmbientBackground
import com.lockin.app.ui.theme.GlassType
import com.lockin.app.ui.theme.LockInPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The colour the window paints before Compose draws anything. It matches
 * `windowSplashScreenBackground`, so the system splash and this overlay are the
 * same flat frame — no flash between them.
 */
val SplashBaseColor = Color(0xFF0B1526)

/**
 * Startup screen: the padlock closes, then the wordmark arrives.
 *
 * The shackle starts lifted clear of the body and drops into it on a spring —
 * a small overshoot so it reads as a click rather than a slide. The body is
 * drawn *after* the shackle, so the descending legs disappear behind it and the
 * lock looks shut. Total runtime is ~1s; the hub is already composed underneath,
 * so the fade reveals a finished screen rather than a half-built one.
 */
@Composable
fun StartupSplash(onFinished: () -> Unit) {
    val overlay = remember { Animatable(1f) }
    val wash = remember { Animatable(0f) }
    val mark = remember { Animatable(0f) }
    val hook = remember { Animatable(0f) }
    val fill = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { wash.animateTo(1f, tween(180, easing = LinearOutSlowInEasing)) }
        launch { mark.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }
        launch {
            delay(140)
            hook.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
        launch {
            delay(320)
            fill.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }
        delay(840)
        overlay.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlay.value }
            // Swallow taps for the second it is up.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(SplashBaseColor))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = wash.value },
        ) {
            LockInAmbientBackground()
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = mark.value
                    translationY = (1f - mark.value) * 14.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LockMark(hookProgress = hook.value)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "LockIn",
                style = GlassType.Display,
                fontSize = 42.sp,
                letterSpacing = (-1.4).sp,
                color = LockInPalette.HeaderInk,
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LockInPalette.HeaderInk.copy(alpha = 0.18f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fill.value)
                        .clip(RoundedCornerShape(2.dp))
                        .background(LockInPalette.HeaderInk),
                )
            }
        }
    }
}

/**
 * The launcher mark, drawn from the same geometry as
 * `res/drawable/ic_launcher_foreground.xml` (108-unit grid, content x 36..72,
 * y 30..78) so the icon and the splash can never drift apart.
 *
 * @param hookProgress 0 = shackle fully lifted, 1 = seated in the body.
 */
@Composable
private fun LockMark(
    hookProgress: Float,
    modifier: Modifier = Modifier,
) {
    val bodyColor = LockInPalette.HeaderInk
    val accent = LockInPalette.Cyan

    // The box is the *seated* lock only (x 36..72, y 30..78). The raised
    // shackle deliberately draws outside it — Canvas does not clip, so the hook
    // gets its headroom without reserving empty space once it lands.
    val markHeight = 96.dp
    val markWidth = markHeight * (36f / 48f)

    Canvas(modifier = modifier.size(width = markWidth, height = markHeight)) {
        val unit = size.height / 48f
        val lift = (1f - hookProgress.coerceIn(0f, 1.15f)) * 14f * unit

        fun x(v: Float) = (v - 36f) * unit
        fun y(v: Float) = (v - 30f) * unit

        // Shackle first — the body covers the legs as they come down.
        val shackle = Path().apply {
            moveTo(x(42f), y(52f) - lift)
            lineTo(x(42f), y(45.25f) - lift)
            arcTo(
                rect = Rect(x(42f), y(33.25f) - lift, x(66f), y(57.25f) - lift),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false,
            )
            lineTo(x(66f), y(52f) - lift)
        }
        drawPath(
            path = shackle,
            color = accent,
            style = Stroke(width = 6.5f * unit, cap = StrokeCap.Round),
        )

        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(x(36f), y(52f)),
            size = Size(36f * unit, 26f * unit),
            cornerRadius = CornerRadius(7.5f * unit, 7.5f * unit),
        )

        drawCircle(color = accent, radius = 3.6f * unit, center = Offset(x(54f), y(61f)))
        drawRoundRect(
            color = accent,
            topLeft = Offset(x(52.4f), y(61f)),
            size = Size(3.2f * unit, 7.4f * unit),
            cornerRadius = CornerRadius(1.6f * unit, 1.6f * unit),
        )
    }
}
