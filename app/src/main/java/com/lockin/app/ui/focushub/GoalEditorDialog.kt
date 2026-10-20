package com.lockin.app.ui.focushub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockin.app.domain.model.CourseModule
import com.lockin.app.ui.components.GlassPill
import com.lockin.app.ui.theme.GlassType
import com.lockin.app.ui.theme.LockInPalette

/**
 * Sets the weekly intention for one module: what to finish, and how many
 * focused minutes it should take. The note and the target are what the Active
 * Locked overlay shows while the timer runs.
 */
@Composable
fun GoalEditorDialog(
    module: CourseModule,
    onDismiss: () -> Unit,
    onSave: (minutes: Int, note: String) -> Unit,
) {
    var note by remember(module.id) { mutableStateOf(module.goalNote) }
    var minutes by remember(module.id) { mutableStateOf(module.weeklyGoalMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LockInPalette.SheetSurface,
        titleContentColor = LockInPalette.TextPrimary,
        textContentColor = LockInPalette.TextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text(
                    text = "Weekly goal",
                    style = GlassType.ScreenTitle.copy(fontSize = 20.sp, lineHeight = 24.sp),
                    color = LockInPalette.TextPrimary,
                )
                Text(
                    text = "${module.code} · ${module.name}",
                    fontSize = 11.sp,
                    color = LockInPalette.TextTertiary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 90) note = it },
                    label = { Text("What will you finish?") },
                    placeholder = { Text("e.g. Finish the socket programming set") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LockInPalette.Cyan,
                        unfocusedBorderColor = LockInPalette.SurfaceTint.copy(alpha = 0.14f),
                        focusedTextColor = LockInPalette.TextPrimary,
                        unfocusedTextColor = LockInPalette.TextPrimary,
                        focusedLabelColor = LockInPalette.Cyan,
                        unfocusedLabelColor = LockInPalette.TextTertiary,
                        cursorColor = LockInPalette.Cyan,
                    ),
                )

                Text(
                    text = "WEEKLY TARGET",
                    style = GlassType.Eyebrow,
                    color = LockInPalette.TextTertiary,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 60, 120, 180).forEach { option ->
                        val selected = minutes == option
                        GlassPill(
                            selected = selected,
                            accent = LockInPalette.Cyan,
                            onClick = { minutes = option },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = if (option == 0) "None" else "${option}m",
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) LockInPalette.TextPrimary else LockInPalette.TextSecondary,
                            )
                        }
                    }
                }

                Text(
                    text = if (minutes > 0) {
                        "About ${(minutes / 25)} deep-work sessions of 25 minutes."
                    } else {
                        "Without a target the goal note is still shown during the session."
                    },
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    color = LockInPalette.TextTertiary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(minutes, note) }) {
                Text("Save goal", color = LockInPalette.Cyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LockInPalette.TextSecondary)
            }
        },
    )
}
