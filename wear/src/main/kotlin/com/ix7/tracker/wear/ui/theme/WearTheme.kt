package com.ix7.tracker.wear

import android.bluetooth.*
import android.content.Context
import android.util.Log
import java.util.*

class WearBluetoothManager(private val context: Context) {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var targetCharacteristic: BluetoothGattCharacteristic? = null

    // UUIDs à adapter selon ta M0Robot
    companion object {
        private const val TAG = "WearBLE"
        // À remplacer avec les UUID réels de ta trottinette
        private val SERVICE_UUID = UUID.fromString("00001815-0000-1000-8000-00805f9b34fb")
        private val CHAR_LOCK_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    }

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    /**
     * Se connecte à un appareil Bluetooth par adresse MAC
     */
    fun connectToDevice(macAddress: String) {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported")
            return
        }

        val device = bluetoothAdapter?.getRemoteDevice(macAddress)
        if (device != null) {
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            Log.d(TAG, "Connecting to $macAddress")
        }
    }

    /**
     * Envoie une commande de verrouillage trottinette
     */
    fun lockScooter() {
        sendCommand(byteArrayOf(0x01, 0x02, 0x03)) // À adapter
    }

    /**
     * Envoie une commande de déverrouillage trottinette
     */
    fun unlockScooter() {
        sendCommand(byteArrayOf(0x01, 0x02, 0x04)) // À adapter
    }

    /**
     * Envoie une commande de verrouillage cadenas
     */
    fun lockLock() {
        sendCommand(byteArrayOf(0x02, 0x02, 0x03)) // À adapter
    }

    /**
     * Envoie une commande de déverrouillage cadenas
     */
    fun unlockLock() {
        sendCommand(byteArrayOf(0x02, 0x02, 0x04)) // À adapter
    }

    /**
     * Envoie une commande brute via Bluetooth
     */
    private fun sendCommand(data: ByteArray) {
        if (bluetoothGatt == null || targetCharacteristic == null) {
            Log.e(TAG, "Not connected or characteristic not found")
            return
        }

        targetCharacteristic?.value = data
        bluetoothGatt?.writeCharacteristic(targetCharacteristic!!)
        Log.d(TAG, "Command sent: ${data.contentToString()}")
    }

    /**
     * Callback pour les événements Bluetooth
     */
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server")
                bluetoothGatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server")
                bluetoothGatt?.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")
                val service = gatt?.getService(SERVICE_UUID)
                if (service != null) {
                    targetCharacteristic = service.getCharacteristic(CHAR_LOCK_UUID)
                    Log.d(TAG, "Characteristic found")
                } else {
                    Log.e(TAG, "Service not found")
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic written successfully")
            }
        }
    }

    /**
     * Ferme la connexion
     */
    fun disconnect() {
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}