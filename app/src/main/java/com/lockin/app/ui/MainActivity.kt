package com.lockin.app.ui

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.lockin.app.domain.model.AccentScheme
import com.lockin.app.domain.model.ThemeMode
import com.lockin.app.domain.model.TimerState
import com.lockin.app.domain.repository.PreferencesRepository
import com.lockin.app.service.TimerEngine
import com.lockin.app.service.TimerNotificationHelper
import com.lockin.app.ui.navigation.LockInNavGraph
import com.lockin.app.ui.theme.LockInTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var timerEngine: TimerEngine

    @Inject
    lateinit var notificationHelper: TimerNotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure notification permission is requested on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            val themeMode by preferencesRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val accentScheme by preferencesRepository.accentScheme.collectAsState(initial = AccentScheme.BLUE)

            LockInTheme(mode = themeMode, scheme = accentScheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    LockInNavGraph()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Clear the app-closed heads-up reminder when the user re-opens the app
        notificationHelper.cancelAppClosedNotification()
    }

    override fun onStop() {
        super.onStop()
        // If the user closed or minimized the app while a focus timer is active,
        // immediately post a prominent notification reminder
        val state = timerEngine.timerState.value
        if (state is TimerState.ActiveLocked) {
            notificationHelper.showAppClosedNotification(
                remainingMs = state.remainingMs,
                moduleCode = timerEngine.activeModuleCode,
            )
        }
    }
}
