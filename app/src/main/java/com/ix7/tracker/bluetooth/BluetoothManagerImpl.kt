package com.ix7.tracker.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.ix7.tracker.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BluetoothManagerImpl(
    private val context: Context
) : BluetoothRepository {

    companion object {
        private const val TAG = "BluetoothManager"
    }

    // States
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _scooterData = MutableStateFlow(ScooterData())
    override val scooterData: StateFlow<ScooterData> = _scooterData

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning

    // Components
    private val scanner = BluetoothScanner(context) { devices ->
        _discoveredDevices.value = devices
    }

    // ✅ BluetoothConnector gère tout en interne (dataHandler, etc.)
    private val _connector = BluetoothConnector(
        context = context,
        onDataReceived = { scooterData ->
            // Recevoir les données décodées du connector
            _scooterData.value = scooterData
            Log.d(TAG, "Données mises à jour: ${scooterData.speed}km/h ${scooterData.battery}%")
        },
        onStateChange = { state ->
            _connectionState.value = state
        }
    )

    // ✅ Exposer le connector
    override val connector: BluetoothConnector = _connector

    // Scan
    override suspend fun startScan(): Result<Unit> {
        return try {
            _isScanning.value = true
            scanner.startScan()
            Log.i(TAG, "Scan démarré")
            Result.success(Unit)
        } catch (e: Exception) {
            _isScanning.value = false
            Log.e(TAG, "Erreur scan", e)
            Result.failure(e)
        }
    }

    override suspend fun stopScan(): Result<Unit> {
        scanner.stopScan()
        _isScanning.value = false
        Log.i(TAG, "Scan arrêté")
        return Result.success(Unit)
    }

    // Connexion
    override suspend fun connectToDevice(address: String): Result<Unit> {
        stopScan()
        return _connector.connect(address)
    }

    override suspend fun connect(device: BluetoothDevice): Result<Unit> {
        return connectToDevice(device.address)
    }

    override suspend fun disconnect(): Result<Unit> {
        return _connector.disconnect()
    }

    // Commandes
    override suspend fun sendCommand(command: ByteArray): Result<Unit> {
        return _connector.sendCommand(command)
    }

    // Lifecycle
    override fun initialize(): Result<Unit> {
        return scanner.initialize()
    }

    override fun cleanup() {
        scanner.cleanup()
        _connector.cleanup()
        Log.i(TAG, "Cleanup terminé")
    }

    // Utilitaires
    override fun isBluetoothEnabled(): Boolean {
        return scanner.isBluetoothEnabled()
    }

    override fun hasNecessaryPermissions(): Boolean {
        return PermissionHelper.hasAllBluetoothPermissions(context)
    }

    override fun clearDiscoveredDevices() {
        _discoveredDevices.value = emptyList()
    }
}