package com.ix7.tracker.bluetooth

import android.bluetooth.*
import android.content.Context
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
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private var onDataReceived: ((ByteArray) -> Unit)? = null
    private var dataRequestJob: Job? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connecté - Découverte des services...")
                    onStateChange(ConnectionState.CONNECTED)
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Déconnecté")
                    onStateChange(ConnectionState.DISCONNECTED)
                    dataRequestJob?.cancel()
                    cleanup()
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    onStateChange(ConnectionState.CONNECTING)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services découverts")
                setupNotifications(gatt)
                startDataRequests(gatt) // Demander activement des données
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                characteristic.value?.let { data ->
                    Log.d(TAG, "Données lues: ${data.joinToString(" ") { "%02X".format(it) }}")
                    onDataReceived?.invoke(data)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.value?.let { data ->
                Log.d(TAG, "Notification reçue: ${data.joinToString(" ") { "%02X".format(it) }}")
                onDataReceived?.invoke(data)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Commande envoyée avec succès")
            } else {
                Log.e(TAG, "Échec envoi commande: $status")
            }
        }
    }

    fun connect(address: String, dataCallback: (ByteArray) -> Unit): Result<Unit> {
        return try {
            onDataReceived = dataCallback
            val device = bluetoothAdapter?.getRemoteDevice(address)

            if (device != null) {
                onStateChange(ConnectionState.CONNECTING)
                bluetoothGatt?.close()
                bluetoothGatt = device.connectGatt(context, false, gattCallback)
                Result.success(Unit)
            } else {
                onStateChange(ConnectionState.ERROR)
                Result.failure(Exception("Device not found"))
            }
        } catch (e: Exception) {
            onStateChange(ConnectionState.ERROR)
            Result.failure(e)
        }
    }

    fun disconnect(): Result<Unit> {
        return try {
            dataRequestJob?.cancel()
            bluetoothGatt?.disconnect()
            onStateChange(ConnectionState.DISCONNECTED)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sendCommand(command: ByteArray): Result<Unit> {
        return try {
            bluetoothGatt?.services?.forEach { service ->
                service.characteristics.forEach { characteristic ->
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ||
                        characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
                        characteristic.value = command
                        bluetoothGatt?.writeCharacteristic(characteristic)
                        return Result.success(Unit)
                    }
                }
            }
            Result.failure(Exception("Aucune caractéristique d'écriture trouvée"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun setupNotifications(gatt: BluetoothGatt?) {
        gatt?.services?.forEach { service ->
            Log.d(TAG, "Service: ${service.uuid}")
            service.characteristics.forEach { characteristic ->
                Log.d(TAG, "  Caractéristique: ${characteristic.uuid}, Propriétés: ${characteristic.properties}")

                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                    Log.i(TAG, "Activation notifications pour ${characteristic.uuid}")
                    gatt.setCharacteristicNotification(characteristic, true)

                    val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor?.let {
                        it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(it)
                    }
                }
            }
        }
    }

    private fun startDataRequests(gatt: BluetoothGatt?) {
        // Lancer des requêtes périodiques pour forcer la récupération de données
        dataRequestJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    // Lire toutes les caractéristiques disponibles
                    gatt?.services?.forEach { service ->
                        service.characteristics.forEach { characteristic ->
                            if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                                gatt.readCharacteristic(characteristic)
                                delay(500) // Attendre entre les lectures
                            }
                        }
                    }

                    // Envoyer des commandes de demande de données
                    sendDataRequestCommands(gatt)

                    delay(2000) // Répéter toutes les 2 secondes
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la demande de données", e)
                }
            }
        }
    }

    private fun sendDataRequestCommands(gatt: BluetoothGatt?) {
        val commands = listOf(
            byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x03.toByte(), 0x22.toByte(), 0x01.toByte(), 0x00.toByte(), 0xFF.toByte()),
            byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x06.toByte(), 0x20.toByte(), 0x61.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x04.toByte(), 0xFF.toByte()),
            byteArrayOf(0xA5.toByte(), 0x5A.toByte(), 0x02.toByte(), 0x01.toByte(), 0x00.toByte()),
            byteArrayOf(0x01.toByte(), 0x02.toByte()), // Commande simple
            byteArrayOf(0x00.toByte(), 0x01.toByte())  // Autre commande simple
        )

        gatt?.services?.forEach { service ->
            service.characteristics.forEach { characteristic ->
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ||
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {

                    commands.forEach { command ->
                        try {
                            characteristic.value = command
                            gatt.writeCharacteristic(characteristic)
                            Thread.sleep(200)
                        } catch (e: Exception) {
                            Log.e(TAG, "Erreur envoi commande", e)
                        }
                    }
                }
            }
        }
    }

    fun cleanup() {
        dataRequestJob?.cancel()
        bluetoothGatt?.close()
        bluetoothGatt = null
        onDataReceived = null
    }
}