package com.ix7.tracker.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ix7.tracker.core.ConnectionState
import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.ScooterData
import com.ix7.tracker.protocol.CommandBuilder
import com.ix7.tracker.protocol.ProtocolConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Connecteur Bluetooth pour le protocole 61 9E (iX7 Pro)
 */
class BluetoothConnector(
    private val context: Context,
    private val onDataReceived: (ScooterData) -> Unit,
    private val onStateChange: (ConnectionState) -> Unit
) {
    companion object {
        private const val TAG = "BT_CONNECTOR"

        // UUIDs Nordic UART Service
        private val SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        private val TX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e") // Write
        private val RX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e") // Notify
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // Intervalles de polling
        private const val POLLING_INTERVAL_MS = 1500L // 1.5 secondes
        private const val INIT_SEQUENCE_DELAY_MS = 300L
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var readCharacteristic: BluetoothGattCharacteristic? = null

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO)

    private val dataHandler = BluetoothDataHandler()
    private var currentScooterData = ScooterData()

    private var isPolling = false
    private var pollingRunnable: Runnable? = null

    // ========== GATT CALLBACK ==========

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "✅ Connecté au GATT server")
                    onStateChange(ConnectionState.CONNECTED)

                    // Découvrir les services
                    handler.postDelayed({
                        gatt.discoverServices()
                    }, 600)
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "❌ Déconnecté du GATT server")
                    onStateChange(ConnectionState.DISCONNECTED)
                    stopPolling()
                    cleanup()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "📋 Services découverts")

                val service = gatt.getService(SERVICE_UUID)
                if (service != null) {
                    writeCharacteristic = service.getCharacteristic(TX_CHAR_UUID)
                    readCharacteristic = service.getCharacteristic(RX_CHAR_UUID)

                    if (writeCharacteristic != null && readCharacteristic != null) {
                        Log.i(TAG, "✅ Caractéristiques trouvées")

                        // Activer les notifications
                        enableNotifications()
                    } else {
                        Log.e(TAG, "❌ Caractéristiques manquantes")
                        onStateChange(ConnectionState.ERROR)
                    }
                } else {
                    Log.e(TAG, "❌ Service NUS non trouvé")
                    onStateChange(ConnectionState.ERROR)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value
            if (data != null && data.isNotEmpty()) {
                // Traiter les données reçues
                val parsedData = dataHandler.handleData(data)
                if (parsedData != null) {
                    // Merger avec les données actuelles
                    currentScooterData = currentScooterData.copy(
                        speed = if (parsedData.speed > 0) parsedData.speed else currentScooterData.speed,
                        battery = if (parsedData.battery > 0) parsedData.battery else currentScooterData.battery,
                        currentMode = parsedData.currentMode ?: currentScooterData.currentMode,
                        odometer = if (parsedData.odometer > 0) parsedData.odometer else currentScooterData.odometer,
                        temperature = if (parsedData.temperature > 0) parsedData.temperature else currentScooterData.temperature,
                        isLocked = parsedData.isLocked,
                        headlightsOn = parsedData.headlightsOn,
                        neonOn = parsedData.neonOn,
                        isUnlocked = parsedData.isUnlocked,
                        cruiseControl = parsedData.cruiseControl
                    )

                    // Notifier
                    onDataReceived(currentScooterData)
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "✅ Notifications activées")

                // Lancer la séquence d'initialisation
                scope.launch {
                    startInitSequence()
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "✅ Écriture réussie")
            } else {
                Log.e(TAG, "❌ Erreur d'écriture: $status")
            }
        }
    }

    // ========== CONNEXION ==========

    suspend fun connect(address: String): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                Log.i(TAG, "🔌 Connexion à $address...")
                onStateChange(ConnectionState.CONNECTING)

                val device = context.getSystemService(Context.BLUETOOTH_SERVICE)
                    .let { it as android.bluetooth.BluetoothManager }
                    .adapter
                    .getRemoteDevice(address)

                bluetoothGatt = device.connectGatt(context, false, gattCallback)

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur connexion", e)
                onStateChange(ConnectionState.ERROR)
                Result.failure(e)
            }
        }
    }

    suspend fun connect(device: BluetoothDevice): Result<Unit> {
        return connect(device.address)
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

    // ========== NOTIFICATIONS ==========

    private fun enableNotifications() {
        readCharacteristic?.let { char ->
            bluetoothGatt?.setCharacteristicNotification(char, true)

            val descriptor = char.getDescriptor(CCCD_UUID)
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            bluetoothGatt?.writeDescriptor(descriptor)
        }
    }

    // ========== INITIALISATION ==========

    private suspend fun startInitSequence() {
        Log.i(TAG, "🚀 Séquence d'initialisation...")

        // Envoyer toutes les commandes d'init
        val initCommands = CommandBuilder.getInitSequence()

        for (command in initCommands) {
            sendCommand(command)
            delay(INIT_SEQUENCE_DELAY_MS)
        }

        Log.i(TAG, "✅ Initialisation terminée")

        // Démarrer le polling
        startPolling()
    }

    // ========== POLLING ==========

    private fun startPolling() {
        if (isPolling) return

        isPolling = true
        Log.i(TAG, "🔄 Démarrage du polling (${POLLING_INTERVAL_MS}ms)")

        pollingRunnable = object : Runnable {
            override fun run() {
                if (isPolling) {
                    scope.launch {
                        val keepAlive = CommandBuilder.buildKeepAliveCommand()
                        sendCommand(keepAlive)
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

    // ========== ENVOI DE COMMANDES ==========

    suspend fun sendCommand(command: ByteArray): Result<Unit> {
        val hex = command.joinToString(" ") { "%02X".format(it) }
        Log.d(TAG, "📤 Envoi: $hex")

        if (writeCharacteristic == null) {
            Log.e(TAG, "❌ writeCharacteristic est NULL")
            return Result.failure(Exception("writeCharacteristic null"))
        }

        if (bluetoothGatt == null) {
            Log.e(TAG, "❌ bluetoothGatt est NULL")
            return Result.failure(Exception("bluetoothGatt null"))
        }

        return withContext(Dispatchers.IO) {
            try {
                writeCharacteristic?.value = command
                val success = bluetoothGatt?.writeCharacteristic(writeCharacteristic)

                if (success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("writeCharacteristic a échoué"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception dans sendCommand", e)
                Result.failure(e)
            }
        }
    }

    // ========== COMMANDES DE CONTRÔLE ==========

    /**
     * Met à jour les données actuelles (appelée automatiquement lors de la réception)
     */
    fun updateCurrentData(data: ScooterData) {
        currentScooterData = data
    }

    /**
     * Active/désactive le néon
     */
    fun setNeon(enabled: Boolean) {
        Log.i(TAG, "🎨 Commande: Néon ${if (enabled) "ON" else "OFF"}")

        val command = CommandBuilder.buildToggleNeonCommand(
            neonOn = enabled,
            currentData = currentScooterData
        )

        scope.launch {
            sendCommand(command)
        }
    }

    /**
     * Active/désactive les lumières
     */
    fun setLights(enabled: Boolean) {
        Log.i(TAG, "💡 Commande: Lumières ${if (enabled) "ON" else "OFF"}")

        val command = CommandBuilder.buildToggleLightsCommand(
            lightsOn = enabled,
            currentData = currentScooterData
        )

        scope.launch {
            sendCommand(command)
        }
    }

    /**
     * Active/désactive le débridage
     */
    fun setUnlocked(unlocked: Boolean) {
        Log.i(TAG, "🔓 Commande: ${if (unlocked) "DÉBRIDAGE" else "BRIDAGE"}")

        val command = CommandBuilder.buildToggleUnlockCommand(
            unlocked = unlocked,
            currentData = currentScooterData
        )

        scope.launch {
            sendCommand(command)
        }
    }

    /**
     * Change le mode de conduite
     */
    fun setRideMode(mode: RideMode) {
        Log.i(TAG, "🏍️ Commande: Mode → $mode")

        // Mettre à jour localement d'abord
        currentScooterData = currentScooterData.copy(currentMode = mode)

        val command = CommandBuilder.buildChangeModeCommand(
            mode = mode,
            currentData = currentScooterData
        )

        scope.launch {
            sendCommand(command)
        }
    }

    // ========== NETTOYAGE ==========

    fun cleanup() {
        stopPolling()
        scope.cancel()
        handler.removeCallbacksAndMessages(null)
        bluetoothGatt?.close()
        bluetoothGatt = null
        dataHandler.reset()
        Log.d(TAG, "🧹 Nettoyage terminé")
    }
}