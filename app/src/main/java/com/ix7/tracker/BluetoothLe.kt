package com.ix7.tracker

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.*

data class BluetoothDevice(
    val name: String?,
    val address: String
)

@SuppressLint("MissingPermission")
class BluetoothLe(private val context: Context) {

    companion object {
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
        private const val SCAN_PERIOD: Long = 10000
        private const val TAG = "BluetoothLe"
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        manager.adapter
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var scanning = false
    private val handler = Handler(Looper.getMainLooper())
    private val discoveredDevices = mutableMapOf<String, BluetoothDevice>()

    private var connectionCallback: ((Int) -> Unit)? = null
    private var notificationCallback: ((ByteArray) -> Unit)? = null

    // Vérifier les permissions
    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Scanner les dispositifs
    fun startScan(onDevicesFound: (List<BluetoothDevice>) -> Unit) {
        if (!hasPermissions()) {
            Log.e(TAG, "Permissions manquantes pour le scan Bluetooth")
            return
        }

        bluetoothScanner = bluetoothAdapter?.bluetoothLeScanner

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val deviceName = device.name
                val deviceAddress = device.address

                Log.d(TAG, "Dispositif trouvé: $deviceName - $deviceAddress")

                // Filtrer pour M0Robot ou afficher tous les appareils pour debug
                if (deviceName != null) {
                    // Afficher TOUS les appareils trouvés dans les logs
                    Log.d(TAG, "Appareil BLE détecté: '$deviceName' - $deviceAddress")

                    // Chercher M0Robot (insensible à la casse)
                    if (deviceName.contains("M0Robot", ignoreCase = true) ||
                        deviceName.contains("Robot", ignoreCase = true) ||
                        deviceName.contains("M0", ignoreCase = true)) {

                        if (!discoveredDevices.containsKey(deviceAddress)) {
                            discoveredDevices[deviceAddress] = BluetoothDevice(deviceName, deviceAddress)
                            Log.i(TAG, "TROTTINETTE TROUVÉE: $deviceName - $deviceAddress")
                            onDevicesFound(discoveredDevices.values.toList())
                        }
                    }
                } else {
                    // Même les appareils sans nom peuvent être votre trottinette
                    Log.d(TAG, "Appareil sans nom détecté: $deviceAddress")
                    // Optionnel : ajouter les appareils sans nom aussi
                    if (!discoveredDevices.containsKey(deviceAddress)) {
                        discoveredDevices[deviceAddress] = BluetoothDevice("Appareil inconnu", deviceAddress)
                        onDevicesFound(discoveredDevices.values.toList())
                    }
                }
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                Log.d(TAG, "Batch de ${results.size} résultats reçu")
                for (result in results) {
                    onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Erreur de scan: $errorCode")
                when(errorCode) {
                    SCAN_FAILED_ALREADY_STARTED -> Log.e(TAG, "Scan déjà démarré")
                    SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> Log.e(TAG, "Échec d'enregistrement de l'app")
                    SCAN_FAILED_INTERNAL_ERROR -> Log.e(TAG, "Erreur interne")
                    SCAN_FAILED_FEATURE_UNSUPPORTED -> Log.e(TAG, "Fonctionnalité non supportée")
                }
            }
        }

        // Configuration du scan pour être plus agressif
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Scan plus rapide
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        // Pas de filtre pour voir TOUS les appareils
        scanning = true
        discoveredDevices.clear()

        // Démarrer le scan sans filtre pour voir tous les appareils
        bluetoothScanner?.startScan(null, scanSettings, scanCallback)
        Log.i(TAG, "Scan démarré - recherche de M0Robot...")

        // Arrêter le scan après la période définie
        handler.postDelayed({
            stopScan()
            Log.i(TAG, "Scan terminé après $SCAN_PERIOD ms")
            if (discoveredDevices.isEmpty()) {
                Log.w(TAG, "Aucune trottinette M0Robot trouvée. Vérifiez que:")
                Log.w(TAG, "1. La trottinette est allumée")
                Log.w(TAG, "2. Le Bluetooth de la trottinette est activé")
                Log.w(TAG, "3. Vous êtes proche de la trottinette")
            }
        }, SCAN_PERIOD)
    }

    // Arrêter le scan
    fun stopScan() {
        if (!hasPermissions()) return

        scanning = false
        scanCallback?.let {
            bluetoothScanner?.stopScan(it)
            Log.d(TAG, "Scan arrêté")
        }
        scanCallback = null
    }

