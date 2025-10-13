package com.ix7.tracker.bluetooth

import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.*
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

data class LockState(
    val isDetected: Boolean = false,
    val isConnected: Boolean = false,
    val isLocked: Boolean = true,
    val batteryLevel: Int = 0,
    val name: String = "",
    val address: String = ""
)

class LockManager(private val context: Context) {

    companion object {
        private const val TAG = "🔐LockManager"
        private const val LOCK_NAME = "iPhone9"

        // ✅ VRAIS UUIDs de la serrure
        private const val LOCK_SERVICE_UUID = "0000fee7-0000-1000-8000-00805f9b34fb"
        private const val LOCK_WRITE_UUID = "000036f5-0000-1000-8000-00805f9b34fb"
        private const val LOCK_NOTIFY_UUID = "000036f6-0000-1000-8000-00805f9b34fb"

        // Reste inchangé
        private val AES_KEY = byteArrayOf(32, 87, 47, 82, 54, 75, 63, 71, 48, 80, 65, 88, 17, 99, 45, 43)
        private val CMD_GET_TOKEN = byteArrayOf(6, 1, 6)
        private val CMD_UNLOCK = byteArrayOf(5, 1, 1, 1)
        private val CMD_LOCK = byteArrayOf(5, 7, 1, 1)
    }



    private val _lockState = MutableStateFlow(LockState())
    val lockState: StateFlow<LockState> = _lockState

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var lockPassword: String = "896647"
    private var authToken: ByteArray? = null
    private var scanCallback: ScanCallback? = null
    private var detectedLockAddress: String? = null

    // 🔄 Auto-reconnexion
    private var reconnectionJob: Job? = null
    private var wasConnected = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 📊 Statistiques de scan
    private val scannedDevices = mutableSetOf<String>()
    private var scanCount = 0

    // 🔐 Chiffrement AES
    private fun encryptAES(data: ByteArray): ByteArray {
        return try {
            val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            val keySpec = SecretKeySpec(AES_KEY, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val paddedData = data.copyOf(16)
            cipher.doFinal(paddedData)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur chiffrement: ${e.message}")
            data
        }
    }

    // 🔓 Déchiffrement AES
    private fun decryptAES(data: ByteArray): ByteArray {
        return try {
            val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            val keySpec = SecretKeySpec(AES_KEY, "AES")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            cipher.doFinal(data)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur déchiffrement: ${e.message}")
            data
        }
    }

    // 📡 Parse advertising data pour extraire état et batterie
    private fun parseAdvertisingData(scanRecord: ByteArray?): Pair<Boolean?, Int?> {
        if (scanRecord == null) return Pair(null, null)

        var isLocked: Boolean? = null
        var batteryLevel: Int? = null

        try {
            var index = 0
            while (index < scanRecord.size - 1) {
                val length = scanRecord[index].toInt() and 0xFF
                if (length == 0 || index + length >= scanRecord.size) break

                val type = scanRecord[index + 1].toInt() and 0xFF

                // 0xFF = Manufacturer Specific Data
                if (type == 0xFF && length >= 4) {
                    val manufacturerData = scanRecord.copyOfRange(index + 2, index + 1 + length)

                    Log.d(TAG, "   📦 Manufacturer Data: ${manufacturerData.joinToString(" ") { "%02X".format(it) }}")

                    if (manufacturerData.size >= 10) {
                        // Format simple : byte 9 contient l'état
                        val stateByte = manufacturerData[8].toInt() and 0xFF
                        isLocked = stateByte == 0x01

                        // Batterie : peut-être dans le byte 9 ?
                        if (manufacturerData.size >= 11) {
                            batteryLevel = manufacturerData[9].toInt() and 0xFF

                            // Si > 100, c'est pas la batterie
                            if (batteryLevel!! > 100) {
                                batteryLevel = null
                            }
                        }

                        Log.d(TAG, "   🔍 State byte: 0x${"%02X".format(stateByte)}")
                        Log.d(TAG, "   🔍 Battery byte: ${if (batteryLevel != null) "$batteryLevel%" else "N/A"}")
                    }
                }

                index += 1 + length
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur parsing advertising: ${e.message}")
        }

        return Pair(isLocked, batteryLevel)
    }

    // 📡 Scanner pour détecter la serrure - SCAN CONTINU
    fun startScanning(bluetoothAdapter: BluetoothAdapter) {
        val scanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            Log.e(TAG, "❌ Scanner BLE non disponible")
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name
                val address = device.address

                // 📊 COMPTEUR DE SCANS
                scanCount++
                if (scanCount % 50 == 0) {
                    Log.d(TAG, "📊 Scan #$scanCount - ${scannedDevices.size} appareils uniques détectés")
                }

                // 🔍 LOGGER TOUS LES APPAREILS (avec leur nom)
                if (name != null && !scannedDevices.contains(address)) {
                    scannedDevices.add(address)
                    Log.d(TAG, "🔍 Appareil détecté: \"$name\" - $address - RSSI: ${result.rssi} dBm")
                } else if (name == null && !scannedDevices.contains(address)) {
                    scannedDevices.add(address)
                    Log.d(TAG, "🔍 Appareil sans nom: $address - RSSI: ${result.rssi} dBm")
                }

                // 🎯 Filtrer uniquement SmartLockMi3
                if (name?.trim()?.startsWith(LOCK_NAME) == true) {
                    Log.d(TAG, "")
                    Log.d(TAG, "═══════════════════════════════════════")
                    Log.d(TAG, "✅ $LOCK_NAME DÉTECTÉ!")
                    Log.d(TAG, "═══════════════════════════════════════")
                    Log.d(TAG, "📍 Adresse: $address")
                    Log.d(TAG, "📶 RSSI: ${result.rssi} dBm")

                    // Parse advertising data
                    val scanRecord = result.scanRecord?.bytes
                    val (isLocked, battery) = parseAdvertisingData(scanRecord)

                    // Mettre à jour l'état
                    detectedLockAddress = device.address
                    val currentState = _lockState.value
                    _lockState.value = currentState.copy(
                        isDetected = true,
                        name = name,
                        address = device.address,
                        isLocked = isLocked ?: currentState.isLocked,
                        batteryLevel = battery ?: currentState.batteryLevel
                    )

                    Log.d(TAG, "📊 État: ${if (_lockState.value.isLocked) "🔒 LOCKED" else "🔓 UNLOCKED"}")
                    Log.d(TAG, "🔋 Batterie: ${_lockState.value.batteryLevel}%")
                    Log.d(TAG, "═══════════════════════════════════════")
                    Log.d(TAG, "")
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "❌ Scan échoué: code $errorCode")
            }
        }

        try {
            scanner.startScan(null, settings, scanCallback)
            Log.d(TAG, "")
            Log.d(TAG, "╔═══════════════════════════════════════╗")
            Log.d(TAG, "║   🔍 SCAN CONTINU DÉMARRÉ            ║")
            Log.d(TAG, "║   Recherche: $LOCK_NAME              ║")
            Log.d(TAG, "║   Mode: LOW_LATENCY                  ║")
            Log.d(TAG, "╚═══════════════════════════════════════╝")
            Log.d(TAG, "")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Permission Bluetooth manquante: ${e.message}")
        }
    }

