package com.ix7.tracker.core

import java.util.*

/**
 * Données complètes du scooter M0Robot
 */
data class ScooterData(
    // Données temps réel
    val speed: Float = 0f,               // km/h
    val battery: Float = 0f,             // %
    val voltage: Float = 0f,             // V
    val current: Float = 0f,             // A
    val power: Float = 0f,               // W
    val temperature: Float = 0f,         // °C
    val batteryTemperature: Float = 0f,  // °C

    // Totaux et historique
    val odometer: Float = 0f,            // km total
    val tripDistance: Float = 0f,        // km ce trajet
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
    val scooterType: ScooterType = ScooterType.UNKNOWN,
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

/**
 * Types de scooters supportés avec leurs UUIDs
 */
enum class ScooterType(
    val uuid_service: String,
    val uuid_notify: String,
    val uuid_write: String,
    val description: String
) {
    TYPE_1("0000ffe0-0000-1000-8000-00805f9b34fb",
        "0000ffe1-0000-1000-8000-00805f9b34fb",
        "0000ffe1-0000-1000-8000-00805f9b34fb",
        "Type 1 - Standard"),

    TYPE_2("6e400001-b5a3-f393-e0a9-e50e24dcca9e",
        "6e400002-b5a3-f393-e0a9-e50e24dcca9e",
        "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
        "Type 2 - Nordic"),

    TYPE_3("0000ae00-0000-1000-8000-00805f9b34fb",
        "0000ae01-0000-1000-8000-00805f9b34fb",
        "0000ae02-0000-1000-8000-00805f9b34fb",
        "Type 3 - AE00"),

    TYPE_4("0000fff0-0000-1000-8000-00805f9b34fb",
        "0000fff3-0000-1000-8000-00805f9b34fb",
        "0000fff7-0000-1000-8000-00805f9b34fb",
        "Type 4 - FFF0"),

    UNKNOWN("", "", "", "Inconnu")
}