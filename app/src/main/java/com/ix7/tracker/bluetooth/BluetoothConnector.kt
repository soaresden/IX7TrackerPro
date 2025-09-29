package com.ix7.tracker.bluetooth

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ix7.tracker.core.ConnectionState
import kotlinx.coroutines.*
import java.util.*

class BluetoothConnector(
    private val context: Context,
    private val onStateChange: (ConnectionState) -> Unit
) {
    companion object {
        private const val TAG = "BluetoothConnector"

        // UUIDs CORRECTS - Nordic UART Service (votre robot)
        private val SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        private val TX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        private val RX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private var onDataReceived: ((ByteArray) -> Unit)? = null
    private var dataRequestJob: Job? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var currentDeviceAddress: String? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 3
    private val handler = Handler(Looper.getMainLooper())

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when {
                newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS -> {
                    Log.i(TAG, "✓ Connecté au robot - Attente stabilisation...")
                    onStateChange(ConnectionState.CONNECTED)
                    reconnectAttempts = 0

                    handler.postDelayed({
                        Log.i(TAG, "→ Découverte des services...")
                        gatt?.discoverServices()
                    }, 600)
                }

                newState == BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "✗ Déconnecté (status: $status)")
                    onStateChange(ConnectionState.DISCONNECTED)
                    dataRequestJob?.cancel()

                    if ((status == 133 || status == 62) && reconnectAttempts < maxReconnectAttempts) {
                        reconnectAttempts++
                        Log.w(TAG, "⟳ Reconnexion $reconnectAttempts/$maxReconnectAttempts")
                        reconnectWithDelay()
                    }
                }

                newState == BluetoothProfile.STATE_CONNECTING -> {
                    Log.d(TAG, "→ Connexion en cours...")
                    onStateChange(ConnectionState.CONNECTING)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✓ Services découverts")
                logAllServices(gatt)

                if (setupNotifications(gatt)) {
                    Log.i(TAG, "✓ Configuration réussie - Prêt !")
                    startDataRequests(gatt)
                } else {
                    Log.e(TAG, "✗ Échec configuration")
                    onStateChange(ConnectionState.ERROR)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.value?.let { data ->
                val hex = data.joinToString(" ") { "%02X".format(it) }
                Log.d(TAG, "🔔 Notification (${data.size}B): $hex")
                onDataReceived?.invoke(data)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value
                Log.d(TAG, "✓ Écriture OK - Envoyé: ${data.joinToString(" ") { "%02X".format(it) }}")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✓ CCCD écrit - Notifications ON")
            }
        }
    }

    private fun reconnectWithDelay() {
        handler.postDelayed({
            currentDeviceAddress?.let { address ->
                bluetoothGatt?.close()
                bluetoothGatt = null
                bluetoothAdapter?.getRemoteDevice(address)?.let {
                    bluetoothGatt = it.connectGatt(context, false, gattCallback)
                }
            }
        }, 2000)
    }

    private fun logAllServices(gatt: BluetoothGatt?) {
        Log.d(TAG, "=== Services disponibles ===")
        gatt?.services?.forEach { service ->
            Log.d(TAG, "Service: ${service.uuid}")
            service.characteristics.forEach { char ->
                val props = mutableListOf<String>()
                if (char.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) props.add("READ")
                if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) props.add("WRITE")
                if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) props.add("WRITE_NO_RESP")
                if (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) props.add("NOTIFY")
                Log.d(TAG, "  └─ ${char.uuid} [${props.joinToString()}]")
            }
        }
    }

    private fun setupNotifications(gatt: BluetoothGatt?): Boolean {
        val service = gatt?.getService(SERVICE_UUID)
        if (service == null) {
            Log.e(TAG, "✗ Service Nordic UART non trouvé")
            return false
        }

        val rxChar = service.getCharacteristic(RX_CHAR_UUID)
        if (rxChar == null) {
            Log.e(TAG, "✗ Caractéristique RX non trouvée")
            return false
        }

        writeCharacteristic = service.getCharacteristic(TX_CHAR_UUID)
        if (writeCharacteristic == null) {
            Log.e(TAG, "✗ Caractéristique TX non trouvée")
            return false
        }

        val notifyEnabled = gatt.setCharacteristicNotification(rxChar, true)
        if (!notifyEnabled) {
            Log.e(TAG, "✗ Échec activation notifications")
            return false
        }

        val descriptor = rxChar.getDescriptor(CCCD_UUID)
        if (descriptor == null) {
            Log.e(TAG, "✗ Descripteur CCCD non trouvé")
            return false
        }

        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        return gatt.writeDescriptor(descriptor)
    }

    private fun startDataRequests(gatt: BluetoothGatt?) {
        dataRequestJob?.cancel()
        dataRequestJob = CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            Log.i(TAG, "🚀 Démarrage des demandes de données")

            while (isActive) {
                try {
                    // Commande 1: Demande de statut (0x20)
                    val cmd1 = byteArrayOf(
                        0x55.toByte(), 0xAA.toByte(),  // Header
                        0x02.toByte(),                  // Length
                        0x20.toByte(),                  // Command: STATUS
                        0x01.toByte(),                  // SubCommand
                        0x23.toByte()                   // Checksum
                    )
                    sendCommandInternal(gatt, cmd1)
                    delay(500)

                    // Commande 2: Demande de données (0x22)
                    val cmd2 = byteArrayOf(
                        0x55.toByte(), 0xAA.toByte(),  // Header
                        0x02.toByte(),                  // Length
                        0x22.toByte(),                  // Command: REQUEST_DATA
                        0x01.toByte(),                  // SubCommand
                        0x21.toByte()                   // Checksum
                    )
                    sendCommandInternal(gatt, cmd2)
                    delay(2000)

                } catch (e: Exception) {
                    Log.e(TAG, "Erreur envoi commande", e)
                }
            }
        }
    }

    private fun sendCommandInternal(gatt: BluetoothGatt?, command: ByteArray) {
        writeCharacteristic?.let { char ->
            char.value = command
            gatt?.writeCharacteristic(char)
        }
    }

    fun connect(address: String, dataCallback: (ByteArray) -> Unit): Result<Unit> {
        return try {
            currentDeviceAddress = address
            onDataReceived = dataCallback
            reconnectAttempts = 0

            bluetoothAdapter?.getRemoteDevice(address)?.let { device ->
                Log.i(TAG, "→ Connexion à $address...")
                onStateChange(ConnectionState.CONNECTING)
                bluetoothGatt?.close()
                bluetoothGatt = device.connectGatt(context, false, gattCallback)
                Result.success(Unit)
            } ?: Result.failure(Exception("Device not found"))
        } catch (e: Exception) {
            onStateChange(ConnectionState.ERROR)
            Result.failure(e)
        }
    }

    fun disconnect(): Result<Unit> {
        dataRequestJob?.cancel()
        reconnectAttempts = maxReconnectAttempts
        bluetoothGatt?.disconnect()
        onStateChange(ConnectionState.DISCONNECTED)
        return Result.success(Unit)
    }

    fun sendCommand(command: ByteArray): Result<Unit> {
        sendCommandInternal(bluetoothGatt, command)
        return Result.success(Unit)
    }

    fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        dataRequestJob?.cancel()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}