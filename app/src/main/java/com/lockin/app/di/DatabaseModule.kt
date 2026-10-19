package com.lockin.app.di

import android.content.Context
import androidx.room.Room
import com.lockin.app.data.local.db.LockInDatabase
import com.lockin.app.data.local.db.dao.ModuleDao
import com.lockin.app.data.local.db.dao.SessionDao
import com.lockin.app.data.local.db.dao.TriggerDao
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
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideModuleDao(db: LockInDatabase): ModuleDao = db.moduleDao()

    @Provides
    fun provideSessionDao(db: LockInDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideTriggerDao(db: LockInDatabase): TriggerDao = db.triggerDao()
}

