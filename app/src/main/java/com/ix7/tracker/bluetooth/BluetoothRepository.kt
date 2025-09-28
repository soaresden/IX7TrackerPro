package com.ix7.tracker.bluetooth

import com.ix7.tracker.core.*
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface pour la gestion Bluetooth (Repository Pattern)
 * Permet de découpler la logique métier de l'implémentation Android
 */
interface BluetoothRepository {

    // États observables
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>>
    val connectionState: StateFlow<ConnectionState>
    val scooterData: StateFlow<ScooterData>
    val isScanning: StateFlow<Boolean>

    // Opérations principales
    suspend fun startScan(): Result<Unit>
    suspend fun stopScan(): Result<Unit>
    suspend fun connectToDevice(address: String): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    suspend fun sendCommand(command: ByteArray): Result<Unit>

    // Gestion du cycle de vie
    fun initialize(): Result<Unit>
    fun cleanup()

    // Utilitaires
    fun isBluetoothEnabled(): Boolean
    fun hasNecessaryPermissions(): Boolean
    fun clearDiscoveredDevices()
}