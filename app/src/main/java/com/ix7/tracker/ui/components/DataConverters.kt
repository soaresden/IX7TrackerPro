package com.ix7.tracker.ui.components

import java.util.*

// ════════════════════════════════════════════════════════════════
// 🔄 DATA CONVERSION FUNCTIONS
// ════════════════════════════════════════════════════════════════

/**
 * Convert any battery cycle object to BatteryCycleData
 */
fun convertToBatteryCycleData(
    cycleNumber: Int = 0,
    startDate: Date,
    endDate: Date,
    startBattery: Int = 0,
    endBattery: Int = 0,
    tripCount: Int = 0
): BatteryCycleData {
    val cycleDifference = (startBattery - endBattery).coerceIn(0, 100)

    return BatteryCycleData(
        cycleNumber = cycleNumber,
        startDate = startDate,
        endDate = endDate,
        startBattery = startBattery,
        endBattery = endBattery,
        tripCount = tripCount,
        cycleDifference = cycleDifference
    )
}

/**
 * Convert to ModeStatsData with all stats
 */
fun convertToModeStatsData(
    mode: String,
    count: Int = 0,
    avgDistance: Float = 0f,
    avgDuration: Long = 0L,
    avgSpeed: Float = 0f,
    avgBatteryUsed: Float = 0f,
    efficiency: Float = 0f,
    maxSpeed: Float = 0f
): ModeStatsData {
    return ModeStatsData(
        mode = mode,
        modeName = mode,
        count = count,
        totalDuration = avgDuration,
        avgSpeed = avgSpeed,
        avgDistance = avgDistance,
        avgDuration = avgDuration,
        avgBatteryUsed = avgBatteryUsed,
        efficiency = efficiency,
        maxSpeed = maxSpeed
    )
}

/**
 * Build a list of BatteryCycleData from raw data
 */
fun buildBatteryCyclesList(cycles: List<Map<String, Any>>): List<BatteryCycleData> {
    return cycles.mapIndexed { index, cycleData ->
        convertToBatteryCycleData(
            cycleNumber = index + 1,
            startDate = (cycleData["startDate"] as? Date) ?: Date(),
            endDate = (cycleData["endDate"] as? Date) ?: Date(),
            startBattery = (cycleData["startBattery"] as? Int) ?: 0,
            endBattery = (cycleData["endBattery"] as? Int) ?: 0,
            tripCount = (cycleData["tripCount"] as? Int) ?: 0
        )
    }
}

/**
 * Build a list of ModeStatsData from raw data
 */
fun buildModeStatsList(stats: List<Map<String, Any>>): List<ModeStatsData> {
    return stats.map { modeData ->
        convertToModeStatsData(
            mode = (modeData["mode"] as? String) ?: "UNKNOWN",
            count = (modeData["count"] as? Int) ?: 0,
            avgDistance = (modeData["avgDistance"] as? Float) ?: 0f,
            avgDuration = (modeData["avgDuration"] as? Long) ?: 0L,
            avgSpeed = (modeData["avgSpeed"] as? Float) ?: 0f,
            avgBatteryUsed = (modeData["avgBatteryUsed"] as? Float) ?: 0f,
            efficiency = (modeData["efficiency"] as? Float) ?: 0f,
            maxSpeed = (modeData["maxSpeed"] as? Float) ?: 0f
        )
    }
}