package com.ix7.tracker.di

import android.content.Context
import com.ix7.tracker.bluetooth.BluetoothRepository
import com.ix7.tracker.utils.LogManager
import com.ix7.tracker.utils.PreferencesManager

/**
 * Module d'injection de dépendances simple
 * (Alternative à Dagger/Hilt pour ce projet)
 */
object AppModule {
    fun provideBluetoothRepository(context: Context): BluetoothRepository {
        return BluetoothManagerImpl(context)
    }

    fun provideLogManager(): LogManager {
        return LogManager.getInstance()
    }

    fun providePreferencesManager(context: Context): PreferencesManager {
        return PreferencesManager(context)
    }
}