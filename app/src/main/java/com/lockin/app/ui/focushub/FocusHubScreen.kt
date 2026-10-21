package com.lockin.app.ui.focushub

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.app.Activity
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.domain.model.CourseModule
import com.lockin.app.domain.model.TimerState
import com.lockin.app.domain.model.TriggerProfile
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.domain.model.displayName
import com.lockin.app.ui.components.GlassButton
import com.lockin.app.ui.components.GlassCard
import com.lockin.app.ui.components.GlassPrimaryButton
import com.lockin.app.ui.theme.GlassRadius
import com.lockin.app.ui.theme.GlassTokens
import com.lockin.app.ui.theme.GlassType
import com.lockin.app.ui.theme.LockInPalette
import com.lockin.app.ui.theme.LockInSerif
import com.lockin.app.ui.vault.components.CameraBarcodeScannerView
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Diameter of the header ring — the settings gear uses the same box so the two line up. */
private val RingDiameter = 46.dp

/**
 * Screen 1 — Today.
 *
 * A tinted sky, one white focus card, the lock-mode row, and a seven-day strip.
 * Everything the screen shows is either an action or a number.
 */
@Composable
fun FocusHubScreen(
    onNavigateToActiveSession: (Long) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCourses: () -> Unit = {},
    onNavigateToVault: () -> Unit = {},
    viewModel: FocusHubViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val timerState by viewModel.timerState.collectAsState()
    val modules by viewModel.modules.collectAsState()
    val selectedModule by viewModel.selectedModule.collectAsState()
    val selectedDurationMinutes by viewModel.selectedDurationMinutes.collectAsState()
    val selectedTriggerProfile by viewModel.selectedTriggerProfile.collectAsState()
    val cardScan by viewModel.cardScan.collectAsState()
    val armOutcome by viewModel.armOutcome.collectAsState()
    val streakDays by viewModel.streakDays.collectAsState()
    val weeklyFocusByModule by viewModel.weeklyFocusByModule.collectAsState()
    val weekDays by viewModel.lastSevenDays.collectAsState()

    val isSessionActive = timerState is TimerState.ActiveLocked ||
        timerState is TimerState.ArmingTrigger
    val isPaused = (timerState as? TimerState.ActiveLocked)?.isPaused == true

    val remainingMs = when (val state = timerState) {
        is TimerState.ActiveLocked -> state.remainingMs
        is TimerState.ArmingTrigger -> state.plannedDurationMs
        else -> null
    }

    val goalTarget = selectedModule?.weeklyGoalMinutes ?: 0
    val goalMinutes = selectedModule?.let { weeklyFocusByModule[it.id] ?: 0 } ?: 0
    val goalFraction = if (goalTarget > 0) {
        (goalMinutes.toFloat() / goalTarget).coerceIn(0f, 1f)
    } else {
        0f
    }

    var showStopDialog by remember { mutableStateOf(false) }
    var showGoalEditor by remember { mutableStateOf(false) }
    var showArmScanner by remember { mutableStateOf(false) }
    var unknownCode by remember { mutableStateOf<String?>(null) }
    // Set when the scanner was opened by the Start button rather than by the
    // lock strip: a successful scan then starts the session straight away.
    var pendingStart by remember { mutableStateOf(false) }

    // A session can only begin once a key is armed, so the start button says so.
    val startLabel = when {
        selectedModule == null -> "Add a module first"
        selectedTriggerProfile == null -> "Scan key to start"
        else -> "Start session"
    }

    fun startNow() {
        viewModel.startSession(context) { sessionId ->
            onNavigateToActiveSession(sessionId)
        }
    }

    val estimatedEndTime = remember(selectedDurationMinutes) {
        LocalTime.now()
            .plusMinutes(selectedDurationMinutes.toLong())
            .format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 12.dp, bottom = 26.dp),
        ) {
            HubHeader(
                streakDays = streakDays,
                goalFraction = goalFraction,
                hasGoal = goalTarget > 0,
                onOpenSettings = onNavigateToSettings,
            )

            Spacer(modifier = Modifier.height(22.dp))

            ModuleRail(
                modules = modules,
                selectedModule = selectedModule,
                onSelectModule = { viewModel.selectModule(it) },
                onAddCourse = onNavigateToCourses,
            )

            Spacer(modifier = Modifier.height(16.dp))

            FocusCard(
                module = selectedModule,
                goalMinutes = goalMinutes,
                goalTarget = goalTarget,
                isSessionActive = isSessionActive,
                isPaused = isPaused,
                remainingLabel = remainingMs?.let { "${formatClock(it)} left" },
                endTimeLabel = "Ends $estimatedEndTime",
                selectedDurationMinutes = selectedDurationMinutes,
                showPresets = !isSessionActive,
                startLabel = startLabel,
                onSelectDuration = { viewModel.selectDuration(it) },
                onEditGoal = { showGoalEditor = true },
                onStart = {
                    when {
                        selectedModule == null -> onNavigateToCourses()
                        // No key armed yet — scan first, then the session starts
                        // itself once the key resolves.
                        selectedTriggerProfile == null -> {
                            pendingStart = true
                            showArmScanner = true
                        }
                        else -> startNow()
                    }
                },
                onViewSession = {
                    val sessionId = when (val state = timerState) {
                        is TimerState.ActiveLocked -> state.sessionId
                        else -> 1L
                    }
                    if (sessionId > 0L) onNavigateToActiveSession(sessionId)
                },
                onStopSession = {
                    // Always hand off to the session screen: it owns the unlock
                    // check and the end-of-session summary. Ending here would
                    // skip the summary entirely.
                    val locked = timerState as? TimerState.ActiveLocked
                    if (locked != null && locked.sessionId > 0L) {
                        onNavigateToActiveSession(locked.sessionId)
                    } else {
                        showStopDialog = true
                    }
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // The lock type is whatever key is armed — one strip, no picker.
            LockStrip(
                armedTrigger = selectedTriggerProfile,
                locked = isSessionActive,
                onScan = { showArmScanner = true },
                onClear = { viewModel.armTrigger(null) },
            )

            Spacer(modifier = Modifier.height(26.dp))

            WeekStrip(days = weekDays)
        }
    }

    LaunchedEffect(armOutcome) {
        when (val outcome = armOutcome) {
            is ArmOutcome.Armed -> {
                showArmScanner = false
                viewModel.stopCardScan()
                viewModel.consumeArmOutcome()
                if (pendingStart) {
                    pendingStart = false
                    startNow()
                }
            }
            is ArmOutcome.Unknown -> {
                showArmScanner = false
                viewModel.stopCardScan()
                pendingStart = false
                unknownCode = outcome.value
                viewModel.consumeArmOutcome()
            }
            null -> Unit
        }
    }

    if (showArmScanner) {
        ArmScanDialog(
            cardValue = cardScan,
            startsSession = pendingStart,
            onStartCardScan = { activity -> viewModel.startCardScan(activity) },
            onStopCardScan = { viewModel.stopCardScan() },
            onDismiss = {
                showArmScanner = false
                pendingStart = false
                viewModel.stopCardScan()
            },
            onDetected = { type, value -> viewModel.armScannedKey(type, value) },
        )
    }

    val pendingCode = unknownCode
    if (pendingCode != null) {
        AlertDialog(
            onDismissRequest = { unknownCode = null },
            title = {
                Text(
                    text = "Unregistered code",
                    fontWeight = FontWeight.Bold,
                    color = LockInPalette.TextPrimary,
                )
            },
            text = {
                Text(
                    text = "$pendingCode isn't a saved key yet. Register it in the Trigger Vault?",
                    color = LockInPalette.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        unknownCode = null
                        onNavigateToVault()
                    },
                ) {
                    Text("Register", color = LockInPalette.Cyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { unknownCode = null }) {
                    Text("Not now", color = LockInPalette.TextSecondary)
                }
            },
            containerColor = LockInPalette.SheetSurface,
            titleContentColor = LockInPalette.TextPrimary,
            textContentColor = LockInPalette.TextSecondary,
        )
    }

    if (showGoalEditor) {
        val goalModule = selectedModule
        if (goalModule != null) {
            GoalEditorDialog(
                module = goalModule,
                onDismiss = { showGoalEditor = false },
                onSave = { minutes, note ->
                    viewModel.setModuleGoal(goalModule, minutes, note)
                    showGoalEditor = false
                },
            )
        }
    }

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
                    text = "Ending the session early will cancel your current deep work timer.",
                    color = LockInPalette.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStopDialog = false
                        viewModel.stopActiveSession(context)
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
//  Header — "Today" + date, a bare gear, and the week ring
// ============================================================================

@Composable
private fun HubHeader(
    streakDays: Int,
    goalFraction: Float,
    hasGoal: Boolean,
    onOpenSettings: () -> Unit,
) {
    val ink = LockInPalette.HeaderInk
    val today = remember { LocalDate.now() }
    val dateLabel = remember {
        today.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Today",
                style = GlassType.Display,
                color = ink,
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = dateLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = ink.copy(alpha = 0.70f),
            )
        }

        // Gear and ring live in one row, top aligned, and the gear's touch box is
        // the same 46dp as the ring — so their centres sit on the same line.
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(RingDiameter)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = ink.copy(alpha = 0.90f),
                    modifier = Modifier.size(20.dp),
                )
            }

            WeekRing(
                fraction = if (hasGoal) goalFraction else (streakDays / 7f).coerceIn(0f, 1f),
                centerLabel = if (hasGoal) "${(goalFraction * 100).toInt()}%" else "${streakDays}d",
                caption = if (hasGoal) "GOAL" else "STREAK",
            )
        }
    }
}

