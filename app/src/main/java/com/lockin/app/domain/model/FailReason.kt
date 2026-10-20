package com.lockin.app.domain.model

enum class FailReason {
    USER_CANCELLED,
    TRIGGER_LOST,

    /** The user stayed in a blacklisted distraction app for the whole grace window. */
    DISTRACTION,
}
