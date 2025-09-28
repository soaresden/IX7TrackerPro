package com.ix7.tracker.bluetooth

import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.ix7.tracker.core.ConnectionState
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
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.value?.let { data ->
                onDataReceived?.invoke(data)
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
            bluetoothGatt?.disconnect()
            onStateChange(ConnectionState.DISCONNECTED)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sendCommand(command: ByteArray): Result<Unit> {
        return try {
            // Logique d'envoi de commande simplifiée
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun setupNotifications(gatt: BluetoothGatt?) {
        gatt?.services?.forEach { service ->
            service.characteristics.forEach { characteristic ->
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                    gatt.setCharacteristicNotification(characteristic, true)

                    // Configurer le descripteur CCCD
                    val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor?.let {
                        it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(it)
                    }
                }
            }
        }
    }

    fun cleanup() {
        bluetoothGatt?.close()
        bluetoothGatt = null
        onDataReceived = null
    }
}