/** Small ring that reports either this week's goal or the streak. */
@Composable
private fun WeekRing(
    fraction: Float,
    centerLabel: String,
    caption: String,
) {
    val track = LockInPalette.HeaderInk.copy(alpha = 0.22f)
    val arc = LockInPalette.HeaderInk
    val ink = LockInPalette.HeaderInk

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(RingDiameter)) {
                val stroke = 2.5.dp.toPx()
                val radius = (size.minDimension - stroke) / 2f
                val topLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)

                drawCircle(
                    color = track,
                    radius = radius,
                    center = center,
                    style = Stroke(width = stroke),
                )
                if (fraction > 0.01f) {
                    drawArc(
                        color = arc,
                        startAngle = -90f,
                        sweepAngle = fraction * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Text(
                text = centerLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = ink,
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = caption,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.1.sp,
            color = ink.copy(alpha = 0.55f),
        )
    }
}

// ============================================================================
//  Module rail
// ============================================================================

@Composable
private fun ModuleRail(
    modules: List<CourseModule>,
    selectedModule: CourseModule?,
    onSelectModule: (CourseModule) -> Unit,
    onAddCourse: () -> Unit,
) {
    val ink = LockInPalette.InkButton
    val onInk = LockInPalette.OnInkButton
    val skyInk = LockInPalette.HeaderInk

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 1.dp),
    ) {
        items(modules, key = { it.id }) { module ->
            val isSelected = selectedModule?.id == module.id
            val shape = RoundedCornerShape(GlassRadius.Pill)

            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(if (isSelected) ink else Color.White.copy(alpha = 0.26f))
                    .border(
                        width = 1.dp,
                        brush = if (isSelected) {
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                        } else {
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.20f)),
                            )
                        },
                        shape = shape,
                    )
                    .clickable { onSelectModule(module) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) onInk else skyInk),
                )
                Text(
                    text = module.code,
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) onInk else skyInk,
                    maxLines = 1,
                )
            }
        }

        item { AddModuleChip(onClick = onAddCourse) }
    }
}

