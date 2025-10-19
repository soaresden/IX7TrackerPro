package com.ix7.tracker.ui.components

import java.util.*

/**
 * Données pour les cycles de batterie (utilisé par ModBatteryCycles)
 */
data class BatteryCycleData(
    val startDate: Date,
    val endDate: Date,
    val startBattery: Int,
    val endBattery: Int,
    val distance: Float,
    val duration: Long
)

/**
 * Données pour la comparaison des modes (utilisé par ModComparison)
 */
data class ModeStatsData(
    val modeName: String,
    val tripCount: Int,
    val totalDistance: Float,
    val totalDuration: Long,
    val avgBatteryUsage: Float,
    val avgSpeed: Float
)