package com.ix7.tracker

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothManager(private val context: Context) {

    private val bluetoothLe = BluetoothLe(context)
    private val bluetoothDeviceScanner = BluetoothDeviceScanner(context)

    // États observables pour l'UI
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _receivedData = MutableStateFlow<ScooterData?>(null)
    val receivedData: StateFlow<ScooterData?> = _receivedData.asStateFlow()

    init {
        // Configurer les listeners
        bluetoothLe.setOnConnectionStateChangedListener { isConnected ->
            _connectionState.value = if (isConnected) {
                ConnectionState.CONNECTED
            } else {
                ConnectionState.DISCONNECTED
            }
        }

        bluetoothLe.setOnDataReceivedListener { data ->
            parseScooterData(data)
        }
    }

    fun startDiscovery() {
        LogManager.logInfo("Démarrage de la découverte...")
        _discoveredDevices.value = emptyList()
        _connectionState.value = ConnectionState.SCANNING

        bluetoothDeviceScanner.startScan { device, rssi ->
            val deviceInfo = BluetoothDeviceInfo(
                name = device.name,
                address = device.address
            )

            val currentDevices = _discoveredDevices.value.toMutableList()
            if (currentDevices.none { it.address == deviceInfo.address }) {
                currentDevices.add(deviceInfo)
                _discoveredDevices.value = currentDevices
                LogManager.logInfo("Appareil ajouté: ${device.name} (${device.address}) - Signal: ${rssi}dBm")
            }
        }
    }

    fun stopDiscovery() {
        bluetoothDeviceScanner.stopScan()
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun connectToDevice(address: String) {
        stopDiscovery()
        _connectionState.value = ConnectionState.CONNECTING
        bluetoothLe.connect(address)
    }

    fun disconnect() {
        bluetoothLe.disconnect()
    }

    fun cleanup() {
        stopDiscovery()
        disconnect()
    }

    private fun parseScooterData(data: ByteArray) {
        try {
            // Parsing basique - à améliorer selon le vrai protocole M0Robot
            if (data.size >= 10) {
                val speed = if (data.size > 2) ((data[1].toInt() and 0xFF) * 0.1f) else 0f
                val battery = if (data.size > 3) (data[2].toInt() and 0xFF).toFloat() else 0f
                val voltage = if (data.size > 5) ((data[4].toInt() and 0xFF) + (data[5].toInt() and 0xFF) * 0.1f) else 0f
                val current = if (data.size > 7) ((data[6].toInt() and 0xFF) + (data[7].toInt() and 0xFF) * 0.01f) else 0f
                val temp = if (data.size > 8) (data[8].toInt() and 0xFF) - 20 else 25

                val scooterData = ScooterData(
                    speed = speed,
                    battery = battery,
                    voltage = voltage,
                    current = current,
                    power = voltage * current,
                    temperature = temp,
                    odometer = 282.5f, // Valeur fixe pour test
                    totalRideTime = "164H 35M 0S",
                    firmwareVersion = "84.c.a (0439085e)",
                    bluetoothVersion = "0.d.5 (04cf)",
                    appVersion = "11.2.9"
                )

                _receivedData.value = scooterData
                LogManager.logInfo("Données mises à jour: Vitesse=${speed}km/h, Batterie=${battery}%")
            }
        } catch (e: Exception) {
            LogManager.logError("Erreur parsing données", e)
            generateTestData()
        }
    }

    private fun generateTestData() {
        val testData = ScooterData(
            speed = (0..25).random().toFloat(),
            battery = (60..90).random().toFloat(),
            voltage = 49.2f,
            current = 0.1f,
            power = 7.9f,
            temperature = 27,
            odometer = 282.5f,
            totalRideTime = "164H 35M 0S",
            firmwareVersion = "84.c.a (0439085e)",
            bluetoothVersion = "0.d.5 (04cf)",
            appVersion = "11.2.9"
        )
        _receivedData.value = testData
    }
}