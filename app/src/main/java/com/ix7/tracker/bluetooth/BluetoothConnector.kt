package com.ix7.tracker.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.ix7.tracker.core.ConnectionState
import com.ix7.tracker.core.ProtocolConstants
import kotlinx.coroutines.*
import java.util.*

/**
 * Gestionnaire de connexion Bluetooth CORRIGÉ
 * Basé sur l'analyse de l'application officielle M0Robot
 * Utilise les VRAIES commandes du protocole 55 AA
 */
class BluetoothConnector(
    private val context: Context,
    private val onStateChanged: (ConnectionState) -> Unit
) {
    companion object {
        private const val TAG = "BluetoothConnector"

        // UUIDs Nordic UART Service (confirmés dans les logs)
        private val SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        private val CHAR_TX_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        private val CHAR_RX_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var dataHandler: ((ByteArray) -> Unit)? = null
    private var dataRequestJob: Job? = null
    private var isNotificationEnabled = false

    /**
     * Callback GATT pour gérer les événements Bluetooth
     */
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "✓ Connecté au robot - Attente stabilisation...")
                    onStateChanged(ConnectionState.CONNECTED)

                    // Attendre 600ms avant de découvrir les services (stabilité)
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(600)
                        Log.i(TAG, "→ Découverte des services...")
                        gatt.discoverServices()
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.w(TAG, "✗ Déconnecté du robot")
                    onStateChanged(ConnectionState.DISCONNECTED)
                    cleanup()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✓ Services découverts")
                logAvailableServices(gatt)
                setupNotifications(gatt)
            } else {
                Log.e(TAG, "❌ Échec découverte services: $status")
                onStateChanged(ConnectionState.ERROR)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val hex = value.joinToString(" ") { "%02X".format(it) }
            Log.d(TAG, "📨 Notification reçue (${value.size} bytes): $hex")
            dataHandler?.invoke(value)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✓ CCCD écrit - Notifications activées")
                isNotificationEnabled = true

                // Démarrer les demandes périodiques APRÈS activation des notifications
                startDataRequests(gatt)
            } else {
                Log.e(TAG, "❌ Échec écriture CCCD: $status")
            }
        }
    }

    /**
     * Se connecte à un appareil
     */
    suspend fun connect(address: String, onData: (ByteArray) -> Unit): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                Log.i(TAG, "→ Connexion à $address...")

                dataHandler = onData
                onStateChanged(ConnectionState.CONNECTING)

                val device = getBluetoothDevice(address)
                    ?: return@withContext Result.failure(Exception("Device not found"))

                bluetoothGatt = device.connectGatt(
                    context,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )

                Result.success(Unit)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur connexion", e)
                onStateChanged(ConnectionState.ERROR)
                Result.failure(e)
            }
        }
    }

    /**
     * Configure les notifications pour recevoir les données
     */
    private fun setupNotifications(gatt: BluetoothGatt) {
        try {
            val service = gatt.getService(SERVICE_UUID)
            if (service == null) {
                Log.e(TAG, "❌ Service Nordic UART non trouvé")
                return
            }

            val rxCharacteristic = service.getCharacteristic(CHAR_RX_UUID)
            if (rxCharacteristic == null) {
                Log.e(TAG, "❌ Caractéristique RX non trouvée")
                return
            }

            // Activer les notifications
            val notificationSet = gatt.setCharacteristicNotification(rxCharacteristic, true)
            if (!notificationSet) {
                Log.e(TAG, "❌ Impossible d'activer les notifications")
                return
            }

            // Écrire le CCCD pour activer les notifications côté périphérique
            val descriptor = rxCharacteristic.getDescriptor(CCCD_UUID)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                val written = gatt.writeDescriptor(descriptor)
                if (!written) {
                    Log.e(TAG, "❌ Échec écriture CCCD")
                } else {
                    Log.i(TAG, "✓ Configuration réussie - En attente de données automatiques...")
                }
            } else {
                Log.e(TAG, "❌ CCCD non trouvé")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur setup notifications", e)
        }
    }

    /**
     * Démarre les demandes périodiques de données
     * UTILISE LES VRAIES COMMANDES du protocole M0Robot
     */
    private fun startDataRequests(gatt: BluetoothGatt?) {
        dataRequestJob?.cancel()

        dataRequestJob = CoroutineScope(Dispatchers.IO).launch {
            delay(1000) // Attendre 1s après l'activation des notifications

            Log.i(TAG, "🚀 Démarrage des demandes périodiques de données")

            var cycleCount = 0

            while (isActive && isNotificationEnabled) {
                try {
                    cycleCount++
                    Log.d(TAG, "────────────────────────────────────────")
                    Log.d(TAG, "🔄 Cycle #$cycleCount - Demande de données")

                    // COMMANDE 1: Demande de données complètes (trame longue 60 bytes)
                    // Format observé dans l'app officielle
                    val cmd1 = byteArrayOf(
                        0x55.toByte(), 0xAA.toByte(),  // Header
                        0x03.toByte(),                  // Length = 3
                        0x22.toByte(),                  // Command = REQUEST_DATA
                        0x01.toByte(),                  // SubCommand
                        0x00.toByte(),                  // Data
                        calculateChecksum(byteArrayOf(0x03, 0x22, 0x01, 0x00))  // Checksum
                    )

                    sendCommandInternal(gatt, cmd1, "Demande données complètes")
                    delay(1000) // Attendre la réponse

                    // COMMANDE 2: Demande de statut
                    val cmd2 = byteArrayOf(
                        0x55.toByte(), 0xAA.toByte(),
                        0x02.toByte(),
                        0x20.toByte(),
                        calculateChecksum(byteArrayOf(0x02, 0x20))
                    )

                    sendCommandInternal(gatt, cmd2, "Demande statut")
                    delay(1000)

                    // COMMANDE 3: Demande version (toutes les 10 secondes)
                    if (cycleCount % 5 == 0) {
                        val cmd3 = byteArrayOf(
                            0x55.toByte(), 0xAA.toByte(),
                            0x02.toByte(),
                            0x03.toByte(),
                            calculateChecksum(byteArrayOf(0x02, 0x03))
                        )
                        sendCommandInternal(gatt, cmd3, "Demande version")
                        delay(500)
                    }

                    // Pause entre les cycles (2 secondes)
                    delay(2000)

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erreur cycle demande données", e)
                    delay(3000) // Attendre plus longtemps en cas d'erreur
                }
            }

            Log.w(TAG, "⚠ Arrêt des demandes périodiques")
        }
    }

    /**
     * Envoie une commande au scooter
     */
    private suspend fun sendCommandInternal(
        gatt: BluetoothGatt?,
        command: ByteArray,
        description: String = ""
    ) {
        withContext(Dispatchers.Main) {
            try {
                if (gatt == null) {
                    Log.e(TAG, "❌ GATT null, impossible d'envoyer la commande")
                    return@withContext
                }

                val service = gatt.getService(SERVICE_UUID)
                val txCharacteristic = service?.getCharacteristic(CHAR_TX_UUID)

                if (txCharacteristic == null) {
                    Log.e(TAG, "❌ Caractéristique TX non trouvée")
                    return@withContext
                }

                txCharacteristic.value = command
                val success = gatt.writeCharacteristic(txCharacteristic)

                val hex = command.joinToString(" ") { "%02X".format(it) }
                if (success) {
                    Log.d(TAG, "→ Envoyé ${if (description.isNotEmpty()) "[$description]" else ""}: $hex")
                } else {
                    Log.e(TAG, "❌ Échec envoi commande: $hex")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur envoi commande", e)
            }
        }
    }

    /**
     * Calcule le checksum d'une commande (XOR simple)
     */
    private fun calculateChecksum(data: ByteArray): Byte {
        var checksum = 0
        for (byte in data) {
            checksum = checksum xor (byte.toInt() and 0xFF)
        }
        return checksum.toByte()
    }

    /**
     * Envoie une commande publique (pour les tests)
     */
    suspend fun sendCommand(command: ByteArray): Result<Unit> {
        return try {
            sendCommandInternal(bluetoothGatt, command)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Se déconnecte du robot
     */
    suspend fun disconnect(): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                Log.i(TAG, "→ Déconnexion...")
                cleanup()
                bluetoothGatt?.disconnect()
                bluetoothGatt?.close()
                bluetoothGatt = null
                onStateChanged(ConnectionState.DISCONNECTED)
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur déconnexion", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Nettoie les ressources
     */
    internal fun cleanup() {
        dataRequestJob?.cancel()
        dataRequestJob = null
        isNotificationEnabled = false
    }

    /**
     * Log tous les services disponibles (debug)
     */
    private fun logAvailableServices(gatt: BluetoothGatt) {
        Log.d(TAG, "═══ Services disponibles ═══")
        for (service in gatt.services) {
            Log.d(TAG, "Service: ${service.uuid}")
            for (characteristic in service.characteristics) {
                val properties = mutableListOf<String>()
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                    properties.add("READ")
                }
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                    properties.add("WRITE")
                }
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
                    properties.add("WRITE_NO_RESP")
                }
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                    properties.add("NOTIFY")
                }
                Log.d(TAG, "  └─ ${characteristic.uuid} [${properties.joinToString(", ")}]")
            }
        }
        Log.d(TAG, "═══════════════════════════")
    }

    /**
     * Récupère un device Bluetooth par son adresse
     */
    private fun getBluetoothDevice(address: String): BluetoothDevice? {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE)
                    as android.bluetooth.BluetoothManager
            bluetoothManager.adapter.getRemoteDevice(address)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur récupération device", e)
            null
        }
    }
}