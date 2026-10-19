package com.lockin.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = ModuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["module_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("module_id"), Index("start_time")],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "module_id")
    val moduleId: Long?,

    @ColumnInfo(name = "planned_duration_ms")
    val plannedDurationMs: Long,

    @ColumnInfo(name = "actual_duration_ms")
    val actualDurationMs: Long,

    @ColumnInfo(name = "start_time")
    val startTime: Instant,

    @ColumnInfo(name = "end_time")
    val endTime: Instant?,

    @ColumnInfo(name = "trigger_type")
    val triggerType: String,

    @ColumnInfo(name = "trigger_value")
    val triggerValue: String?,

    @ColumnInfo(name = "outcome")
    val outcome: String,

    @ColumnInfo(name = "break_count")
    val breakCount: Int = 0,

    @ColumnInfo(name = "notes")
    val notes: String? = null,
)
