package com.lockin.app.domain.model

import java.time.Instant

data class TriggerProfile(
    val id: Long = 0,
    val type: TriggerType,
    val label: String,
    val value: String,
    val isStrict: Boolean = false,
    val autoPrompt: Boolean = false,
    val createdAt: Instant = Instant.now(),
)