    // 🛑 Arrêter le scan
    fun stopScanning(bluetoothAdapter: BluetoothAdapter) {
        scanCallback?.let {
            try {
                bluetoothAdapter.bluetoothLeScanner?.stopScan(it)
                Log.d(TAG, "🛑 Scan arrêté")
                Log.d(TAG, "📊 Total: $scanCount scans, ${scannedDevices.size} appareils uniques")
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ Erreur arrêt scan: ${e.message}")
            }
        }
        scanCallback = null
        scannedDevices.clear()
        scanCount = 0
    }

    // 🔌 Connexion à la serrure

    // 🔌 Connexion à la serrure
    fun connect(bluetoothAdapter: BluetoothAdapter) {
        val address = detectedLockAddress ?: run {
            Log.e(TAG, "❌ Aucune serrure détectée")
            return
        }

        try {
            // ⚠️ IMPORTANT : Fermer toute connexion existante d'abord
            bluetoothGatt?.let {
                Log.d(TAG, "🧹 Nettoyage de l'ancienne connexion...")
                try {
                    it.disconnect()
                    it.close()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erreur nettoyage: ${e.message}")
                }
                bluetoothGatt = null
                writeCharacteristic = null
                notifyCharacteristic = null
                authToken = null
            }

            // ⏰ Attendre 500ms avant de reconnecter
            Thread.sleep(500)

            val device = bluetoothAdapter.getRemoteDevice(address)
            Log.d(TAG, "")
            Log.d(TAG, "╔═══════════════════════════════════════╗")
            Log.d(TAG, "║   🔌 CONNEXION EN COURS...           ║")
            Log.d(TAG, "╚═══════════════════════════════════════╝")
            Log.d(TAG, "📍 Adresse: $address")
            Log.d(TAG, "🎯 Nom: ${lockState.value.name}")
            Log.d(TAG, "")

            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Permission Bluetooth manquante: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur connexion: ${e.message}")
        }
    }

