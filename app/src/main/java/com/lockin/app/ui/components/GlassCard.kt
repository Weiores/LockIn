package com.lockin.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lockin.app.ui.theme.AppleBlue
import com.lockin.app.ui.theme.LocalDarkTheme

/**
 * Authentic Apple Glassmorphic Card.
 * Clean frosted translucent surface without artificial multi-color gradients or harsh borders.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    contentPadding: Dp = 18.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = LocalDarkTheme.current
    val backgroundColor = if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.65f)
    } else {
        Color.White.copy(alpha = 0.80f)
    }

    val shadowColor = if (isDark) {
        Color.Black.copy(alpha = 0.35f)
    } else {
        Color.Black.copy(alpha = 0.04f)
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 6.dp else 4.dp,
                shape = shape,
                spotColor = shadowColor,
                ambientColor = shadowColor,
            )
            .clip(shape)
            .background(backgroundColor),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

/**
 * Interactive Apple Glassmorphic Button with tactile spring feedback.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "glassButtonScale",
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                spotColor = containerColor.copy(alpha = 0.25f),
                ambientColor = containerColor.copy(alpha = 0.15f),
            )
            .clip(shape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        content()
    }
}
