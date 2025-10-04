package com.ix7.tracker.bluetooth

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ix7.tracker.core.*
import kotlinx.coroutines.*
import java.util.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HoverboardData(
    val speed: Float = 0f,
    val battery: Int = 0,
    val voltage: Float = 0f,
    val current: Float = 0f,
    val temperature: Int = 0,
    val odometer: Int = 0,
    val tripDistance: Int = 0,
    val mode: Int = 0,
    val errors: Int = 0,
    val headlightsOn: Boolean = false,
    val neonOn: Boolean = false,
    val cruiseControlOn: Boolean = false
)

object ProtocolDecoder {
    fun decode(data: ByteArray): HoverboardData? {
        if (data.size < 10 || data[0] != 0x61.toByte() || data[1] != 0x9E.toByte()) {
            return null
        }

        val type = data[2].toInt() and 0xFF

        return when (type) {
            0x30 -> decodeModeFrame(data)
            0x3E -> decodeTelemetryFrame(data)
            0x32 -> decodeBatteryFrame(data)
            else -> null
        }
    }

    private fun decodeModeFrame(data: ByteArray): HoverboardData {
        if (data.size < 8) return HoverboardData()

        val byte6 = data[6].toInt() and 0xFF
        val byte7 = data[7].toInt() and 0xFF

        val headlightsOn = byte6 == 0xC0 && byte7 == 0x35
        val neonOn = byte6 == 0xC0 && byte7 == 0x34

        val mode = when {
            byte6 == 0xC3 && byte7 == 0xE1 -> 0
            byte6 == 0xC3 && byte7 == 0x6B -> 0
            byte6 == 0x4A && byte7 == 0x36 -> 1
            byte6 == 0x4A && byte7 == 0x35 -> 2
            byte6 == 0x4A && byte7 == 0x34 -> 3
            else -> 0
        }

        return HoverboardData(
            headlightsOn = headlightsOn,
            neonOn = neonOn,
            mode = mode
        )
    }

    private fun decodeTelemetryFrame(data: ByteArray): HoverboardData {
        if (data.size < 16) return HoverboardData()
        return HoverboardData()
    }

    private fun decodeBatteryFrame(data: ByteArray): HoverboardData {
        if (data.size < 12) return HoverboardData()
        return HoverboardData()
    }
}

class BluetoothConnector(
    private val context: Context,
    private val onStateChange: (ConnectionState) -> Unit,
    private val onDataDecoded: (ByteArray) -> Unit
) {
    companion object {
        private const val TAG = "BluetoothConnector"
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

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "✅ Connecté au GATT server")
                    onStateChange(ConnectionState.CONNECTED)

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

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            characteristic.value?.let { data ->
                val hex = data.joinToString(" ") { "%02X".format(it) }
                val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())

                Log.e("BLE_RAW", "[$timestamp] SIZE:${data.size} $hex")

                val type = if (data.size >= 3) {
                    when (data[2]) {
                        0x30.toByte() -> "MODE"
                        0x37.toByte() -> "STATUS"
                        0x3E.toByte() -> "TELEMETRY"
                        0x32.toByte() -> "BATTERY"
                        0x3A.toByte() -> "ACTION"
                        else -> "TYPE_${"%02X".format(data[2])}"
                    }
                } else "SHORT"

                Log.e("BLE_TYPE", "[$timestamp] $type")

                ProtocolDecoder.decode(data)?.let { hoverData ->
                    // Traitement des données décodées
                }

                onDataReceived?.invoke(data)
                onDataDecoded(data)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✅ Services découverts")

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

                handler.postDelayed({
                    sendInitialCommands()
                }, 200)
            } else {
                Log.e(TAG, "❌ Échec écriture descriptor: $status")
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

    private fun enableNotifications(gatt: BluetoothGatt): Boolean {
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

        val service = gatt.getService(SERVICE_UUID)
        if (service == null) {
            Log.e(TAG, "❌ Service NUS non trouvé")
            return false
        }

        Log.i(TAG, "✅ Service NUS trouvé: ${service.uuid}")

        val charNotify = service.getCharacteristic(CHAR_NOTIFY_UUID)
        val charWrite = service.getCharacteristic(CHAR_WRITE_UUID)

        if (charNotify == null || charWrite == null) {
            Log.e(TAG, "❌ Characteristics NUS non trouvées")
            return false
        }

        Log.i(TAG, "✅ Characteristics NUS trouvées")

        writeCharacteristic = charWrite

        val notifySuccess = gatt.setCharacteristicNotification(charNotify, true)
        if (!notifySuccess) {
            Log.e(TAG, "❌ Échec setCharacteristicNotification")
            return false
        }

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

    private fun sendInitialCommands() {
        scope.launch {
            Log.i(TAG, "📤 Envoi commandes REPLAY depuis Wireshark...")
            delay(200)

            val commands = listOf(
                byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x15, 0x9E.toByte(), 0xD3.toByte(), 0x5A, 0x8E.toByte(), 0xCB.toByte()),
                byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x53, 0x2E, 0xE0.toByte(), 0x19, 0xCB.toByte()),
                byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xDE.toByte(), 0x3C, 0xBD.toByte(), 0xCA.toByte())
            )

            commands.forEachIndexed { index, cmd ->
                val hex = cmd.joinToString(" ") { "%02X".format(it) }
                Log.d(TAG, "→ Replay ${index + 1}: $hex")
                sendCommand(cmd)
                delay(100)
            }

            Log.i(TAG, "✅ Commandes replay envoyées - En attente réponse...")

            delay(300)
            startPolling()
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            Log.i(TAG, "🔄 Polling démarré (toutes les 300ms)")

            val pollingCmd = byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xDE.toByte(), 0x3C, 0xBD.toByte(), 0xCA.toByte())

            while (isActive && isNotificationsEnabled) {
                sendCommand(pollingCmd)
                delay(300)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        Log.d(TAG, "⏸️ Polling arrêté")
    }

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

    fun disconnect(): Result<Unit> {
        Log.i(TAG, "🔌 Déconnexion...")
        stopPolling()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        onStateChange(ConnectionState.DISCONNECTED)
        return Result.success(Unit)
    }

    suspend fun sendCommand(command: ByteArray): Result<Unit> {
        val hex = command.joinToString(" ") { "%02X".format(it) }
        Log.e("BluetoothConnector", "📤 sendCommand() appelé avec: $hex")

        if (writeCharacteristic == null) {
            Log.e("BluetoothConnector", "❌ writeCharacteristic est NULL")
            return Result.failure(Exception("writeCharacteristic null"))
        }

        if (bluetoothGatt == null) {
            Log.e("BluetoothConnector", "❌ bluetoothGatt est NULL")
            return Result.failure(Exception("bluetoothGatt null"))
        }

        return withContext(Dispatchers.IO) {
            try {
                writeCharacteristic?.value = command
                val success = bluetoothGatt?.writeCharacteristic(writeCharacteristic)
                Log.e("BluetoothConnector", "📤 writeCharacteristic retourne: $success")

                if (success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("writeCharacteristic a échoué"))
                }
            } catch (e: Exception) {
                Log.e("BluetoothConnector", "❌ Exception dans sendCommand", e)
                Result.failure(e)
            }
        }
    }

    fun cleanup() {
        stopPolling()
        scope.cancel()
        handler.removeCallbacksAndMessages(null)
        bluetoothGatt?.close()
        bluetoothGatt = null
        Log.d(TAG, "🧹 Nettoyage terminé")
    }
}