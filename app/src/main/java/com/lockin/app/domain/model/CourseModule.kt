package com.lockin.app.domain.model

import java.time.Instant

data class CourseModule(
    val id: Long = 0,
    val code: String,
    val name: String,
    val colorHex: String,
    val createdAt: Instant = Instant.now(),
    val isArchived: Boolean = false,
)
