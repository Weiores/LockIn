package com.lockin.app.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lockin.app.ui.analytics.AnalyticsScreen
import com.lockin.app.ui.components.LockInBottomNavBar
import com.lockin.app.ui.components.LockInTab
import com.lockin.app.ui.focushub.FocusHubScreen
import com.lockin.app.ui.notes.NotesScreen
import com.lockin.app.ui.scanner.BarcodeScannerScreen
import com.lockin.app.ui.scanner.NfcScannerScreen
import com.lockin.app.ui.settings.SettingsScreen
import com.lockin.app.ui.theme.LockInPalette
import com.lockin.app.ui.timer.ActiveSessionOverlay
import com.lockin.app.ui.vault.TriggerVaultScreen

/**
 * Four-tab shell with a floating frosted-glass pill bar.
 *
 * The bar is only rendered on the four anchor destinations — the fullscreen
 * Active Locked and Grace Period overlays own the entire canvas.
 */
@Composable
fun LockInNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentTab: LockInTab? = when {
        currentDestination?.hasRoute(Screen.FocusHub::class) == true -> LockInTab.FocusHub
        currentDestination?.hasRoute(Screen.TriggerVault::class) == true -> LockInTab.TriggerVault
        currentDestination?.hasRoute(Screen.Notes::class) == true -> LockInTab.Notes
        currentDestination?.hasRoute(Screen.Analytics::class) == true -> LockInTab.Analytics
        else -> null
    }

    Scaffold(
        containerColor = LockInPalette.Midnight,
        bottomBar = {
            if (currentTab != null) {
                LockInBottomNavBar(
                    selectedTab = currentTab,
                    onTabSelected = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.FocusHub,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = {
                val from = initialState.destination.tabIndex
                val to = targetState.destination.tabIndex
                if (from != null && to != null && from != to) {
                    // Tab hop: short eased drift in the direction of travel, no scale pop.
                    val direction = if (to > from) 1 else -1
                    slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { width ->
                        width * direction / 8
                    } + fadeIn(tween(180, easing = LinearOutSlowInEasing))
                } else {
                    fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.97f, animationSpec = tween(260, easing = FastOutSlowInEasing))
                }
            },
            exitTransition = {
                val from = initialState.destination.tabIndex
                val to = targetState.destination.tabIndex
                if (from != null && to != null && from != to) {
                    val direction = if (to > from) 1 else -1
                    slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { width ->
                        -width * direction / 8
                    } + fadeOut(tween(180, easing = LinearOutSlowInEasing))
                } else {
                    fadeOut(tween(180, easing = LinearOutSlowInEasing)) +
                        scaleOut(targetScale = 0.98f, animationSpec = tween(180, easing = LinearOutSlowInEasing))
                }
            },
            popEnterTransition = {
                fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                    scaleIn(initialScale = 0.98f, animationSpec = tween(260, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                fadeOut(tween(180, easing = LinearOutSlowInEasing)) +
                    scaleOut(targetScale = 0.97f, animationSpec = tween(180, easing = LinearOutSlowInEasing))
            },
        ) {
            composable<Screen.FocusHub> {
                FocusHubScreen(
                    onNavigateToActiveSession = { sessionId ->
                        navController.navigate(Screen.ActiveSession(sessionId))
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings) { launchSingleTop = true }
                    },
                    onNavigateToCourses = {
                        navController.navigate(Screen.Analytics) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }

            composable<Screen.ActiveSession> { backStackEntry ->
                val route = backStackEntry.toRoute<Screen.ActiveSession>()
                ActiveSessionOverlay(
                    sessionId = route.sessionId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable<Screen.TriggerVault> {
                TriggerVaultScreen(
                    onOpenBarcodeScanner = {
                        navController.navigate(Screen.BarcodeScanner) { launchSingleTop = true }
                    },
                    onOpenNfcScanner = {
                        navController.navigate(Screen.NfcScanner) { launchSingleTop = true }
                    },
                )
            }

            composable<Screen.Notes> {
                NotesScreen()
            }

            composable<Screen.Analytics> {
                AnalyticsScreen()
            }

            composable<Screen.Settings> {
                SettingsScreen()
            }

            composable<Screen.BarcodeScanner> {
                BarcodeScannerScreen(
                    onSaved = { navController.popBackStack() },
                    onClose = { navController.popBackStack() },
                )
            }

            composable<Screen.NfcScanner> {
                NfcScannerScreen(
                    onSaved = { navController.popBackStack() },
                    onClose = { navController.popBackStack() },
                )
            }
        }
    }
}

/** Tab → destination mapping. */
private val LockInTab.route: Screen
    get() = when (this) {
        LockInTab.FocusHub -> Screen.FocusHub
        LockInTab.TriggerVault -> Screen.TriggerVault
        LockInTab.Notes -> Screen.Notes
        LockInTab.Analytics -> Screen.Analytics
    }

/**
 * Which anchor tab a destination belongs to, or null for full-screen routes
 * (session overlay, scanners, settings). Drives the directional tab slide.
 */
private val NavDestination.tabIndex: Int?
    get() = when {
        hasRoute(Screen.FocusHub::class) -> 0
        hasRoute(Screen.TriggerVault::class) -> 1
        hasRoute(Screen.Notes::class) -> 2
        hasRoute(Screen.Analytics::class) -> 3
        else -> null
    }
