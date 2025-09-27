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

        LogManager.logInfo("=== DÉBUT DIAGNOSTIC DÉTAILLÉ ===")

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

        // DIAGNOSTIC COMPLET de toutes les caractéristiques
        LogManager.logInfo("=== ANALYSE COMPLÈTE DES CARACTÉRISTIQUES ===")
        for (char in serviceFound.characteristics) {
            val properties = char.properties
            LogManager.logInfo("Caractéristique: ${char.uuid}")
            LogManager.logInfo("  Propriétés: $properties")
            LogManager.logInfo("  READ: ${(properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0}")
            LogManager.logInfo("  WRITE: ${(properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0}")
            LogManager.logInfo("  WRITE_NO_RESPONSE: ${(properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0}")
            LogManager.logInfo("  NOTIFY: ${(properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0}")
            LogManager.logInfo("  INDICATE: ${(properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0}")

            // Analyser les descripteurs
            LogManager.logInfo("  Descripteurs trouvés: ${char.descriptors.size}")
            for (desc in char.descriptors) {
                LogManager.logInfo("    Descripteur: ${desc.uuid}")
            }
        }

        // D'après le code original, M0Robot utilise LA MÊME caractéristique pour write ET notify
        var targetCharacteristic: BluetoothGattCharacteristic? = null

        // SOLUTION : Utiliser 2 caractéristiques distinctes
        var writeCharacteristic: BluetoothGattCharacteristic? = null
        var notifyCharacteristic: BluetoothGattCharacteristic? = null

        // Pour le service 6e400001, essayer TOUTES les caractéristiques
        if (appType == 1) {
            // 6e400002 pour écrire (WRITE)
            writeCharacteristic = serviceFound.getCharacteristic(UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"))
            // 6e400003 pour les notifications (NOTIFY)
            notifyCharacteristic = serviceFound.getCharacteristic(UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"))

            LogManager.logInfo("6e400002 pour écriture: ${writeCharacteristic != null}")
            LogManager.logInfo("6e400003 pour notifications: ${notifyCharacteristic != null}")
        }

        if (writeCharacteristic == null) {
            LogManager.logError("Aucune caractéristique d'écriture trouvée")
            return
        }

        if (notifyCharacteristic == null) {
            LogManager.logError("Aucune caractéristique de notification trouvée")
            return
        }

        characteristic = targetCharacteristic
        LogManager.logInfo("=== CARACTÉRISTIQUE D'ÉCRITURE: ${writeCharacteristic.uuid} ===")
        LogManager.logInfo("=== CARACTÉRISTIQUE DE NOTIFICATION: ${notifyCharacteristic.uuid} ===")

        // Configurer les notifications sur la BONNE caractéristique
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            LogManager.logInfo("Configuration des notifications sur ${notifyCharacteristic.uuid}")

            val setNotifResult = gatt?.setCharacteristicNotification(notifyCharacteristic, true)
            LogManager.logInfo("setCharacteristicNotification résultat: $setNotifResult")

            // Utiliser le bon descripteur CCCD
            val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            val descriptor = notifyCharacteristic.getDescriptor(cccdUuid)

            if (descriptor != null) {
                LogManager.logInfo("*** DESCRIPTEUR CCCD TROUVÉ SUR 6e400003 ! ***")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                val writeResult = gatt?.writeDescriptor(descriptor)
                LogManager.logInfo("writeDescriptor résultat: $writeResult")
            } else {
                LogManager.logError("Descripteur CCCD non trouvé sur 6e400003")
            }
        }

        LogManager.logInfo("=== FIN DIAGNOSTIC ===")
        onConnectionStateChangedListener?.invoke(true)

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            LogManager.logInfo("***** DÉBUT ENVOI COMMANDES M0ROBOT *****")
            startM0RobotCommands()
        }, 2000)
    }

    private fun startM0RobotCommands() {
        LogManager.logInfo("startM0RobotCommands() appelée")

        // Envoyer les commandes M0Robot pour récupérer les infos
        val commands = listOf(
            byteArrayOf(0x55.toByte(), 0x31.toByte(), 0x86.toByte()), // Vitesse/Batterie
            byteArrayOf(0x55.toByte(), 0x32.toByte(), 0x87.toByte()), // Voltage/Courant
            byteArrayOf(0x55.toByte(), 0x33.toByte(), 0x88.toByte()), // Température/Odomètre
            byteArrayOf(0x55.toByte(), 0x34.toByte(), 0x89.toByte()), // Infos système
            byteArrayOf(0x55.toByte(), 0x35.toByte(), 0x8A.toByte())  // Version firmware
        )

        LogManager.logInfo("Nombre de commandes à envoyer: ${commands.size}")

        // Envoyer immédiatement la première commande
        if (commands.isNotEmpty()) {
            sendData(commands[0])
            LogManager.logInfo("Première commande M0Robot envoyée immédiatement: ${commands[0].joinToString(" ") { "%02X".format(it) }}")
        }

        // Envoyer les autres avec délai
        commands.drop(1).forEachIndexed { index, command ->
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                LogManager.logInfo("Envoi commande M0Robot ${index + 2}: ${command.joinToString(" ") { "%02X".format(it) }}")
                sendData(command)
            }, ((index + 1) * 1000).toLong()) // 1 seconde entre chaque
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