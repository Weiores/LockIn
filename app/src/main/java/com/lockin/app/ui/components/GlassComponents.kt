package com.lockin.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockin.app.ui.theme.GlassRadius
import com.lockin.app.ui.theme.GlassTokens
import kotlin.math.roundToInt
import com.lockin.app.ui.theme.GlassType
import com.lockin.app.ui.theme.LockInPalette
import kotlin.math.cos
import kotlin.math.sin

// ============================================================================
//  Canvas
// ============================================================================

/**
 * Full-bleed page wash: a tinted sky at the top fading to paper, plus two very
 * soft blooms so the transition never bands. Both come from the active theme.
 */
@Composable
fun LockInAmbientBackground(
    modifier: Modifier = Modifier,
    glowPrimary: Color = LockInPalette.AmbientBlue,
    glowSecondary: Color = LockInPalette.AmbientSecondary,
    baseTop: Color = LockInPalette.Obsidian,
    baseBottom: Color = LockInPalette.Midnight,
    /**
     * The wash itself is painted once, at window level, so it runs behind the
     * status bar without a seam. Screens that only need their own bloom on top
     * of it pass `false`.
     */
    drawWash: Boolean = true,
) {
    val isDark = LockInPalette.IsDark
    val wash = GlassTokens.Canvas
    val glowAlpha = if (isDark) 0.55f else 0.75f
    val glowAlphaSecondary = if (isDark) 0.30f else 0.45f

    Canvas(modifier = modifier.fillMaxSize()) {
        if (drawWash) drawRect(brush = wash)

        // Sky bloom behind the header
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(glowPrimary.copy(alpha = glowAlpha), Color.Transparent),
                center = Offset(size.width * 0.78f, size.height * 0.02f),
                radius = size.width * 0.90f,
            ),
        )

        // Paper bloom, low and wide, keeps the hand-over soft
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(glowSecondary.copy(alpha = glowAlphaSecondary), Color.Transparent),
                center = Offset(size.width * 0.15f, size.height * 0.42f),
                radius = size.width * 1.05f,
            ),
        )
    }
}

// ============================================================================
//  Surfaces
// ============================================================================

/**
 * The one card of the design: a white sheet with a wide, low-contrast shadow
 * and a barely-there edge. No gradient rim, no glow.
 *
 * @param tint optional flat colour layered over the fill (pass an alpha colour).
 * @param glowColor optional tinted shadow, used for active/selected surfaces.
 * @param onClick when supplied the card becomes tappable with a press spring.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassRadius.Card,
    contentPadding: Dp = 18.dp,
    shape: Shape = RoundedCornerShape(cornerRadius),
    tint: Color? = null,
    glowColor: Color? = null,
    borderBrush: Brush = GlassTokens.CardEdge,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = LockInPalette.IsDark
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 700f),
        label = "glassCardScale",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .then(
                if (glowColor != null) {
                    Modifier.shadow(
                        elevation = 22.dp,
                        shape = shape,
                        ambientColor = glowColor.copy(alpha = 0.28f),
                        spotColor = glowColor.copy(alpha = 0.38f),
                    )
                } else {
                    Modifier.shadow(
                        elevation = 20.dp,
                        shape = shape,
                        ambientColor = if (isDark) {
                            Color.Black.copy(alpha = 0.50f)
                        } else {
                            Color(0xFF1B2A44).copy(alpha = 0.10f)
                        },
                        spotColor = if (isDark) {
                            Color.Black.copy(alpha = 0.60f)
                        } else {
                            Color(0xFF1B2A44).copy(alpha = 0.16f)
                        },
                    )
                },
            )
            .clip(shape)
            .background(GlassTokens.CardFill)
            .then(if (tint != null) Modifier.background(tint) else Modifier)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

/**
 * Interactive glass button with tactile spring feedback (kept API-compatible
 * with the pre-refactor call sites in the Settings screen).
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = GlassRadius.Pill,
    containerColor: Color = LockInPalette.Cyan,
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
                elevation = 10.dp,
                shape = shape,
                spotColor = containerColor.copy(alpha = 0.35f),
                ambientColor = containerColor.copy(alpha = 0.22f),
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

/**
 * Primary CTA: one solid ink pill with a wide, quiet shadow. Light mode gets a
 * near-black pill with white type; dark mode inverts to a near-white pill.
 */
