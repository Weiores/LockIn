package com.lockin.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.domain.model.WeeklyGoal
import com.lockin.app.domain.model.WeeklyNote
import com.lockin.app.ui.components.GlassBadge
import com.lockin.app.ui.components.GlassButton
import com.lockin.app.ui.components.GlassCard
import com.lockin.app.ui.components.GlassMicroLabel
import com.lockin.app.ui.components.LockInAmbientBackground
import com.lockin.app.ui.theme.GlassRadius
import com.lockin.app.ui.theme.GlassType
import com.lockin.app.ui.theme.LockInPalette
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Screen — the weekly journal.
 *
 * Two blocks on one sheet: tick-box goals for the week, then freeform notes.
 * Everything is persisted in Room, so the page survives process death and the
 * prev/next week arrows always land on real data.
 */
@Composable
fun NotesScreen(
    viewModel: NotesViewModel = hiltViewModel(),
) {
    val weekStart by viewModel.weekStart.collectAsState()
    val isCurrentWeek by viewModel.isCurrentWeek.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val carryOverCount by viewModel.previousWeekOpenGoals.collectAsState()

    // null = composer hidden, otherwise the note being edited (empty body = new note)
    var composerNote by remember { mutableStateOf<WeeklyNote?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LockInAmbientBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 12.dp, bottom = 30.dp),
        ) {
            NotesHeader(
                weekStart = weekStart,
                isCurrentWeek = isCurrentWeek,
                goalsDone = goals.count { it.isDone },
                goalsTotal = goals.size,
                notesCount = notes.size,
                onPreviousWeek = viewModel::goToPreviousWeek,
                onNextWeek = viewModel::goToNextWeek,
            )

            Spacer(modifier = Modifier.height(20.dp))

            WeeklyGoalsCard(
                goals = goals,
                carryOverCount = carryOverCount,
                onAddGoal = viewModel::addGoal,
                onToggleGoal = viewModel::toggleGoal,
                onDeleteGoal = viewModel::deleteGoal,
                onCarryOver = viewModel::carryOverOpenGoals,
            )

            Spacer(modifier = Modifier.height(22.dp))

            NotesBlock(
                notes = notes,
                onCreateNote = { composerNote = WeeklyNote(weekStart = weekStart, body = "") },
                onEditNote = { composerNote = it },
                onDeleteNote = viewModel::deleteNote,
            )
        }
    }

    val noteInComposer = composerNote
    if (noteInComposer != null) {
        NoteComposerSheet(
            note = noteInComposer,
            onDismiss = { composerNote = null },
            onSave = { body ->
                if (noteInComposer.id == 0L) {
                    viewModel.addNote(body)
                } else {
                    viewModel.updateNote(noteInComposer.id, body)
                }
                composerNote = null
            },
            onDelete = {
                viewModel.deleteNote(noteInComposer.id)
                composerNote = null
            },
        )
    }
}

// ============================================================================
//  Header
// ============================================================================

@Composable
private fun NotesHeader(
    weekStart: LocalDate,
    isCurrentWeek: Boolean,
    goalsDone: Int,
    goalsTotal: Int,
    notesCount: Int,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    val ink = LockInPalette.HeaderInk
    val summary = when {
        goalsTotal > 0 -> "$goalsDone/$goalsTotal goals · $notesCount notes"
        else -> "No goals yet — add one below"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isCurrentWeek) "This Week" else "Week Plan",
                style = GlassType.Display,
                color = ink,
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = formatWeekRange(weekStart),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = ink.copy(alpha = 0.70f),
            )
        }

        HeaderNavButton(
            icon = Icons.Filled.ChevronLeft,
            contentDescription = "Previous week",
            onClick = onPreviousWeek,
        )

        Spacer(modifier = Modifier.width(10.dp))

        HeaderNavButton(
            icon = Icons.Filled.ChevronRight,
            contentDescription = "Next week",
            onClick = onNextWeek,
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = summary,
        fontSize = 10.5.sp,
        lineHeight = 15.sp,
        color = ink.copy(alpha = 0.58f),
    )
}

/** Rounded 34dp icon button that sits on the tinted sky. */
@Composable
private fun HeaderNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(LockInPalette.SurfaceTint.copy(alpha = 0.10f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = LockInPalette.HeaderInk.copy(alpha = 0.85f),
            modifier = Modifier.size(17.dp),
        )
    }
}

internal fun formatWeekRange(weekStart: LocalDate): String {
    val end = weekStart.plusDays(6)
    return if (weekStart.month == end.month) {
        "${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))} – ${end.format(DateTimeFormatter.ofPattern("d"))}"
    } else {
        "${weekStart.format(DateTimeFormatter.ofPattern("d MMM"))} – ${end.format(DateTimeFormatter.ofPattern("d MMM"))}"
    }
}

// ============================================================================
//  Weekly goals
// ============================================================================

