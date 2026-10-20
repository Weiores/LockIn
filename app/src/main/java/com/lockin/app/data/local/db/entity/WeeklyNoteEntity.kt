package com.lockin.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A freeform note written against a specific week.
 *
 * [weekStart] holds the ISO epoch day of the Monday that begins the week so the
 * query is a plain integer comparison (no date maths in SQL).
 */
@Entity(tableName = "weekly_notes")
data class WeeklyNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "week_start")
    val weekStart: Long,

    @ColumnInfo(name = "body")
    val body: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now(),
)
