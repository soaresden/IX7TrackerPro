package com.ix7.tracker.data

import com.ix7.tracker.core.SpeedLimitMode
import com.ix7.tracker.core.WheelMode
import com.ix7.tracker.core.RideMode
import java.util.*

data class TripLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String = ""
)

data class SpeedStats(
    val range0: Long = 0,
    val range0_10: Long = 0,
    val range10_20: Long = 0,
    val range20_30: Long = 0,
    val range30_40: Long = 0,
    val range40_50: Long = 0,
    val range50_60: Long = 0,
    val rangeAbove60: Long = 0
)

// ✅ Utilise maintenant les enums de com.ix7.tracker.core
data class TripSettings(
    val ridingMode: RideMode,           // Au lieu de RidingMode
    val driveMode: WheelMode,            // Au lieu de DriveMode
    val speedLock: SpeedLimitMode        // Au lieu de SpeedLock
)

data class Trip(
    val id: String,
    val startDate: Date,
    val startBattery: Int,
    val startOdometer: Float,
    val startLocation: TripLocation,
    val endDate: Date,
    val endBattery: Int,
    val endOdometer: Float,
    val endLocation: TripLocation,
    val distance: Float,
    val duration: Long,
    val maxSpeed: Float,
    val avgSpeed: Float,
    val energyUsed: Float,
    val speedStats: SpeedStats,
    val settings: TripSettings,
    val routePoints: List<TripLocation> = emptyList()
)

data class BatteryCycle(
    val cycleNumber: Int,
    val startBattery: Int,
    val endBattery: Int,
    val startDate: Date,
    val endDate: Date,
    val trips: List<Trip>,
    val totalDistance: Float,
    val totalDuration: Long,
    val aggregatedSpeedStats: SpeedStats
)

// Stats par mode
data class ModeStats(
    val settings: TripSettings,
    val tripCount: Int,
    val totalDistance: Float,
    val totalDuration: Long,
    val avgSpeed: Float,
    val maxSpeed: Float,
    val avgBatteryConsumption: Float, // % par km
    val aggregatedSpeedStats: SpeedStats
)