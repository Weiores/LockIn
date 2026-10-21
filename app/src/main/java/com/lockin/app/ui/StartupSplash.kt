package com.lockin.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * Startup screen: the wordmark on the same wash the hub uses, with a single
 * line that fills once. It never blocks longer than ~600ms — the hub is already
 * composed underneath, so the fade reveals a finished screen rather than a
 * half-built one.
 */
@Composable
fun StartupSplash(onFinished: () -> Unit) {
    val overlay = remember { Animatable(1f) }
    val wash = remember { Animatable(0f) }
    val mark = remember { Animatable(0f) }
    val fill = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { wash.animateTo(1f, tween(200, easing = LinearOutSlowInEasing)) }
        launch { mark.animateTo(1f, tween(300, easing = FastOutSlowInEasing)) }
        delay(120)
        launch { fill.animateTo(1f, tween(360, easing = FastOutSlowInEasing)) }
        delay(500)
        overlay.animateTo(0f, tween(240, easing = FastOutSlowInEasing))
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
