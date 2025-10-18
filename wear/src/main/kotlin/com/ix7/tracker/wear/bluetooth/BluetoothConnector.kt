package com.ix7.tracker.wear.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import java.util.UUID

/**
 * Gestionnaire de connexion Bluetooth pour Wear OS
 * Gère la connexion GATT et les commandes
 */
class BluetoothConnector(
    private val context: Context,
    private val onStateChange: (ConnectionState, ScooterData?) -> Unit
) {
    companion object {
        private const val TAG = "BT_CONNECTOR"

        // UUIDs M0Robot
        private val SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        private val TX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        private val RX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var readCharacteristic: BluetoothGattCharacteristic? = null
    private var currentData = ScooterData()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "✅ Connecté au GATT")
                    onStateChange(ConnectionState.CONNECTED, null)
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "❌ Déconnecté")
                    onStateChange(ConnectionState.DISCONNECTED, null)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "🔍 Services découverts")
                setupCharacteristics(gatt)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            val data = characteristic?.value ?: return
            Log.d(TAG, "📦 Données reçues: ${data.size} bytes")

            // Parser basique (à améliorer selon le format M0Robot)
            if (data.size >= 10) {
                // Exemple: offset bytes pour vitesse, batterie, etc.
                currentData = ScooterData(
                    speed = (data[5].toInt() and 0xFF).toFloat(),
                    battery = (data[6].toInt() and 0xFF),
                    temperature = (data[7].toInt() and 0xFF).toFloat(),
                    odometer = (data[8].toInt() and 0xFF).toFloat()
                )
                onStateChange(ConnectionState.CONNECTED, currentData)
            }
        }
    }

    fun connect(address: String) {
        try {
            Log.d(TAG, "🔗 Connexion à $address")
            val adapter = BluetoothAdapter.getDefaultAdapter()
            val device = adapter.getRemoteDevice(address)
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Permission manquante: ${e.message}")
            onStateChange(ConnectionState.ERROR, null)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur connexion: ${e.message}", e)
            onStateChange(ConnectionState.ERROR, null)
        }
    }

    fun disconnect() {
        try {
            bluetoothGatt?.let {
                it.disconnect()
                it.close()
            }
            bluetoothGatt = null
            Log.d(TAG, "⚡ Déconnexion complète")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur déconnexion: ${e.message}")
        }
    }

    fun sendCommand(command: ByteArray) {
        try {
            writeCharacteristic?.let { char ->
                char.value = command
                bluetoothGatt?.writeCharacteristic(char)
                Log.d(TAG, "📤 Commande envoyée: ${command.joinToString(" ") { "%02X".format(it) }}")
            } ?: Log.e(TAG, "❌ Characteristic non disponible")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur sendCommand: ${e.message}", e)
        }
    }

    private fun setupCharacteristics(gatt: BluetoothGatt?) {
        val service = gatt?.getService(SERVICE_UUID)
        if (service == null) {
            Log.e(TAG, "❌ Service non trouvé")
            return
        }

        writeCharacteristic = service.getCharacteristic(TX_CHAR_UUID)
        readCharacteristic = service.getCharacteristic(RX_CHAR_UUID)

        readCharacteristic?.let { char ->
            try {
                gatt?.setCharacteristicNotification(char, true)
                val descriptor = char.getDescriptor(CCCD_UUID)
                descriptor?.value = byteArrayOf(0x01, 0x00)
                gatt?.writeDescriptor(descriptor)
                Log.d(TAG, "🔔 Notifications activées")
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ Permission: ${e.message}")
            }
        }
    }

    fun cleanup() {
        disconnect()
    }
}