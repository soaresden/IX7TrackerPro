package com.ix7.tracker.wear.bluetooth

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
        private const val TAG = "LockManager"
        private const val LOCK_NAME = "iPhone9"

        private const val LOCK_SERVICE_UUID = "0000fee7-0000-1000-8000-00805f9b34fb"
        private const val LOCK_WRITE_UUID = "000036f5-0000-1000-8000-00805f9b34fb"
        private const val LOCK_NOTIFY_UUID = "000036f6-0000-1000-8000-00805f9b34fb"

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

    private var reconnectionJob: Job? = null
    private var wasConnected = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val scannedDevices = mutableSetOf<String>()
    private var scanCount = 0

    private fun encryptAES(data: ByteArray): ByteArray {
        return try {
            val cipher = try {
                Cipher.getInstance("AES/ECB/NoPadding", "BC")
            } catch (e: Exception) {
                Cipher.getInstance("AES/ECB/NoPadding")
            }

            val keySpec = SecretKeySpec(AES_KEY, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val paddedData = data.copyOf(16)
            cipher.doFinal(paddedData)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption error: ${e.message}")
            data
        }
    }

    private fun decryptAES(data: ByteArray): ByteArray {
        return try {
            val cipher = try {
                Cipher.getInstance("AES/ECB/NoPadding", "BC")
            } catch (e: Exception) {
                Cipher.getInstance("AES/ECB/NoPadding")
            }

            val keySpec = SecretKeySpec(AES_KEY, "AES")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            cipher.doFinal(data)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption error: ${e.message}")
            data
        }
    }

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

                if (type == 0xFF && length >= 4) {
                    val manufacturerData = scanRecord.copyOfRange(index + 2, index + 1 + length)

                    if (manufacturerData.size >= 10) {
                        val stateByte = manufacturerData[8].toInt() and 0xFF
                        isLocked = stateByte == 0x01

                        if (manufacturerData.size >= 11) {
                            batteryLevel = manufacturerData[9].toInt() and 0xFF
                            if (batteryLevel > 100) {
                                batteryLevel = null
                            }
                        }
                    }
                }

                index += 1 + length
            }
        } catch (e: Exception) {
            Log.e(TAG, "Advertising parse error: ${e.message}")
        }

        return Pair(isLocked, batteryLevel)
    }

    fun startScanning(bluetoothAdapter: BluetoothAdapter) {
        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            Log.e(TAG, "BLE Scanner not available")
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

                scanCount++
                if (scanCount % 50 == 0) {
                    Log.d(TAG, "Scan #$scanCount - ${scannedDevices.size} unique devices")
                }

                if (name != null && !scannedDevices.contains(address)) {
                    scannedDevices.add(address)
                    Log.d(TAG, "Found: $name at $address")
                }

                if (name != null && name.trim().startsWith(LOCK_NAME)) {
                    Log.d(TAG, "=== LOCK DETECTED ===")
                    Log.d(TAG, "Address: $address")

                    val scanRecord = result.scanRecord?.bytes
                    val (isLocked, battery) = parseAdvertisingData(scanRecord)

                    detectedLockAddress = device.address
                    val currentState = _lockState.value
                    val newIsLocked = isLocked ?: currentState.isLocked
                    val newBattery = battery ?: currentState.batteryLevel

                    _lockState.value = currentState.copy(
                        isDetected = true,
                        name = name,
                        address = device.address,
                        isLocked = newIsLocked,
                        batteryLevel = newBattery
                    )

                    Log.d(TAG, "State: ${if (newIsLocked) "LOCKED" else "UNLOCKED"}")
                    Log.d(TAG, "=== ===")
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed: $errorCode")
            }
        }

        try {
            scanner.startScan(null, settings, scanCallback)
            Log.d(TAG, "BLE scan started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Scan permission error: ${e.message}")
        }
    }

    fun stopScanning(bluetoothAdapter: BluetoothAdapter) {
        try {
            val scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner != null) {
                scanner.stopScan(scanCallback)
                Log.d(TAG, "BLE scan stopped")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Stop scan permission error: ${e.message}")
        }
    }

    fun connect(bluetoothAdapter: BluetoothAdapter) {
        val address = detectedLockAddress
        if (address == null) {
            Log.e(TAG, "No address detected - scan first!")
            return
        }

        try {
            val device = bluetoothAdapter.getRemoteDevice(address)
            Log.d(TAG, "Connecting to $address")

            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)

        } catch (e: SecurityException) {
            Log.e(TAG, "Connect permission error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Connect error: ${e.message}")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val isConnected = newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS
            val isDisconnected = newState == BluetoothProfile.STATE_DISCONNECTED

            when {
                isConnected -> {
                    Log.d(TAG, "GATT Connected")
                    bluetoothGatt = gatt
                    wasConnected = true

                    try {
                        gatt.discoverServices()
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Discover permission error: ${e.message}")
                    }
                }
                isDisconnected -> {
                    Log.d(TAG, "GATT Disconnected")
                    _lockState.value = _lockState.value.copy(isConnected = false)

                    if (wasConnected && detectedLockAddress != null) {
                        Log.d(TAG, "Reconnection attempt...")
                        startReconnection()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return
            }

            Log.d(TAG, "Services discovered")

            val service = gatt.getService(UUID.fromString(LOCK_SERVICE_UUID))

            if (service == null) {
                Log.e(TAG, "Service not found: $LOCK_SERVICE_UUID")
                return
            }

            Log.d(TAG, "Service found!")

            writeCharacteristic = service.getCharacteristic(UUID.fromString(LOCK_WRITE_UUID))
            notifyCharacteristic = service.getCharacteristic(UUID.fromString(LOCK_NOTIFY_UUID))

            Log.d(TAG, "Write char: ${writeCharacteristic != null}")
            Log.d(TAG, "Notify char: ${notifyCharacteristic != null}")

            val notifyChar = notifyCharacteristic
            if (notifyChar != null) {
                try {
                    gatt.setCharacteristicNotification(notifyChar, true)
                    val descriptor = notifyChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))

                    if (descriptor != null) {
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                        Log.d(TAG, "Notifications enabled")
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Notification permission error: ${e.message}")
                }
            }

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                requestToken()
            }, 500)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value
            if (data == null || data.size != 16) {
                return
            }

            val decrypted = decryptAES(data)
            Log.d(TAG, "Response: ${decrypted.joinToString(" ") { "%02X".format(it) }}")

            when (decrypted[0].toInt()) {
                6 -> handleTokenResponse(decrypted)
                5 -> handleLockResponse(decrypted)
            }
        }

        private fun handleTokenResponse(decrypted: ByteArray) {
            if (decrypted[1].toInt() == 2) {
                authToken = decrypted.copyOfRange(3, 7)
                _lockState.value = _lockState.value.copy(isConnected = true)
                Log.d(TAG, "Token received - LOCK AUTHENTICATED!")
            }
        }

        private fun handleLockResponse(decrypted: ByteArray) {
            val success = decrypted[3].toInt() == 0
            val action = when (decrypted[1].toInt()) {
                2 -> "UNLOCK"
                8 -> "LOCK"
                else -> "UNKNOWN"
            }

            if (success) {
                val isLocked = action == "LOCK"
                _lockState.value = _lockState.value.copy(isLocked = isLocked)
                Log.d(TAG, "$action successful - State: ${if (isLocked) "LOCKED" else "UNLOCKED"}")
            } else {
                Log.e(TAG, "$action failed")
            }
        }
    }

    private fun requestToken() {
        Log.d(TAG, "Requesting token...")
        val data = CMD_GET_TOKEN + lockPassword.toByteArray()
        val encrypted = encryptAES(data)

        val char = writeCharacteristic
        if (char != null) {
            char.value = encrypted
            try {
                bluetoothGatt?.writeCharacteristic(char)
                Log.d(TAG, "Token request sent")
            } catch (e: SecurityException) {
                Log.e(TAG, "Write permission error: ${e.message}")
            }
        }
    }

    fun unlock() {
        val token = authToken
        if (token == null) {
            Log.e(TAG, "Token not available for unlock")
            return
        }

        val data = CMD_UNLOCK + token
        val encrypted = encryptAES(data)

        val char = writeCharacteristic
        if (char != null) {
            char.value = encrypted
            try {
                bluetoothGatt?.writeCharacteristic(char)
                Log.d(TAG, "UNLOCK command sent")
            } catch (e: SecurityException) {
                Log.e(TAG, "Unlock write error: ${e.message}")
            }
        }
    }

    fun lock() {
        val token = authToken
        if (token == null) {
            Log.e(TAG, "Token not available for lock")
            return
        }

        val data = CMD_LOCK + token
        val encrypted = encryptAES(data)

        val char = writeCharacteristic
        if (char != null) {
            char.value = encrypted
            try {
                bluetoothGatt?.writeCharacteristic(char)
                Log.d(TAG, "LOCK command sent")
            } catch (e: SecurityException) {
                Log.e(TAG, "Lock write error: ${e.message}")
            }
        }
    }

    fun disconnect() {
        wasConnected = false
        reconnectionJob?.cancel()

        Log.d(TAG, "Disconnecting...")

        try {
            val gatt = bluetoothGatt
            if (gatt != null) {
                gatt.disconnect()
                Thread.sleep(200)
                gatt.close()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Disconnect permission error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error: ${e.message}")
        }

        bluetoothGatt = null
        writeCharacteristic = null
        notifyCharacteristic = null
        authToken = null

        _lockState.value = _lockState.value.copy(isConnected = false)
        Log.d(TAG, "Disconnected")
    }

    private fun startReconnection() {
        reconnectionJob = coroutineScope.launch {
            repeat(5) { attempt ->
                delay(2000)
                Log.d(TAG, "Reconnection attempt ${attempt + 1}/5...")
                try {
                    val btManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as BluetoothManager
                    val bluetoothAdapter = btManager.adapter
                    if (bluetoothAdapter != null) {
                        connect(bluetoothAdapter)
                    } else {
                        Log.e(TAG, "Bluetooth adapter not available")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Reconnection error: ${e.message}")
                }
            }
        }
    }

    fun setPassword(newPassword: String) {
        lockPassword = newPassword
        Log.d(TAG, "Password updated")
    }

    fun cleanup() {
        reconnectionJob?.cancel()
        coroutineScope.cancel()
    }
}