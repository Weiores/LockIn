package com.lockin.app.ui.timer

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.domain.model.CourseModule
import com.lockin.app.domain.model.FailReason
import com.lockin.app.domain.model.TimerState
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.domain.model.displayName
import com.lockin.app.ui.components.GlassButton
import com.lockin.app.ui.components.GlassCard
import com.lockin.app.ui.components.GlassMicroLabel
import com.lockin.app.ui.components.GlassPill
import com.lockin.app.ui.components.GlassPrimaryButton
import com.lockin.app.ui.components.LockInAmbientBackground
import com.lockin.app.ui.components.StatusDot
import com.lockin.app.ui.theme.GlassRadius
import com.lockin.app.ui.theme.GlassTokens
import com.lockin.app.ui.theme.GlassType
import com.lockin.app.ui.theme.LockInPalette
import com.lockin.app.ui.theme.LockInSerif
import com.lockin.app.ui.vault.components.CameraBarcodeScannerView
import com.lockin.app.util.Constants

/**
 * Hosts the fullscreen session experience: arming, active locked, grace period
 * and the terminal result cards. Only the *phase* is animated so the 500ms
 * countdown tick never re-triggers a transition.
 */
@Composable
fun ActiveSessionOverlay(
    sessionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val timerState by viewModel.timerState.collectAsState()
    val activeModule by viewModel.activeModule.collectAsState()
    val activeGoal by viewModel.activeModuleGoal.collectAsState()

    val keyError by viewModel.keyError.collectAsState()

    var showStopDialog by remember { mutableStateOf(false) }
    var showUnlockSheet by remember { mutableStateOf(false) }
    var unlockError by remember { mutableStateOf<String?>(null) }

    // Two things can end a session (the stop action and the engine going idle).
    // Popping twice would pop the hub itself and leave an empty back stack.
    var leftScreen by remember { mutableStateOf(false) }
    val leave: () -> Unit = {
        if (!leftScreen) {
            leftScreen = true
            onNavigateBack()
        }
    }

    LaunchedEffect(sessionId) {
        if (sessionId > 0L) viewModel.loadModuleForSession(sessionId)
    }

    val phase = timerState.toPhase()

    AnimatedContent(
        targetState = phase,
        transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
        label = "activeSessionPhase",
    ) { currentPhase ->
        when (currentPhase) {
            SessionPhase.Active -> {
                val state = timerState as? TimerState.ActiveLocked
                if (state != null) {
                    ActiveLockedScreen(
                        state = state,
                        moduleCode = activeModule?.code,
                        goal = activeGoal,
                        onTogglePause = {
                            if (state.isPaused) viewModel.resume() else viewModel.pause()
                        },
                        onStop = {
                            if (state.triggerType == TriggerType.BARCODE || state.triggerType == TriggerType.NFC) {
                                unlockError = null
                                showUnlockSheet = true
                            } else {
                                showStopDialog = true
                            }
                        },
                        onUnlockEarly = {
                            unlockError = null
                            showUnlockSheet = true
                        },
                    )
                }
            }

            SessionPhase.Arming -> {
                val state = timerState as? TimerState.ArmingTrigger
                if (state != null) {
                    ArmingScreen(
                        state = state,
                        activity = activity,
                        keyError = keyError,
                        onBarcodeScanned = { viewModel.checkScannedBarcode(it) },
                        onCancel = {
                            // Nothing started, so there is no summary to show —
                            // go straight back to idle.
                            viewModel.dismiss(context)
                            leave()
                        },
                        onStartNfcListening = { viewModel.startNfcArmingListener(it) },
                        onStopNfcListening = { viewModel.stopNfcArmingListener() },
                    )
                }
            }

            SessionPhase.Completed -> {
                val state = timerState as? TimerState.Completed
                if (state != null) {
                    SessionResultScreen(
                        title = "Session Complete",
                        message = "You locked in for ${state.actualDurationMs / 60_000L} minutes of deep work." +
                            if (state.breakCount > 0) {
                                " Picked up ${state.breakCount} time(s)."
                            } else {
                                " Zero distractions."
                            },
                        detail = sessionDetail(state.actualDurationMs, activeModule),
                        accent = LockInPalette.Mint,
                        isFailure = false,
                        onDismiss = {
                            viewModel.dismiss(context)
                            leave()
                        },
                    )
                }
            }

            SessionPhase.Failed -> {
                val state = timerState as? TimerState.Failed
                if (state != null) {
                    val message = when (state.reason) {
                        FailReason.USER_CANCELLED -> "The study session was ended early."
                        FailReason.TRIGGER_LOST -> "The hardware trigger connection was lost."
                        FailReason.DISTRACTION -> "You stayed in a distraction app for ${Constants.DISTRACTION_GRACE_SECONDS} seconds — the session was ended automatically."
                    }
                    SessionResultScreen(
                        title = "Session Ended",
                        message = message,
                        detail = sessionDetail(state.actualDurationMs, activeModule),
                        accent = LockInPalette.Coral,
                        isFailure = true,
                        onDismiss = {
                            viewModel.dismiss(context)
                            leave()
                        },
                    )
                }
            }

            SessionPhase.Idle -> {
                LaunchedEffect(Unit) { leave() }
            }
        }
    }

    // ------------------------------------------------------------- unlock sheet
    if (showUnlockSheet) {
        val sessionType = (timerState as? TimerState.ActiveLocked)?.triggerType ?: TriggerType.MANUAL
        val sessionKey = viewModel.activeTriggerValue
        val accent = when (sessionType) {
            TriggerType.NFC -> LockInPalette.Mint
            else -> LockInPalette.Cyan
        }

        // Ending a session with a different key than it started with is the
        // whole point of the lock, so the check compares value *and* type.
        val acceptKey: (String) -> Unit = { candidate ->
            if (sessionKey == null || sessionKey.equals(candidate, ignoreCase = true)) {
                showUnlockSheet = false
                viewModel.stopSession(context)
            } else {
                unlockError = "That's a different key — this session was opened with " +
                    "${sessionType.displayName.lowercase()} \"${sessionKey?.take(14)}\". Use the same one."
            }
        }

        Dialog(
            onDismissRequest = { showUnlockSheet = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LockInPalette.SheetSurface.copy(alpha = 0.98f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            GlassMicroLabel(text = "${sessionType.displayName.uppercase()} REQUIRED")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Unlock Early",
                                fontFamily = LockInSerif,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = LockInPalette.TextPrimary,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(LockInPalette.SurfaceTint.copy(alpha = 0.06f))
                                .border(1.dp, GlassTokens.RimLight, CircleShape)
                                .clickable { showUnlockSheet = false },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = LockInPalette.TextSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    if (sessionType == TriggerType.NFC) {
                        LaunchedEffect(sessionType, activity) {
                            if (activity == null) return@LaunchedEffect
                            viewModel.observeCardUid(activity).collect { uid -> acceptKey(uid) }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(accent.copy(alpha = 0.08f))
                                .border(1.dp, GlassTokens.rimLight(accent), RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Contactless,
                                    contentDescription = "Tap card",
                                    tint = accent,
                                    modifier = Modifier.size(54.dp),
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Tap the card you started with",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = LockInPalette.TextSecondary,
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, GlassTokens.rimLight(accent), RoundedCornerShape(24.dp)),
                        ) {
                            CameraBarcodeScannerView(onBarcodeDetected = { scannedValue ->
                                acceptKey(scannedValue)
                            })
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = unlockError ?: if (sessionType == TriggerType.NFC) {
                            "Hold the same card against the back of the phone."
                        } else {
                            "Point the camera at the same barcode you started with."
                        },
                        fontSize = 11.5.sp,
                        color = if (unlockError != null) LockInPalette.Coral else LockInPalette.TextTertiary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    GlassButton(
                        onClick = {
                            showUnlockSheet = false
                            viewModel.stopSession(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = LockInPalette.Coral.copy(alpha = 0.16f),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "End session without a key",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LockInPalette.Coral,
                            )
                        }
                    }
                }
            }
        }
    }

    // --------------------------------------------------------------- stop dialog
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = {
                Text(
                    text = "Stop Session?",
                    fontWeight = FontWeight.Bold,
                    color = LockInPalette.TextPrimary,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to stop this study session? Your timer will end immediately.",
                    color = LockInPalette.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopDialog = false
                        viewModel.stopSession(context)
                    },
                ) {
                    Text("Stop", color = LockInPalette.Coral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopDialog = false }) {
                    Text("Cancel", color = LockInPalette.Cyan)
                }
            },
            containerColor = LockInPalette.SheetSurface,
            titleContentColor = LockInPalette.TextPrimary,
            textContentColor = LockInPalette.TextSecondary,
        )
    }
}

