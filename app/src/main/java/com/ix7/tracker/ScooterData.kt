package com.ix7.tracker

data class BluetoothDeviceInfo(
    val name: String,
    val address: String
)

data class ScooterData(
    val speed: Float? = null,
    val battery: Int? = null,
    val voltage: Float? = null,
    val current: Float? = null,
    val power: Float? = null,
    val temperature: Int? = null,
    val odometer: Float? = null,
    val totalRideTime: String? = null,
    val batteryState: String? = null,
    val errorCodes: List<String> = emptyList(),
    val warningCodes: List<String> = emptyList(),
    val firmwareVersion: String? = null,
    val bluetoothVersion: String? = null,
    val appVersion: String? = null,
    val driveMode: String? = null,
    val leftIndicator: Boolean = false,
    val rightIndicator: Boolean = false,
    val warnings: List<String> = emptyList(),
    val headlight: Boolean = false,
    val taillight: Boolean = false,
    val rawData: ByteArray? = null,
    val dataSource: String? = null
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
        if (driveMode != other.driveMode) return false
        if (leftIndicator != other.leftIndicator) return false
        if (rightIndicator != other.rightIndicator) return false
        if (warnings != other.warnings) return false
        if (headlight != other.headlight) return false
        if (taillight != other.taillight) return false
        if (rawData != null) {
            if (other.rawData == null) return false
            if (!rawData.contentEquals(other.rawData)) return false
        } else if (other.rawData != null) return false
        if (dataSource != other.dataSource) return false

        return true
    }

    override fun hashCode(): Int {
        var result = speed?.hashCode() ?: 0
        result = 31 * result + (battery ?: 0)
        result = 31 * result + (voltage?.hashCode() ?: 0)
        result = 31 * result + (current?.hashCode() ?: 0)
        result = 31 * result + (power?.hashCode() ?: 0)
        result = 31 * result + (temperature ?: 0)
        result = 31 * result + (odometer?.hashCode() ?: 0)
        result = 31 * result + (totalRideTime?.hashCode() ?: 0)
        result = 31 * result + (batteryState?.hashCode() ?: 0)
        result = 31 * result + errorCodes.hashCode()
        result = 31 * result + warningCodes.hashCode()
        result = 31 * result + (firmwareVersion?.hashCode() ?: 0)
        result = 31 * result + (bluetoothVersion?.hashCode() ?: 0)
        result = 31 * result + (appVersion?.hashCode() ?: 0)
        result = 31 * result + (driveMode?.hashCode() ?: 0)
        result = 31 * result + leftIndicator.hashCode()
        result = 31 * result + rightIndicator.hashCode()
        result = 31 * result + warnings.hashCode()
        result = 31 * result + headlight.hashCode()
        result = 31 * result + taillight.hashCode()
        result = 31 * result + (rawData?.contentHashCode() ?: 0)
        result = 31 * result + (dataSource?.hashCode() ?: 0)
        return result
    }
}

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED
}

data class ScooterCategory(
    val title: String,
    val icon: String,
    val items: List<ScooterDataItem>
)

data class ScooterDataItem(
    val label: String,
    val value: String,
    val unit: String = "",
    val isAvailable: Boolean = true
)