package com.ix7.tracker

/**
 * États de connexion Bluetooth possibles
 */
enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Données reçues du scooter M0Robot
 */
data class ScooterData(
    val speed: Float = 0f,
    val battery: Float = 0f,
    val voltage: Float = 0f,
    val current: Float = 0f,
    val power: Float = 0f,
    val temperature: Int = 0,
    val odometer: Float = 0f,
    val totalRideTime: String = "0H 0M 0S",
    val batteryState: Int = 0,
    val errorCodes: Int = 0,
    val warningCodes: Int = 0,
    val firmwareVersion: String = "V1.0.0",
    val bluetoothVersion: String = "V4.0",
    val appVersion: String = "V1.0.0"
)

/**
 * Représente un appareil Bluetooth découvert
 */
data class BluetoothDeviceInfo(
    val name: String?,
    val address: String
)