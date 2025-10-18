package com.ix7.tracker.wear.bluetooth

// ===== ENUMS =====

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

// ===== DATA CLASSES =====

data class ScooterInfo(
    val name: String,
    val address: String,
    val rssi: Int = 0,
    val distance: String = ""
)

data class ScooterData(
    val speed: Float = 0f,
    val battery: Int = 0,
    val temperature: Float = 0f,
    val odometer: Float = 0f,
    val voltage: Float = 0f,
    val current: Float = 0f
)