@Composable
private fun WeeklyGoalsCard(
    goals: List<WeeklyGoal>,
    carryOverCount: Int,
    onAddGoal: (String) -> Unit,
    onToggleGoal: (WeeklyGoal) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onCarryOver: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val doneCount = goals.count { it.isDone }
    val progress = if (goals.isEmpty()) 0f else doneCount.toFloat() / goals.size
    val accent = LockInPalette.Cyan

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassMicroLabel(text = "WEEKLY GOALS", color = LockInPalette.TextTertiary)
            GlassBadge(
                text = "$doneCount / ${goals.size}",
                color = accent,
                background = accent.copy(alpha = 0.12f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress rail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(LockInPalette.SurfaceTint.copy(alpha = 0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (goals.isEmpty()) {
            Text(
                text = "Nothing planned for this week yet. One concrete goal beats five vague ones.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = LockInPalette.TextTertiary,
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            goals.forEach { goal ->
                GoalRow(
                    goal = goal,
                    onToggle = { onToggleGoal(goal) },
                    onDelete = { onDeleteGoal(goal.id) },
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (carryOverCount > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            CarryOverChip(count = carryOverCount, onClick = onCarryOver)
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Inline "add a goal" field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        color = LockInPalette.TextPrimary,
                    ),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onAddGoal(draft)
                            draft = ""
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (draft.isEmpty()) {
                    Text(
                        text = "Add a goal for this week…",
                        fontSize = 13.sp,
                        color = LockInPalette.TextTertiary,
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .clickable {
                        onAddGoal(draft)
                        draft = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Add goal",
                    tint = accent,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
private fun GoalRow(
    goal: WeeklyGoal,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = LockInPalette.Cyan

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (goal.isDone) accent else androidx.compose.ui.graphics.Color.Transparent)
                .border(
                    width = if (goal.isDone) 0.dp else 1.5.dp,
                    color = if (goal.isDone) accent else LockInPalette.SurfaceTint.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(8.dp),
                )
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (goal.isDone) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Done",
                    tint = LockInPalette.OnAccent,
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(11.dp))

        Text(
            text = goal.text,
            fontSize = 12.5.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium,
            color = if (goal.isDone) LockInPalette.TextTertiary else LockInPalette.TextPrimary,
            textDecoration = if (goal.isDone) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete goal",
                tint = LockInPalette.TextTertiary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun CarryOverChip(count: Int, onClick: () -> Unit) {
    val accent = LockInPalette.Amber
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassRadius.Pill))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(GlassRadius.Pill))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Bring $count open ${if (count == 1) "goal" else "goals"} from last week",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
    }
}

// ============================================================================
//  Notes
// ============================================================================

@Composable
private fun NotesBlock(
    notes: List<WeeklyNote>,
    onCreateNote: () -> Unit,
    onEditNote: (WeeklyNote) -> Unit,
    onDeleteNote: (Long) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Notes",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = LockInPalette.TextPrimary,
            )
            GlassBadge(
                text = if (notes.size == 1) "1 NOTE" else "${notes.size} NOTES",
                color = LockInPalette.TextTertiary,
                background = LockInPalette.SurfaceTint.copy(alpha = 0.05f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (notes.isEmpty()) {
            EmptyNotesCard(onClick = onCreateNote)
        } else {
            notes.forEach { note ->
                NoteCard(
                    note = note,
                    onClick = { onEditNote(note) },
                    onDelete = { onDeleteNote(note.id) },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            AddNoteCard(onClick = onCreateNote)
        }
    }
}

@Composable
private fun NoteCard(
    note: WeeklyNote,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = GlassRadius.Item,
        contentPadding = 14.dp,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = note.body,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = LockInPalette.TextPrimary,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassMicroLabel(
                    text = formatNoteStamp(note.updatedAt),
                    color = LockInPalette.TextTertiary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete note",
                        tint = LockInPalette.TextTertiary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddNoteCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GlassRadius.Item))
            .background(LockInPalette.SurfaceTint.copy(alpha = 0.045f))
            .border(
                width = 1.dp,
                color = LockInPalette.SurfaceTint.copy(alpha = 0.10f),
                shape = RoundedCornerShape(GlassRadius.Item),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "+  New note",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = LockInPalette.TextSecondary,
        )
    }
}

@Composable
private fun EmptyNotesCard(onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = GlassRadius.Item,
        contentPadding = 22.dp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "No notes yet",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = LockInPalette.TextPrimary,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "Write down why this week matters, what you finished, and what slipped.",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = LockInPalette.TextTertiary,
            )
            Spacer(modifier = Modifier.height(14.dp))
            GlassButton(
                onClick = onClick,
                containerColor = LockInPalette.Cyan.copy(alpha = 0.16f),
            ) {
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(42.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Write a note",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LockInPalette.Cyan,
                    )
                }
            }
        }
    }
}

internal fun formatNoteStamp(instant: java.time.Instant): String {
    val stamp = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val today = LocalDate.now()
    val day = when (stamp.toLocalDate()) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> stamp.format(DateTimeFormatter.ofPattern("EEE d MMM"))
    }
    return "$day · ${stamp.format(DateTimeFormatter.ofPattern("h:mm a"))}"
}

// ============================================================================
//  Note composer
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteComposerSheet(
    note: WeeklyNote,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var body by remember { mutableStateOf(note.body) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LockInPalette.SheetSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (note.id == 0L) "New note" else "Edit note",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LockInPalette.TextPrimary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.id != 0L) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .clickable(onClick = onDelete),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete note",
                                tint = LockInPalette.Coral,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(11.dp))
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
            }

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                placeholder = {
                    Text(
                        text = "What needs to happen this week?",
                        color = LockInPalette.TextTertiary,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = 260.dp),
                shape = RoundedCornerShape(GlassRadius.Item),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LockInPalette.Cyan,
                    unfocusedBorderColor = LockInPalette.SurfaceTint.copy(alpha = 0.12f),
                    focusedTextColor = LockInPalette.TextPrimary,
                    unfocusedTextColor = LockInPalette.TextPrimary,
                    cursorColor = LockInPalette.Cyan,
                    focusedContainerColor = LockInPalette.SurfaceTint.copy(alpha = 0.04f),
                    unfocusedContainerColor = LockInPalette.SurfaceTint.copy(alpha = 0.04f),
                ),
            )

            GlassButton(
                onClick = { onSave(body) },
                modifier = Modifier.fillMaxWidth(),
                containerColor = LockInPalette.Cyan.copy(alpha = 0.16f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (note.id == 0L) "Save note" else "Update note",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LockInPalette.Cyan,
                    )
                }
            }
        }
    }
}
