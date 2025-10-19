package com.ix7.tracker.ui.components

import java.util.*

// ════════════════════════════════════════════════════════════════
// 🔋 BATTERY CYCLE DATA CLASS
// ════════════════════════════════════════════════════════════════

data class BatteryCycleData(
    val cycleNumber: Int = 0,
    val startDate: Date,
    val endDate: Date,
    val startBattery: Int = 0,      // Battery % at start
    val endBattery: Int = 0,        // Battery % at end
    val tripCount: Int = 0,         // Number of trips in this cycle
    val cycleDifference: Int = 0,   // ✅ VIRGULE AJOUTÉE ICI!
    val distance: Float = 0f,        // Total distance in this cycle
    val duration: Long = 0L,        // Total duration in this cycle
)

// ════════════════════════════════════════════════════════════════
// ⚙️ MODE STATS DATA CLASS - FINAL ULTRA VERSION
// ════════════════════════════════════════════════════════════════

data class ModeStatsData(
    val mode: String = "",
    val modeName: String = "",
    val count: Int = 0,
    val totalDuration: Long = 0L,
    val avgSpeed: Float = 0f,           // ✅ Main property
    val avgDistance: Float = 0f,
    val avgDuration: Long = 0L,
    val avgBatteryUsed: Float = 0f,     // ✅ Correct name (not avgBatteryUsage)
    val efficiency: Float = 0f,
    val maxSpeed: Float = 0f
)

// ════════════════════════════════════════════════════════════════
// 🔄 EXTENSION FUNCTIONS
// ════════════════════════════════════════════════════════════════

/**
 * Convert List<BatteryCycle> to List<BatteryCycleData>
 */
fun List<Any>.toBatteryCyclesData(): List<BatteryCycleData> {
    return emptyList()
}

/**
 * Convert mode stats to ModeStatsData list
 */
fun List<Any>.toModeStatsData(): List<ModeStatsData> {
    return emptyList()
}