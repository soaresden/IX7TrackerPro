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
    val battery: Int = 0,
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
    val appVersion: String = "V1.0.0",
    val dataSource: String = "Unknown",
    val rawData: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScooterData

        if (speed != other.speed) return false
        if (battery != other.battery) return false
        if (voltage != other.voltage) return false
        if (current != other.current) return false
        if (power != other.power) return false
        if (temperature != other.temperature) return false
        if (odometer != other.odometer) return false
        if (totalRideTime != other.totalRideTime) return false
        if (batteryState != other.batteryState) return false
        if (errorCodes != other.errorCodes) return false
        if (warningCodes != other.warningCodes) return false
        if (firmwareVersion != other.firmwareVersion) return false
        if (bluetoothVersion != other.bluetoothVersion) return false
        if (appVersion != other.appVersion) return false
        if (dataSource != other.dataSource) return false
        if (rawData != null) {
            if (other.rawData == null) return false
            if (!rawData.contentEquals(other.rawData)) return false
        } else if (other.rawData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = speed.hashCode()
        result = 31 * result + battery
        result = 31 * result + voltage.hashCode()
        result = 31 * result + current.hashCode()
        result = 31 * result + power.hashCode()
        result = 31 * result + temperature
        result = 31 * result + odometer.hashCode()
        result = 31 * result + totalRideTime.hashCode()
        result = 31 * result + batteryState
        result = 31 * result + errorCodes
        result = 31 * result + warningCodes
        result = 31 * result + firmwareVersion.hashCode()
        result = 31 * result + bluetoothVersion.hashCode()
        result = 31 * result + appVersion.hashCode()
        result = 31 * result + dataSource.hashCode()
        result = 31 * result + (rawData?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Représente un appareil Bluetooth découvert
 */
data class BluetoothDeviceInfo(
    val name: String?,
    val address: String
)