package com.ix7.tracker.ui.components

import com.ix7.tracker.data.BatteryCycle as DataBatteryCycle
import com.ix7.tracker.data.ModeStats as DataModeStats

/**
 * Convertit une BatteryCycle du package data en BatteryCycleData pour les composables
 */
fun DataBatteryCycle.toBatteryCycleData(): BatteryCycleData {
    return BatteryCycleData(
        startDate = this.startDate,
        endDate = this.endDate,
        startBattery = this.startBattery,
        endBattery = this.endBattery,
        distance = this.totalDistance,  // ✅ C'est totalDistance, pas distance
        duration = this.totalDuration   // ✅ C'est totalDuration, pas duration
    )
}

/**
 * Convertit une liste de BatteryCycle du package data
 */
fun List<DataBatteryCycle>.toBatteryCyclesData(): List<BatteryCycleData> {
    return this.map { it.toBatteryCycleData() }
}

/**
 * Convertit une ModeStats du package data en ModeStatsData pour les composables
 */
fun DataModeStats.toModeStatsData(): ModeStatsData {
    return ModeStatsData(
        modeName = this.settings.ridingMode.name,  // ✅ Extraire du settings
        tripCount = this.tripCount,
        totalDistance = this.totalDistance,
        totalDuration = this.totalDuration,
        avgBatteryUsage = this.avgBatteryConsumption,  // ✅ C'est avgBatteryConsumption, pas avgBatteryUsage
        avgSpeed = this.avgSpeed
    )
}

/**
 * Convertit une liste de ModeStats du package data
 */
fun List<DataModeStats>.toModeStatsData(): List<ModeStatsData> {
    return this.map { it.toModeStatsData() }
}