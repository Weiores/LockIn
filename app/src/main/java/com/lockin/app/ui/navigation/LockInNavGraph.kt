package com.lockin.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lockin.app.ui.focushub.FocusHubScreen
import com.lockin.app.ui.settings.SettingsScreen
import com.lockin.app.ui.timer.ActiveSessionOverlay
import com.lockin.app.ui.vault.TriggerVaultScreen

private data class BottomNavItem(
    val route: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.FocusHub, "Focus", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.TriggerVault, "Triggers", Icons.Filled.Key, Icons.Outlined.Key),
    BottomNavItem(Screen.Analytics, "Analytics", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    BottomNavItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun LockInNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Dynamically hide bottom navigation bar during active session overlay
    val showBottomBar = currentDestination?.let { dest ->
        bottomNavItems.any { dest.hasRoute(it.route::class) }
    } ?: true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(item.route::class)
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.FocusHub,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(280)) + scaleIn(initialScale = 0.96f, animationSpec = tween(280)) },
            exitTransition = { fadeOut(tween(200)) + scaleOut(targetScale = 0.98f, animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(tween(280)) + scaleIn(initialScale = 0.98f, animationSpec = tween(280)) },
            popExitTransition = { fadeOut(tween(200)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200)) },
        ) {
            composable<Screen.FocusHub> {
                FocusHubScreen(
                    onNavigateToActiveSession = { sessionId ->
                        navController.navigate(Screen.ActiveSession(sessionId))
                    },
                )
            }
            composable<Screen.ActiveSession> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.ActiveSession>()
                ActiveSessionOverlay(
                    sessionId = route.sessionId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }
            composable<Screen.TriggerVault> {
                TriggerVaultScreen()
            }
            composable<Screen.Analytics> {
                PlaceholderScreen(title = "Analytics")
            }
            composable<Screen.Settings> {
                SettingsScreen()
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