    // 🔄 AUTO-RECONNEXION
    private fun startReconnection(bluetoothAdapter: BluetoothAdapter) {
        // Annuler toute reconnexion en cours
        reconnectionJob?.cancel()

        Log.d(TAG, "")
        Log.d(TAG, "╔═══════════════════════════════════════╗")
        Log.d(TAG, "║   🔄 AUTO-RECONNEXION DÉMARRÉE       ║")
        Log.d(TAG, "║   Tentatives: toutes les 2s          ║")
        Log.d(TAG, "║   Durée max: 15 secondes             ║")
        Log.d(TAG, "╚═══════════════════════════════════════╝")
        Log.d(TAG, "")

        reconnectionJob = coroutineScope.launch {
            val startTime = System.currentTimeMillis()
            val maxDuration = 15_000L // 15 secondes
            var attemptCount = 0

            while (System.currentTimeMillis() - startTime < maxDuration) {
                attemptCount++

                if (_lockState.value.isConnected) {
                    Log.d(TAG, "✅ Reconnexion réussie après $attemptCount tentative(s)")
                    return@launch
                }

                Log.d(TAG, "🔄 Tentative de reconnexion #$attemptCount...")

                try {
                    connect(bluetoothAdapter)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erreur tentative #$attemptCount: ${e.message}")
                }

                // Attendre 2 secondes avant la prochaine tentative
                delay(2000)
            }

            Log.e(TAG, "")
            Log.e(TAG, "╔═══════════════════════════════════════╗")
            Log.e(TAG, "║   ❌ RECONNEXION ÉCHOUÉE              ║")
            Log.e(TAG, "║   Tentatives: $attemptCount                    ║")
            Log.e(TAG, "║   Durée: 15 secondes                 ║")
            Log.e(TAG, "╚═══════════════════════════════════════╝")
            Log.e(TAG, "")
        }
    }

