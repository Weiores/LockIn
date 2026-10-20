package com.lockin.app.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DistractionGuard @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val usageStatsManager = context.getSystemService<UsageStatsManager>()
    private val appOpsManager = context.getSystemService<AppOpsManager>()

    /**
     * Checks if the user has granted PACKAGE_USAGE_STATS permission via System Settings.
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = appOpsManager ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Creates an intent to navigate the user directly to the Usage Access settings screen.
     */
    fun getUsageAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

    /**
     * Retrieves the package name of the app currently in the foreground using UsageEvents.
     */
    fun getForegroundPackageName(): String? {
        val usm = usageStatsManager ?: return null
        val now = System.currentTimeMillis()
        val startTime = now - 5000L // Query the last 5 seconds

        val usageEvents = usm.queryEvents(startTime, now)
        val event = UsageEvents.Event()
        var lastResumedPackage: String? = null

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastResumedPackage = event.packageName
            }
        }

        return lastResumedPackage
    }

    /**
     * Synchronised read of the foreground package. Used by the distraction guard
     * while it counts down its grace window, where several reads must agree with
     * each other even though they come from a different coroutine.
     */
    @Synchronized
    fun readForegroundPackageName(): String? =
        if (hasUsageStatsPermission()) getForegroundPackageName() else null

    /**
     * Periodically polls the foreground application package at the given [pollIntervalMs].
     */
    fun observeForegroundApp(pollIntervalMs: Long = 1500L): Flow<String?> = flow {
        while (true) {
            val currentPkg = if (hasUsageStatsPermission()) {
                getForegroundPackageName()
            } else {
                null
            }
            emit(currentPkg)
            delay(pollIntervalMs)
        }
    }.distinctUntilChanged()
}

