package com.ix7.tracker.bluetooth

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ix7.tracker.core.*
import kotlinx.coroutines.*
import java.util.*



data class HoverboardData(
    val speed: Float = 0f,
    val battery: Int = 0,
    val voltage: Float = 0f,
    val current: Float = 0f,
    val temperature: Int = 0,
    val odometer: Int = 0,
    val tripDistance: Int = 0,
    val mode: Int = 0,
    val errors: Int = 0
)

object ProtocolDecoder {
    fun decode(data: ByteArray): HoverboardData? {
        if (data.size < 10 || data[0] != 0x61.toByte() || data[1] != 0x9E.toByte()) {
            return null
        }

        val type = data[2].toInt() and 0xFF

        return when (type) {
            0x3E -> decode16ByteFrame(data)  // Trame longue
            0x32 -> decode12ByteFrame(data)  // Trame moyenne
            0x30 -> decode10ByteFrame(data)  // Trame courte
            else -> null
        }
    }

    private fun decode16ByteFrame(data: ByteArray): HoverboardData {
        // Extraction des données depuis les bytes
        val byte6 = data[6].toInt() and 0xFF
        val byte7 = data[7].toInt() and 0xFF

        return HoverboardData(
            battery = byte6,  // Approximation
            mode = byte7
        )
    }

    private fun decode12ByteFrame(data: ByteArray): HoverboardData {
        val byte6 = data[6].toInt() and 0xFF
        val byte7 = data[7].toInt() and 0xFF

        return HoverboardData(
            voltage = (byte6 * 256 + byte7) / 100f
        )
    }

    private fun decode10ByteFrame(data: ByteArray): HoverboardData {
        val byte6 = data[6].toInt() and 0xFF

        return HoverboardData(
            temperature = byte6
        )
    }
}

/**
 * Gestionnaire de connexion Bluetooth BLE
 * CORRIGÉ : Séquence de connexion identique à l'app officielle
 */