    // Se connecter à un dispositif
    fun connect(
        mac: String,
        onConnectionStateChange: (Int) -> Unit
    ) {
        if (!hasPermissions()) {
            Log.e(TAG, "Permissions manquantes pour la connexion")
            return
        }

        connectionCallback = onConnectionStateChange

        val device = bluetoothAdapter?.getRemoteDevice(mac)

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.i(TAG, "Connecté au serveur GATT")
                        connectionCallback?.invoke(STATE_CONNECTED)
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.i(TAG, "Déconnecté du serveur GATT")
                        connectionCallback?.invoke(STATE_DISCONNECTED)
                        bluetoothGatt?.close()
                        bluetoothGatt = null
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Services découverts")
                    // Lister tous les services pour debug
                    gatt.services.forEach { service ->
                        Log.d(TAG, "Service: ${service.uuid}")
                        service.characteristics.forEach { char ->
                            Log.d(TAG, "  Characteristic: ${char.uuid}")
                        }
                    }
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                val data = characteristic.value
                Log.d(TAG, "Données reçues: ${data.contentToString()}")
                notificationCallback?.invoke(data)
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val data = characteristic.value
                    Log.d(TAG, "Caractéristique lue: ${data.contentToString()}")
                    notificationCallback?.invoke(data)
                }
            }
        }

        bluetoothGatt = device?.connectGatt(context, false, gattCallback)
        Log.d(TAG, "Tentative de connexion à $mac")
    }

    // Activer les notifications
    fun startNotifications(
        mac: String,
        serviceUUID: String,
        characteristicUUID: String,
        onNotification: (ByteArray) -> Unit
    ) {
        if (!hasPermissions()) {
            Log.e(TAG, "Permissions manquantes")
            return
        }

        notificationCallback = onNotification

        val service = bluetoothGatt?.getService(UUID.fromString(serviceUUID))
        if (service == null) {
            Log.e(TAG, "Service non trouvé: $serviceUUID")
            // Essayer avec des UUID alternatifs communs pour les trottinettes
            val alternativeUUIDs = listOf(
                "0000fee0-0000-1000-8000-00805f9b34fb",
                "0000ffe0-0000-1000-8000-00805f9b34fb",
                "6e400001-b5a3-f393-e0a9-e50e24dcca9e" // Nordic UART Service
            )

            for (altUUID in alternativeUUIDs) {
                val altService = bluetoothGatt?.getService(UUID.fromString(altUUID))
                if (altService != null) {
                    Log.d(TAG, "Service alternatif trouvé: $altUUID")
                    startNotificationsWithService(altService, characteristicUUID, onNotification)
                    return
                }
            }
            Log.e(TAG, "Aucun service compatible trouvé")
            return
        }

        startNotificationsWithService(service, characteristicUUID, onNotification)
    }

    private fun startNotificationsWithService(
        service: BluetoothGattService,
        characteristicUUID: String,
        onNotification: (ByteArray) -> Unit
    ) {
        val characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID))
        if (characteristic == null) {
            Log.e(TAG, "Caractéristique non trouvée: $characteristicUUID")
            // Essayer de lire la première caractéristique disponible
            service.characteristics.firstOrNull()?.let { firstChar ->
                Log.d(TAG, "Utilisation de la première caractéristique: ${firstChar.uuid}")
                enableNotificationForCharacteristic(firstChar)
            }
            return
        }

        enableNotificationForCharacteristic(characteristic)
    }

    private fun enableNotificationForCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.setCharacteristicNotification(characteristic, true)

        // Configurer le descripteur pour les notifications
        val descriptor = characteristic.getDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        )

        descriptor?.let {
            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            bluetoothGatt?.writeDescriptor(it)
            Log.d(TAG, "Notifications activées pour ${characteristic.uuid}")
        }

        // Essayer aussi de lire la caractéristique directement
        bluetoothGatt?.readCharacteristic(characteristic)
    }

    // Déconnexion
    fun disconnect() {
        if (!hasPermissions()) return

        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        Log.d(TAG, "Déconnexion")
    }

    // Envoyer une commande (si nécessaire pour votre trottinette)
    fun writeCommand(
        serviceUUID: String,
        characteristicUUID: String,
        command: ByteArray
    ) {
        if (!hasPermissions()) return

        val service = bluetoothGatt?.getService(UUID.fromString(serviceUUID))
        val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUUID))

        characteristic?.let {
            it.value = command
            bluetoothGatt?.writeCharacteristic(it)
            Log.d(TAG, "Commande envoyée: ${command.contentToString()}")
        }
    }
}