package com.lockin.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lockin.app.data.local.db.converter.Converters
import com.lockin.app.data.local.db.dao.ModuleDao
import com.lockin.app.data.local.db.dao.SessionDao
import com.lockin.app.data.local.db.dao.TriggerDao
import com.lockin.app.data.local.db.entity.ModuleEntity
import com.lockin.app.data.local.db.entity.SavedTriggerEntity
import com.lockin.app.data.local.db.entity.SessionEntity

@Database(
    entities = [
        ModuleEntity::class,
        SessionEntity::class,
        SavedTriggerEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class LockInDatabase : RoomDatabase() {
    abstract fun moduleDao(): ModuleDao
    abstract fun sessionDao(): SessionDao
    abstract fun triggerDao(): TriggerDao
}
