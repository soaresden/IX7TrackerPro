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
            LogManager.logInfo("Données M0Robot reçues: ${data.joinToString(" ") { "%02X".format(it) }}")
            parseM0RobotData(data)
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

    private fun parseM0RobotData(data: ByteArray) {
        if (data.size < 3) {
            LogManager.logInfo("Données M0Robot trop courtes: ${data.size} bytes")
            return
        }

        // Vérifier que c'est une réponse M0Robot (commence par 0x55)
        if (data[0] != 0x55.toByte()) {
            LogManager.logInfo("Pas une réponse M0Robot valide: premier byte = ${data[0]}")
            return
        }

        val commandType = data[1].toInt() and 0xFF
        LogManager.logInfo("Réponse M0Robot de type: 0x${commandType.toString(16).uppercase()}")

        val currentData = _receivedData.value ?: ScooterData()
        var updatedData = currentData

        when (commandType) {
            0x31 -> {
                // Réponse vitesse/batterie
                if (data.size >= 10) {
                    val speed = ((data[2].toInt() and 0xFF) shl 8 or (data[3].toInt() and 0xFF)) / 100f
                    val battery = (data[4].toInt() and 0xFF).toFloat()

                    updatedData = currentData.copy(
                        speed = speed,
                        battery = battery
                    )
                    LogManager.logInfo("M0Robot - Vitesse: ${speed}km/h, Batterie: ${battery}%")
                }
            }

            0x32 -> {
                // Réponse voltage/courant
                if (data.size >= 10) {
                    val voltage = ((data[2].toInt() and 0xFF) shl 8 or (data[3].toInt() and 0xFF)) / 100f
                    val current = ((data[4].toInt() and 0xFF) shl 8 or (data[5].toInt() and 0xFF)) / 100f

                    updatedData = currentData.copy(
                        voltage = voltage,
                        current = current,
                        power = voltage * current
                    )
                    LogManager.logInfo("M0Robot - Tension: ${voltage}V, Courant: ${current}A")
                }
            }

            0x33 -> {
                // Réponse température/odomètre
                if (data.size >= 10) {
                    val temperature = (data[2].toInt() and 0xFF) - 40 // Offset de température
                    val odometer = ((data[4].toInt() and 0xFF) shl 24 or
                            (data[5].toInt() and 0xFF) shl 16 or
                            (data[6].toInt() and 0xFF) shl 8 or
                            (data[7].toInt() and 0xFF)) / 1000f

                    updatedData = currentData.copy(
                        temperature = temperature,
                        odometer = odometer
                    )
                    LogManager.logInfo("M0Robot - Température: ${temperature}°C, Odomètre: ${odometer}km")
                }
            }

            0x34 -> {
                // Réponse infos système
                if (data.size >= 8) {
                    val errorCodes = data[2].toInt() and 0xFF
                    val warningCodes = data[3].toInt() and 0xFF
                    val batteryState = data[4].toInt() and 0xFF

                    updatedData = currentData.copy(
                        errorCodes = errorCodes,
                        warningCodes = warningCodes,
                        batteryState = batteryState
                    )
                    LogManager.logInfo("M0Robot - Erreurs: $errorCodes, Avertissements: $warningCodes")
                }
            }

            0x35 -> {
                // Réponse version firmware
                if (data.size >= 8) {
                    val majorVersion = data[2].toInt() and 0xFF
                    val minorVersion = data[3].toInt() and 0xFF
                    val patchVersion = data[4].toInt() and 0xFF

                    val firmwareVersion = "$majorVersion.$minorVersion.$patchVersion"

                    updatedData = currentData.copy(
                        firmwareVersion = firmwareVersion
                    )
                    LogManager.logInfo("M0Robot - Firmware: $firmwareVersion")
                }
            }

            else -> {
                LogManager.logInfo("Type de réponse M0Robot non reconnu: 0x${commandType.toString(16).uppercase()}")
            }
        }

        // Mettre à jour les données seulement si on a reçu de vraies données
        if (updatedData != currentData) {
            _receivedData.value = updatedData
            LogManager.logInfo("Données M0Robot mises à jour")
        }
    }
}