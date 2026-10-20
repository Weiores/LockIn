package com.lockin.app.di

import android.content.Context
import androidx.room.Room
import com.lockin.app.data.local.db.LockInDatabase
import com.lockin.app.data.local.db.MIGRATION_1_2
import com.lockin.app.data.local.db.MIGRATION_2_3
import com.lockin.app.data.local.db.dao.ModuleDao
import com.lockin.app.data.local.db.dao.SessionDao
import com.lockin.app.data.local.db.dao.TriggerDao
import com.lockin.app.data.local.db.dao.WeeklyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LockInDatabase =
        Room.databaseBuilder(
            context,
            LockInDatabase::class.java,
            "lockin_database",
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideModuleDao(db: LockInDatabase): ModuleDao = db.moduleDao()

    @Provides
    fun provideSessionDao(db: LockInDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideTriggerDao(db: LockInDatabase): TriggerDao = db.triggerDao()

    @Provides
    fun provideWeeklyDao(db: LockInDatabase): WeeklyDao = db.weeklyDao()
}

