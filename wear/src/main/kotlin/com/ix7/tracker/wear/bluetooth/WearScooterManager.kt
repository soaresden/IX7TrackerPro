package com.ix7.tracker.wear.bluetooth

import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

enum class ConnectionState {
    DISCONNECTED, SCANNING, CONNECTING, CONNECTED, ERROR
}

data class ScooterData(
    val speed: Float = 0f,
    val battery: Int = 0,
    val temperature: Float = 0f,
    val odometer: Float = 0f,
    val isLocked: Boolean = true
)

data class BluetoothDeviceInfo(
    val address: String,
    val name: String,
    val rssi: Int
)

class WearScooterManager(private val context: Context) {
    companion object {
        private const val TAG = "WEAR_SCOOTER"
        private const val SCOOTER_NAME = "M0Robot"

        // ✅ UUIDs CORRIGÉS - IXPORTANT: TX pour ÉCRIRE, RX pour LIRE
        private val SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        private val TX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")  // Pour ÉCRIRE (commandes)
        private val RX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")  // Pour LIRE (notifications)
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // ✅ Commandes vérifiées du projet GitHub
        private val CMD_LOCK = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4B, 0x35, 0x34, 0x6C, 0xCB.toByte())
        private val CMD_UNLOCK = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4B, 0x34, 0x34, 0x6D, 0xCB.toByte())
    }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _scooterData = MutableStateFlow(ScooterData())
    val scooterData: StateFlow<ScooterData> = _scooterData.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var scanCallback: ScanCallback? = null
    private var detectedScooterAddress: String? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "✅ GATT Connecté")
                    bluetoothGatt = gatt
                    _connectionState.value = ConnectionState.CONNECTED

                    try {
                        gatt?.discoverServices()
                        Log.d(TAG, "🔍 Découverte des services...")
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Discover permission: ${e.message}")
                        _connectionState.value = ConnectionState.ERROR
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "❌ GATT Déconnecté")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    cleanup()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "❌ Découverte échouée: $status")
                _connectionState.value = ConnectionState.ERROR
                return
            }

            Log.d(TAG, "✅ Services découverts")

            val service = gatt?.getService(SERVICE_UUID)
            if (service == null) {
                Log.e(TAG, "❌ Service NON TROUVÉ: $SERVICE_UUID")
                Log.d(TAG, "🔍 Services disponibles sur le device:")
                gatt?.services?.forEach { svc ->
                    Log.d(TAG, "  - ${svc.uuid}")
                }
                _connectionState.value = ConnectionState.ERROR
                return
            }

            Log.d(TAG, "✅ Service TROUVÉ!")
            Log.d(TAG, "🔍 Characteristics dans ce service:")

            // ✅ Trouver les deux characteristics avec les UUIDs CORRECTS
            txCharacteristic = service.getCharacteristic(TX_CHAR_UUID)
            rxCharacteristic = service.getCharacteristic(RX_CHAR_UUID)

            // Afficher TOUTES les characteristics disponibles pour debug
            service.characteristics.forEach { char ->
                Log.d(TAG, "  Char: ${char.uuid}")
                if (char.uuid.toString().equals(TX_CHAR_UUID.toString(), ignoreCase = true)) {
                    Log.d(TAG, "    ✅ C'EST TX (écriture)!")
                }
                if (char.uuid.toString().equals(RX_CHAR_UUID.toString(), ignoreCase = true)) {
                    Log.d(TAG, "    ✅ C'EST RX (notification)!")
                }
                char.descriptors.forEach { desc ->
                    Log.d(TAG, "      Desc: ${desc.uuid}")
                }
            }

            // Vérifier les résultats
            Log.d(TAG, "=== RÉSULTATS ===")
            Log.d(TAG, "txCharacteristic trouvé: ${txCharacteristic != null} (pour écriture)")
            Log.d(TAG, "rxCharacteristic trouvé: ${rxCharacteristic != null} (pour notification)")

            if (txCharacteristic == null || rxCharacteristic == null) {
                Log.e(TAG, "❌ CHARACTERISTICS NON TROUVÉES!")
                _connectionState.value = ConnectionState.ERROR
                return
            }

            // ✅ Activer notifications sur RX
            rxCharacteristic?.let { char ->
                try {
                    gatt?.setCharacteristicNotification(char, true)
                    val descriptor = char.getDescriptor(CCCD_UUID)
                    if (descriptor != null) {
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt?.writeDescriptor(descriptor)
                        Log.d(TAG, "🔔 Notifications activées sur RX")
                    } else {
                        Log.w(TAG, "⚠️ CCCD descriptor pas trouvé")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Notification error: ${e.message}")
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            val data = characteristic?.value ?: return
            Log.d(TAG, "📦 Données reçues sur RX: ${data.size} bytes - ${data.joinToString(" ") { "%02X".format(it) }}")

            if (data.size >= 10) {
                val newData = _scooterData.value.copy(
                    speed = (data[5].toInt() and 0xFF).toFloat(),
                    battery = data[6].toInt() and 0xFF,
                    temperature = (data[7].toInt() and 0xFF).toFloat(),
                    odometer = (data[8].toInt() and 0xFF).toFloat()
                )
                _scooterData.value = newData
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "✅ Caractéristique écrite avec succès sur TX")
            } else {
                Log.e(TAG, "❌ Erreur écriture caractéristique: $status")
            }
        }
    }

    // ========== SCAN & DÉCOUVERTE ==========

    fun startScanning(bluetoothAdapter: BluetoothAdapter) {
        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "❌ BLE Scanner non disponible")
            _connectionState.value = ConnectionState.ERROR
            return
        }

        // Effacer les devices précédents
        _discoveredDevices.value = emptyList()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name ?: ""
                val address = device.address

                Log.d(TAG, "📡 Trouvé: $name ($address)")

                // Ajouter à la liste même si ce n'est pas M0Robot (on laisse l'utilisateur choisir)
                if (name.isNotEmpty() && address != null) {
                    val deviceInfo = BluetoothDeviceInfo(address, name, result.rssi)
                    val currentList = _discoveredDevices.value.toMutableList()

                    // Éviter les doublons
                    if (!currentList.any { it.address == address }) {
                        currentList.add(deviceInfo)
                        _discoveredDevices.value = currentList
                        Log.d(TAG, "✅ Device ajouté à la liste: $name")
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "❌ Scan échoué: $errorCode")
                _connectionState.value = ConnectionState.ERROR
            }
        }

        try {
            _connectionState.value = ConnectionState.SCANNING
            _isScanning.value = true
            scanner.startScan(null, settings, scanCallback)
            Log.d(TAG, "🔍 Scan démarré...")
        } catch (e: SecurityException) {
            Log.e(TAG, "Scan permission: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    fun stopScanning(bluetoothAdapter: BluetoothAdapter) {
        try {
            val scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner != null && scanCallback != null) {
                scanner.stopScan(scanCallback)
                Log.d(TAG, "🛑 Scan arrêté")
            }
            _isScanning.value = false
        } catch (e: SecurityException) {
            Log.e(TAG, "Stop scan permission: ${e.message}")
        }
    }

    // ========== CONNEXION ==========

    fun connectToDevice(address: String, bluetoothAdapter: BluetoothAdapter) {
        if (address.isEmpty()) {
            Log.e(TAG, "❌ Adresse vide")
            _connectionState.value = ConnectionState.ERROR
            return
        }

        detectedScooterAddress = address
        _connectionState.value = ConnectionState.CONNECTING

        // Arrêter le scan
        try {
            val scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner != null && scanCallback != null) {
                scanner.stopScan(scanCallback)
            }
            _isScanning.value = false
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping scan: ${e.message}")
        }

        connectToScooter(bluetoothAdapter)
    }

    private fun connectToScooter(bluetoothAdapter: BluetoothAdapter) {
        val address = detectedScooterAddress
        if (address == null) {
            Log.e(TAG, "❌ Pas d'adresse")
            _connectionState.value = ConnectionState.ERROR
            return
        }

        try {
            val device = bluetoothAdapter.getRemoteDevice(address)
            Log.d(TAG, "🔗 Connexion à $address...")
            bluetoothGatt = device.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Connect permission: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
        } catch (e: Exception) {
            Log.e(TAG, "Connect error: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    // ========== COMMANDES ==========

    fun lockScooter() {
        Log.d(TAG, "🔒 Envoi commande LOCK...")
        sendCommand(CMD_LOCK)
    }

    fun unlockScooter() {
        Log.d(TAG, "🔓 Envoi commande UNLOCK...")
        sendCommand(CMD_UNLOCK)
    }

    private fun sendCommand(command: ByteArray) {
        Log.d(TAG, "📤 Envoi commande (${command.size} bytes): ${command.joinToString(" ") { "%02X".format(it) }}")

        val char = txCharacteristic
        if (char == null) {
            Log.e(TAG, "❌ TX Characteristic est NULL!")
            return
        }

        if (bluetoothGatt == null) {
            Log.e(TAG, "❌ BluetoothGatt est NULL!")
            return
        }

        try {
            char.value = command
            bluetoothGatt?.writeCharacteristic(char)
            Log.d(TAG, "✅ Commande envoyée via TX: ${command.size} bytes")
        } catch (e: SecurityException) {
            Log.e(TAG, "Write permission: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Write error: ${e.message}")
        }
    }

    // ========== GESTION CYCLES VIE ==========

    fun disconnect() {
        Log.d(TAG, "⚡ Déconnexion...")
        try {
            bluetoothGatt?.let {
                it.disconnect()
                Thread.sleep(200)
                it.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error: ${e.message}")
        }
        cleanup()
    }

    fun cleanup() {
        bluetoothGatt = null
        txCharacteristic = null
        rxCharacteristic = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun getConnectionState(): ConnectionState = _connectionState.value
}