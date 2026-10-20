package com.lockin.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lockin.app.data.local.db.converter.Converters
import com.lockin.app.data.local.db.dao.ModuleDao
import com.lockin.app.data.local.db.dao.SessionDao
import com.lockin.app.data.local.db.dao.TriggerDao
import com.lockin.app.data.local.db.dao.WeeklyDao
import com.lockin.app.data.local.db.entity.ModuleEntity
import com.lockin.app.data.local.db.entity.SavedTriggerEntity
import com.lockin.app.data.local.db.entity.SessionEntity
import com.lockin.app.data.local.db.entity.WeeklyGoalEntity
import com.lockin.app.data.local.db.entity.WeeklyNoteEntity

@Database(
    entities = [
        ModuleEntity::class,
        SessionEntity::class,
        SavedTriggerEntity::class,
        WeeklyNoteEntity::class,
        WeeklyGoalEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class LockInDatabase : RoomDatabase() {
    abstract fun moduleDao(): ModuleDao
    abstract fun sessionDao(): SessionDao
    abstract fun triggerDao(): TriggerDao
    abstract fun weeklyDao(): WeeklyDao
}

/**
 * v2 adds the per-module goal columns. Existing rows keep their history and
 * simply start without a goal.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE modules ADD COLUMN weekly_goal_minutes INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL(
            "ALTER TABLE modules ADD COLUMN goal_note TEXT NOT NULL DEFAULT ''",
        )
    }
}

/**
 * v3 adds the weekly journal. Both tables are empty on upgrade, so nothing has
 * to be backfilled.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS weekly_notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                week_start INTEGER NOT NULL,
                body TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_weekly_notes_week_start ON weekly_notes(week_start)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS weekly_goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                week_start INTEGER NOT NULL,
                text TEXT NOT NULL,
                is_done INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_weekly_goals_week_start ON weekly_goals(week_start)")
    }
}
