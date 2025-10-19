package com.ix7.tracker.data

import java.util.Date

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