@Composable
private fun AddModuleChip(onClick: () -> Unit) {
    val ink = LockInPalette.HeaderInk
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(GlassRadius.Pill))
            .drawDashedBorder(ink.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = ink.copy(alpha = 0.85f),
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = "Add",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = ink.copy(alpha = 0.85f),
            maxLines = 1,
        )
    }
}

/** Dashed 1dp outline — used only by the "add" chip. */
private fun Modifier.drawDashedBorder(color: Color): Modifier = this.then(
    Modifier.drawBehind {
        drawRoundRect(
            color = color,
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f),
        )
    },
)

// ============================================================================
//  The focus card
// ============================================================================

@Composable
private fun FocusCard(
    module: CourseModule?,
    goalMinutes: Int,
    goalTarget: Int,
    isSessionActive: Boolean,
    isPaused: Boolean,
    remainingLabel: String?,
    endTimeLabel: String,
    selectedDurationMinutes: Int,
    showPresets: Boolean,
    startLabel: String,
    onSelectDuration: (Int) -> Unit,
    onEditGoal: () -> Unit,
    onStart: () -> Unit,
    onViewSession: () -> Unit,
    onStopSession: () -> Unit,
) {
    val accent = LockInPalette.Cyan

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = GlassRadius.Card,
        contentPadding = 22.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "TODAY'S FOCUS",
                style = GlassType.Eyebrow,
                color = LockInPalette.TextTertiary,
                modifier = Modifier.weight(1f),
            )
            if (isSessionActive) {
                LiveBadge(label = if (isPaused) "PAUSED" else "LIVE")
            } else {
                GoalChip(
                    label = if (goalTarget > 0) "${goalMinutes}m / ${goalTarget}m" else "Set goal",
                    onClick = onEditGoal,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = module?.name ?: "Pick a module",
            style = GlassType.CardTitle,
            color = LockInPalette.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(9.dp))

        Text(
            text = when {
                module == null -> "Choose a module above to give this session a target."
                module.goalNote.isNotBlank() -> module.goalNote
                goalTarget > 0 -> "Bank $goalTarget focused minutes this week."
                else -> "Add a weekly goal so it stays in front of you while you study."
            },
            style = GlassType.Body,
            color = LockInPalette.TextSecondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showPresets) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(25, 50, 90).forEach { minutes ->
                        DurationChip(
                            minutes = minutes,
                            selected = selectedDurationMinutes == minutes,
                            onClick = { onSelectDuration(minutes) },
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = if (isPaused) "Paused" else "In session",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = accent,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = remainingLabel ?: endTimeLabel,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = LockInPalette.TextTertiary,
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSessionActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassPrimaryButton(
                    text = "View Session",
                    onClick = onViewSession,
                    modifier = Modifier.weight(1f),
                    leadingIcon = Icons.Filled.PlayArrow,
                    showTrailingArrow = false,
                )
                GlassButton(
                    onClick = onStopSession,
                    modifier = Modifier.weight(0.55f),
                    cornerRadius = GlassRadius.Pill,
                    containerColor = LockInPalette.Coral.copy(alpha = 0.12f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = null,
                            tint = LockInPalette.Coral,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Stop",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LockInPalette.Coral,
                        )
                    }
                }
            }
        } else {
            GlassPrimaryButton(
                text = startLabel,
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = when {
                    module == null -> Icons.Filled.Add
                    else -> Icons.Filled.PlayArrow
                },
                showTrailingArrow = true,
            )
        }
    }
}