class BluetoothConnector(
    private val context: Context,
    private val onStateChange: (ConnectionState) -> Unit
) {
    companion object {
        private const val TAG = "BluetoothConnector"
        // ✅ APRÈS
// Nordic UART Service (NUS) - Ton hoverboard utilise ça !
        private val SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        private val CHAR_WRITE_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        private val CHAR_NOTIFY_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var onDataReceived: ((ByteArray) -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var isNotificationsEnabled = false
    private var pollingJob: Job? = null

    /**
     * Callback GATT
     */
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "✅ Connecté au GATT server")
                    onStateChange(ConnectionState.CONNECTED)

                    // Découvrir les services
                    handler.postDelayed({
                        val result = gatt.discoverServices()
                        Log.d(TAG, "Découverte services lancée: $result")
                    }, 500)
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.w(TAG, "❌ Déconnecté du GATT server")
                    onStateChange(ConnectionState.DISCONNECTED)
                    stopPolling()
                    isNotificationsEnabled = false
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✅ Services découverts")

                // Activer les notifications
                val success = enableNotifications(gatt)
                if (success) {
                    Log.i(TAG, "✅ Notifications activées")
                } else {
                    Log.e(TAG, "❌ Échec activation notifications")
                }
            } else {
                Log.e(TAG, "❌ Échec découverte services: $status")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✅ Descriptor écrit avec succès")
                isNotificationsEnabled = true

                // MAINTENANT on peut envoyer des commandes
                handler.postDelayed({
                    sendInitialCommands()
                }, 200)
            } else {
                Log.e(TAG, "❌ Échec écriture descriptor: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.value?.let { data ->
                val hex = data.joinToString(" ") { "%02X".format(it) }
                Log.d(TAG, "📥 Data reçue: $hex")

                // Décoder le protocole
                ProtocolDecoder.decode(data)?.let { hoverData ->
                    Log.i(TAG, "🔋 Batterie: ${hoverData.battery}%")
                    Log.i(TAG, "⚡ Voltage: ${hoverData.voltage}V")
                    Log.i(TAG, "🌡️ Température: ${hoverData.temperature}°C")
                }

                onDataReceived?.invoke(data)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "✅ Commande envoyée")
            } else {
                Log.e(TAG, "❌ Échec envoi commande: $status")
            }
        }
    }

    /**
     * Active les notifications sur la characteristic
     */
    /**
     * Active les notifications sur la characteristic
     */
    private fun enableNotifications(gatt: BluetoothGatt): Boolean {
        // 🔍 DIAGNOSTIC : Lister TOUS les services disponibles
        Log.i(TAG, "📋 === LISTE DE TOUS LES SERVICES DÉCOUVERTS ===")
        gatt.services.forEach { service ->
            Log.i(TAG, "  📦 Service: ${service.uuid}")
            service.characteristics.forEach { char ->
                val props = char.properties
                val propsStr = buildString {
                    if (props and BluetoothGattCharacteristic.PROPERTY_READ != 0) append("READ ")
                    if (props and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) append("WRITE ")
                    if (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) append("NOTIFY ")
                }
                Log.i(TAG, "    └─ Char: ${char.uuid} [$propsStr]")
            }
        }
        Log.i(TAG, "📋 === FIN DE LA LISTE ===")

        // Chercher le service NUS
        val service = gatt.getService(SERVICE_UUID)

        if (service == null) {
            Log.e(TAG, "❌ Service NUS non trouvé")
            return false
        }

        Log.i(TAG, "✅ Service NUS trouvé: ${service.uuid}")

// Characteristic pour NOTIFY
        val charNotify = service.getCharacteristic(CHAR_NOTIFY_UUID)
// Characteristic pour WRITE
        val charWrite = service.getCharacteristic(CHAR_WRITE_UUID)

        if (charNotify == null || charWrite == null) {
            Log.e(TAG, "❌ Characteristics NUS non trouvées")
            return false
        }

        Log.i(TAG, "✅ Characteristics NUS trouvées")

// Sauvegarder la characteristic WRITE
        writeCharacteristic = charWrite

// Activer les notifications sur NOTIFY
        val notifySuccess = gatt.setCharacteristicNotification(charNotify, true)

        if (!notifySuccess) {
            Log.e(TAG, "❌ Échec setCharacteristicNotification")
            return false
        }

// Écrire dans le descriptor CCCD sur la characteristic NOTIFY
        val descriptor = charNotify.getDescriptor(CCCD_UUID)
        if (descriptor == null) {
            Log.e(TAG, "❌ Descriptor CCCD non trouvé")
            Log.e(TAG, "Descriptors disponibles: ${charNotify.descriptors.map { it.uuid }}")
            return false
        }

        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        val writeSuccess = gatt.writeDescriptor(descriptor)

        Log.d(TAG, "Écriture descriptor CCCD: $writeSuccess")
        return writeSuccess
    }

    /**
     * Envoie les commandes initiales (comme l'app officielle)
     */
    private fun sendInitialCommands() {
        scope.launch {
            Log.i(TAG, "📤 Envoi commandes REPLAY depuis Wireshark...")
            delay(200)

            // Vraies commandes capturées de l'app officielle
            val commands = listOf(
                byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x15, 0x9E.toByte(), 0xD3.toByte(), 0x5A, 0x8E.toByte(), 0xCB.toByte()),
                byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x53, 0x2E, 0xE0.toByte(), 0x19, 0xCB.toByte()),
                byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xDE.toByte(), 0x3C, 0xBD.toByte(), 0xCA.toByte())
            )

            commands.forEachIndexed { index, cmd ->
                val hex = cmd.joinToString(" ") { "%02X".format(it) }
                Log.d(TAG, "→ Replay ${index + 1}: $hex")
                sendCommand(cmd)
                delay(100) // Toutes les 100ms comme l'app officielle
            }

            Log.i(TAG, "✅ Commandes replay envoyées - En attente réponse...")

            // Puis démarrer le polling avec la même commande
            delay(300)
            startPolling()
        }
    }

    /**
     * Démarre le polling automatique (comme l'app officielle)
     */
    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            Log.i(TAG, "🔄 Polling démarré (toutes les 300ms)")

            // Commande de polling capturée
            val pollingCmd = byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xDE.toByte(), 0x3C, 0xBD.toByte(), 0xCA.toByte())

            while (isActive && isNotificationsEnabled) {
                sendCommand(pollingCmd)
                delay(300)
            }
        }
    }

    /**
     * Arrête le polling
     */
    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        Log.d(TAG, "⏸️ Polling arrêté")
    }

    /**
     * Connexion au device
     */
    fun connect(address: String, dataCallback: (ByteArray) -> Unit): Result<Unit> {
        return try {
            onDataReceived = dataCallback

            val device = bluetoothAdapter?.getRemoteDevice(address)
            if (device == null) {
                return Result.failure(Exception("Device introuvable"))
            }

            Log.i(TAG, "🔌 Connexion à $address...")
            onStateChange(ConnectionState.CONNECTING)

            bluetoothGatt?.close()
            bluetoothGatt = device.connectGatt(context, false, gattCallback)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur connexion", e)
            onStateChange(ConnectionState.ERROR)
            Result.failure(e)
        }
    }

    /**
     * Déconnexion
     */
    fun disconnect(): Result<Unit> {
        Log.i(TAG, "🔌 Déconnexion...")
        stopPolling()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        onStateChange(ConnectionState.DISCONNECTED)
        return Result.success(Unit)
    }

    /**
     * Envoi d'une commande
     */
    fun sendCommand(command: ByteArray): Result<Unit> {
        if (!isNotificationsEnabled) {
            Log.w(TAG, "⚠️ Notifications pas encore activées")
            return Result.failure(Exception("Notifications not ready"))
        }

        val characteristic = writeCharacteristic
        if (characteristic == null) {
            Log.e(TAG, "❌ Characteristic non disponible")
            return Result.failure(Exception("Characteristic not found"))
        }

        return try {
            ProtocolUtils.logFrame("→ ENVOI", command)
            characteristic.value = command
            val success = bluetoothGatt?.writeCharacteristic(characteristic) ?: false

            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Write failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur envoi", e)
            Result.failure(e)
        }
    }

    /**
     * Nettoyage
     */
    fun cleanup() {
        stopPolling()
        scope.cancel()
        handler.removeCallbacksAndMessages(null)
        bluetoothGatt?.close()
        bluetoothGatt = null
        Log.d(TAG, "🧹 Nettoyage terminé")
    }


    // Ajoute dans BluetoothConnector.kt
    private fun sendCapturedCommands() {
        scope.launch {
            delay(200)

            // Bytes capturés de Wireshark (les vraies commandes)
            val commands = listOf(
                byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x15, 0x9E.toByte(), 0xD3.toByte(), 0x5A, 0x8E.toByte(), 0xCB.toByte()),
                byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x53, 0x2E, 0xE0.toByte(), 0x19, 0xCB.toByte()),
                byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xDE.toByte(), 0x3C, 0xBD.toByte(), 0xCA.toByte())
            )

            commands.forEach { cmd ->
                Log.d(TAG, "→ Replay: ${cmd.joinToString(" ") { "%02X".format(it) }}")
                sendCommand(cmd)
                delay(100)
            }

            Log.i(TAG, "✅ Commandes replay envoyées")
        }
    }
}