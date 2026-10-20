package com.lockin.app.ui.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.domain.model.TimerState
import com.lockin.app.ui.components.GlassButton
import com.lockin.app.ui.components.GlassCard
import com.lockin.app.ui.components.TimerRingCanvas
import com.lockin.app.ui.theme.AppleBlue
import com.lockin.app.ui.theme.AppleGreen
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
                                    text = "You locked in for $minutes minutes of deep work.",
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

                else -> {
                    // When Idle or Cancelled, automatically navigate back
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
