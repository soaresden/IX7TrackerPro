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

        // UUIDs Nordic UART (votre robot utilise CEUX-CI)
        private val SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        private val TX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        private val RX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private var onDataReceived: ((ByteArray) -> Unit)? = null
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
                    Log.i(TAG, "✓ Configuration réussie - En attente de données automatiques...")
                } else {
                    Log.e(TAG, "✗ Échec configuration")
                    onStateChange(ConnectionState.ERROR)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.value?.let { data ->
                val hex = data.joinToString(" ") { "%02X".format(it) }
                Log.i(TAG, "🔔 NOTIFICATION REÇUE (${data.size}B): $hex")
                onDataReceived?.invoke(data)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✓ CCCD écrit - Notifications activées")
                sendInitCommands(gatt)  // ← AJOUTER ICI
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
        val service = gatt?.getService(SERVICE_UUID) ?: return false

        // Activer notifications sur RX
        val rxChar = service.getCharacteristic(RX_CHAR_UUID)
        if (rxChar != null) {
            gatt.setCharacteristicNotification(rxChar, true)
            rxChar.getDescriptor(CCCD_UUID)?.let { desc ->
                desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(desc)
            }
        }

        // IMPORTANT: Activer AUSSI sur TX !
        val txChar = service.getCharacteristic(TX_CHAR_UUID)
        if (txChar != null) {
            gatt.setCharacteristicNotification(txChar, true)
            txChar.getDescriptor(CCCD_UUID)?.let { desc ->
                desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(desc)
            }
        }

        writeCharacteristic = txChar
        return true
    }

    private fun sendInitCommands(gatt: BluetoothGatt?) {
        CoroutineScope(Dispatchers.IO).launch {
            delay(500) // Attendre que notifications soient bien activées

            val commands = listOf(
                byteArrayOf(0xF0.toByte(), 0x55.toByte(), 0xAA.toByte(), 0xA5.toByte()),  // Init 1
                byteArrayOf(0xAA.toByte(), 0x55.toByte()),  // Init 2
                byteArrayOf(0x5A.toByte(), 0xA5.toByte(), 0x00.toByte()),  // Init 3
                byteArrayOf(0x20.toByte()),  // Simple status request
                byteArrayOf(0x01.toByte()),  // Simple data request
                byteArrayOf(0xFF.toByte(), 0xFF.toByte()),  // Wake-up
            )

            commands.forEach { cmd ->
                val hex = cmd.joinToString(" ") { "%02X".format(it) }
                Log.d(TAG, "→ Test commande: $hex")
                writeCharacteristic?.value = cmd
                gatt?.writeCharacteristic(writeCharacteristic)
                delay(300)
            }

            Log.i(TAG, "✓ Commandes d'init envoyées - En attente de réponse...")
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
        reconnectAttempts = maxReconnectAttempts
        bluetoothGatt?.disconnect()
        onStateChange(ConnectionState.DISCONNECTED)
        return Result.success(Unit)
    }

    fun sendCommand(command: ByteArray): Result<Unit> {
        writeCharacteristic?.let { char ->
            char.value = command
            bluetoothGatt?.writeCharacteristic(char)
        }
        return Result.success(Unit)
    }

    fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}