@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    showTrailingArrow: Boolean = true,
    height: Dp = 54.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = LockInPalette.IsDark
    val ink = LockInPalette.InkButton
    val onInk = LockInPalette.OnInkButton
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "primaryCtaScale",
    )
    val shape = RoundedCornerShape(GlassRadius.Pill)

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.40f else 0.16f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.50f else 0.24f),
            )
            .clip(shape)
            .background(ink)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .height(height),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = onInk,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(9.dp))
            }
            Text(
                text = text,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.1.sp,
                color = onInk,
            )
            if (showTrailingArrow) {
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = onInk.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * Pill chip / status pill. Selected state gets an accent fill, accent rim and
 * a soft outer glow.
 */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accent: Color = LockInPalette.Cyan,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 13.dp, vertical = 7.dp),
    shape: Shape = RoundedCornerShape(GlassRadius.Pill),
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val fill: Brush = if (selected) {
        Brush.verticalGradient(
            listOf(accent.copy(alpha = 0.16f), accent.copy(alpha = 0.10f)),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                LockInPalette.SurfaceTint.copy(alpha = 0.05f),
                LockInPalette.SurfaceTint.copy(alpha = 0.03f),
            ),
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(
                width = 1.dp,
                brush = if (selected) {
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.42f), accent.copy(alpha = 0.18f)),
                    )
                } else {
                    GlassTokens.CardEdge
                },
                shape = shape,
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

/** Circular icon button. Pass `onSky = true` when it sits on the tinted header. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = LockInPalette.TextSecondary,
    accent: Color? = null,
    onSky: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                when {
                    accent != null -> Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.14f), accent.copy(alpha = 0.08f)),
                    )
                    onSky -> Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.12f)),
                    )
                    else -> Brush.verticalGradient(
                        listOf(
                            LockInPalette.SurfaceTint.copy(alpha = 0.07f),
                            LockInPalette.SurfaceTint.copy(alpha = 0.04f),
                        ),
                    )
                },
            )
            .border(
                width = 1.dp,
                brush = when {
                    accent != null -> Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.12f)),
                    )
                    onSky -> Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0.18f)),
                    )
                    else -> GlassTokens.CardEdge
                },
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.45f),
        )
    }
}

/** Small status dot with an optional halo, used everywhere state is reported. */
@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 6.dp,
    halo: Boolean = true,
) {
    Box(
        modifier = modifier.size(size * if (halo) 2.4f else 1f),
        contentAlignment = Alignment.Center,
    ) {
        if (halo) {
            Box(
                modifier = Modifier
                    .size(size * 2.4f)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
        )
    }
}

/** Section header: title on the left, optional trailing label / caption. */
@Composable
fun GlassSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = LockInPalette.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailing?.invoke()
    }
}

/** Small uppercase badge, e.g. "3 REGISTERED" / "LIVE". */
@Composable
fun GlassBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LockInPalette.TextTertiary,
    background: Color = LockInPalette.SurfaceTint.copy(alpha = 0.05f),
    dot: Color? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(GlassRadius.Pill))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (dot != null) {
            StatusDot(color = dot, size = 4.dp, halo = false)
        }
        Text(
            text = text,
            style = GlassType.Eyebrow,
            color = color,
            maxLines = 1,
        )
    }
}

// ============================================================================
//  Luminous ring
// ============================================================================

/**
 * Circular progress dial with a layered glow, sweep-gradient arc and a node
 * dot riding the head of the arc. Content is centred inside the ring.
 *
 * @param progress 0f..1f of the arc that should be lit.
 */
