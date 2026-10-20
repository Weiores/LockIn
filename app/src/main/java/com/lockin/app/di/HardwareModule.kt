package com.lockin.app.di

import android.content.Context
import com.lockin.app.hardware.haptics.HapticFeedbackHelper
import com.lockin.app.hardware.nfc.NfcCardReader
import com.lockin.app.hardware.wifi.WifiZoneDetector
import com.lockin.app.service.DistractionGuard
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
    fun provideHapticFeedbackHelper(@ApplicationContext context: Context): HapticFeedbackHelper =
        HapticFeedbackHelper(context)

    @Provides
    @Singleton
    fun provideNfcCardReader(@ApplicationContext context: Context): NfcCardReader =
        NfcCardReader(context)

    @Provides
    @Singleton
    fun provideWifiZoneDetector(@ApplicationContext context: Context): WifiZoneDetector =
        WifiZoneDetector(context)

    @Provides
    @Singleton
    fun provideDistractionGuard(@ApplicationContext context: Context): DistractionGuard =
        DistractionGuard(context)
}

