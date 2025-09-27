package com.ix7.tracker

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * Gestionnaire Bluetooth pour la communication avec le scooter M0Robot
 */
@SuppressLint("MissingPermission")
class BluetoothManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var isScanning = false

    // États observables
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _receivedData = MutableStateFlow<ScooterData?>(null)
    val receivedData: StateFlow<ScooterData?> = _receivedData.asStateFlow()

    // Callback pour la découverte des appareils (API moderne)
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val deviceName = device.name
                Log.d("BluetoothManager", "Device found: ${deviceName ?: "Unknown"} - ${device.address}")

                // Filtrer les appareils M0Robot/scooters
                if (deviceName != null && isScooterDevice(deviceName)) {
                    val deviceInfo = BluetoothDeviceInfo(
                        name = deviceName,
                        address = device.address
                    )

                    val currentDevices = _discoveredDevices.value.toMutableList()
                    if (currentDevices.none { it.address == deviceInfo.address }) {
                        currentDevices.add(deviceInfo)
                        _discoveredDevices.value = currentDevices
                        Log.d("BluetoothManager", "Scooter added: $deviceName")
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BluetoothManager", "Scan failed with error: $errorCode")
            _connectionState.value = ConnectionState.ERROR
            isScanning = false
        }
    }

    // Callback pour l'API legacy (Android < 5.0)
    private val legacyScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        val deviceName = device?.name
        Log.d("BluetoothManager", "Legacy scan - Device found: ${deviceName ?: "Unknown"} - ${device?.address}")

        if (device != null && deviceName != null && isScooterDevice(deviceName)) {
            val deviceInfo = BluetoothDeviceInfo(
                name = deviceName,
                address = device.address
            )

            val currentDevices = _discoveredDevices.value.toMutableList()
            if (currentDevices.none { it.address == deviceInfo.address }) {
                currentDevices.add(deviceInfo)
                _discoveredDevices.value = currentDevices
                Log.d("BluetoothManager", "Legacy scooter added: $deviceName")
            }
        }
    }

    // Callback GATT pour la communication
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d("BluetoothManager", "Connected to GATT server")
                    _connectionState.value = ConnectionState.CONNECTED
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d("BluetoothManager", "Disconnected from GATT server")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    bluetoothGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BluetoothManager", "Services discovered")
                startDataReading()
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic?.value?.let { data ->
                    parseScooterData(data)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.value?.let { data ->
                parseScooterData(data)
            }
        }
    }

    /**
     * Vérifie si le nom de l'appareil correspond à un scooter
     */
    private fun isScooterDevice(deviceName: String): Boolean {
        val scooterPrefixes = listOf(
            "M0", "H1", "M1", "Mini", "Plus", "X1", "X3", "M6",
            "GoKart", "A6", "MI", "N3MTenbot", "miniPLUS_",
            "MiniPro", "SFSO", "V5Robot", "EO STREET", "XRIDER",
            "TECAR", "MAX", "i10", "NEXRIDE", "E-WHEELS", "E12",
            "E9PRO", "T10", "MQRobot"
        )

        return scooterPrefixes.any { prefix ->
            deviceName.startsWith(prefix, ignoreCase = true)
        }
    }

    /**
     * Démarre la découverte des appareils Bluetooth
     */
    fun startDiscovery() {
        Log.d("BluetoothManager", "Starting discovery...")

        if (!hasBluetoothPermission()) {
            Log.e("BluetoothManager", "Missing Bluetooth permissions")
            _connectionState.value = ConnectionState.ERROR
            return
        }

        if (bluetoothAdapter == null) {
            Log.e("BluetoothManager", "Bluetooth adapter is null")
            _connectionState.value = ConnectionState.ERROR
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.e("BluetoothManager", "Bluetooth is not enabled")
            _connectionState.value = ConnectionState.ERROR
            return
        }

        // Nettoyer la liste précédente
        _discoveredDevices.value = emptyList()
        _connectionState.value = ConnectionState.SCANNING
        isScanning = true

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Utiliser l'API moderne
                bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                bluetoothLeScanner?.startScan(null, settings, scanCallback)
                Log.d("BluetoothManager", "Modern scan started")
            } else {
                // Utiliser l'API legacy
                bluetoothAdapter.startLeScan(legacyScanCallback)
                Log.d("BluetoothManager", "Legacy scan started")
            }
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Failed to start scan: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
            isScanning = false
        }
    }

    /**
     * Arrête la découverte des appareils
     */
    fun stopDiscovery() {
        Log.d("BluetoothManager", "Stopping discovery...")

        if (!isScanning) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bluetoothLeScanner?.stopScan(scanCallback)
            } else {
                bluetoothAdapter?.stopLeScan(legacyScanCallback)
            }

            isScanning = false
            if (_connectionState.value == ConnectionState.SCANNING) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
            Log.d("BluetoothManager", "Scan stopped")
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Failed to stop scan: ${e.message}")
        }
    }

    /**
     * Se connecte à un appareil Bluetooth
     */
    fun connectToDevice(address: String) {
        Log.d("BluetoothManager", "Connecting to device: $address")

        if (!hasBluetoothPermission()) return

        stopDiscovery() // Arrêter le scan avant la connexion

        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device != null) {
            _connectionState.value = ConnectionState.CONNECTING
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        } else {
            Log.e("BluetoothManager", "Device not found: $address")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    /**
     * Déconnecte l'appareil actuel
     */
    fun disconnect() {
        Log.d("BluetoothManager", "Disconnecting...")
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Nettoie les ressources
     */
    fun cleanup() {
        stopDiscovery()
        disconnect()
    }

    /**
     * Vérifie les permissions Bluetooth
     */
    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android < 12
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Démarre la lecture des données du scooter
     */
    private fun startDataReading() {
        // TODO: Implémenter la lecture des caractéristiques spécifiques du M0Robot
        // Pour l'instant, générer des données de test
        generateTestData()
    }

    /**
     * Parse les données reçues du scooter
     */
    private fun parseScooterData(data: ByteArray) {
        // TODO: Implémenter le décodage du protocole M0Robot
        // Pour l'instant, générer des données de test
        generateTestData()
    }

    /**
     * Génère des données de test pour le développement
     */
    private fun generateTestData() {
        val testData = ScooterData(
            speed = (0..25).random().toFloat(),
            battery = (20..100).random().toFloat(),
            voltage = (36f + (0..4).random()),
            current = (0..5).random().toFloat(),
            power = (0..500).random().toFloat(),
            temperature = (20..45).random(),
            odometer = (100f + (0..200).random()),
            totalRideTime = "164H 35M 0S"
        )
        _receivedData.value = testData
    }
}