package com.lockin.app.ui.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.domain.model.FailReason
import com.lockin.app.domain.model.TimerState
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.ui.components.GlassButton
import com.lockin.app.ui.components.GlassCard
import com.lockin.app.ui.components.TimerRingCanvas
import com.lockin.app.ui.theme.AppleBlue
import com.lockin.app.ui.theme.AppleGreen
import com.lockin.app.ui.theme.AppleOrange
import com.lockin.app.ui.theme.AppleRed
import com.lockin.app.ui.theme.LocalDarkTheme

@Composable
fun ActiveSessionOverlay(
    sessionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val isDark = LocalDarkTheme.current
    val timerState by viewModel.timerState.collectAsState()
    val activeModule by viewModel.activeModule.collectAsState()
    var showStopDialog by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        if (sessionId > 0) {
            viewModel.loadModuleForSession(sessionId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AnimatedContent(
            targetState = timerState,
            transitionSpec = { fadeIn(spring()) togetherWith fadeOut(spring()) },
            label = "activeSessionStateTransition",
        ) { state ->
            when (state) {
                // --- 1. Arming Trigger (e.g. Waiting for Phone to be placed Face-Down) ---
                is TimerState.ArmingTrigger -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Top Bar: Active Module Badge & Trigger Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (activeModule != null) {
                                GlassCard(
                                    cornerRadius = 14.dp,
                                    contentPadding = 8.dp,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        val moduleColor = try {
                                            Color(android.graphics.Color.parseColor(activeModule!!.colorHex))
                                        } catch (e: Exception) {
                                            AppleBlue
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(moduleColor),
                                        )
                                        Text(
                                            text = activeModule!!.code,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            GlassCard(
                                cornerRadius = 14.dp,
                                contentPadding = 8.dp,
                            ) {
                                Text(
                                    text = "ARMING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = AppleOrange,
                                )
                            }
                        }

                        // Center: Flip to Lock Instruction & Animated Graphic
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulseArming")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 0.95f,
                                targetValue = 1.05f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse,
                                ),
                                label = "pulseScale",
                            )

                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(AppleBlue.copy(alpha = if (isDark) 0.12f else 0.08f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ScreenRotation,
                                    contentDescription = "Place Phone Face-Down",
                                    tint = AppleBlue,
                                    modifier = Modifier.size(64.dp),
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Flip to Lock In",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Place your phone face-down on the desk.\nA haptic pulse will confirm your session start.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp,
                                )
                            }
                        }

                        // Bottom: Cancel Arming CTA
                        GlassButton(
                            onClick = {
                                viewModel.stopSession(context)
                                onNavigateBack()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Cancel Session",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                // --- 2. Active Locked Countdown ---
                is TimerState.ActiveLocked -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Top Bar: Active Module Badge & Trigger Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (activeModule != null) {
                                GlassCard(
                                    cornerRadius = 14.dp,
                                    contentPadding = 8.dp,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        val moduleColor = try {
                                            Color(android.graphics.Color.parseColor(activeModule!!.colorHex))
                                        } catch (e: Exception) {
                                            AppleBlue
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(moduleColor),
                                        )
                                        Text(
                                            text = activeModule!!.code,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            // Trigger pill
                            GlassCard(
                                cornerRadius = 14.dp,
                                contentPadding = 8.dp,
                            ) {
                                Text(
                                    text = state.triggerType.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Center: Clean Floating Circular Ring
                        TimerRingCanvas(
                            remainingMs = state.remainingMs,
                            totalMs = state.totalMs,
                            isPaused = state.isPaused,
                            size = 270.dp,
                            strokeWidth = 12.dp,
                        )

                        // Bottom: Control Buttons (Pause / Resume & Stop)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Pause / Resume Glass Button
                            IconButton(
                                onClick = {
                                    if (state.isPaused) viewModel.resume() else viewModel.pause()
                                },
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                                    ),
                            ) {
                                Icon(
                                    imageVector = if (state.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                    contentDescription = if (state.isPaused) "Resume" else "Pause",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(26.dp),
                                )
                            }

                            Spacer(modifier = Modifier.width(28.dp))

                            // Functional Stop / Give Up Button
                            IconButton(
                                onClick = { showStopDialog = true },
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(AppleRed.copy(alpha = if (isDark) 0.18f else 0.10f)),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Stop,
                                    contentDescription = "Stop Session",
                                    tint = AppleRed,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                    }
                }

                // --- 3. Grace Period Warning (10s countdown if phone picked up) ---
                is TimerState.GracePeriodWarning -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "graceWarningPulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.15f,
                        targetValue = 0.35f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "pulseAlpha",
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppleRed.copy(alpha = pulseAlpha)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            // Header: Warning Pill
                            GlassCard(
                                cornerRadius = 14.dp,
                                contentPadding = 8.dp,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = "Warning",
                                        tint = AppleRed,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "PHONE LIFTED",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = AppleRed,
                                    )
                                }
                            }

                            // Center: Urgent 10-Second Grace Ring
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                val graceSeconds = (state.graceRemainingMs / 1000).coerceAtLeast(0L)
                                val graceFraction = (state.graceRemainingMs.toFloat() / 10000f).coerceIn(0f, 1f)

                                Box(
                                    modifier = Modifier.size(240.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    // Animated Circular Ring for Grace Countdown
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                        val stroke = 12.dp.toPx()
                                        val radius = (size.minDimension - stroke) / 2
                                        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)

                                        // Background Track
                                        drawCircle(
                                            color = AppleRed.copy(alpha = 0.2f),
                                            radius = radius,
                                            center = center,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                                        )

                                        // Active Countdown Arc
                                        drawArc(
                                            color = AppleRed,
                                            startAngle = -90f,
                                            sweepAngle = 360f * graceFraction,
                                            useCenter = false,
                                            topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius),
                                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = stroke,
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                            ),
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            text = "${graceSeconds}s",
                                            style = MaterialTheme.typography.displayLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = AppleRed,
                                        )
                                        Text(
                                            text = "Remaining",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                Text(
                                    text = "Return phone face-down now to continue your session!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }

                            // Bottom: Give Up Action
                            GlassButton(
                                onClick = {
                                    viewModel.stopSession(context)
                                    onNavigateBack()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = AppleRed.copy(alpha = 0.20f),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Give Up & Fail Session",
                                        fontWeight = FontWeight.Bold,
                                        color = AppleRed,
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 4. Completed Session ---
                is TimerState.Completed -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = AppleGreen,
                                    modifier = Modifier.size(60.dp),
                                )

                                Text(
                                    text = "Session Complete!",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )

                                val minutes = state.actualDurationMs / (60 * 1000)
                                Text(
                                    text = "You locked in for $minutes minutes of deep work.${if (state.breakCount > 0) " Picked up ${state.breakCount} time(s)." else " Zero distractions!"}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )

                                GlassButton(
                                    onClick = {
                                        viewModel.dismiss(context)
                                        onNavigateBack()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    containerColor = AppleGreen,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Return to Focus Hub",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 5. Failed / Broken Session (Grace Expired or Aborted) ---
                is TimerState.Failed -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Session Failed",
                                    tint = AppleRed,
                                    modifier = Modifier.size(60.dp),
                                )

                                Text(
                                    text = if (state.reason == FailReason.GRACE_EXPIRED) "Session Broken" else "Session Cancelled",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )

                                val failureText = when (state.reason) {
                                    FailReason.GRACE_EXPIRED -> "The 10-second grace period expired because your phone was not returned face-down."
                                    FailReason.USER_CANCELLED -> "The study session was ended early."
                                    FailReason.TRIGGER_LOST -> "The hardware trigger connection was lost."
                                }

                                Text(
                                    text = failureText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )

                                val loggedMinutes = state.actualDurationMs / (60 * 1000)
                                if (loggedMinutes > 0) {
                                    Text(
                                        text = "Logged $loggedMinutes minute(s) before interruption.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                GlassButton(
                                    onClick = {
                                        viewModel.dismiss(context)
                                        onNavigateBack()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "Return to Focus Hub",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 6. Idle / Other ---
                else -> {
                    LaunchedEffect(Unit) {
                        onNavigateBack()
                    }
                }
            }
        }
    }

    // Stop Confirmation Dialog
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = {
                Text(
                    text = "Stop Session?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to stop this study session? Your timer will end immediately.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopDialog = false
                        viewModel.stopSession(context)
                        onNavigateBack()
                    },
                ) {
                    Text("Stop", color = AppleRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
