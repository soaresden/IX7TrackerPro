package com.ix7.tracker.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.ix7.tracker.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BluetoothManagerImpl(private val context: Context) : BluetoothRepository {

    companion object {
        private const val TAG = "BluetoothManager"
    }

    // Scanner et Connector
    private var scanner: BluetoothScanner? = null
    private var connector: BluetoothConnector? = null

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    // États observables
    override val discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val scooterData = MutableStateFlow(ScooterData())
    override val isScanning = MutableStateFlow(false)

    // Callback du connector
    private val connectionCallback = object : BluetoothConnector.ConnectionCallback {
        override fun onConnected() {
            Log.d(TAG, "✅ Connecté")
            connectionState.value = ConnectionState.CONNECTED
        }

        override fun onDisconnected() {
            Log.d(TAG, "❌ Déconnecté")
            connectionState.value = ConnectionState.DISCONNECTED
        }

        override fun onDataReceived(data: ByteArray) {
            Log.d(TAG, "📥 Données: ${data.joinToString(" ") { "%02X".format(it) }}")
            // TODO: Parser et mettre à jour scooterData
        }

        override fun onError(message: String) {
            Log.e(TAG, "❌ Erreur: $message")
            connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    // Initialisation
    override fun initialize(): Result<Unit> {
        return try {
            scanner = BluetoothScanner(context) { devices ->
                discoveredDevices.value = devices
            }
            scanner?.initialize()
            Log.d(TAG, "✅ Initialisé")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur init: ${e.message}")
            Result.failure(e)
        }
    }

    // SCAN
    override suspend fun startScan(): Result<Unit> {
        isScanning.value = true
        val result = scanner?.startScan() ?: Result.failure(Exception("Scanner non initialisé"))
        if (result.isFailure) {
            isScanning.value = false
        }
        return result
    }

    override suspend fun stopScan(): Result<Unit> {
        isScanning.value = false
        return scanner?.stopScan() ?: Result.failure(Exception("Scanner non initialisé"))
    }

    // CONNEXION
    override suspend fun connectToDevice(address: String): Result<Unit> {
        return try {
            Log.d(TAG, "🔗 Connexion à $address")

            // Arrêter le scan
            stopScan()

            connectionState.value = ConnectionState.CONNECTING

            // Créer le BluetoothDevice
            val device = bluetoothAdapter?.getRemoteDevice(address)
                ?: return Result.failure(Exception("BluetoothAdapter non disponible"))

            Log.d(TAG, "📱 Device: ${device.name} (${device.address})")

            // Créer et connecter
            connector = BluetoothConnector(context, connectionCallback)
            connector?.connect(device)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur connexion: ${e.message}")
            connectionState.value = ConnectionState.DISCONNECTED
            Result.failure(e)
        }
    }

    override suspend fun connect(device: BluetoothDevice): Result<Unit> {
        return try {
            connector = BluetoothConnector(context, connectionCallback)
            connector?.connect(device)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return try {
            connector?.disconnect()
            connector = null
            connectionState.value = ConnectionState.DISCONNECTED
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // CLEANUP
    override fun cleanup() {
        scanner?.cleanup()
        connector?.disconnect()
    }

    // UTILITAIRES
    override fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
    override fun hasNecessaryPermissions(): Boolean = true
    override fun clearDiscoveredDevices() {
        discoveredDevices.value = emptyList()
    }

    // STUBS
    override suspend fun sendCommand(command: ByteArray): Result<Unit> = Result.success(Unit)
    override suspend fun unlockScooter(): Result<Unit> = Result.success(Unit)
}