@Composable
fun GlowingRingCanvas(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    strokeWidth: Dp = 6.dp,
    colors: List<Color> = listOf(LockInPalette.SkyBlue, LockInPalette.Cyan),
    trackColor: Color = LockInPalette.SurfaceTint.copy(alpha = 0.055f),
    glowColor: Color = colors.last(),
    glowStrength: Float = 1f,
    showNode: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label = "ringProgress",
    )
    // Captured here because the draw scope below is not composable.
    val nodeColor = LockInPalette.SurfaceTint

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val radius = (this.size.minDimension - stroke) / 2f
            val center = this.center
            val arcTopLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            val sweep = animatedProgress * 360f

            // Track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (sweep > 0.5f) {
                // Layered halo — three widening, fading passes fake a real glow.
                listOf(4.2f to 0.05f, 2.6f to 0.09f, 1.7f to 0.16f).forEach { (mult, alpha) ->
                    rotate(degrees = -90f, pivot = center) {
                        drawArc(
                            color = glowColor.copy(alpha = alpha * glowStrength),
                            startAngle = 0f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = stroke * mult, cap = StrokeCap.Round),
                        )
                    }
                }

                // Luminous arc
                rotate(degrees = -90f, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(colors = colors, center = center),
                        startAngle = 0f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }

                // Node at the head of the arc
                if (showNode) {
                    val angleRad = Math.toRadians((-90f + sweep).toDouble())
                    val node = Offset(
                        x = center.x + radius * cos(angleRad).toFloat(),
                        y = center.y + radius * sin(angleRad).toFloat(),
                    )
                    drawCircle(
                        color = glowColor.copy(alpha = 0.30f * glowStrength),
                        radius = stroke * 2.6f,
                        center = node,
                    )
                    drawCircle(
                        color = nodeColor,
                        radius = stroke * 0.85f,
                        center = node,
                    )
                }
            }
        }

        content()
    }
}

// ============================================================================
//  Floating bottom navigation
// ============================================================================

/** The four anchor tabs of the app. Settings lives behind the header menu. */
enum class LockInTab(
    val label: String,
    val icon: ImageVector,
) {
    FocusHub("Focus Hub", Icons.Outlined.Schedule),
    TriggerVault("Trigger Vault", Icons.Outlined.QrCodeScanner),
    Notes("Notes", Icons.Outlined.EditNote),
    Analytics("Analytics", Icons.Outlined.BarChart),
}

/**
 * Floating frosted-glass pill bar. The selection pill physically travels
 * between tabs on a spring instead of snapping, and each item cross-fades its
 * ink colour as it gains or loses selection.
 */
@Composable
fun LockInBottomNavBar(
    selectedTab: LockInTab,
    onTabSelected: (LockInTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val barShape = RoundedCornerShape(30.dp)
    val ink = LockInPalette.InkButton
    val onInk = LockInPalette.OnInkButton
    val tabs = LockInTab.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val itemShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 18.dp,
                    shape = barShape,
                    ambientColor = Color.Black.copy(alpha = 0.14f),
                    spotColor = Color.Black.copy(alpha = 0.20f),
                )
                .clip(barShape)
                .background(GlassTokens.NavFill)
                .border(width = 1.dp, brush = GlassTokens.CardEdge, shape = barShape)
                .padding(6.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val gap = 4.dp
            val slotWidth = (maxWidth - gap * (tabs.size - 1)) / tabs.size

            // Fractional slot position lets the pill glide between tabs.
            val pillProgress by animateFloatAsState(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                label = "navPillTravel",
            )

            Box(
                modifier = Modifier
                    .offset {
                        val stride = slotWidth.toPx() + gap.toPx()
                        IntOffset((pillProgress * stride).roundToInt(), 0)
                    }
                    .size(width = slotWidth, height = 50.dp)
                    .clip(itemShape)
                    .background(ink),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) onInk else LockInPalette.TextTertiary,
                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                        label = "navItemInk",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(itemShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(tab) },
                            )
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = contentColor,
                                modifier = Modifier.size(17.dp),
                            )
                            Text(
                                text = tab.label,
                                fontSize = 9.5.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                letterSpacing = 0.1.sp,
                                color = contentColor,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
//  Small helpers
// ============================================================================

/** Tracked-out micro label used for screen eyebrows. */
@Composable
fun GlassMicroLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LockInPalette.TextTertiary,
    style: TextStyle = GlassType.Micro,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 1dp hairline divider that fades out at both ends. */
@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    val tint = LockInPalette.SurfaceTint
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        tint.copy(alpha = 0.09f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}
