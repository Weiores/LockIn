package com.lockin.app.domain.model

import java.time.Instant

data class StudySession(
    val id: Long = 0,
    val moduleId: Long?,
    val plannedDurationMs: Long,
    val actualDurationMs: Long,
    val startTime: Instant,
    val endTime: Instant?,
    val triggerType: TriggerType,
    val triggerValue: String?,
    val outcome: SessionOutcome,
    val breakCount: Int = 0,
    val notes: String? = null,
)
