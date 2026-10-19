package com.lockin.app.util

object Constants {
    const val TIMER_NOTIFICATION_CHANNEL_ID = "lockin_timer_channel"
    const val GENERAL_NOTIFICATION_CHANNEL_ID = "lockin_general_channel"
    const val TIMER_NOTIFICATION_ID = 1001
    const val GRACE_NOTIFICATION_ID = 1002
    const val WIFI_PROMPT_NOTIFICATION_ID = 1003

    const val DEFAULT_POMODORO_MINUTES = 25
    const val DEFAULT_BREAK_MINUTES = 5
    const val DEFAULT_GRACE_SECONDS = 10

    // Flip detection thresholds
    const val FLIP_Z_FACE_DOWN_THRESHOLD = -8.0f
    const val FLIP_Z_FACE_UP_THRESHOLD = -5.0f
}