/** Coral status pill that reports a running session. */
@Composable
private fun LiveBadge(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(GlassRadius.Pill))
            .background(LockInPalette.Coral.copy(alpha = 0.10f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(LockInPalette.Coral),
        )
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
            color = LockInPalette.Coral,
        )
    }
}

/** Tappable goal summary that opens the goal editor. */
@Composable
private fun GoalChip(label: String, onClick: () -> Unit) {
    val accent = LockInPalette.Cyan
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(GlassRadius.Pill))
            .background(accent.copy(alpha = 0.09f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Flag,
            contentDescription = "Edit goal",
            tint = accent,
            modifier = Modifier.size(10.dp),
        )
        Text(
            text = label,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            color = accent,
            maxLines = 1,
        )
    }
}

@Composable
private fun DurationChip(minutes: Int, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(GlassRadius.Pill)
    val tint = LockInPalette.SurfaceTint
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) tint.copy(alpha = 0.07f) else Color.Transparent)
            .border(
                width = 1.dp,
                brush = if (selected) {
                    Brush.verticalGradient(
                        listOf(tint.copy(alpha = 0.16f), tint.copy(alpha = 0.08f)),
                    )
                } else {
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                },
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = "${minutes}m",
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) LockInPalette.TextPrimary else LockInPalette.TextTertiary,
        )
    }
}

