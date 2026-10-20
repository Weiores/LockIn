package com.lockin.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** One tick-box the user wants to finish inside a given week. */
@Entity(tableName = "weekly_goals")
data class WeeklyGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** ISO epoch day of the Monday that begins the week. */
    @ColumnInfo(name = "week_start")
    val weekStart: Long,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo(name = "is_done")
    val isDone: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),
)
