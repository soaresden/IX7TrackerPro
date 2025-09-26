package com.ix7.tracker

/**
 * Modèle de données pour les informations du scooter M0Robot
 * Ce fichier n'est nécessaire que si vous n'avez pas déjà défini ces classes ailleurs
 */

// Ces classes sont déjà définies dans BluetoothManager.kt
// Créez ce fichier seulement si vous préférez séparer les modèles de données

/*
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

data class BluetoothDeviceInfo(
    val name: String?,
    val address: String
)

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}
*/