// ============================================================================
//  Lock strip — what key is armed, and the scan that arms it
// ============================================================================

/**
 * One line, one idea: which key will arm the next session. No type picker —
 * the key you scan decides the lock type.
 */
@Composable
private fun LockStrip(
    armedTrigger: TriggerProfile?,
    locked: Boolean,
    onScan: () -> Unit,
    onClear: () -> Unit,
) {
    val ink = LockInPalette.HeaderInk
    val shape = RoundedCornerShape(GlassRadius.Pill)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = "LOCK",
            style = GlassType.Eyebrow,
            color = LockInPalette.TextTertiary,
        )

        Spacer(modifier = Modifier.width(1.dp))

        if (armedTrigger == null) {
            Row(
                modifier = Modifier
                    .clip(shape)
                    .drawDashedBorder(ink.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onScan,
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    tint = ink.copy(alpha = 0.85f),
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = "Scan a key",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = ink.copy(alpha = 0.85f),
                    maxLines = 1,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(shape)
                    .background(LockInPalette.Cyan.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        brush = GlassTokens.rimLight(LockInPalette.Cyan),
                        shape = shape,
                    )
                    .padding(start = 11.dp, end = 5.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = armedTrigger.type.lockIcon(),
                    contentDescription = armedTrigger.type.displayName,
                    tint = LockInPalette.Cyan,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = "${armedTrigger.type.displayName} · ${armedTrigger.label}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = LockInPalette.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable(
                            enabled = !locked,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClear,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Disarm key",
                        tint = LockInPalette.TextTertiary,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        }
    }
}

private fun TriggerType.lockIcon(): ImageVector = when (this) {
    TriggerType.BARCODE -> Icons.Filled.QrCodeScanner
    TriggerType.NFC -> Icons.Filled.Contactless
    TriggerType.WIFI -> Icons.Filled.Wifi
    TriggerType.MANUAL -> Icons.Filled.PlayArrow
}

/**
 * Sheet used to arm a session: barcode or card, whichever the key is. It only
 * resolves — saving a new key is the Trigger Vault's job.
 */
@Composable
private fun ArmScanDialog(
    cardValue: String?,
    startsSession: Boolean = false,
    onStartCardScan: (Activity) -> Unit,
    onStopCardScan: () -> Unit,
    onDismiss: () -> Unit,
    onDetected: (TriggerType, String) -> Unit,
) {
    val activity = LocalContext.current as? Activity
    var mode by remember { mutableStateOf(TriggerType.BARCODE) }
    val accent = if (mode == TriggerType.NFC) LockInPalette.Mint else LockInPalette.Cyan

    LaunchedEffect(mode) {
        if (mode == TriggerType.NFC && activity != null) onStartCardScan(activity) else onStopCardScan()
    }

    LaunchedEffect(cardValue) {
        val value = cardValue
        if (mode == TriggerType.NFC && value != null) onDetected(TriggerType.NFC, value)
    }

    DisposableEffect(Unit) { onDispose { onStopCardScan() } }

    Dialog(
        onDismissRequest = onDismiss,
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
                        Text(
                            text = if (startsSession) "START SESSION" else "ARM SESSION",
                            style = GlassType.Eyebrow,
                            color = LockInPalette.TextTertiary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (mode == TriggerType.NFC) "Tap your card" else "Scan your key",
                            fontFamily = LockInSerif,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = LockInPalette.TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (startsSession) {
                                "The same key ends the session."
                            } else {
                                "This key locks the session."
                            },
                            fontSize = 12.sp,
                            color = LockInPalette.TextSecondary,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(LockInPalette.SurfaceTint.copy(alpha = 0.06f))
                            .clickable(onClick = onDismiss),
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

                Spacer(modifier = Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArmModeChip(
                        icon = Icons.Filled.QrCodeScanner,
                        label = "Barcode",
                        selected = mode == TriggerType.BARCODE,
                        onClick = { mode = TriggerType.BARCODE },
                    )
                    ArmModeChip(
                        icon = Icons.Filled.Contactless,
                        label = "Card",
                        selected = mode == TriggerType.NFC,
                        onClick = { mode = TriggerType.NFC },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (mode == TriggerType.NFC) {
                                accent.copy(alpha = 0.08f)
                            } else {
                                Color.Black
                            },
                        )
                        .border(
                            width = 1.dp,
                            brush = GlassTokens.rimLight(accent),
                            shape = RoundedCornerShape(24.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (mode == TriggerType.NFC) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Contactless,
                                contentDescription = "Tap card",
                                tint = accent,
                                modifier = Modifier.size(52.dp),
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Hold the card against the back of the phone",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = LockInPalette.TextSecondary,
                            )
                        }
                    } else {
                        CameraBarcodeScannerView(
                            onBarcodeDetected = { value -> onDetected(TriggerType.BARCODE, value) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Only keys you have already registered can be armed.",
                    fontSize = 11.5.sp,
                    color = LockInPalette.TextTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Small two-state chip used inside the arm sheet. */
@Composable
private fun ArmModeChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(GlassRadius.Pill)
    val accent = if (selected) LockInPalette.Cyan else LockInPalette.TextTertiary

    Row(
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) LockInPalette.Cyan.copy(alpha = 0.12f) else LockInPalette.SurfaceTint.copy(alpha = 0.05f),
            )
            .border(
                width = 1.dp,
                color = if (selected) LockInPalette.Cyan.copy(alpha = 0.30f) else LockInPalette.SurfaceTint.copy(alpha = 0.10f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
    }
}

// ============================================================================
//  Seven-day strip
// ============================================================================

@Composable
private fun WeekStrip(days: List<DayFocus>) {
    val range = remember {
        val today = LocalDate.now()
        val start = today.minusDays(6)
        val monthDay = DateTimeFormatter.ofPattern("MMM d")
        val day = DateTimeFormatter.ofPattern("d")
        if (start.month == today.month) {
            "${start.format(monthDay)} – ${today.format(day)}"
        } else {
            "${start.format(monthDay)} – ${today.format(monthDay)}"
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "TIMELINE",
                style = GlassType.Eyebrow,
                color = LockInPalette.TextTertiary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = range,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = LockInPalette.TextTertiary,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            days.forEach { day ->
                DayColumn(day = day, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DayColumn(day: DayFocus, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    val ink = LockInPalette.InkButton
    val onInk = LockInPalette.OnInkButton

    Column(
        modifier = modifier
            .clip(shape)
            .background(if (day.isToday) ink else Color.Transparent)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = day.letter,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (day.isToday) onInk.copy(alpha = 0.75f) else LockInPalette.TextTertiary,
        )
        Spacer(modifier = Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .size(if (day.minutes > 0) 7.dp else 6.dp)
                .clip(CircleShape)
                .background(
                    when {
                        day.isToday && day.minutes > 0 -> onInk
                        day.minutes > 0 -> LockInPalette.Cyan
                        day.isToday -> onInk.copy(alpha = 0.35f)
                        else -> LockInPalette.SurfaceTint.copy(alpha = 0.14f)
                    },
                ),
        )
    }
}

// ============================================================================
//  Formatting
// ============================================================================

private fun formatClock(remainingMs: Long): String {
    val safeMs = remainingMs.coerceAtLeast(0L)
    val totalSeconds = safeMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}
