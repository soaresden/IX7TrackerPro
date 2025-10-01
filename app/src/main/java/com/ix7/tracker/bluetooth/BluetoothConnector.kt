package com.ix7.tracker.bluetooth

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.ix7.tracker.core.*
import kotlinx.coroutines.*
import java.util.*

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
        private val SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        private val CHAR_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
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
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value
            if (data != null && data.isNotEmpty()) {
                ProtocolUtils.logFrame("← REÇU", data)
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
    private fun enableNotifications(gatt: BluetoothGatt): Boolean {
        val service = gatt.getService(SERVICE_UUID)
        if (service == null) {
            Log.e(TAG, "Service FFE0 non trouvé")
            return false
        }

        val characteristic = service.getCharacteristic(CHAR_UUID)
        if (characteristic == null) {
            Log.e(TAG, "Characteristic FFE1 non trouvée")
            return false
        }

        // Sauvegarder pour les write
        writeCharacteristic = characteristic

        // Activer les notifications localement
        val notifySuccess = gatt.setCharacteristicNotification(characteristic, true)
        if (!notifySuccess) {
            Log.e(TAG, "Échec setCharacteristicNotification")
            return false
        }

        // Écrire dans le descriptor CCCD
        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor == null) {
            Log.e(TAG, "Descriptor CCCD non trouvé")
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
            Log.i(TAG, "📤 Envoi commandes initiales...")

            // 1. Demande de statut
            delay(100)
            sendCommand(ProtocolUtils.buildStatusRequest())

            // 2. Demande de données
            delay(300)
            sendCommand(ProtocolUtils.buildDataRequest())

            // 3. Démarrer le polling
            delay(500)
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

            while (isActive && isNotificationsEnabled) {
                sendCommand(ProtocolUtils.buildDataRequest())
                delay(300) // 3 fois par seconde
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
}