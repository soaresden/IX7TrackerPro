package com.ix7.tracker

import android.os.Build
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
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

    // UUIDs connus pour les scooters M0Robot
    companion object {
        private const val TAG = "BluetoothManager"

        // Service UART Nordic - Service principal pour la télémétrie
        private val UART_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        private val UART_TX_CHARACTERISTIC_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        private val UART_RX_CHARACTERISTIC_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")

        // Services alternatifs trouvés sur certains scooters
        private val ALT_SERVICE_UUID_1 = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        private val ALT_TX_CHAR_UUID_1 = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

        private val ALT_SERVICE_UUID_2 = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        private val ALT_TX_CHAR_UUID_2 = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
        private val ALT_RX_CHAR_UUID_2 = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")

        // Descripteur pour notifications
        private val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // Services standards
        private val DEVICE_NAME_UUID = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")
        private val BATTERY_SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        private val BATTERY_LEVEL_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    }

    // États observables
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _receivedData = MutableStateFlow<ScooterData?>(null)
    val receivedData: StateFlow<ScooterData?> = _receivedData.asStateFlow()

    // Variable pour stocker la caractéristique d'écriture trouvée
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    // Callback pour la découverte des appareils
    private val discoveryCallback = object : BluetoothAdapter.LeScanCallback {
        override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
            device?.let { bluetoothDevice ->
                val deviceName = bluetoothDevice.name ?: "Appareil inconnu"

                // Filtre pour les scooters M0Robot et compatibles
                if (Utils.isMQRobot(deviceName)) {
                    Log.i(TAG, "M0Robot device detected: $deviceName (RSSI: $rssi)")

                    val deviceInfo = BluetoothDeviceInfo(
                        name = deviceName,
                        address = bluetoothDevice.address
                    )

                    val currentDevices = _discoveredDevices.value.toMutableList()
                    if (currentDevices.none { it.address == deviceInfo.address }) {
                        currentDevices.add(deviceInfo)
                        _discoveredDevices.value = currentDevices
                        Log.i(TAG, "Device added to list: $deviceName")
                    }
                }
            }
        }
    }

    // Callback GATT pour la communication
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d(TAG, "Connection state changed: status=$status, newState=$newState")

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server")
                    _connectionState.value = ConnectionState.CONNECTED
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    bluetoothGatt = null
                    writeCharacteristic = null
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.value = ConnectionState.CONNECTING
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered successfully")
                gatt?.services?.let { services ->
                    Log.i(TAG, "Found ${services.size} services")
                    services.forEach { service ->
                        Log.d(TAG, "Service: ${service.uuid}")
                        service.characteristics.forEach { characteristic ->
                            Log.d(TAG, "  Characteristic: ${characteristic.uuid} (Properties: ${characteristic.properties})")
                        }
                    }
                    startDataReading()
                }
            } else {
                Log.e(TAG, "Service discovery failed with status: $status")
                _connectionState.value = ConnectionState.ERROR
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic?.value?.let { data ->
                    Log.d(TAG, "Read data from ${characteristic.uuid}: ${Utils.bytesToHex(data)}")
                    parseCharacteristicData(data, characteristic.uuid)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.value?.let { data ->
                Log.d(TAG, "Notification from ${characteristic.uuid}: ${Utils.bytesToHex(data)}")
                parseCharacteristicData(data, characteristic.uuid)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Write successful to ${characteristic?.uuid}")
            } else {
                Log.e(TAG, "Write failed to ${characteristic?.uuid}, status: $status")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Descriptor write successful: ${descriptor?.uuid}")
            } else {
                Log.e(TAG, "Descriptor write failed: ${descriptor?.uuid}, status: $status")
            }
        }
    }

    /**
     * Démarre la découverte des appareils Bluetooth
     */
    fun startDiscovery() {
        if (!hasBluetoothPermission()) {
            Log.e(TAG, "Missing Bluetooth permissions")
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            Log.e(TAG, "Bluetooth is not enabled")
            return
        }

        Log.i(TAG, "Starting device discovery...")
        _discoveredDevices.value = emptyList()
        _connectionState.value = ConnectionState.SCANNING
        bluetoothAdapter.startLeScan(discoveryCallback)
    }

    /**
     * Arrête la découverte des appareils
     */
    fun stopDiscovery() {
        if (!hasBluetoothPermission()) return

        Log.i(TAG, "Stopping device discovery...")
        bluetoothAdapter?.stopLeScan(discoveryCallback)
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    /**
     * Se connecte à un appareil Bluetooth
     */
    fun connectToDevice(address: String) {
        if (!hasBluetoothPermission()) return

        Log.i(TAG, "Attempting to connect to device: $address")
        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device != null) {
            _connectionState.value = ConnectionState.CONNECTING
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        } else {
            Log.e(TAG, "Could not get remote device for address: $address")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    /**
     * Déconnecte l'appareil actuel
     */
    fun disconnect() {
        Log.i(TAG, "Disconnecting from device...")
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        writeCharacteristic = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Nettoie les ressources
     */
    fun cleanup() {
        Log.i(TAG, "Cleaning up Bluetooth resources...")
        stopDiscovery()
        disconnect()
    }

    /**
     * Vérifie les permissions Bluetooth
     */
    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 11 et moins
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_ADMIN
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Démarre la lecture des données du scooter
     */
    private fun startDataReading() {
        Log.i(TAG, "Starting data reading from scooter...")

        bluetoothGatt?.services?.forEach { service ->
            when (service.uuid) {
                UART_SERVICE_UUID -> {
                    Log.i(TAG, "Found UART service - setting up telemetry")
                    setupUartService(service)
                }
                ALT_SERVICE_UUID_1 -> {
                    Log.i(TAG, "Found alternative service 1")
                    setupAlternativeService1(service)
                }
                ALT_SERVICE_UUID_2 -> {
                    Log.i(TAG, "Found alternative service 2")
                    setupAlternativeService2(service)
                }
                BATTERY_SERVICE_UUID -> {
                    Log.i(TAG, "Found battery service")
                    setupBatteryService(service)
                }
                else -> {
                    // Vérifier tous les autres services pour des caractéristiques de notification
                    setupGenericService(service)
                }
            }
        }

        // Envoyer des commandes pour demander les données
        requestScooterData()
    }

    private fun setupUartService(service: BluetoothGattService) {
        val rxCharacteristic = service.getCharacteristic(UART_RX_CHARACTERISTIC_UUID)
        rxCharacteristic?.let { enableNotifications(it) }

        val txCharacteristic = service.getCharacteristic(UART_TX_CHARACTERISTIC_UUID)
        txCharacteristic?.let { writeCharacteristic = it }
    }

    private fun setupAlternativeService1(service: BluetoothGattService) {
        val characteristic = service.getCharacteristic(ALT_TX_CHAR_UUID_1)
        characteristic?.let {
            enableNotifications(it)
            writeCharacteristic = it
        }
    }

    private fun setupAlternativeService2(service: BluetoothGattService) {
        val rxCharacteristic = service.getCharacteristic(ALT_RX_CHAR_UUID_2)
        rxCharacteristic?.let { enableNotifications(it) }

        val txCharacteristic = service.getCharacteristic(ALT_TX_CHAR_UUID_2)
        txCharacteristic?.let { writeCharacteristic = it }
    }

    private fun setupBatteryService(service: BluetoothGattService) {
        val batteryCharacteristic = service.getCharacteristic(BATTERY_LEVEL_UUID)
        batteryCharacteristic?.let {
            bluetoothGatt?.readCharacteristic(it)
        }
    }

    private fun setupGenericService(service: BluetoothGattService) {
        service.characteristics.forEach { characteristic ->
            if (characteristic.uuid != DEVICE_NAME_UUID) {
                val properties = characteristic.properties

                if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                    Log.d(TAG, "Enabling notifications for ${characteristic.uuid}")
                    enableNotifications(characteristic)
                } else if (properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                    Log.d(TAG, "Reading characteristic ${characteristic.uuid}")
                    bluetoothGatt?.readCharacteristic(characteristic)
                }

                if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 && writeCharacteristic == null) {
                    Log.d(TAG, "Found writable characteristic: ${characteristic.uuid}")
                    writeCharacteristic = characteristic
                }
            }
        }
    }

    private fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)
        descriptor?.let {
            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            bluetoothGatt?.writeDescriptor(it)
        }
    }

    /**
     * Envoie des commandes pour demander les données de télémétrie
     */
    private fun requestScooterData() {
        writeCharacteristic?.let { characteristic ->
            // Commandes courantes pour demander les données de télémétrie
            val commands = listOf(
                byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x03, 0x22, 0x01, 0x01, 0x20, 0x44), // Xiaomi
                byteArrayOf(0x5A, 0xA5, 0x06, 0x20, 0x61, 0x7D, 0x02, 0x01, 0x00, 0x46, 0x40), // Générique
                byteArrayOf(0x01, 0x02), // Simple
                byteArrayOf(0xA5, 0x5A, 0x04, 0x01, 0x00, 0x05) // Alternative
            )

            commands.forEach { command ->
                Log.d(TAG, "Sending command: ${Utils.bytesToHex(command)}")
                characteristic.value = command
                bluetoothGatt?.writeCharacteristic(characteristic)
                Thread.sleep(200) // Pause entre les commandes
            }
        } ?: Log.w(TAG, "No write characteristic available for sending commands")
    }

    /**
     * Parse les données reçues des caractéristiques
     */
    private fun parseCharacteristicData(data: ByteArray, uuid: UUID) {
        Log.d(TAG, "Parsing data from $uuid: ${Utils.bytesToHex(data)}")

        // Ignorer les données qui sont clairement du texte
        if (Utils.isTextData(data)) {
            val text = String(data)
            Log.d(TAG, "Received text data: '$text' - skipping telemetry parsing")
            return
        }

        when (uuid) {
            BATTERY_LEVEL_UUID -> {
                if (data.isNotEmpty()) {
                    val battery = data[0].toUByte().toInt()
                    Log.i(TAG, "Standard battery level: $battery%")
                    updateScooterData(battery = battery, dataSource = "Battery Service")
                }
            }
            UART_RX_CHARACTERISTIC_UUID, ALT_TX_CHAR_UUID_1, ALT_RX_CHAR_UUID_2 -> {
                Log.i(TAG, "Received telemetry data")
                parseTelemetryData(data)
            }
            else -> {
                if (data.size >= 2) {
                    Log.d(TAG, "Analyzing unknown characteristic data")
                    analyzePotentialTelemetryData(data, uuid.toString())
                }
            }
        }
    }

    private fun parseTelemetryData(data: ByteArray) {
        if (data.size < 4) {
            Log.w(TAG, "Telemetry data too short: ${data.size} bytes")
            return
        }

        // Analyser selon différents protocoles possibles
        when {
            // Protocole Xiaomi/Ninebot
            data.size >= 8 && data[0] == 0x55.toByte() && data[1] == 0xAA.toByte() -> {
                parseXiaomiProtocol(data)
            }
            // Autre protocole possible
            data.size >= 6 && data[0] == 0x5A.toByte() && data[1] == 0xA5.toByte() -> {
                parseGenericProtocol(data)
            }
            else -> {
                Log.d(TAG, "Unknown telemetry protocol, trying generic parsing")
                parseGenericTelemetry(data)
            }
        }
    }

    private fun parseXiaomiProtocol(data: ByteArray) {
        Log.d(TAG, "Parsing Xiaomi/Ninebot protocol")
        if (data.size >= 6) {
            val command = data[3].toUByte().toInt()
            Log.d(TAG, "Xiaomi command: 0x${"%02x".format(command)}")

            when (command) {
                0x20, 0x21, 0x22 -> {
                    if (data.size >= 10) {
                        val speed = ((data[4].toUByte().toInt() shl 8) or data[5].toUByte().toInt()) / 100.0f
                        val battery = data[6].toUByte().toInt()
                        val voltage = ((data[7].toUByte().toInt() shl 8) or data[8].toUByte().toInt()) / 100.0f

                        Log.i(TAG, "Xiaomi telemetry - Speed: ${speed}km/h, Battery: $battery%, Voltage: ${voltage}V")
                        updateScooterData(speed = speed, battery = battery, voltage = voltage, dataSource = "Xiaomi Protocol")
                    }
                }
            }
        }
    }

    private fun parseGenericProtocol(data: ByteArray) {
        Log.d(TAG, "Parsing generic protocol")
        parseGenericTelemetry(data)
    }

    private fun parseGenericTelemetry(data: ByteArray) {
        Log.d(TAG, "Attempting generic telemetry parsing")

        val hasReasonableValues = data.any { it.toUByte().toInt() in 0..100 }

        if (!hasReasonableValues) {
            Log.w(TAG, "Data values seem unreasonable for telemetry, skipping")
            return
        }

        Log.i(TAG, "Raw telemetry data received but protocol unknown")
        Log.i(TAG, "Hex: ${Utils.bytesToHex(data)}")
        Log.i(TAG, "Dec: ${data.joinToString(" ") { it.toUByte().toString() }}")

        updateScooterData(dataSource = "Unknown Protocol", rawData = data)
    }

    private fun analyzePotentialTelemetryData(data: ByteArray, source: String) {
        Log.d(TAG, "Analyzing potential telemetry from $source")

        val hasLowValues = data.any { it.toUByte().toInt() in 0..50 }
        val hasHighValues = data.any { it.toUByte().toInt() in 200..255 }

        if (hasLowValues && !hasHighValues) {
            Log.i(TAG, "Data might contain telemetry values")
            parseGenericTelemetry(data)
        } else {
            Log.d(TAG, "Data doesn't look like telemetry")
        }
    }

    private fun updateScooterData(
        speed: Float? = null,
        battery: Int? = null,
        voltage: Float? = null,
        current: Float? = null,
        power: Float? = null,
        temperature: Int? = null,
        odometer: Float? = null,
        dataSource: String? = null,
        rawData: ByteArray? = null
    ) {
        val currentData = _receivedData.value ?: ScooterData()

        val updatedData = currentData.copy(
            speed = speed ?: currentData.speed,
            battery = battery ?: currentData.battery,
            voltage = voltage ?: currentData.voltage,
            current = current ?: currentData.current,
            power = power ?: currentData.power,
            temperature = temperature ?: currentData.temperature,
            odometer = odometer ?: currentData.odometer,
            dataSource = dataSource ?: currentData.dataSource,
            rawData = rawData ?: currentData.rawData
        )

        Log.i(TAG, "Updated scooter data: $updatedData")
        _receivedData.value = updatedData
    }

    /**
     * Envoie des données vers le scooter
     */
    fun sendData(data: ByteArray): Boolean {
        return writeCharacteristic?.let { characteristic ->
            Log.d(TAG, "Sending data: ${Utils.bytesToHex(data)}")
            characteristic.value = data
            bluetoothGatt?.writeCharacteristic(characteristic) ?: false
        } ?: false
    }

    /**
     * Vérifie si le gestionnaire est connecté
     */
    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.CONNECTED
    }

    /**
     * Vérifie si le Bluetooth est activé
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
}