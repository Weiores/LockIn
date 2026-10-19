package com.lockin.app.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation route definitions for LockIn.
 * Each object/class corresponds to a destination in the NavGraph.
 */
sealed interface Screen {

    /** Tab 1 — Focus Hub dashboard */
    @Serializable
    data object FocusHub : Screen

    /** Tab 2 — Trigger Vault (manage saved triggers) */
    @Serializable
    data object TriggerVault : Screen

    /** Tab 3 — Study Analytics */
    @Serializable
    data object Analytics : Screen

    /** Tab 4 — Settings */
    @Serializable
    data object Settings : Screen

    /** Active session overlay (full-screen, launched on top) */
    @Serializable
    data class ActiveSession(
        val sessionId: Long = -1L,
    ) : Screen

    /** Barcode scanner screen */
    @Serializable
    data object BarcodeScanner : Screen
}
