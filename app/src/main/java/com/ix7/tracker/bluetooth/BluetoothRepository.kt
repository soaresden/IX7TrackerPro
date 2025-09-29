package com.ix7.tracker.bluetooth

import android.bluetooth.BluetoothDevice
import com.ix7.tracker.core.*
import kotlinx.coroutines.flow.StateFlow

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

    // Nouvelle méthode pour la connexion directe
    suspend fun connect(device: BluetoothDevice): Result<Unit>
    suspend fun unlockScooter(): Result<Unit>
}