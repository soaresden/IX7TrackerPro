package com.ix7.tracker.core

import java.util.*

/**
 * Données complètes du scooter M0Robot
 */
data class ScooterData(
    // Données temps réel
    val speed: Float = 0f,
    val battery: Float = 0f,
    val voltage: Float = 0f,
    val current: Float = 0f,
    val power: Float = 0f,
    val temperature: Float = 0f,
    val batteryTemperature: Float = 0f,

    // Totaux et historique
    val odometer: Float = 0f,
    val tripDistance: Float = 0f,
    val totalRideTime: String = "0H 0M 0S",

    // États système
    val batteryState: Int = 0,
    val errorCodes: Int = 0,
    val warningCodes: Int = 0,

    // Versions
    val firmwareVersion: String = "V1.0.0",
    val bluetoothVersion: String = "V4.0",
    val appVersion: String = "V1.0.0",

    // Métadonnées
    val lastUpdate: Date = Date(),
    val isConnected: Boolean = false
)

/**
 * Informations d'un appareil Bluetooth découvert
 */
data class BluetoothDeviceInfo(
    val name: String?,
    val address: String,
    val rssi: Int = 0,
    val isScooter: Boolean = false,
    val scooterType: ScooterType = ScooterType.UNKNOWN,  // Référence celui de Constants.kt
    val distance: String = ""
)

/**
 * États de connexion possibles
 */
enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}