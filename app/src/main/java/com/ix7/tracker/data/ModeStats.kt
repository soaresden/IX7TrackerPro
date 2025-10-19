package com.ix7.tracker.data

data class ModeStats(
    val settings: TripSettings,
    val tripCount: Int,
    val totalDistance: Float,
    val totalDuration: Long,
    val avgSpeed: Float,
    val maxSpeed: Float,
    val avgBatteryConsumption: Float,
    val aggregatedSpeedStats: SpeedStats
)