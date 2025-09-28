package com.ix7.tracker.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import com.ix7.tracker.core.BluetoothDeviceInfo
import com.ix7.tracker.protocol.ScooterDetector

class BluetoothScanner(
    private val context: Context,
    private val onDevicesFound: (List<BluetoothDeviceInfo>) -> Unit
) {
    companion object {
        private const val TAG = "BluetoothScanner"
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private var isScanning = false
    private val discoveredDevices = mutableListOf<BluetoothDeviceInfo>()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let { handleScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Scan failed: $errorCode")
            isScanning = false
        }
    }

    private fun handleScanResult(result: ScanResult) {
        val device = result.device
        val deviceName = device.name ?: return // Ignorer les devices sans nom

        // FILTRER UNIQUEMENT LES SCOOTERS
        if (!ScooterDetector.isScooterDevice(deviceName)) {
            return // Ignorer les devices non-scooter
        }

        val deviceInfo = BluetoothDeviceInfo(
            name = deviceName,
            address = device.address,
            rssi = result.rssi,
            isScooter = true, // Tous les devices ici sont des scooters
            scooterType = ScooterDetector.detectScooterType(deviceName),
            distance = ScooterDetector.estimateDistance(result.rssi)
        )

        val existingIndex = discoveredDevices.indexOfFirst { it.address == device.address }
        if (existingIndex >= 0) {
            discoveredDevices[existingIndex] = deviceInfo
        } else {
            discoveredDevices.add(deviceInfo)
        }

        // Trier par signal et limiter à 5 devices max
        val sortedDevices = discoveredDevices
            .sortedByDescending { it.rssi }
            .take(5) // MAX 5 SCOOTERS AFFICHÉS

        onDevicesFound(sortedDevices)
        Log.d(TAG, "Scooter trouvé: $deviceName (${result.rssi} dBm)")
    }

    fun startScan(): Result<Unit> {
        return try {
            if (bluetoothAdapter?.isEnabled == true && !isScanning) {
                discoveredDevices.clear()
                isScanning = true
                bluetoothLeScanner?.startScan(scanCallback)
                Log.d(TAG, "Scan démarré - Recherche de scooters uniquement")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Bluetooth indisponible"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stopScan(): Result<Unit> {
        return try {
            if (isScanning) {
                bluetoothLeScanner?.stopScan(scanCallback)
                isScanning = false
                Log.d(TAG, "Scan arrêté")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun initialize(): Result<Unit> {
        return if (bluetoothAdapter?.isEnabled == true) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Bluetooth désactivé"))
        }
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun cleanup() {
        if (isScanning) {
            bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
        }
    }
}