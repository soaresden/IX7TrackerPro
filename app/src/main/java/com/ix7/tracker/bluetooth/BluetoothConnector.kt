package com.ix7.tracker.bluetooth

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ix7.tracker.core.ConnectionState
import com.ix7.tracker.core.ScooterData
import com.ix7.tracker.core.SpeedLimitMode
import com.ix7.tracker.protocol.ProtocolConstants
import com.ix7.tracker.protocol.ScooterDataParser // 🔥 NOUVEAU IMPORT
import kotlinx.coroutines.*
import java.util.*
import com.ix7.tracker.core.WheelMode


/**
 * Gestionnaire de connexion Bluetooth GATT
 * VERSION MODIFIÉE - Utilise ScooterDataParser
 */
class BluetoothConnector(
    private val context: Context,
    private val onDataReceived: (ScooterData) -> Unit,
    private val onStateChange: (ConnectionState) -> Unit
) {

    companion object {
        private const val TAG = "BT_CONNECTOR"

        // UUIDs M0Robot
        private val SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        private val TX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        private val RX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // Commandes init
        private val CMD_INIT_1 = byteArrayOf(0x61, 0x9E.toByte(), 0x02, 0x14, 0x55, 0x01, 0xCC.toByte(), 0xCB.toByte())
        private val CMD_KEEP_ALIVE = byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xDE.toByte(), 0x3C, 0xBD.toByte(), 0xCA.toByte())

        // Timings
        private const val INIT_SEQUENCE_DELAY_MS = 500L
        private const val POLLING_INTERVAL_MS = 500L
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var readCharacteristic: BluetoothGattCharacteristic? = null

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO)
    private var currentScooterData = ScooterData()

    private var isPolling = false
    private var pollingRunnable: Runnable? = null

    // Callback Bluetooth
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "✅ Connecté au GATT")
                    onStateChange(ConnectionState.CONNECTED)
                    bluetoothGatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "❌ Déconnecté")
                    onStateChange(ConnectionState.DISCONNECTED)
                    stopPolling()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "🔍 Services découverts")
                setupCharacteristics()
                scope.launch {
                    startInitSequence()
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            characteristic?.value?.let { data ->
                // 🔥 NOUVEAU : Utilise ScooterDataParser au lieu de BluetoothDataHandler
                val scooterData = ScooterDataParser.parseFrame(data, currentScooterData)

                if (scooterData != null) {
                    currentScooterData = scooterData
                    onDataReceived(scooterData)
                }
            }
        }
    }

    // Connexion
    fun connect(device: BluetoothDevice): Result<Unit> {
        return try {
            Log.i(TAG, "🔗 Connexion à ${device.name} (${device.address})")
            onStateChange(ConnectionState.CONNECTING)
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur connexion", e)
            Result.failure(e)
        }
    }

    // Déconnexion
    fun disconnect(): Result<Unit> {
        Log.i(TAG, "🔌 Déconnexion...")
        stopPolling()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        onStateChange(ConnectionState.DISCONNECTED)
        return Result.success(Unit)
    }

    // Configuration des caractéristiques
    private fun setupCharacteristics() {
        val service = bluetoothGatt?.getService(SERVICE_UUID)

        writeCharacteristic = service?.getCharacteristic(TX_CHAR_UUID)
        readCharacteristic = service?.getCharacteristic(RX_CHAR_UUID)

        readCharacteristic?.let { char ->
            bluetoothGatt?.setCharacteristicNotification(char, true)

            val descriptor = char.getDescriptor(CCCD_UUID)
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            bluetoothGatt?.writeDescriptor(descriptor)
        }
    }

    // Initialisation
    private suspend fun startInitSequence() {
        Log.i(TAG, "🚀 Séquence d'initialisation...")

        sendCommand(CMD_INIT_1)
        delay(INIT_SEQUENCE_DELAY_MS)

        Log.i(TAG, "✅ Initialisation terminée")
        startPolling()
    }

    // Polling
    private fun startPolling() {
        if (isPolling) return

        isPolling = true
        Log.i(TAG, "🔄 Démarrage du polling")

        pollingRunnable = object : Runnable {
            override fun run() {
                if (isPolling) {
                    scope.launch {
                        sendCommand(CMD_KEEP_ALIVE)
                    }
                    handler.postDelayed(this, POLLING_INTERVAL_MS)
                }
            }
        }

        handler.post(pollingRunnable!!)
    }

    private fun stopPolling() {
        if (!isPolling) return
        isPolling = false
        pollingRunnable?.let { handler.removeCallbacks(it) }
        pollingRunnable = null
        Log.i(TAG, "⏸️ Polling arrêté")
    }

    // Envoi de commande
    suspend fun sendCommand(command: ByteArray): Result<Unit> {
        val hex = command.joinToString(" ") { "%02X".format(it) }
        Log.d(TAG, "📤 Envoi: $hex")

        if (writeCharacteristic == null || bluetoothGatt == null) {
            Log.e(TAG, "❌ Characteristic ou GATT null")
            return Result.failure(Exception("Not connected"))
        }

        return withContext(Dispatchers.IO) {
            try {
                writeCharacteristic?.value = command
                val success = bluetoothGatt?.writeCharacteristic(writeCharacteristic)

                if (success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Write failed"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception dans sendCommand", e)
                Result.failure(e)
            }
        }
    }

    // Commandes de contrôle simplifiées
    fun updateCurrentData(data: ScooterData) {
        currentScooterData = data
    }

    /**
     * ✅ CORRIGÉ - Commandes néon trouvées dans les logs
     */
    fun setNeon(enabled: Boolean) {
        Log.i(TAG, "🎨 Néon ${if (enabled) "ON" else "OFF"}")

        val command = if (enabled) {
            byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC5.toByte(), 0x35, 0x34, 0xD0.toByte(), 0xCA.toByte())
        } else {
            byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC5.toByte(), 0x34, 0x34, 0xD3.toByte(), 0xCA.toByte())
        }

        scope.launch {
            sendCommand(command)
        }
    }

    /**
     * ✅ Phares (déjà correct)
     */
    fun setLights(enabled: Boolean) {
        Log.i(TAG, "💡 Lumières ${if (enabled) "ON" else "OFF"}")

        val command = if (enabled) {
            byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC6.toByte(), 0x35, 0x34, 0xD1.toByte(), 0xCA.toByte())
        } else {
            byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC6.toByte(), 0x34, 0x34, 0xD2.toByte(), 0xCA.toByte())
        }

        scope.launch {
            sendCommand(command)
        }
    }

    /**
     * ✅ Verrouillage
     */
    fun setLock(locked: Boolean) {
        Log.i(TAG, "🔒 ${if (locked) "Verrouillage" else "Déverrouillage"}")

        val command = if (locked) {
            byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4B, 0x35, 0x34, 0x6C, 0xCB.toByte())
        } else {
            byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4B, 0x34, 0x34, 0x6D, 0xCB.toByte())
        }

        scope.launch {
            sendCommand(command)
        }
    }
    /**
     * ✅ Mode 1 roue / 2 roues
     */
    fun setWheelMode(mode: WheelMode) {
        Log.i(TAG, "🏍️ Mode roues: $mode")

        val command = when (mode) {
            WheelMode.ONE_WHEEL -> byteArrayOf(
                0x61, 0x9E.toByte(), 0x3C, 0x17, 0x35,
                0x8F.toByte(), 0x35, 0x35, 0x34, 0x34, 0x34, 0x34, 0x22, 0xCB.toByte()
            )
            WheelMode.TWO_WHEELS -> byteArrayOf(
                0x61, 0x9E.toByte(), 0x3C, 0x17, 0x35,
                0x8F.toByte(), 0x36, 0x35, 0x34, 0x34, 0x34, 0x34, 0x21, 0xCB.toByte()
            )
        }

        scope.launch {
            sendCommand(command)
        }
    }
    /**
     * Mode 1 roue / 2 roues
     */

    /**
     * Mode limiteur de vitesse (bridé/débridé)
     */
    fun setSpeedLimitMode(mode: SpeedLimitMode) {
        Log.i(TAG, "🚦 Mode: $mode")

        // TODO: Trouver les vraies commandes
        // Pour l'instant, on met juste à jour localement
        currentScooterData = currentScooterData.copy(speedLimitMode = mode)
        onDataReceived(currentScooterData)
    }

    // Nettoyage
    fun cleanup() {
        stopPolling()
        scope.cancel()
        handler.removeCallbacksAndMessages(null)
        bluetoothGatt?.close()
        bluetoothGatt = null
        Log.d(TAG, "🧹 Nettoyage terminé")
    }
}