package com.lockin.app.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lockin.app.ui.analytics.components.AddModuleDialog
import com.lockin.app.ui.analytics.components.AnalyticsMetricCards
import com.lockin.app.ui.analytics.components.ModuleDistributionChart
import com.lockin.app.ui.analytics.components.SessionHistorySection
import com.lockin.app.ui.analytics.components.WeeklyBarChartCanvas

/**
 * Screen 4: Study Analytics & Academic Module Tracker.
 * Displays high-level focus metrics, custom Canvas 7-day study bar chart,
 * academic module distribution, and complete interactive session logs.
 */
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsState()
    val modules by viewModel.modules.collectAsState()
    val filteredSessions by viewModel.filteredSessions.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedDateKey by viewModel.selectedDateKey.collectAsState()
    val showAddModuleDialog by viewModel.showAddModuleDialog.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // --- Screen Title & Subtitle ---
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Academic Performance & Deep Work Logs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // --- 1. Top Summary Metric Cards (2x2 Apple Glass Grid) ---
        AnalyticsMetricCards(
            summary = summary,
        )

        // --- 2. Custom Canvas 7-Day Weekly Bar Chart ---
        WeeklyBarChartCanvas(
            dailyFocusMs = summary.dailyFocusMs,
            selectedDateKey = selectedDateKey,
            onSelectDate = { viewModel.selectDateKey(it) },
        )

        // --- 3. Academic Module Distribution (Segmented Progress + Breakdown) ---
        ModuleDistributionChart(
            moduleBreakdown = summary.moduleBreakdown,
            totalFocusTimeMs = summary.totalFocusTimeMs,
            onAddModuleClick = { viewModel.showAddModule(true) },
        )

        // --- 4. Recent Session History with Filter Chips ---
        SessionHistorySection(
            sessions = filteredSessions,
            modules = modules,
            selectedFilter = selectedFilter,
            onSelectFilter = { viewModel.selectFilter(it) },
            onSeedSampleData = { viewModel.seedSampleStudyData() },
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    // --- Add Course Module Dialog ---
    if (showAddModuleDialog) {
        AddModuleDialog(
            onDismiss = { viewModel.showAddModule(false) },
            onConfirm = { code, name, colorHex ->
                viewModel.addModule(code, name, colorHex)
            },
        )
    }
}
