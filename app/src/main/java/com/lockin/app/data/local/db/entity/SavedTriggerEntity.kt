package com.lockin.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "saved_triggers")
data class SavedTriggerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "value")
    val value: String,

    @ColumnInfo(name = "is_strict")
    val isStrict: Boolean = false,

    @ColumnInfo(name = "auto_prompt")
    val autoPrompt: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),
)
