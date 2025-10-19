package com.ix7.tracker.data

import com.ix7.tracker.core.SpeedLimitMode
import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.WheelMode
import java.util.Date

// ===== MODÈLES MÉTIER =====

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
    val settings: TripSettings
)

data class TripLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String
)

data class SpeedStats(
    val range0: Long,
    val range0_10: Long,
    val range10_20: Long,
    val range20_30: Long,
    val range30_40: Long,
    val range40_50: Long,
    val range50_60: Long,
    val rangeAbove60: Long
)

data class TripSettings(
    val ridingMode: RideMode,
    val driveMode: WheelMode,
    val speedLock: SpeedLimitMode
)