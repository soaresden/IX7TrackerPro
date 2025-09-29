package com.ix7.tracker.bluetooth

import android.bluetooth.*
import android.content.Context
import android.util.Log
import com.ix7.tracker.core.ConnectionState
import kotlinx.coroutines.*
import java.util.*

/**
 * Gestionnaire de connexion Bluetooth CORRIGÉ pour hoverboards utilisant le protocole FFE0/FFE1
 * Basé sur l'analyse de l'application officielle
 */
class BluetoothConnector(
    private val context: Context,
    private val onStateChange: (ConnectionState) -> Unit
) {
    companion object {
        private const val TAG = "BluetoothConnector"

        // UUIDs CORRECTS pour le hoverboard (protocole 55 AA)
        private val SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        private val CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private var onDataReceived: ((ByteArray) -> Unit)? = null
    private var dataRequestJob: Job? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "✓ Connecté au hoverboard - Découverte des services...")
                    onStateChange(ConnectionState.CONNECTED)
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "✗ Déconnecté du hoverboard")
                    onStateChange(ConnectionState.DISCONNECTED)
                    dataRequestJob?.cancel()
                    cleanup()
                }
                BluetoothProfile.STATE_CONNECTING -> {
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
                    Log.i(TAG, "✓ Notifications configurées - Démarrage demande de données")
                    startDataRequests(gatt)
                } else {
                    Log.e(TAG, "✗ Échec configuration notifications")
                    onStateChange(ConnectionState.ERROR)
                }
            } else {
                Log.e(TAG, "✗ Échec découverte services: $status")
                onStateChange(ConnectionState.ERROR)
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                characteristic.value?.let { data ->
                    val hex = data.joinToString(" ") { "%02X".format(it) }
                    Log.d(TAG, "📖 Données lues (${data.size} bytes): $hex")
                    onDataReceived?.invoke(data)
                }
            } else {
                Log.w(TAG, "Échec lecture: $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.value?.let { data ->
                val hex = data.joinToString(" ") { "%02X".format(it) }
                Log.d(TAG, "🔔 Notification reçue (${data.size} bytes): $hex")
                onDataReceived?.invoke(data)
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "✓ Commande envoyée avec succès")
            } else {
                Log.e(TAG, "✗ Échec envoi commande: $status")
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✓ Descriptor écrit avec succès (notifications activées)")
            } else {
                Log.e(TAG, "✗ Échec écriture descriptor: $status")
            }
        }
    }

    /**
     * Log tous les services disponibles pour diagnostic
     */
    private fun logAllServices(gatt: BluetoothGatt?) {
        Log.d(TAG, "=== Services Bluetooth disponibles ===")
        gatt?.services?.forEach { service ->
            Log.d(TAG, "Service: ${service.uuid}")
            service.characteristics.forEach { char ->
                val props = mutableListOf<String>()
                if (char.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) props.add("READ")
                if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) props.add("WRITE")
                if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) props.add("WRITE_NO_RESP")
                if (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) props.add("NOTIFY")
                Log.d(TAG, "  └─ Char: ${char.uuid} [${props.joinToString(", ")}]")
            }
        }
        Log.d(TAG, "====================================")
    }

    /**
     * Configure les notifications sur le service FFE0/FFE1
     */
    private fun setupNotifications(gatt: BluetoothGatt?): Boolean {
        // Récupérer le service FFE0 (le seul qui fonctionne pour ce hoverboard)
        val service = gatt?.getService(SERVICE_UUID)

        if (service == null) {
            Log.e(TAG, "✗ Service FFE0 NON TROUVÉ ! Le hoverboard n'est pas compatible")
            return false
        }

        Log.i(TAG, "✓ Service FFE0 trouvé")

        // Récupérer la characteristic FFE1 (read/write/notify)
        val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID)

        if (characteristic == null) {
            Log.e(TAG, "✗ Characteristic FFE1 NON TROUVÉE !")
            return false
        }

        Log.i(TAG, "✓ Characteristic FFE1 trouvée")

        // Sauvegarder pour l'écriture
        writeCharacteristic = characteristic

        // Activer les notifications localement
        val notificationEnabled = gatt.setCharacteristicNotification(characteristic, true)
        if (!notificationEnabled) {
            Log.e(TAG, "✗ Échec activation notifications locales")
            return false
        }

        Log.i(TAG, "✓ Notifications locales activées")

        // Activer les notifications sur le device (écrire dans le CCCD)
        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor == null) {
            Log.e(TAG, "✗ CCCD descriptor non trouvé")
            return false
        }

        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        val descriptorWritten = gatt.writeDescriptor(descriptor)

        if (!descriptorWritten) {
            Log.e(TAG, "✗ Échec écriture CCCD descriptor")
            return false
        }

        Log.i(TAG, "✓ CCCD descriptor écrit - Notifications activées sur le device")
        return true
    }

    /**
     * Envoie périodiquement des commandes pour demander les données
     */
    private fun startDataRequests(gatt: BluetoothGatt?) {
        dataRequestJob?.cancel()

        dataRequestJob = CoroutineScope(Dispatchers.IO).launch {
            delay(1000) // Attendre que les notifications soient bien activées

            Log.i(TAG, "🚀 Démarrage des demandes de données périodiques")

            while (isActive) {
                try {
                    // Commandes basées sur le protocole 55 AA observé
                    val commands = listOf(
                        // Commande 1: Demande données principales (similaire au log)
                        byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x03.toByte(), 0x22.toByte(), 0x01.toByte(), 0x00.toByte(), 0x26.toByte()),

                        // Commande 2: Demande statut
                        byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x02.toByte(), 0x20.toByte(), 0x22.toByte()),

                        // Commande 3: Keep-alive
                        byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x01.toByte(), 0x00.toByte(), 0x56.toByte())
                    )

                    commands.forEach { command ->
                        sendCommandInternal(gatt, command)
                        delay(500) // Délai entre les commandes
                    }

                    delay(2000) // Répéter toutes les 2 secondes
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la demande de données", e)
                }
            }
        }
    }

    /**
     * Envoie une commande sur la characteristic FFE1
     */
    private fun sendCommandInternal(gatt: BluetoothGatt?, command: ByteArray) {
        val char = writeCharacteristic

        if (char == null) {
            Log.e(TAG, "✗ Write characteristic non disponible")
            return
        }

        try {
            char.value = command
            val success = gatt?.writeCharacteristic(char) ?: false

            val hex = command.joinToString(" ") { "%02X".format(it) }
            if (success) {
                Log.d(TAG, "📤 Commande envoyée: $hex")
            } else {
                Log.w(TAG, "⚠ Échec envoi commande: $hex")
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Exception envoi commande", e)
        }
    }

    fun connect(address: String, dataCallback: (ByteArray) -> Unit): Result<Unit> {
        return try {
            onDataReceived = dataCallback
            val device = bluetoothAdapter?.getRemoteDevice(address)

            if (device != null) {
                Log.i(TAG, "→ Connexion à $address...")
                onStateChange(ConnectionState.CONNECTING)
                bluetoothGatt?.close()
                bluetoothGatt = device.connectGatt(context, false, gattCallback)
                Result.success(Unit)
            } else {
                Log.e(TAG, "✗ Device non trouvé: $address")
                onStateChange(ConnectionState.ERROR)
                Result.failure(Exception("Device not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Exception lors de la connexion", e)
            onStateChange(ConnectionState.ERROR)
            Result.failure(e)
        }
    }

    fun disconnect(): Result<Unit> {
        return try {
            Log.i(TAG, "→ Déconnexion...")
            dataRequestJob?.cancel()
            bluetoothGatt?.disconnect()
            onStateChange(ConnectionState.DISCONNECTED)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "✗ Erreur déconnexion", e)
            Result.failure(e)
        }
    }

    fun sendCommand(command: ByteArray): Result<Unit> {
        return try {
            sendCommandInternal(bluetoothGatt, command)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "✗ Erreur envoi commande", e)
            Result.failure(e)
        }
    }

    fun cleanup() {
        dataRequestJob?.cancel()
        bluetoothGatt?.close()
        bluetoothGatt = null
        onDataReceived = null
        writeCharacteristic = null
        Log.d(TAG, "Nettoyage effectué")
    }
}