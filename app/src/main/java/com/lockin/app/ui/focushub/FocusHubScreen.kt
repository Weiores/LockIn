package com.lockin.app.ui.focushub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.domain.model.CourseModule
import com.lockin.app.ui.components.AccentGlassCard
import com.lockin.app.ui.components.GlassCard
import com.lockin.app.ui.components.TimerRingCanvas
import com.lockin.app.ui.theme.AccentBlue
import com.lockin.app.ui.theme.AccentCyan
import com.lockin.app.ui.theme.AccentGreen
import com.lockin.app.ui.theme.AccentOrange
import com.lockin.app.ui.theme.GlassBorderTop
import com.lockin.app.ui.theme.TextPrimary
import com.lockin.app.ui.theme.TextSecondary
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusHubScreen(
    onNavigateToActiveSession: (Long) -> Unit = {},
    viewModel: FocusHubViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val modules by viewModel.modules.collectAsState()
    val selectedModule by viewModel.selectedModule.collectAsState()
    val selectedDurationMinutes by viewModel.selectedDurationMinutes.collectAsState()
    val streakDays by viewModel.streakDays.collectAsState()

    val totalDurationMs = selectedDurationMinutes * 60 * 1000L

    // Calculate estimated end time
    val estimatedEndTime = remember(selectedDurationMinutes) {
        val now = LocalTime.now()
        val end = now.plusMinutes(selectedDurationMinutes.toLong())
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        end.format(formatter)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // --- Header Card: Streak + Wi-Fi Status ---
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
                        tint = AccentOrange,
                        modifier = Modifier.size(28.dp),
                    )
                    Column {
                        Text(
                            text = "$streakDays Days",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Text(
                            text = "Current Streak",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }

                // Wi-Fi status pill
                GlassCard(
                    cornerRadius = 12.dp,
                    contentPadding = 6.dp,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Wifi,
                            contentDescription = "Wi-Fi",
                            tint = AccentGreen,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Ready to Focus",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        // --- Module Picker Header & Horizontal Row ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "COURSE MODULE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = TextSecondary,
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedModule == null,
                        onClick = { viewModel.selectModule(null) },
                        label = { Text("General Focus") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                            selectedLabelColor = AccentBlue,
                            containerColor = Color.White.copy(alpha = 0.05f),
                            labelColor = TextSecondary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (selectedModule == null) AccentBlue.copy(alpha = 0.4f) else GlassBorderTop,
                            enabled = true,
                            selected = selectedModule == null,
                        ),
                    )
                }

                items(modules) { module ->
                    val isSelected = selectedModule?.id == module.id
                    val moduleColor = try {
                        Color(android.graphics.Color.parseColor(module.colorHex))
                    } catch (e: Exception) {
                        AccentBlue
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectModule(module) },
                        label = { Text(module.code) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(moduleColor),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = moduleColor.copy(alpha = 0.2f),
                            selectedLabelColor = moduleColor,
                            containerColor = Color.White.copy(alpha = 0.05f),
                            labelColor = TextSecondary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) moduleColor.copy(alpha = 0.5f) else GlassBorderTop,
                            enabled = true,
                            selected = isSelected,
                        ),
                    )
                }
            }
        }

        // --- Timer Dial GlassCard ---
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TimerRingCanvas(
                    remainingMs = totalDurationMs,
                    totalMs = totalDurationMs,
                    size = 240.dp,
                    strokeWidth = 12.dp,
                )

                // Projected End Time
                Text(
                    text = "Ends at $estimatedEndTime",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )

                // Quick Preset Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val presets = listOf(25, 50, 90)
                    presets.forEach { minutes ->
                        val isSelected = selectedDurationMinutes == minutes
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) AccentBlue.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                                )
                                .clickable { viewModel.selectDuration(minutes) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = "${minutes}m",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AccentCyan else TextSecondary,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- Start CTA Button ---
        AccentGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.startSession(context) { sessionId ->
                        onNavigateToActiveSession(sessionId)
                    }
                },
            accentColor = AccentBlue,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Lock In",
                    tint = AccentBlue,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start & Lock In",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue,
                )
            }
        }
    }
}
