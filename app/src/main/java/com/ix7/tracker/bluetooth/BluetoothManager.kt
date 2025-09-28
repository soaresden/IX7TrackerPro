package com.ix7.tracker.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.util.Log
import com.ix7.tracker.core.*
import com.ix7.tracker.protocol.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import kotlinx.coroutines.runBlocking

/**
 * Implémentation concrète du repository Bluetooth pour Android
 */
@SuppressLint("MissingPermission")
class BluetoothManager(private val context: Context) : BluetoothRepository {

    private val TAG = "BluetoothManager"

    // Composants Bluetooth
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var currentScooterType: ScooterType = ScooterType.UNKNOWN

    // États internes
    private var scanningActive = false

    // StateFlows pour observer les changements
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    override val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _scooterData = MutableStateFlow(ScooterData())
    override val scooterData: StateFlow<ScooterData> = _scooterData.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Callback pour le scan BLE
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val deviceName = device.name
                val rssi = result.rssi

                Log.d(TAG, "Device found: ${deviceName ?: "Unknown"} [${device.address}] RSSI: $rssi")

                if (ScooterDetector.isScooterDevice(deviceName)) {
                    val scooterType = ScooterDetector.detectScooterType(deviceName)
                    val deviceInfo = BluetoothDeviceInfo(
                        name = deviceName,
                        address = device.address,
                        rssi = rssi,
                        isScooter = true,
                        scooterType = scooterType,
                        distance = ScooterDetector.estimateDistance(rssi)
                    )

                    val currentDevices = _discoveredDevices.value.toMutableList()
                    val existingIndex = currentDevices.indexOfFirst { it.address == deviceInfo.address }

                    if (existingIndex >= 0) {
                        // Mettre à jour le RSSI de l'appareil existant
                        currentDevices[existingIndex] = deviceInfo
                    } else {
                        // Ajouter le nouvel appareil
                        currentDevices.add(deviceInfo)
                    }

                    _discoveredDevices.value = currentDevices
                    Log.d(TAG, "Scooter ajouté/mis à jour: $deviceName (${scooterType.description})")
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code: $errorCode")
            _connectionState.value = ConnectionState.ERROR
            _isScanning.value = false
            scanningActive = false
        }
    }

    // Callback pour l'API legacy (Android < 5.0)
    private val legacyScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, _ ->
        val deviceName = device?.name
        Log.d(TAG, "Legacy scan - Device: ${deviceName ?: "Unknown"} [${device?.address}] RSSI: $rssi")

        if (device != null && ScooterDetector.isScooterDevice(deviceName)) {
            val scooterType = ScooterDetector.detectScooterType(deviceName)
            val deviceInfo = BluetoothDeviceInfo(
                name = deviceName,
                address = device.address,
                rssi = rssi,
                isScooter = true,
                scooterType = scooterType,
                distance = ScooterDetector.estimateDistance(rssi)
            )

            val currentDevices = _discoveredDevices.value.toMutableList()
            if (currentDevices.none { it.address == deviceInfo.address }) {
                currentDevices.add(deviceInfo)
                _discoveredDevices.value = currentDevices
                Log.d(TAG, "Legacy scooter ajouté: $deviceName")
            }
        }
    }

    // Callback GATT pour la communication
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connecté au serveur GATT")
                    _connectionState.value = ConnectionState.CONNECTED
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Déconnecté du serveur GATT")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _scooterData.value = _scooterData.value.copy(isConnected = false)
                    bluetoothGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services découverts")
                setupNotifications()
            } else {
                Log.w(TAG, "Échec de découverte des services: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic?.value?.let { data ->
                    processReceivedData(data)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.value?.let { data ->
                processReceivedData(data)
            }
        }
    }

    // Implémentation des méthodes du repository

    override fun initialize(): Result<Unit> {
        return try {
            if (bluetoothAdapter == null) {
                return Result.failure(Exception("Bluetooth non supporté"))
            }

            if (!isBluetoothEnabled()) {
                return Result.failure(Exception("Bluetooth désactivé"))
            }

            if (!hasNecessaryPermissions()) {
                return Result.failure(Exception("Permissions manquantes"))
            }

            Log.d(TAG, "BluetoothManager initialisé avec succès")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur d'initialisation", e)
            Result.failure(e)
        }
    }

    override suspend fun startScan(): Result<Unit> {
        return try {
            if (scanningActive) {
                return Result.success(Unit)
            }

            if (!hasNecessaryPermissions()) {
                return Result.failure(Exception("Permissions Bluetooth manquantes"))
            }

            if (!isBluetoothEnabled()) {
                return Result.failure(Exception("Bluetooth désactivé"))
            }

            clearDiscoveredDevices()
            _connectionState.value = ConnectionState.SCANNING
            _isScanning.value = true
            scanningActive = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                bluetoothLeScanner?.startScan(null, settings, scanCallback)
                Log.d(TAG, "Scan moderne démarré")
            } else {
                bluetoothAdapter?.startLeScan(legacyScanCallback)
                Log.d(TAG, "Scan legacy démarré")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du démarrage du scan", e)
            _connectionState.value = ConnectionState.ERROR
            _isScanning.value = false
            scanningActive = false
            Result.failure(e)
        }
    }

    override suspend fun stopScan(): Result<Unit> {
        return try {
            if (!scanningActive) {
                return Result.success(Unit)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bluetoothLeScanner?.stopScan(scanCallback)
            } else {
                bluetoothAdapter?.stopLeScan(legacyScanCallback)
            }

            scanningActive = false
            _isScanning.value = false

            if (_connectionState.value == ConnectionState.SCANNING) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            Log.d(TAG, "Scan arrêté")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'arrêt du scan", e)
            Result.failure(e)
        }
    }

    override suspend fun connectToDevice(address: String): Result<Unit> {
        return try {
            stopScan()

            val device = bluetoothAdapter?.getRemoteDevice(address)
                ?: return Result.failure(Exception("Appareil non trouvé"))

            // Détecter le type de scooter
            val deviceInfo = _discoveredDevices.value.find { it.address == address }
            currentScooterType = deviceInfo?.scooterType ?: ScooterType.TYPE_1

            _connectionState.value = ConnectionState.CONNECTING
            bluetoothGatt = device.connectGatt(context, false, gattCallback)

            Log.d(TAG, "Connexion en cours vers $address (${currentScooterType.description})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur de connexion", e)
            _connectionState.value = ConnectionState.ERROR
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            _connectionState.value = ConnectionState.DISCONNECTED
            _scooterData.value = _scooterData.value.copy(isConnected = false)

            Log.d(TAG, "Déconnexion effectuée")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur de déconnexion", e)
            Result.failure(e)
        }
    }

    override suspend fun sendCommand(command: ByteArray): Result<Unit> {
        return try {
            val gatt = bluetoothGatt
                ?: return Result.failure(Exception("Pas de connexion GATT"))

            val writeCharacteristic = findWriteCharacteristic(gatt)
                ?: return Result.failure(Exception("Caractéristique d'écriture non trouvée"))

            writeCharacteristic.value = command
            val success = gatt.writeCharacteristic(writeCharacteristic)

            if (success) {
                Log.d(TAG, "Commande envoyée: ${command.joinToString(" ") { "%02X".format(it) }}")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Échec d'envoi de la commande"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur d'envoi de commande", e)
            Result.failure(e)
        }
    }

    override fun cleanup() {
        try {
            // Utiliser runBlocking pour les appels suspend dans cleanup
            runBlocking {
                stopScan()
                disconnect()
            }
            Log.d(TAG, "Nettoyage effectué")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du nettoyage", e)
        }
    }
    override fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    override fun hasNecessaryPermissions(): Boolean {
        return PermissionHelper.hasAllBluetoothPermissions(context)
    }

    override fun clearDiscoveredDevices() {
        _discoveredDevices.value = emptyList()
    }

    // Méthodes privées

    private fun setupNotifications() {
        val gatt = bluetoothGatt ?: return
        val notifyCharacteristic = findNotifyCharacteristic(gatt) ?: return

        val success = gatt.setCharacteristicNotification(notifyCharacteristic, true)
        if (success) {
            val descriptor = notifyCharacteristic.getDescriptor(UUID.fromString(BluetoothConstants.CCCD_UUID))
            descriptor?.let {
                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(it)
                Log.d(TAG, "Notifications configurées")
            }
        }
    }

    private fun findNotifyCharacteristic(gatt: BluetoothGatt): BluetoothGattCharacteristic? {
        val serviceUuid = UUID.fromString(currentScooterType.uuid_service)
        val characteristicUuid = UUID.fromString(currentScooterType.uuid_notify)

        return gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
    }

    private fun findWriteCharacteristic(gatt: BluetoothGatt): BluetoothGattCharacteristic? {
        val serviceUuid = UUID.fromString(currentScooterType.uuid_service)
        val characteristicUuid = UUID.fromString(currentScooterType.uuid_write)

        return gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
    }

    private fun processReceivedData(data: ByteArray) {
        val currentData = _scooterData.value
        val parsedData = DataParser.parseScooterFrame(data, currentData)
        _scooterData.value = parsedData
    }
}