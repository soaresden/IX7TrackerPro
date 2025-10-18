package com.ix7.tracker.wear.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

/**
 * Scanner BLE + Bluetooth Classique EXHAUSTIF pour Wear OS
 * Gère les connexions existantes et déconnecte si nécessaire
 */
class BluetoothScanner(
    private val context: Context,
    private val onScootersFound: (List<ScooterInfo>) -> Unit
) {
    companion object {
        private const val TAG = "BT_SCANNER"
        private val SCOOTER_PREFIXES = listOf(
            "M0Robot", "MiniRobot", "IX7", "MQRobot", "M6", "A6", "MAX", "NEXRIDE", "H1",
            "Ninebot", "Xiaomi", "Segway", "Pro", "Robot", "Scooter", "BLE"
        )
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var isScanning = false
    private val discoveredScooters = mutableListOf<ScooterInfo>()
    private val allScannedDevices = mutableSetOf<String>()

    // BLE Callback
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            Log.d(TAG, "onScanResult called! result=$result")
            result?.let {
                try {
                    handleScanResult(it)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in handleScanResult: ${e.message}", e)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "❌ BLE Scan failed: $errorCode")
        }
    }

    // Bluetooth Classique Broadcast Receiver
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "discoveryReceiver onReceive: action=${intent?.action}")
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val rssi: Short = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                    Log.d(TAG, "ACTION_FOUND: device=$device rssi=$rssi")
                    device?.let {
                        try {
                            handleClassicBluetoothDevice(it, rssi.toInt())
                        } catch (e: Exception) {
                            Log.e(TAG, "Exception in handleClassicBluetoothDevice: ${e.message}", e)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "🛑 Bluetooth classique discovery finished")
                }
            }
        }
    }

    private fun handleScanResult(result: ScanResult) {
        try {
            val device = result.device
            val deviceName = device.name ?: "Unknown_BLE"
            val address = device.address ?: "NO_ADDRESS"
            val rssi = result.rssi

            Log.d(TAG, "📡 [BLE] Device: \"$deviceName\" | $address | RSSI: $rssi dBm")

            if (!allScannedDevices.contains(address)) {
                allScannedDevices.add(address)
            }

            addScooter(deviceName, address, rssi)
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleScanResult: ${e.message}", e)
        }
    }

    private fun handleClassicBluetoothDevice(device: BluetoothDevice, rssi: Int) {
        try {
            val deviceName = device.name ?: "Unknown_CLASSIC"
            val address = device.address ?: "NO_ADDRESS"

            Log.d(TAG, "📡 [CLASSIQUE] Device: \"$deviceName\" | $address | RSSI: $rssi dBm")

            if (!allScannedDevices.contains(address)) {
                allScannedDevices.add(address)
            }

            addScooter(deviceName, address, rssi)
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleClassicBluetoothDevice: ${e.message}", e)
        }
    }

    private fun addScooter(deviceName: String, address: String, rssi: Int) {
        try {
            val scooter = ScooterInfo(
                name = deviceName,
                address = address,
                rssi = rssi,
                distance = estimateDistance(rssi)
            )

            val idx = discoveredScooters.indexOfFirst { it.address == address }
            if (idx >= 0) {
                discoveredScooters[idx] = scooter
            } else {
                discoveredScooters.add(scooter)
            }

            val sorted = discoveredScooters.sortedByDescending { it.rssi }.take(10)
            Log.d(TAG, "📊 Updated list: ${sorted.size} device(s)")
            onScootersFound(sorted)
        } catch (e: Exception) {
            Log.e(TAG, "Error in addScooter: ${e.message}", e)
        }
    }

    private fun logPairedDevices() {
        Log.d(TAG, "📋 === APPAREILS APPAIRÉS ===")
        try {
            val pairedDevices = bluetoothAdapter?.bondedDevices
            if (pairedDevices.isNullOrEmpty()) {
                Log.d(TAG, "❌ Aucun appareil appairé")
                return
            }
            pairedDevices.forEach { device ->
                Log.d(TAG, "  🔗 ${device.name} | ${device.address} | Type: ${device.type}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing paired devices: ${e.message}", e)
        }
    }

    private fun logConnectedDevices() {
        Log.d(TAG, "📋 === APPAREILS CONNECTÉS ===")
        try {
            val pairedDevices = bluetoothAdapter?.bondedDevices
            if (pairedDevices.isNullOrEmpty()) {
                Log.d(TAG, "❌ Aucun appareil connecté")
                return
            }
            pairedDevices.forEach { device ->
                try {
                    val isConnected = device.javaClass.getMethod("isConnected").invoke(device) as? Boolean ?: false
                    if (isConnected) {
                        Log.d(TAG, "  ✓ CONNECTÉ: ${device.name} | ${device.address}")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "  ?: ${device.name} | ${device.address} (impossible de vérifier)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking connected devices: ${e.message}", e)
        }
    }

    private fun disconnectAllDevices() {
        Log.d(TAG, "🔌 === DÉCONNEXION DE TOUS LES APPAREILS ===")
        try {
            val pairedDevices = bluetoothAdapter?.bondedDevices
            if (pairedDevices.isNullOrEmpty()) {
                Log.d(TAG, "Aucun appareil à déconnecter")
                return
            }

            pairedDevices.forEach { device ->
                try {
                    // Méthode de déconnexion
                    device.javaClass.getMethod("disconnect").invoke(device)
                    Log.d(TAG, "  ⚡ Déconnecté: ${device.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "  ❌ Erreur déconnexion ${device.name}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting devices: ${e.message}", e)
        }
    }

    fun startScan() {
        if (isScanning) return
        if (bluetoothAdapter?.isEnabled != true) {
            Log.e(TAG, "❌ Bluetooth off")
            return
        }

        try {
            // 📋 Afficher les appareils connectés
            logPairedDevices()
            logConnectedDevices()

            // 🔌 Déconnecter tous les appareils
            disconnectAllDevices()

            Thread.sleep(500) // Petit délai pour laisser le temps de déconnecter

            discoveredScooters.clear()
            allScannedDevices.clear()
            isScanning = true

            // 1️⃣ BLE Scan
            try {
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build()

                Log.d(TAG, "🔍 BLE Scan started")
                bluetoothLeScanner?.startScan(null, settings, scanCallback)
                Log.d(TAG, "✓ BLE Scan started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ BLE Scan error: ${e.message}", e)
            }

            // 2️⃣ Bluetooth Classique Discovery
            try {
                Log.d(TAG, "🔍 Bluetooth Classique Discovery started")
                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                context.registerReceiver(discoveryReceiver, filter)
                bluetoothAdapter?.startDiscovery()
                Log.d(TAG, "✓ Bluetooth Classique Discovery started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Bluetooth Classique Discovery error: ${e.message}", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in startScan: ${e.message}", e)
            isScanning = false
        }
    }

    fun stopScan() {
        if (!isScanning) return
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            bluetoothAdapter?.cancelDiscovery()
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver: ${e.message}")
            }
            isScanning = false
            Log.d(TAG, "🛑 Scan stopped - ${discoveredScooters.size} device(s) found")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in stopScan: ${e.message}", e)
        }
    }

    fun cleanup() {
        stopScan()
        discoveredScooters.clear()
        allScannedDevices.clear()
    }

    private fun isScooterDevice(name: String): Boolean {
        return SCOOTER_PREFIXES.any { name.contains(it, ignoreCase = true) }
    }

    private fun estimateDistance(rssi: Int): String {
        return when {
            rssi >= -50 -> "Très proche"
            rssi >= -60 -> "Proche"
            rssi >= -70 -> "Moyen"
            rssi >= -80 -> "Loin"
            else -> "Très loin"
        }
    }
}