// ============================================================================
//  Arming
// ============================================================================

@Composable
private fun ArmingScreen(
    state: TimerState.ArmingTrigger,
    activity: Activity?,
    keyError: String?,
    onBarcodeScanned: (String) -> Boolean,
    onCancel: () -> Unit,
    onStartNfcListening: (Activity) -> Unit,
    onStopNfcListening: () -> Unit,
) {
    val pulse = rememberInfiniteTransition(label = "armingPulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "armingPulseScale",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 18.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                StatusDot(color = LockInPalette.Amber, size = 5.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ARMING",
                    style = GlassType.Micro,
                    color = LockInPalette.Amber,
                )
            }

            when (state.triggerType) {
                TriggerType.BARCODE -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Scan Checkpoint Barcode",
                            fontFamily = LockInSerif,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = LockInPalette.TextPrimary,
                        )
                        Text(
                            text = "Point the camera at your registered checkpoint item to begin.",
                            fontSize = 11.5.sp,
                            color = LockInPalette.TextTertiary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .border(
                                    width = 1.dp,
                                    brush = GlassTokens.rimLight(LockInPalette.Cyan),
                                    shape = RoundedCornerShape(24.dp),
                                ),
                        ) {
                            CameraBarcodeScannerView(onBarcodeDetected = { scannedValue ->
                                onBarcodeScanned(scannedValue)
                            })
                        }
                    }
                }

                TriggerType.NFC -> {
                    DisposableEffect(activity) {
                        if (activity != null) onStartNfcListening(activity)
                        onDispose { onStopNfcListening() }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(132.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(LockInPalette.Mint.copy(alpha = 0.10f))
                                .border(1.dp, GlassTokens.rimLight(LockInPalette.Mint), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Contactless,
                                contentDescription = "Tap card",
                                tint = LockInPalette.Mint,
                                modifier = Modifier.size(54.dp),
                            )
                        }
                        Text(
                            text = "Tap Registered Card",
                            fontFamily = LockInSerif,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = LockInPalette.TextPrimary,
                        )
                        Text(
                            text = "Hold your student card or transit pass to the back of the phone to lock in.",
                            fontSize = 11.5.sp,
                            color = LockInPalette.TextTertiary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                        )
                    }
                }

                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(132.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(LockInPalette.Cyan.copy(alpha = 0.09f))
                                .border(1.dp, GlassTokens.rimLight(LockInPalette.Cyan), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Ready to lock in",
                                tint = LockInPalette.Cyan,
                                modifier = Modifier.size(50.dp),
                            )
                        }
                        Text(
                            text = "Ready to Lock In",
                            fontFamily = LockInSerif,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = LockInPalette.TextPrimary,
                        )
                        Text(
                            text = "Your session runs smoothly in the background while you study.",
                            fontSize = 11.5.sp,
                            color = LockInPalette.TextTertiary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (keyError != null) {
                    Text(
                        text = keyError,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = LockInPalette.Coral,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }

                GlassButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = LockInPalette.SurfaceTint.copy(alpha = 0.06f),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Cancel Session",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LockInPalette.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
//  Result (completed / failed)
// ============================================================================

@Composable
private fun SessionResultScreen(
    title: String,
    message: String,
    detail: String,
    accent: Color,
    isFailure: Boolean,
    onDismiss: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LockInAmbientBackground(
            glowPrimary = if (isFailure) LockInPalette.AmbientCrimson else LockInPalette.AmbientBlue,
            glowSecondary = LockInPalette.AmbientBlue,
            drawWash = false,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = GlassRadius.Card,
                contentPadding = 24.dp,
                glowColor = accent.copy(alpha = 0.3f),
                borderBrush = GlassTokens.rimLight(accent),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = if (isFailure) Icons.Filled.WarningAmber else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(46.dp),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = title,
                        fontFamily = LockInSerif,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = LockInPalette.TextPrimary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(GlassRadius.Pill))
                            .background(accent.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = detail,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = message,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp,
                        color = LockInPalette.TextSecondary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    GlassPill(
                        selected = true,
                        accent = accent,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = if (isFailure) "Streak reset" else "Streak extended",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    GlassPrimaryButton(
                        text = "Return to Focus Hub",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        showTrailingArrow = false,
                    )
                }
            }
        }
    }
}

// ============================================================================
//  Phase mapping
// ============================================================================

/** "12m 40s · Data Structures" — how long, and what was studied. */
private fun sessionDetail(actualDurationMs: Long, module: CourseModule?): String {
    val safe = actualDurationMs.coerceAtLeast(0L)
    val minutes = safe / 60_000L
    val seconds = (safe % 60_000L) / 1000L
    val duration = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    val topic = module?.name?.takeIf { it.isNotBlank() }
        ?: module?.code?.takeIf { it.isNotBlank() }
        ?: "General focus"
    return "$duration · $topic"
}

private enum class SessionPhase { Idle, Arming, Active, Completed, Failed }

private fun TimerState.toPhase(): SessionPhase = when (this) {
    is TimerState.ArmingTrigger -> SessionPhase.Arming
    is TimerState.ActiveLocked -> SessionPhase.Active
    is TimerState.Completed -> SessionPhase.Completed
    is TimerState.Failed -> SessionPhase.Failed
    TimerState.Idle -> SessionPhase.Idle
}
