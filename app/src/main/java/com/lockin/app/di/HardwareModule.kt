package com.lockin.app.di

import android.content.Context
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import com.lockin.app.hardware.flip.FlipDetector
import com.lockin.app.hardware.haptics.HapticFeedbackHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HardwareModule {

    @Provides
    @Singleton
    fun provideSensorManager(@ApplicationContext context: Context): SensorManager? =
        context.getSystemService<SensorManager>()

    @Provides
    @Singleton
    fun provideFlipDetector(@ApplicationContext context: Context): FlipDetector =
        FlipDetector(context)

    @Provides
    @Singleton
    fun provideHapticFeedbackHelper(@ApplicationContext context: Context): HapticFeedbackHelper =
        HapticFeedbackHelper(context)
}
