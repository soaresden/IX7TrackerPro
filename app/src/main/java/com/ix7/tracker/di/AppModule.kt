package com.ix7.tracker.di

import android.content.Context
import com.ix7.tracker.bluetooth.BluetoothRepository
import com.ix7.tracker.bluetooth.BluetoothManagerImpl  // <-- AJOUTEZ CETTE LIGNE
import com.ix7.tracker.utils.LogManager
import com.ix7.tracker.utils.PreferencesManager

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