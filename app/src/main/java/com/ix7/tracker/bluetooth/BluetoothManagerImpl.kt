package com.ix7.tracker.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import com.ix7.tracker.core.BluetoothDeviceInfo
import com.ix7.tracker.core.ConnectionState
import com.ix7.tracker.core.ScooterData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implémentation du BluetoothRepository
 * VERSION CORRIGÉE - Types compatibles
 */
class BluetoothManagerImpl(private val context: Context) : BluetoothRepository {

    companion object {
        private const val TAG = "BT_MANAGER"
    }

    // États
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val _scooterData = MutableStateFlow(ScooterData())
    private val _isScanning = MutableStateFlow(false)

    // ✅ AJOUT - Flow pour les trames brutes
    private val _rawFrameFlow = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 64)
    override val rawFrameFlow: Flow<ByteArray> = _rawFrameFlow.asSharedFlow()

    override val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices.asStateFlow()
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    override val scooterData: StateFlow<ScooterData> = _scooterData.asStateFlow()
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Scanner
    private val scanner = BluetoothScanner(
        context = context,
        onDevicesFound = { devices ->
            _discoveredDevices.value = devices
        }
    )

    // Connector
    private val _connector = BluetoothConnector(
        context = context,
        onDataReceived = { scooterData ->
            _scooterData.value = scooterData
            Log.d(TAG, "Données: ${scooterData.speed}km/h ${scooterData.battery}%")
        },
        onStateChange = { state ->
            _connectionState.value = state
        },
        // ✅ AJOUT - Callback pour les données brutes
        onRawDataReceived = { rawData ->
            android.util.Log.d("BT_MANAGER", "🔥 rawData reçu dans callback: ${rawData.size} bytes")
            val emitted = _rawFrameFlow.tryEmit(rawData)
            android.util.Log.d("BT_MANAGER", "✅ tryEmit résultat: $emitted")
        }
    )

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

    // Connexion - CORRIGÉ
    override suspend fun connectToDevice(address: String): Result<Unit> {
        stopScan()

        // Convertir l'adresse en BluetoothDevice
        return try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val device = bluetoothAdapter.getRemoteDevice(address)
            _connector.connect(device)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur conversion address -> device", e)
            Result.failure(e)
        }
    }

    override suspend fun connect(device: BluetoothDevice): Result<Unit> {
        stopScan()
        return _connector.connect(device)
    }

    override suspend fun disconnect(): Result<Unit> {
        // CORRIGÉ - disconnect retourne maintenant Result<Unit>
        _connector.disconnect()
        return Result.success(Unit)
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