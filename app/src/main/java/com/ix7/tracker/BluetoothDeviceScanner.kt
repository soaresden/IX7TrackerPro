package com.ix7.tracker

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager as SystemBluetoothManager
import android.content.Context
import android.util.Log

/**
 * Gestionnaire de scan des appareils Bluetooth
 * Responsable de la découverte des trottinettes M0Robot compatibles
 */
@SuppressLint("MissingPermission")
class BluetoothDeviceScanner(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothDeviceScanner"
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as SystemBluetoothManager
        bluetoothManager.adapter
    }

    private var isScanning = false
    private var onDeviceFoundCallback: ((BluetoothDevice) -> Unit)? = null
    private var onScanStateChangedCallback: ((Boolean) -> Unit)? = null

    private val leScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        if (isM0RobotDevice(device)) {
            Log.d(TAG, "M0Robot trouvé: ${device.name} (${device.address}) RSSI: $rssi dBm")
            onDeviceFoundCallback?.invoke(device)
        } else {
            // Log pour debug - appareil non compatible
            Log.v(TAG, "Appareil ignoré: ${device.name} (${device.address})")
        }
    }

    fun setOnDeviceFoundCallback(callback: (BluetoothDevice) -> Unit) {
        onDeviceFoundCallback = callback
    }

    fun setOnScanStateChangedCallback(callback: (Boolean) -> Unit) {
        onScanStateChangedCallback = callback
    }

    fun startScanning() {
        val adapter = bluetoothAdapter ?: run {
            Log.e(TAG, "BluetoothAdapter non disponible")
            return
        }

        if (isScanning) {
            Log.w(TAG, "Scan déjà en cours")
            return
        }

        isScanning = true
        onScanStateChangedCallback?.invoke(true)

        Log.d(TAG, "Démarrage du scan BLE pour trottinettes M0Robot...")

        // Utiliser le scan LE classique pour compatibilité
        val success = adapter.startLeScan(leScanCallback)
        if (!success) {
            Log.e(TAG, "Impossible de démarrer le scan BLE")
            isScanning = false
            onScanStateChangedCallback?.invoke(false)
        }
    }

    fun stopScanning() {
        val adapter = bluetoothAdapter ?: return

        if (!isScanning) return

        isScanning = false
        onScanStateChangedCallback?.invoke(false)

        adapter.stopLeScan(leScanCallback)
        Log.d(TAG, "Arrêt du scan BLE")
    }

    fun isCurrentlyScanning(): Boolean = isScanning

    /**
     * Vérifie si l'appareil est une trottinette M0Robot compatible
     * Basé sur la liste des préfixes du code original décompilé
     */
    private fun isM0RobotDevice(device: BluetoothDevice): Boolean {
        val name = device.name?.trim() ?: return false

        // Liste complète des préfixes supportés (du code original)
        val supportedPrefixes = listOf(
            "M0", "H1", "M1", "Mini", "Plus", "X1", "X3", "M6",
            "GoKart", "A6", "MI", "N3MTenbot", "miniPLUS_",
            " MiniPro", "SFSO", "V5Robot", "EO STREET", "XRIDER",
            "TECAR", "MAX", "i10", "NEXRIDE", "E-WHEELS",
            "E12", "E9PRO", "T10"
        )

        val isCompatible = supportedPrefixes.any { prefix ->
            name.startsWith(prefix, ignoreCase = true)
        }

        if (isCompatible) {
            Log.d(TAG, "Trottinette compatible détectée: $name")
        }

        return isCompatible
    }

    fun cleanup() {
        stopScanning()
        onDeviceFoundCallback = null
        onScanStateChangedCallback = null
    }
}