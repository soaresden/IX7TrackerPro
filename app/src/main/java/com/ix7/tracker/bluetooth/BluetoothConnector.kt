package com.ix7.tracker.bluetooth

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.UUID

class BluetoothConnector(
    private val context: Context,
    private val callback: ConnectionCallback
) {
    companion object {
        private const val TAG = "BluetoothConnector"
        private val SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        private val RX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        private val TX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private val handler = Handler(Looper.getMainLooper())

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server.")
                    callback.onConnected()
                    Log.i(TAG, "Attempting to start service discovery: ${gatt?.discoverServices()}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server.")
                    callback.onDisconnected()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered")

                val service = gatt?.getService(SERVICE_UUID)
                if (service != null) {
                    txCharacteristic = service.getCharacteristic(TX_CHAR_UUID)
                    rxCharacteristic = service.getCharacteristic(RX_CHAR_UUID)

                    // Activer les notifications comme l'appli officielle
                    rxCharacteristic?.let { enableNotifications(it) }

                    // Notifier que les services sont découverts
                    callback.onServicesDiscovered()
                } else {
                    Log.e(TAG, "Service not found")
                    callback.onError("Service not found")
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Descriptor written successfully")
                // L'appli officielle ne fait RIEN ici automatiquement
                // Elle attend un événement externe (bouton UI, etc.)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic written successfully")
            } else {
                Log.e(TAG, "Characteristic write failed: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.value?.let { data ->
                Log.d(TAG, "Data received: ${data.joinToString(" ") { "%02X".format(it) }}")

                // Vérification header AA 55
                if (data.size >= 2 && data[0] == 0xAA.toByte() && data[1] == 0x55.toByte()) {
                    callback.onDataReceived(data)
                }
            }
        }
    }

    fun connect(device: BluetoothDevice) {
        Log.i(TAG, "Connecting to ${device.address}...")
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    private fun enableNotifications(characteristic: BluetoothGattCharacteristic) {
        Log.i(TAG, "Enabling notifications for ${characteristic.uuid}")

        bluetoothGatt?.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        bluetoothGatt?.writeDescriptor(descriptor)
    }

    // Méthode publique pour envoyer la commande unlock MANUELLEMENT
    fun sendUnlockCommand() {
        Log.d(TAG, "=== SENDING UNLOCK COMMAND ===")

        txCharacteristic?.let { char ->
            val command = byteArrayOf(0xAA.toByte(), 0x55, 0x03, 0x00, 0x02, 0xB9.toByte())
            Log.d(TAG, "Command: ${command.joinToString(" ") { "%02X".format(it) }}")

            char.value = command
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

            bluetoothGatt?.writeCharacteristic(char)
        } ?: Log.e(TAG, "TX Characteristic is null")
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    interface ConnectionCallback {
        fun onConnected()
        fun onDisconnected()
        fun onServicesDiscovered()  // NOUVEAU
        fun onDataReceived(data: ByteArray)
        fun onError(message: String)
    }
}