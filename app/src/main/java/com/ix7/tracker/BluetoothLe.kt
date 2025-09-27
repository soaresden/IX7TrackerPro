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

    // UUID M0Robot multiples (d'après le code original)
    private val POSSIBLE_SERVICE_UUIDS = listOf(
        UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"), // UUID_SERVICE
        UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"), // UUID_SERVICE2
        UUID.fromString("0000ae00-0000-1000-8000-00805f9b34fb"), // UUID_SERVICE3
        UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")  // UUID_SERVICE5
    )

    private val POSSIBLE_NOTIFY_UUIDS = listOf(
        UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"), // UUID_NOTIFY
        UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"), // UUID_NOTIFY2
        UUID.fromString("0000ae01-0000-1000-8000-00805f9b34fb"), // UUID_NOTIFY3
        UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb"), // UUID_NOTIFY4/5
        UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")  // UUID_NOTIFY6
    )

    private val POSSIBLE_READ_UUIDS = listOf(
        UUID.fromString("0000ae02-0000-1000-8000-00805f9b34fb"), // UUID_Read3
        UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb"), // UUID_Read4
        UUID.fromString("0000fff7-0000-1000-8000-00805f9b34fb"), // UUID_Read5
        UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")  // UUID_Read6
    )

    private var onDataReceivedListener: ((ByteArray) -> Unit)? = null
    private var onConnectionStateChangedListener: ((Boolean) -> Unit)? = null

    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
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

                // Lister tous les services disponibles
                gatt?.services?.forEach { service ->
                    LogManager.logInfo("Service trouvé: ${service.uuid}")
                    service.characteristics.forEach { char ->
                        LogManager.logInfo("  Caractéristique: ${char.uuid}")
                    }
                }

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
        var serviceFound: BluetoothGattService? = null
        var appType = 1

        // Chercher le bon service parmi les possibles
        for ((index, serviceUuid) in POSSIBLE_SERVICE_UUIDS.withIndex()) {
            val service = gatt?.getService(serviceUuid)
            if (service != null) {
                LogManager.logInfo("Service M0Robot trouvé: $serviceUuid (type $index)")
                serviceFound = service
                appType = index
                break
            }
        }

        if (serviceFound == null) {
            LogManager.logError("Aucun service M0Robot compatible trouvé")
            return
        }

        // Chercher les caractéristiques compatibles
        var writeChar: BluetoothGattCharacteristic? = null
        var notifyChar: BluetoothGattCharacteristic? = null

        // Essayer de trouver une caractéristique qui peut écrire ET notifier
        for (char in serviceFound.characteristics) {
            val properties = char.properties
            LogManager.logInfo("Caractéristique ${char.uuid}: propriétés = $properties")

            if ((properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                writeChar = char
                LogManager.logInfo("Caractéristique d'écriture trouvée: ${char.uuid}")
            }

            if ((properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                notifyChar = char
                LogManager.logInfo("Caractéristique de notification trouvée: ${char.uuid}")
            }
        }

        if (writeChar == null) {
            LogManager.logError("Aucune caractéristique d'écriture trouvée")
            return
        }

        characteristic = writeChar

        // Configurer les notifications si disponibles
        if (notifyChar != null && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            gatt?.setCharacteristicNotification(notifyChar, true)

            val descriptor = notifyChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt?.writeDescriptor(descriptor)
                LogManager.logInfo("Notifications activées")
            }
        }

        LogManager.logInfo("Caractéristiques M0Robot configurées (type $appType)")
        onConnectionStateChangedListener?.invoke(true)

        // Démarrer l'envoi des commandes M0Robot
        startM0RobotCommands()
    }

    private fun startM0RobotCommands() {
        // Envoyer les commandes M0Robot pour récupérer les infos
        val commands = listOf(
            byteArrayOf(0x55.toByte(), 0x31.toByte(), 0x86.toByte()), // Vitesse/Batterie
            byteArrayOf(0x55.toByte(), 0x32.toByte(), 0x87.toByte()), // Voltage/Courant
            byteArrayOf(0x55.toByte(), 0x33.toByte(), 0x88.toByte()), // Température/Odomètre
            byteArrayOf(0x55.toByte(), 0x34.toByte(), 0x89.toByte()), // Infos système
            byteArrayOf(0x55.toByte(), 0x35.toByte(), 0x8A.toByte())  // Version firmware
        )

        commands.forEachIndexed { index, command ->
            android.os.Handler().postDelayed({
                sendData(command)
                LogManager.logInfo("Commande M0Robot ${index + 1} envoyée: ${command.joinToString(" ") { "%02X".format(it) }}")
            }, (index * 500).toLong()) // 500ms entre chaque commande
        }
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
            LogManager.logError("Caractéristique M0Robot non disponible")
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
                LogManager.logError("Échec de l'envoi des données M0Robot")
            } else {
                LogManager.logInfo("Envoi M0Robot: ${data.joinToString(" ") { "%02X".format(it) }}")
            }
        } catch (e: Exception) {
            LogManager.logError("Erreur lors de l'envoi M0Robot", e)
        }
    }
}