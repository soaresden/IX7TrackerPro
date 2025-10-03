package com.ix7.tracker.core

data class BluetoothDeviceInfo(
    val name: String?,
    val address: String,
    val rssi: Int = 0,
    val isScooter: Boolean = false,
    val scooterType: ScooterType = ScooterType.UNKNOWN,
    val distance: String = ""
)

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}