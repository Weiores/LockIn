package com.lockin.app.util

object Constants {
    const val TIMER_NOTIFICATION_CHANNEL_ID = "lockin_timer_channel"
    const val GENERAL_NOTIFICATION_CHANNEL_ID = "lockin_general_channel"
    const val APP_CLOSED_CHANNEL_ID = "lockin_app_closed_channel"
    const val DISTRACTION_CHANNEL_ID = "lockin_distraction_channel"

    const val TIMER_NOTIFICATION_ID = 1001
    const val DISTRACTION_NOTIFICATION_ID = 1002
    const val WIFI_PROMPT_NOTIFICATION_ID = 1003
    const val APP_CLOSED_REMINDER_NOTIFICATION_ID = 1004
    const val SESSION_ENDED_NOTIFICATION_ID = 1005

    /** Seconds the user gets to leave a blacklisted app before the session dies. */
    const val DISTRACTION_GRACE_SECONDS = 10L

    /** How often the guard reads the foreground package. */
    const val DISTRACTION_POLL_INTERVAL_MS = 700L

    const val DEFAULT_POMODORO_MINUTES = 25
    const val DEFAULT_BREAK_MINUTES = 5
}
