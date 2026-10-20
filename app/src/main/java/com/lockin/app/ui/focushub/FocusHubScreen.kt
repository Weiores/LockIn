package com.lockin.app.ui.focushub

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun FocusHubScreen(
    onNavigateToActiveSession: (Long) -> Unit = {},
    viewModel: FocusHubViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val isDark = LocalDarkTheme.current
    val systemDark = isSystemInDarkTheme()

    val timerState by viewModel.timerState.collectAsState()
    val modules by viewModel.modules.collectAsState()
    val selectedModule by viewModel.selectedModule.collectAsState()
    val selectedDurationMinutes by viewModel.selectedDurationMinutes.collectAsState()
    val selectedTriggerType by viewModel.selectedTriggerType.collectAsState()
    val streakDays by viewModel.streakDays.collectAsState()

    val isSessionActive = timerState is TimerState.ActiveLocked ||
            timerState is TimerState.ArmingTrigger ||
            timerState is TimerState.GracePeriodWarning

    val activeRemainingMs = when (val s = timerState) {
        is TimerState.ActiveLocked -> s.remainingMs
        is TimerState.GracePeriodWarning -> s.sessionRemainingMs
        is TimerState.ArmingTrigger -> s.plannedDurationMs
        else -> null
    }
    val activeTotalMs = when (val s = timerState) {
        is TimerState.ActiveLocked -> s.totalMs
        is TimerState.GracePeriodWarning -> s.totalMs
        is TimerState.ArmingTrigger -> s.plannedDurationMs
        else -> null
    }

    val displayRemainingMs = activeRemainingMs ?: (selectedDurationMinutes * 60 * 1000L)
    val displayTotalMs = activeTotalMs ?: (selectedDurationMinutes * 60 * 1000L)

    var showStopDialog by remember { mutableStateOf(false) }

    val estimatedEndTime = remember(selectedDurationMinutes) {
        val now = LocalTime.now()
        val end = now.plusMinutes(selectedDurationMinutes.toLong())
        val formatter = DateTimeFormatter.ofPattern("h:mm a")
        end.format(formatter)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // --- Top Header: Streak + Wi-Fi Pill + Light/Dark Toggle ---
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Streak badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = AppleOrange,
                        modifier = Modifier.size(26.dp),
                    )
                    Column {
                        Text(
                            text = "$streakDays Days",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Current Streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Wi-Fi status pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.04f),
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Wifi,
                            contentDescription = "Wi-Fi",
                            tint = AppleGreen,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "Ready",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Theme Mode Toggle (Sun/Moon)
                    IconButton(
                        onClick = { viewModel.toggleDarkMode(isDark) },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Light/Dark Mode",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        // --- Trigger Mode Selector (Manual vs Flip to Lock) ---
        AnimatedVisibility(visible = !isSessionActive) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LOCK-IN TRIGGER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val triggerModes = listOf(
                        Triple(TriggerType.MANUAL, "Manual Timer", Icons.Filled.PlayArrow),
                        Triple(TriggerType.FLIP, "Flip to Lock", Icons.Filled.ScreenRotation),
                    )

                    triggerModes.forEach { (type, label, icon) ->
                        val isSelected = selectedTriggerType == type
                        val chipBg by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
                            },
                            animationSpec = spring(),
                            label = "triggerChipBg",
                        )
                        val chipTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipBg)
                                .clickable { viewModel.selectTriggerType(type) }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = chipTextColor,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = chipTextColor,
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Course Module Picker (Horizontal Clean Chips) ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "COURSE MODULE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp),
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    val isGeneralSelected = selectedModule == null
                    val chipBg by animateColorAsState(
                        targetValue = if (isGeneralSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
                        },
                        animationSpec = spring(),
                        label = "generalChipBg",
                    )
                    val chipTextColor = if (isGeneralSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(chipBg)
                            .clickable { viewModel.selectModule(null) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "General Focus",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isGeneralSelected) FontWeight.Bold else FontWeight.Medium,
                            color = chipTextColor,
                        )
                    }
                }

                items(modules) { module ->
                    val isSelected = selectedModule?.id == module.id
                    val moduleColor = try {
                        Color(android.graphics.Color.parseColor(module.colorHex))
                    } catch (e: Exception) {
                        AppleBlue
                    }

                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) {
                            moduleColor
                        } else {
                            if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
                        },
                        animationSpec = spring(),
                        label = "moduleChipBg",
                    )
                    val chipTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(chipBg)
                            .clickable { viewModel.selectModule(module) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (!isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(moduleColor),
                                )
                            }
                            Text(
                                text = module.code,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = chipTextColor,
                            )
                        }
                    }
                }
            }
        }

        // --- Clean Floating Timer Dial (No Square Borders!) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TimerRingCanvas(
                remainingMs = displayRemainingMs,
                totalMs = displayTotalMs,
                isPaused = (timerState as? TimerState.ActiveLocked)?.isPaused ?: false,
                size = 250.dp,
                strokeWidth = 10.dp,
            )

            // Projected End Time
            Text(
                text = if (isSessionActive) "Session in progress" else "Ends at $estimatedEndTime",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Duration Presets (Hidden when session is actively running)
            AnimatedVisibility(visible = !isSessionActive) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(25, 50, 90).forEach { mins ->
                        val isSelected = selectedDurationMinutes == mins
                        val pillBg by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)
                            },
                            animationSpec = spring(),
                            label = "presetPillBg",
                        )
                        val pillTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(pillBg)
                                .clickable { viewModel.selectDuration(mins) }
                                .padding(horizontal = 20.dp, vertical = 9.dp),
                        ) {
                            Text(
                                text = "${mins}m",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = pillTextColor,
                            )
                        }
                    }
                }
            }
        }

        // --- Action Buttons with Animation ---
        AnimatedContent(
            targetState = isSessionActive,
            transitionSpec = { fadeIn(spring()) togetherWith fadeOut(spring()) },
            label = "actionButtonsTransition",
        ) { active ->
            if (active) {
                val activeSessionId = when (val s = timerState) {
                    is TimerState.ActiveLocked -> s.sessionId
                    is TimerState.GracePeriodWarning -> s.sessionId
                    else -> 1L
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Resume / View Session CTA
                    GlassButton(
                        onClick = {
                            if (activeSessionId > 0) {
                                onNavigateToActiveSession(activeSessionId)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "View Session",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "View Session",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }

                    // Stop Session Button
                    GlassButton(
                        onClick = { showStopDialog = true },
                        modifier = Modifier.weight(1f),
                        containerColor = AppleRed.copy(alpha = if (isDark) 0.25f else 0.12f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Stop Session",
                                tint = AppleRed,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Stop",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppleRed,
                            )
                        }
                    }
                }
            } else {
                // Start & Lock In CTA
                GlassButton(
                    onClick = {
                        viewModel.startSession(context) { sessionId ->
                            onNavigateToActiveSession(sessionId)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (selectedTriggerType == TriggerType.FLIP) Icons.Filled.ScreenRotation else Icons.Filled.Lock,
                            contentDescription = "Lock In",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedTriggerType == TriggerType.FLIP) "Arm Flip to Lock" else "Start & Lock In",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
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
                    text = "Ending the session early will cancel your current deep work timer.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopDialog = false
                        viewModel.stopActiveSession(context)
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
