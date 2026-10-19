package com.lockin.app.ui.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.domain.model.TimerState
import com.lockin.app.ui.components.AccentGlassCard
import com.lockin.app.ui.components.GlassCard
import com.lockin.app.ui.components.TimerRingCanvas
import com.lockin.app.ui.theme.AccentBlue
import com.lockin.app.ui.theme.AccentGreen
import com.lockin.app.ui.theme.AccentRed
import com.lockin.app.ui.theme.SurfaceDark
import com.lockin.app.ui.theme.TextPrimary
import com.lockin.app.ui.theme.TextSecondary

@Composable
fun ActiveSessionOverlay(
    sessionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel(),
) {
    val timerState by viewModel.timerState.collectAsState()
    val activeModule by viewModel.activeModule.collectAsState()
    var showGiveUpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) {
        if (sessionId > 0) {
            viewModel.loadModuleForSession(sessionId)
        }
    }

    // Full screen pitch-black OLED background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
    ) {
        when (val state = timerState) {
            is TimerState.ActiveLocked -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 36.dp),
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
                                cornerRadius = 12.dp,
                                contentPadding = 8.dp,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    val moduleColor = try {
                                        Color(android.graphics.Color.parseColor(activeModule!!.colorHex))
                                    } catch (e: Exception) {
                                        AccentBlue
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
                                        color = TextPrimary,
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        // Trigger pill
                        GlassCard(
                            cornerRadius = 12.dp,
                            contentPadding = 8.dp,
                        ) {
                            Text(
                                text = state.triggerType.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                            )
                        }
                    }

                    // Center: Circular Timer Ring
                    TimerRingCanvas(
                        remainingMs = state.remainingMs,
                        totalMs = state.totalMs,
                        isPaused = state.isPaused,
                        size = 280.dp,
                        strokeWidth = 14.dp,
                    )

                    // Bottom: Control Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Pause / Resume Glass Button
                        GlassCard(
                            cornerRadius = 24.dp,
                            contentPadding = 4.dp,
                        ) {
                            IconButton(
                                onClick = {
                                    if (state.isPaused) viewModel.resume() else viewModel.pause()
                                },
                                modifier = Modifier.size(56.dp),
                            ) {
                                Icon(
                                    imageVector = if (state.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                    contentDescription = if (state.isPaused) "Resume" else "Pause",
                                    tint = AccentBlue,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }

                        // Give Up Button
                        GlassCard(
                            cornerRadius = 24.dp,
                            contentPadding = 4.dp,
                        ) {
                            IconButton(
                                onClick = { showGiveUpDialog = true },
                                modifier = Modifier.size(56.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Give Up",
                                    tint = AccentRed.copy(alpha = 0.8f),
                                    modifier = Modifier.size(28.dp),
                                )
                            }
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
                                tint = AccentGreen,
                                modifier = Modifier.size(64.dp),
                            )

                            Text(
                                text = "Session Complete!",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )

                            val minutes = state.actualDurationMs / (60 * 1000)
                            Text(
                                text = "You focused for $minutes minutes of deep work.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                            )

                            AccentGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = AccentGreen,
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.dismiss()
                                        onNavigateBack()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                ) {
                                    Text(
                                        text = "Return to Focus Hub",
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGreen,
                                    )
                                }
                            }
                        }
                    }
                }
            }

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
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Failed",
                                tint = AccentRed,
                                modifier = Modifier.size(56.dp),
                            )

                            Text(
                                text = "Session Ended Early",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )

                            Text(
                                text = "Streak broken. Don't worry, every session counts toward building the habit.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                            )

                            Button(
                                onClick = {
                                    viewModel.dismiss()
                                    onNavigateBack()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.2f)),
                            ) {
                                Text(
                                    text = "Back to Focus Hub",
                                    color = AccentRed,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                // If Idle, automatically exit
                LaunchedEffect(Unit) {
                    onNavigateBack()
                }
            }
        }
    }

    // Give Up Confirmation Dialog
    if (showGiveUpDialog) {
        AlertDialog(
            onDismissRequest = { showGiveUpDialog = false },
            title = {
                Text(
                    text = "Give Up Session?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            },
            text = {
                Text(
                    text = "Ending the session early will mark it as incomplete and reset your daily deep-work momentum.",
                    color = TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGiveUpDialog = false
                        viewModel.giveUp()
                    },
                ) {
                    Text("Give Up", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGiveUpDialog = false }) {
                    Text("Keep Focusing", color = AccentBlue)
                }
            },
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
        )
    }
}
