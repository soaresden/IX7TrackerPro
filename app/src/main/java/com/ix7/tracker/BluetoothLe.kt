package com.ix7.tracker

import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import java.util.*

class BluetoothLe(private val context: Context) {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var characteristic: BluetoothGattCharacteristic? = null

    // UUID pour les trottinettes M0Robot/M365
    private val SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val CHARACTERISTIC_WRITE_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    private val CHARACTERISTIC_NOTIFY_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")

    private var onDataReceivedListener: ((ByteArray) -> Unit)? = null
    private var onConnectionStateChangedListener: ((Boolean) -> Unit)? = null

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    fun setOnDataReceivedListener(listener: (ByteArray) -> Unit) {
        onDataReceivedListener = listener
    }

    fun setOnConnectionStateChangedListener(listener: (Boolean) -> Unit) {
        onConnectionStateChangedListener = listener
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    LogManager.logInfo("Connecté au GATT server")
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        gatt?.discoverServices()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    LogManager.logInfo("Déconnecté du GATT server")
                    onConnectionStateChangedListener?.invoke(false)
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                LogManager.logInfo("Services découverts")
                setupCharacteristics(gatt)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.value?.let { data ->
                LogManager.logInfo("Données reçues: ${data.joinToString(" ") { "%02X".format(it) }}")
                onDataReceivedListener?.invoke(data)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                LogManager.logInfo("Données envoyées avec succès")
            } else {
                LogManager.logError("Erreur lors de l'envoi: $status")
            }
        }
    }

    private fun setupCharacteristics(gatt: BluetoothGatt?) {
        val service = gatt?.getService(SERVICE_UUID)
        if (service == null) {
            LogManager.logError("Service non trouvé")
            return
        }

        // Caractéristique pour écrire
        characteristic = service.getCharacteristic(CHARACTERISTIC_WRITE_UUID)

        // Caractéristique pour les notifications
        val notifyCharacteristic = service.getCharacteristic(CHARACTERISTIC_NOTIFY_UUID)
        if (notifyCharacteristic != null) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                gatt.setCharacteristicNotification(notifyCharacteristic, true)

                // Activer les notifications
                val descriptor = notifyCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
        }

        LogManager.logInfo("Caractéristiques configurées")
        onConnectionStateChangedListener?.invoke(true)
    }

    fun connect(deviceAddress: String) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            LogManager.logError("Permission BLUETOOTH_CONNECT non accordée")
            return
        }

        try {
            val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            bluetoothGatt = device?.connectGatt(context, false, gattCallback)
            LogManager.logInfo("Connexion en cours vers $deviceAddress")
        } catch (e: Exception) {
            LogManager.logError("Erreur lors de la connexion", e)
            onConnectionStateChangedListener?.invoke(false)
        }
    }

    fun disconnect() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt?.disconnect()
        }
    }

    fun sendData(data: ByteArray) {
        if (characteristic == null) {
            LogManager.logError("Caractéristique non disponible")
            return
        }

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            LogManager.logError("Permission BLUETOOTH_CONNECT non accordée")
            return
        }

        try {
            characteristic?.value = data
            val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false
            if (!success) {
                LogManager.logError("Échec de l'envoi des données")
            } else {
                LogManager.logInfo("Envoi: ${data.joinToString(" ") { "%02X".format(it) }}")
            }
        } catch (e: Exception) {
            LogManager.logError("Erreur lors de l'envoi", e)
        }
    }
}