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

    private var scanner: BluetoothScanner? = null
    private var connector: BluetoothConnector? = null

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    // États
    override val discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val scooterData = MutableStateFlow(ScooterData())
    override val isScanning = MutableStateFlow(false)

    private val connectionCallback = object : BluetoothConnector.ConnectionCallback {
        override fun onConnected() {
            Log.d(TAG, "✅ Connected")
            connectionState.value = ConnectionState.CONNECTED
        }

        override fun onDisconnected() {
            Log.d(TAG, "❌ Disconnected")
            connectionState.value = ConnectionState.DISCONNECTED
        }

        override fun onServicesDiscovered() {
            Log.d(TAG, "📋 Services discovered - Ready to send commands")
            // L'appli officielle ne fait RIEN automatiquement ici
            // Vous devez appeler sendUnlockCommand() manuellement depuis l'UI
        }

        override fun onDataReceived(data: ByteArray) {
            Log.d(TAG, "📥 Data: ${data.joinToString(" ") { "%02X".format(it) }}")
            // TODO: Parser les données
        }

        override fun onError(message: String) {
            Log.e(TAG, "❌ Error: $message")
            connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    override fun initialize(): Result<Unit> {
        return try {
            scanner = BluetoothScanner(context) { devices ->
                discoveredDevices.value = devices
            }
            scanner?.initialize()
            Log.d(TAG, "✅ Initialized")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Init error: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun startScan(): Result<Unit> {
        isScanning.value = true
        val result = scanner?.startScan() ?: Result.failure(Exception("Scanner not initialized"))
        if (result.isFailure) {
            isScanning.value = false
        }
        return result
    }

    override suspend fun stopScan(): Result<Unit> {
        isScanning.value = false
        return scanner?.stopScan() ?: Result.failure(Exception("Scanner not initialized"))
    }

    override suspend fun connectToDevice(address: String): Result<Unit> {
        return try {
            Log.d(TAG, "🔗 Connecting to $address")

            stopScan()
            connectionState.value = ConnectionState.CONNECTING

            val device = bluetoothAdapter?.getRemoteDevice(address)
                ?: return Result.failure(Exception("BluetoothAdapter unavailable"))

            Log.d(TAG, "📱 Device: ${device.name} (${device.address})")

            connector = BluetoothConnector(context, connectionCallback)
            connector?.connect(device)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Connection error: ${e.message}")
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

    // NOUVEAU : Méthode pour envoyer la commande unlock
    override suspend fun unlockScooter(): Result<Unit> {
        return try {
            Log.d(TAG, "🔓 Unlocking scooter...")
            connector?.sendUnlockCommand()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unlock error: ${e.message}")
            Result.failure(e)
        }
    }

    override fun cleanup() {
        scanner?.cleanup()
        connector?.disconnect()
    }

    override fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
    override fun hasNecessaryPermissions(): Boolean = true
    override fun clearDiscoveredDevices() {
        discoveredDevices.value = emptyList()
    }

    override suspend fun sendCommand(command: ByteArray): Result<Unit> = Result.success(Unit)
}