    // 🔌 Callback GATT
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "")
                    Log.d(TAG, "╔═══════════════════════════════════════╗")
                    Log.d(TAG, "║   ✅ CONNECTÉ !                      ║")
                    Log.d(TAG, "╚═══════════════════════════════════════╝")
                    Log.d(TAG, "")

                    _lockState.value = _lockState.value.copy(isConnected = true)
                    wasConnected = true

                    // Annuler toute reconnexion en cours
                    reconnectionJob?.cancel()

                    try {
                        gatt.discoverServices()
                    } catch (e: SecurityException) {
                        Log.e(TAG, "❌ Permission manquante: ${e.message}")
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "")
                    Log.d(TAG, "╔═══════════════════════════════════════╗")
                    Log.d(TAG, "║   ⚠️  DÉCONNECTÉ !                   ║")
                    Log.d(TAG, "╚═══════════════════════════════════════╝")
                    Log.d(TAG, "Status: $status")
                    Log.d(TAG, "")

                    _lockState.value = _lockState.value.copy(isConnected = false)

                    // 🔄 Si on était connecté, lancer auto-reconnexion
                    if (wasConnected && detectedLockAddress != null) {
                        Log.d(TAG, "🔄 Déconnexion inattendue → Auto-reconnexion...")
                        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                        startReconnection(bluetoothManager.adapter)
                    } else {
                        wasConnected = false
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "🔍 Services découverts")


                Log.d(TAG, "")
                Log.d(TAG, "╚═══════════════════════════════════════╝")
                Log.d(TAG, "")

                // 🔍 DEBUG: Chercher le service
                Log.d(TAG, "🔍 Recherche du service: $LOCK_SERVICE_UUID")
                val service = gatt.getService(UUID.fromString(LOCK_SERVICE_UUID))

                if (service != null) {
                    Log.d(TAG, "✅ Service trouvé!")

                    writeCharacteristic = service.getCharacteristic(UUID.fromString(LOCK_WRITE_UUID))
                    notifyCharacteristic = service.getCharacteristic(UUID.fromString(LOCK_NOTIFY_UUID))

                    Log.d(TAG, "✅ Write char: ${writeCharacteristic != null}")
                    Log.d(TAG, "✅ Notify char: ${notifyCharacteristic != null}")

                    // Activer les notifications
                    notifyCharacteristic?.let { char ->
                        Log.d(TAG, "📝 Tentative d'activation des notifications...")
                        try {
                            gatt.setCharacteristicNotification(char, true)
                            val descriptor = char.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))

                            if (descriptor != null) {
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                val result = gatt.writeDescriptor(descriptor)
                                Log.d(TAG, "🔔 WriteDescriptor result: $result")
                            } else {
                                Log.e(TAG, "❌ Descriptor non trouvé")
                            }
                        } catch (e: SecurityException) {
                            Log.e(TAG, "❌ Permission manquante: ${e.message}")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Erreur: ${e.message}")
                        }
                    }

                    // ⏰ Attendre 500ms avant de demander le token
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        requestToken()
                    }, 500)

                } else {
                    Log.e(TAG, "❌ Service $LOCK_SERVICE_UUID non trouvé")
                    Log.e(TAG, "💡 Vérifie les UUIDs ci-dessus !")
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value
            if (data != null && data.size == 16) {
                val decrypted = decryptAES(data)
                Log.d(TAG, "📨 Réponse reçue: ${decrypted.joinToString(" ") { "%02X".format(it) }}")

                when (decrypted[0].toInt()) {
                    6 -> { // Token response
                        if (decrypted[1].toInt() == 2) {
                            authToken = decrypted.copyOfRange(3, 7)
                            Log.d(TAG, "🔑 Token reçu: ${authToken?.joinToString(" ") { "%02X".format(it) }}")
                        }
                    }
                    5 -> { // Lock/Unlock response
                        val success = decrypted[3].toInt() == 0
                        val action = when (decrypted[1].toInt()) {
                            2 -> "UNLOCK"
                            8 -> "LOCK"
                            else -> "UNKNOWN"
                        }

                        if (success) {
                            // ✅ METTRE À JOUR L'ÉTAT IMMÉDIATEMENT
                            val isLocked = action == "LOCK"
                            _lockState.value = _lockState.value.copy(isLocked = isLocked)

                            Log.d(TAG, "✅ $action réussi")
                            Log.d(TAG, "📊 Nouvel état: ${if (isLocked) "🔒 LOCKED" else "🔓 UNLOCKED"}")
                        } else {
                            Log.e(TAG, "❌ $action échoué")
                        }
                    }
                }
            }
        }
    }

    // 🔑 Demander le token d'authentification
    private fun requestToken() {
        val data = CMD_GET_TOKEN + lockPassword.toByteArray()
        val encrypted = encryptAES(data)

        writeCharacteristic?.let { char ->
            char.value = encrypted
            try {
                bluetoothGatt?.writeCharacteristic(char)
                Log.d(TAG, "🔑 Demande de token envoyée")
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ Permission manquante: ${e.message}")
            }
        }
    }

    // 🔓 Déverrouiller
    fun unlock() {
        val token = authToken ?: run {
            Log.e(TAG, "❌ Pas de token - connectez-vous d'abord")
            return
        }

        val data = CMD_UNLOCK + token
        val encrypted = encryptAES(data)

        writeCharacteristic?.let { char ->
            char.value = encrypted
            try {
                bluetoothGatt?.writeCharacteristic(char)
                Log.d(TAG, "🔓 Commande UNLOCK envoyée")
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ Permission manquante: ${e.message}")
            }
        }
    }

    // 🔒 Verrouiller
    fun lock() {
        val token = authToken ?: run {
            Log.e(TAG, "❌ Pas de token - connectez-vous d'abord")
            return
        }

        val data = CMD_LOCK + token
        val encrypted = encryptAES(data)

        writeCharacteristic?.let { char ->
            char.value = encrypted
            try {
                bluetoothGatt?.writeCharacteristic(char)
                Log.d(TAG, "🔒 Commande LOCK envoyée")
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ Permission manquante: ${e.message}")
            }
        }
    }

    // 🔌 Déconnexion
    // 🔌 Déconnexion
    fun disconnect() {
        // Marquer comme déconnexion volontaire
        wasConnected = false

        // Annuler toute reconnexion en cours
        reconnectionJob?.cancel()

        Log.d(TAG, "🧹 Déconnexion et nettoyage...")

        try {
            bluetoothGatt?.let {
                it.disconnect()
                // ⏰ Attendre un peu avant de fermer
                Thread.sleep(200)
                it.close()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Permission manquante: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur déconnexion: ${e.message}")
        }

        bluetoothGatt = null
        writeCharacteristic = null
        notifyCharacteristic = null
        authToken = null

        _lockState.value = _lockState.value.copy(isConnected = false)
        Log.d(TAG, "✅ Déconnexion complète")
    }

    // 🔄 Changer le mot de passe
    fun setPassword(newPassword: String) {
        lockPassword = newPassword
        Log.d(TAG, "🔑 Mot de passe mis à jour")
    }

    // 🧹 Cleanup
    fun cleanup() {
        reconnectionJob?.cancel()
        coroutineScope.cancel()
    }
}