package com.lockin.app.ui.analytics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockin.app.domain.model.CourseModule
import com.lockin.app.ui.analytics.util.AnalyticsFormatters
import com.lockin.app.ui.components.GlassCard
import com.lockin.app.ui.theme.AppleBlue
import com.lockin.app.ui.theme.LocalDarkTheme

/**
 * Apple-style Course Module Study Distribution Component.
 * Features a segmented multi-color progress track and an itemized breakdown
 * of time and percentage spent per university course module.
 */
@Composable
fun ModuleDistributionChart(
    moduleBreakdown: Map<CourseModule, Long>,
    totalFocusTimeMs: Long,
    onAddModuleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalDarkTheme.current

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 20.dp,
    ) {
        // --- Header Row: Title + Add Module Action ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "COURSE MODULES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp,
            )

            // Apple-style Glass Add Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.8f)
                        else Color(0xFFE5E5EA).copy(alpha = 0.85f),
                    )
                    .clickable(onClick = onAddModuleClick)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Module",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = "Add Module",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Proportional Segmented Progress Bar (Apple Screen Time / Storage style) ---
        if (moduleBreakdown.isNotEmpty() && totalFocusTimeMs > 0L) {
            val validEntries = moduleBreakdown.filter { it.value > 0L }
            if (validEntries.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    validEntries.forEach { (module, durationMs) ->
                        val targetWeight = (durationMs.toFloat() / totalFocusTimeMs.toFloat()).coerceAtLeast(0.01f)
                        val animatedWeight by animateFloatAsState(
                            targetValue = targetWeight,
                            animationSpec = tween(durationMillis = 600),
                            label = "segmentWeight_${module.code}",
                        )

                        val segmentColor = AnalyticsFormatters.parseModuleColor(module.colorHex)

                        Box(
                            modifier = Modifier
                                .weight(animatedWeight)
                                .height(14.dp)
                                .background(segmentColor),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            } else {
                EmptyDistributionTrack(isDark = isDark)
                Spacer(modifier = Modifier.height(18.dp))
            }
        } else {
            EmptyDistributionTrack(isDark = isDark)
            Spacer(modifier = Modifier.height(18.dp))
        }

        // --- Itemized Module Breakdown List ---
        if (moduleBreakdown.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = "No course modules yet",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                moduleBreakdown.forEach { (module, durationMs) ->
                    val percentage = if (totalFocusTimeMs > 0L) {
                        ((durationMs.toDouble() / totalFocusTimeMs.toDouble()) * 100).toInt()
                    } else {
                        0
                    }
                    val moduleColor = AnalyticsFormatters.parseModuleColor(module.colorHex)

                    ModuleRow(
                        module = module,
                        durationMs = durationMs,
                        percentage = percentage,
                        moduleColor = moduleColor,
                        isDark = isDark,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDistributionTrack(isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(
                if (isDark) Color.White.copy(alpha = 0.06f)
                else Color.Black.copy(alpha = 0.06f),
            ),
    )
}

@Composable
private fun ModuleRow(
    module: CourseModule,
    durationMs: Long,
    percentage: Int,
    moduleColor: Color,
    isDark: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: Color dot + Code + Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(moduleColor),
            )
            Column {
                Text(
                    text = module.code,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = module.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right: Duration + Percentage pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = AnalyticsFormatters.formatDuration(durationMs),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(moduleColor.copy(alpha = if (isDark) 0.20f else 0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = moduleColor,
                )
            }
        }
    }
}
