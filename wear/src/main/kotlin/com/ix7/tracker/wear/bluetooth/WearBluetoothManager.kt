package com.ix7.tracker.wear.bluetooth

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestionnaire centralisé Bluetooth pour Wear OS
 * Gère scan, connexion, lock/unlock du cadenas
 */
class WearBluetoothManager(private val context: Context) {
    companion object {
        private const val TAG = "WEAR_BT_MGR"
    }

    // States
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredScooters = MutableStateFlow<List<ScooterInfo>>(emptyList())
    val discoveredScooters: StateFlow<List<ScooterInfo>> = _discoveredScooters.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _scooterData = MutableStateFlow<ScooterData?>(null)
    val scooterData: StateFlow<ScooterData?> = _scooterData.asStateFlow()

    // Composants
    private val scanner = BluetoothScanner(context) { scooters ->
        _discoveredScooters.value = scooters
    }
    private val connector = BluetoothConnector(context) { state, data ->
        _connectionState.value = state
        if (data != null) {
            _scooterData.value = data
        }
    }

    // Scan
    suspend fun startScan() {
        Log.d(TAG, "🔍 Démarrage du scan...")
        _isScanning.value = true
        scanner.startScan()
    }

    suspend fun stopScan() {
        Log.d(TAG, "🛑 Arrêt du scan")
        _isScanning.value = false
        scanner.stopScan()
    }

    // Connexion
    suspend fun connectToScooter(address: String, name: String) {
        Log.d(TAG, "🔗 Connexion à $name ($address)")
        stopScan()
        _connectionState.value = ConnectionState.CONNECTING
        connector.connect(address)
    }

    suspend fun disconnect() {
        Log.d(TAG, "⚡ Déconnexion")
        connector.disconnect()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    // Cadenas
    fun lockScooter() {
        Log.d(TAG, "🔒 Verrouillage trottinette")
        connector.sendCommand(byteArrayOf(0x61, 0x9E.toByte(), 0x0E, 0x01, 0x01, 0x00, 0xFB.toByte()))
    }

    fun unlockScooter() {
        Log.d(TAG, "🔓 Déverrouillage trottinette")
        connector.sendCommand(byteArrayOf(0x61, 0x9E.toByte(), 0x0E, 0x00, 0x01, 0x00, 0xFC.toByte()))
    }

    fun cleanup() {
        scanner.cleanup()
        connector.cleanup()
    }
}