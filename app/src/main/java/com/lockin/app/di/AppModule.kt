package com.lockin.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Singleton provisions (e.g. SensorManager, NfcAdapter, CameraX)
    // will be added in Phase 3+
}

