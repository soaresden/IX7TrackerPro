package com.ix7.tracker.data

import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.WheelMode
import com.ix7.tracker.core.SpeedLimitMode

data class Scooter(
    val name: String,
    val address: String,
    val rssi: Int,
    val type: Int = 1
)

data class ScooterStatus(
    val isConnected: Boolean = false,
    val batteryLevel: Int = 0,
    val speed: Float = 0f,
    val odometer: Float = 0f,
    val temperature: Float = 0f,
    val voltage: Float = 0f,
    val current: Float = 0f,
    val ridingMode: RideMode = RideMode.ECO,
    val driveMode: WheelMode = WheelMode.TWO_WHEELS,
    val speedLock: SpeedLimitMode = SpeedLimitMode.LIMITED,
    val timestamp: Long = System.currentTimeMillis()
)

data class Location(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)