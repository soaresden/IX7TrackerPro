package com.ix7.tracker

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

class BluetoothDeviceScanner(private val context: Context) {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var isScanning = false

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    fun startScan(onDeviceFound: (BluetoothDevice, Int) -> Unit) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            LogManager.logError("Permission BLUETOOTH_SCAN non accordée")
            return
        }

        if (isScanning) {
            LogManager.logInfo("Scan déjà en cours")
            return
        }

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val rssi = result.rssi

                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    val deviceName = device.name
                    if (deviceName != null && deviceName.startsWith("M0Robot", ignoreCase = true)) {
                        LogManager.logInfo("M0Robot trouvé: $deviceName ($rssi dBm)")
                        onDeviceFound(device, rssi)
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                LogManager.logError("Échec du scan: $errorCode")
                isScanning = false
            }
        }

        try {
            bluetoothLeScanner?.startScan(scanCallback)
            isScanning = true
            LogManager.logInfo("Scan BLE démarré - recherche de M0Robot...")
        } catch (e: Exception) {
            LogManager.logError("Erreur lors du démarrage du scan", e)
        }
    }

    fun stopScan() {
        if (!isScanning || scanCallback == null) {
            return
        }

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            try {
                bluetoothLeScanner?.stopScan(scanCallback)
                isScanning = false
                LogManager.logInfo("Scan BLE arrêté")
            } catch (e: Exception) {
                LogManager.logError("Erreur lors de l'arrêt du scan", e)
            }
        }
    }

    fun isScanning(): Boolean = isScanning
}