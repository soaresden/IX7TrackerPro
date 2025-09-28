package com.ix7.tracker.bluetooth

import android.content.Context
import com.ix7.tracker.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BluetoothManagerImpl(private val context: Context) : BluetoothRepository {

    private val scanner = BluetoothScanner(context) { devices ->
        _discoveredDevices.value = devices
    }

    private val connector = BluetoothConnector(context) { state ->
        _connectionState.value = state
    }

    private val dataHandler = BluetoothDataHandler(
        onDataUpdate = { scooterData ->
            _scooterData.value = scooterData
        },
        sendCommand = { command ->
            // Déléguer l'envoi de commande au connector
            connector.sendCommand(command)
        }
    )

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    private val _scooterData = MutableStateFlow(ScooterData())
    private val _isScanning = MutableStateFlow(false)

    override val connectionState: StateFlow<ConnectionState> = _connectionState
    override val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices
    override val scooterData: StateFlow<ScooterData> = _scooterData
    override val isScanning: StateFlow<Boolean> = _isScanning

    override suspend fun startScan(): Result<Unit> {
        _isScanning.value = true
        return scanner.startScan()
    }

    override suspend fun stopScan(): Result<Unit> {
        _isScanning.value = false
        return scanner.stopScan()
    }

    override suspend fun connectToDevice(address: String): Result<Unit> {
        return connector.connect(address) { byteArrayData ->
            dataHandler.handleData(byteArrayData)
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return connector.disconnect()
    }

    override suspend fun sendCommand(command: ByteArray): Result<Unit> {
        return connector.sendCommand(command)
    }

    override fun initialize(): Result<Unit> {
        return scanner.initialize()
    }

    override fun cleanup() {
        scanner.cleanup()
        connector.cleanup()
    }

    override fun isBluetoothEnabled(): Boolean = scanner.isBluetoothEnabled()
    override fun hasNecessaryPermissions(): Boolean = PermissionHelper.hasAllBluetoothPermissions(context)
    override fun clearDiscoveredDevices() {
        _discoveredDevices.value = emptyList()
    }
}