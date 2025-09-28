package com.ix7.tracker.bluetooth

import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class BluetoothManagerImpl(private val context: Context) : BluetoothRepository {

    companion object {
        private const val TAG = "BluetoothManager"
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices

    private val _scooterData = MutableStateFlow(ScooterData())
    val scooterData: StateFlow<ScooterData> = _scooterData

    private val _isScanning = MutableStateFlow(false)
    val isScanningFlow: StateFlow<Boolean> = _isScanning

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let { scanResult ->
                val device = scanResult.device
                val rssi = scanResult.rssi
                val deviceName = device.name ?: "Unknown"

                Log.d(TAG, "Device found: $deviceName [${device.address}] RSSI: $rssi")

                val isScooter = deviceName.contains("M0Robot", ignoreCase = true) ||
                        deviceName.contains("Xiaomi", ignoreCase = true) ||
                        deviceName.contains("Ninebot", ignoreCase = true)

                if (isScooter) {
                    Log.d(TAG, "Scooter détecté: $deviceName")
                }

                val deviceInfo = BluetoothDeviceInfo(
                    name = deviceName,
                    address = device.address,
                    rssi = rssi,
                    isScooter = isScooter,
                    distance = estimateDistance(rssi)
                )

                val currentDevices = _discoveredDevices.value.toMutableList()
                val existingIndex = currentDevices.indexOfFirst { it.address == device.address }

                if (existingIndex >= 0) {
                    currentDevices[existingIndex] = deviceInfo
                } else {
                    currentDevices.add(deviceInfo)
                }

                _discoveredDevices.value = currentDevices.sortedByDescending { it.rssi }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Scan failed with error: $errorCode")
            _isScanning.value = false
            isScanning = false
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            Log.d(TAG, "=== CONNEXION STATE CHANGE ===")
            Log.d(TAG, "Status: $status")
            Log.d(TAG, "New State: $newState (${getConnectionStateName(newState)})")
            Log.d(TAG, "Device: ${gatt?.device?.address}")
            Log.d(TAG, "================================")

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "✅ CONNECTÉ au serveur GATT - Démarrage de la découverte des services")
                    _connectionState.value = ConnectionState.CONNECTED
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.w(TAG, "❌ DÉCONNECTÉ du serveur GATT (Status: $status)")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    cleanup()
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    Log.i(TAG, "🔄 CONNEXION en cours...")
                    _connectionState.value = ConnectionState.CONNECTING
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    Log.i(TAG, "🔄 DÉCONNEXION en cours...")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            Log.d(TAG, "=== SERVICES DÉCOUVERTS ===")
            Log.d(TAG, "Status: $status")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✅ Services découverts avec succès")

                gatt?.services?.forEachIndexed { index, service ->
                    Log.d(TAG, "📱 Service $index: ${service.uuid}")
                    Log.d(TAG, "   Type: ${getServiceTypeName(service.type)}")

                    service.characteristics.forEachIndexed { charIndex, characteristic ->
                        Log.d(TAG, "   📡 Caractéristique $charIndex: ${characteristic.uuid}")
                        Log.d(TAG, "      Propriétés: ${getPropertiesString(characteristic.properties)}")
                        Log.d(TAG, "      Permissions: ${characteristic.permissions}")

                        // Activer les notifications pour toutes les caractéristiques qui le supportent
                        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                            Log.i(TAG, "🔔 Activation des notifications pour ${characteristic.uuid}")
                            enableNotification(gatt, characteristic)
                        }

                        // Lire les caractéristiques qui le supportent
                        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                            Log.i(TAG, "📖 Lecture de la caractéristique ${characteristic.uuid}")
                            gatt?.readCharacteristic(characteristic)
                        }
                    }

                    Log.d(TAG, "   ---")
                }

                // Essayer de démarrer la communication
                startDataCommunication(gatt)

            } else {
                Log.e(TAG, "❌ Échec de la découverte des services: $status")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)

            Log.d(TAG, "=== CARACTÉRISTIQUE LUES ===")
            Log.d(TAG, "UUID: ${characteristic?.uuid}")
            Log.d(TAG, "Status: $status")

            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                val data = characteristic.value
                Log.d(TAG, "✅ Données lues: ${data?.let { bytesToHex(it) } ?: "null"}")
                Log.d(TAG, "Taille: ${data?.size ?: 0} bytes")

                if (data != null && data.isNotEmpty()) {
                    parseScooterData(data, "READ")
                }
            } else {
                Log.e(TAG, "❌ Échec de lecture: $status")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)

            Log.d(TAG, "=== ÉCRITURE CARACTÉRISTIQUE ===")
            Log.d(TAG, "UUID: ${characteristic?.uuid}")
            Log.d(TAG, "Status: $status")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✅ Écriture réussie")
            } else {
                Log.e(TAG, "❌ Échec d'écriture: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)

            Log.d(TAG, "=== NOTIFICATION REÇUE ===")
            Log.d(TAG, "UUID: ${characteristic?.uuid}")

            val data = characteristic?.value
            if (data != null && data.isNotEmpty()) {
                Log.d(TAG, "✅ Données reçues: ${bytesToHex(data)}")
                Log.d(TAG, "Taille: ${data.size} bytes")
                Log.d(TAG, "ASCII: ${String(data, Charsets.UTF_8).filter { it.isLetterOrDigit() || it.isWhitespace() }}")

                parseScooterData(data, "NOTIFICATION")
            } else {
                Log.w(TAG, "⚠️ Notification vide")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)

            Log.d(TAG, "=== DESCRIPTEUR ÉCRIT ===")
            Log.d(TAG, "UUID: ${descriptor?.uuid}")
            Log.d(TAG, "Status: $status")

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✅ Notification activée avec succès")
            } else {
                Log.e(TAG, "❌ Échec d'activation des notifications: $status")
            }
        }
    }

    private fun enableNotification(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
        try {
            Log.d(TAG, "📡 Tentative d'activation des notifications pour ${characteristic.uuid}")

            // Activer les notifications localement
            val success = gatt?.setCharacteristicNotification(characteristic, true)
            Log.d(TAG, "SetCharacteristicNotification result: $success")

            // Configurer le descripteur CCCD
            val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            if (descriptor != null) {
                Log.d(TAG, "✅ Descripteur CCCD trouvé")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                val writeResult = gatt?.writeDescriptor(descriptor)
                Log.d(TAG, "WriteDescriptor result: $writeResult")
            } else {
                Log.w(TAG, "⚠️ Descripteur CCCD non trouvé pour ${characteristic.uuid}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de l'activation des notifications", e)
        }
    }

    private fun startDataCommunication(gatt: BluetoothGatt?) {
        Log.d(TAG, "=== DÉMARRAGE COMMUNICATION ===")

        // Essayer différentes commandes pour déclencher les données
        val commands = listOf(
            byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x03.toByte(), 0x22.toByte(), 0x01.toByte(), 0x00.toByte(), 0xFF.toByte()), // Commande standard
            byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x06.toByte(), 0x20.toByte(), 0x61.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x04.toByte(), 0xFF.toByte()), // Demande status
            byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x02.toByte(), 0x20.toByte(), 0x03.toByte(), 0xFF.toByte()), // Heartbeat
            byteArrayOf(0xA5.toByte(), 0x5A.toByte(), 0x02.toByte(), 0x01.toByte(), 0x00.toByte()), // Alternative
            byteArrayOf(0xFF.toByte(), 0x55.toByte(), 0xAA.toByte(), 0x00.toByte()) // Simple ping
        )

        gatt?.services?.forEach { service ->
            service.characteristics.forEach { characteristic ->
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ||
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {

                    Log.d(TAG, "🔧 Test des commandes sur ${characteristic.uuid}")

                    commands.forEachIndexed { index, command ->
                        try {
                            Log.d(TAG, "📤 Envoi commande $index: ${bytesToHex(command)}")
                            characteristic.value = command
                            gatt.writeCharacteristic(characteristic)
                            Thread.sleep(500) // Attendre entre les commandes
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Erreur envoi commande $index", e)
                        }
                    }
                }
            }
        }
    }

    private fun parseScooterData(data: ByteArray, source: String) {
        Log.d(TAG, "=== ANALYSE DONNÉES ($source) ===")
        Log.d(TAG, "Hex: ${bytesToHex(data)}")
        Log.d(TAG, "Decimal: ${data.joinToString(", ") { it.toUByte().toString() }}")
        Log.d(TAG, "Binary: ${data.joinToString(" ") { String.format("%08d", Integer.toBinaryString(it.toInt() and 0xFF).toInt()) }}")

        try {
            // Essayer différents formats de parsing
            parseNinebotFormat(data)
            parseXiaomiFormat(data)
            parseGenericFormat(data)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur parsing données", e)
        }
    }

    private fun parseNinebotFormat(data: ByteArray) {
        if (data.size >= 3 && data[0] == 0x55.toByte() && data[1] == 0xAA.toByte()) {
            Log.d(TAG, "🔍 Format Ninebot détecté")
            val length = data[2].toUByte().toInt()

            if (data.size >= length + 4) {
                val command = data[3].toUByte().toInt()
                Log.d(TAG, "Commande: 0x${command.toString(16)}")

                when (command) {
                    0x21 -> parseSpeedData(data.sliceArray(4..data.size-2))
                    0x22 -> parseBatteryData(data.sliceArray(4..data.size-2))
                    0x23 -> parseOdometerData(data.sliceArray(4..data.size-2))
                    else -> Log.d(TAG, "Commande inconnue: 0x${command.toString(16)}")
                }
            }
        }
    }

    private fun parseXiaomiFormat(data: ByteArray) {
        if (data.size >= 2 && data[0] == 0xA5.toByte() && data[1] == 0x5A.toByte()) {
            Log.d(TAG, "🔍 Format Xiaomi détecté")
            // TODO: Implémenter le parsing Xiaomi
        }
    }

    private fun parseGenericFormat(data: ByteArray) {
        Log.d(TAG, "🔍 Tentative parsing générique")

        // Rechercher des patterns de vitesse (généralement 0-60 km/h)
        for (i in 0 until data.size - 1) {
            val speed = data.getShort(i)
            if (speed in 0..600) { // 0-60 km/h en dixièmes
                Log.d(TAG, "🚀 Vitesse possible: ${speed/10.0} km/h à l'offset $i")
                updateScooterData(speed = speed/10.0f)
            }
        }

        // Rechercher des patterns de batterie (0-100%)
        for (i in data.indices) {
            val battery = data[i].toUByte().toInt()
            if (battery in 0..100) {
                Log.d(TAG, "🔋 Batterie possible: $battery% à l'offset $i")
                updateScooterData(battery = battery.toFloat())
            }
        }
    }

    private fun parseSpeedData(payload: ByteArray) {
        if (payload.isNotEmpty()) {
            val speed = payload.getShort(0) / 100.0f
            Log.i(TAG, "🚀 Vitesse: $speed km/h")
            updateScooterData(speed = speed)
        }
    }

    private fun parseBatteryData(payload: ByteArray) {
        if (payload.isNotEmpty()) {
            val battery = payload[0].toUByte().toFloat()
            Log.i(TAG, "🔋 Batterie: $battery%")
            updateScooterData(battery = battery)
        }
    }

    private fun parseOdometerData(payload: ByteArray) {
        if (payload.size >= 4) {
            val odometer = payload.getInt(0) / 1000.0f
            Log.i(TAG, "📏 Odomètre: $odometer km")
            updateScooterData(odometer = odometer)
        }
    }

    private fun updateScooterData(
        speed: Float? = null,
        battery: Float? = null,
        odometer: Float? = null,
        temperature: Float? = null
    ) {
        val current = _scooterData.value
        _scooterData.value = current.copy(
            speed = speed ?: current.speed,
            battery = battery ?: current.battery,
            odometer = odometer ?: current.odometer,
            temperature = temperature ?: current.temperature,
            lastUpdate = System.currentTimeMillis()
        )

        Log.i(TAG, "📊 Données mises à jour: ${_scooterData.value}")
    }

    private fun cleanup() {
        Log.d(TAG, "🧹 Nettoyage des ressources")
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    private fun estimateDistance(rssi: Int): String {
        val distance = when {
            rssi > -50 -> "< 1m"
            rssi > -60 -> "1-3m"
            rssi > -70 -> "3-10m"
            rssi > -80 -> "10-30m"
            else -> "> 30m"
        }
        return distance
    }

    // Fonctions utilitaires
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(it) }
    }

    private fun ByteArray.getShort(offset: Int): Int {
        return if (offset + 1 < size) {
            ((this[offset].toInt() and 0xFF) shl 8) or (this[offset + 1].toInt() and 0xFF)
        } else 0
    }

    private fun ByteArray.getInt(offset: Int): Int {
        return if (offset + 3 < size) {
            ((this[offset].toInt() and 0xFF) shl 24) or
                    ((this[offset + 1].toInt() and 0xFF) shl 16) or
                    ((this[offset + 2].toInt() and 0xFF) shl 8) or
                    (this[offset + 3].toInt() and 0xFF)
        } else 0
    }

    private fun getConnectionStateName(state: Int): String {
        return when (state) {
            BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
            BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
            BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
            BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
            else -> "UNKNOWN($state)"
        }
    }

    private fun getServiceTypeName(type: Int): String {
        return when (type) {
            BluetoothGattService.SERVICE_TYPE_PRIMARY -> "PRIMARY"
            BluetoothGattService.SERVICE_TYPE_SECONDARY -> "SECONDARY"
            else -> "UNKNOWN($type)"
        }
    }

    private fun getPropertiesString(properties: Int): String {
        val props = mutableListOf<String>()
        if (properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) props.add("READ")
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) props.add("WRITE")
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) props.add("WRITE_NO_RESPONSE")
        if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) props.add("NOTIFY")
        if (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) props.add("INDICATE")
        return props.joinToString(", ")
    }

    // Fonctions publiques pour l'interface
    suspend fun startScan() {
        if (bluetoothAdapter?.isEnabled == true && !isScanning) {
            Log.d(TAG, "🔍 Démarrage du scan...")
            _isScanning.value = true
            isScanning = true
            _discoveredDevices.value = emptyList()
            bluetoothLeScanner?.startScan(scanCallback)
        } else {
            Log.w(TAG, "❌ Impossible de démarrer le scan (Bluetooth activé: ${bluetoothAdapter?.isEnabled}, Scan en cours: $isScanning)")
        }
    }

    suspend fun stopScan() {
        if (isScanning) {
            Log.d(TAG, "⏹️ Arrêt du scan")
            bluetoothLeScanner?.stopScan(scanCallback)
            _isScanning.value = false
            isScanning = false
        }
    }

    suspend fun connectToDevice(address: String) {
        Log.d(TAG, "🔗 Tentative de connexion à $address")

        // Arrêter le scan si en cours
        if (isScanning) {
            stopScan()
        }

        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device != null) {
            _connectionState.value = ConnectionState.CONNECTING
            bluetoothGatt?.close() // Fermer toute connexion existante
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        } else {
            Log.e(TAG, "❌ Appareil introuvable: $address")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    suspend fun disconnect() {
        Log.d(TAG, "🔌 Déconnexion demandée")
        bluetoothGatt?.disconnect()
        cleanup()
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}

// Classes de données pour l'état
enum class ConnectionState {
    DISCONNECTED, SCANNING, CONNECTING, CONNECTED, ERROR
}

data class BluetoothDeviceInfo(
    val name: String?,
    val address: String,
    val rssi: Int,
    val isScooter: Boolean,
    val scooterType: String = "Type 1 - Standard",
    val distance: String = ""
)

data class ScooterData(
    val speed: Float = 0f,
    val battery: Float = 0f,
    val odometer: Float = 0f,
    val temperature: Float = 0f,
    val voltage: Float = 0f,
    val current: Float = 0f,
    val power: Float = 0f,
    val batteryTemperature: Float = 0f,
    val errorCodes: Int = 0,
    val warningCodes: Int = 0,
    val firmwareVersion: String = "",
    val bluetoothVersion: String = "",
    val totalRideTime: String = "",
    val tripDistance: Float = 0f,
    val lastUpdate: Long = 0L
)