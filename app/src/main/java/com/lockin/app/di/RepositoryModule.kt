package com.lockin.app.di

import com.lockin.app.data.repository.ModuleRepositoryImpl
import com.lockin.app.data.repository.PreferencesRepositoryImpl
import com.lockin.app.data.repository.SessionRepositoryImpl
import com.lockin.app.data.repository.TriggerRepositoryImpl
import com.lockin.app.data.repository.WeeklyRepositoryImpl
import com.lockin.app.domain.repository.ModuleRepository
import com.lockin.app.domain.repository.PreferencesRepository
import com.lockin.app.domain.repository.SessionRepository
import com.lockin.app.domain.repository.TriggerRepository
import com.lockin.app.domain.repository.WeeklyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindModuleRepository(impl: ModuleRepositoryImpl): ModuleRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindTriggerRepository(impl: TriggerRepositoryImpl): TriggerRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindWeeklyRepository(impl: WeeklyRepositoryImpl): WeeklyRepository
}

