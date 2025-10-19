package com.ix7.tracker.wear.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Location(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = ""
)

@Serializable
data class SpeedStats(
    val range0: Int = 0,           // 0 km/h
    val range0_10: Int = 0,        // 0-10 km/h
    val range10_20: Int = 0,       // 10-20 km/h
    val range20_30: Int = 0,       // 20-30 km/h
    val range30_40: Int = 0,       // 30-40 km/h
    val range40_50: Int = 0,       // 40-50 km/h
    val range50_60: Int = 0,       // 50-60 km/h
    val rangeAbove60: Int = 0      // > 60 km/h
)

@Serializable
data class TripSettings(
    val ridingMode: String = "ECO",      // PIETON, ECO, RACE, SPORT
    val driveMode: String = "NORMAL",    // NORMAL, LIGHT
    val speedLock: String = "NONE"       // NONE, LIMITED
)

@Serializable
@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // Timestamps
    val startDate: Long,
    val endDate: Long,

    // Battery
    val startBattery: Int,
    val endBattery: Int,

    // Odometer
    val startOdometer: Float,
    val endOdometer: Float,

    // Speed
    val maxSpeed: Float = 0f,
    val avgSpeed: Float = 0f,

    // Distance & Duration
    val distance: Float = 0f,
    val duration: Long = 0L,

    // Energy
    val energyUsed: Float = 0f,

    // Locations
    @Embedded(prefix = "start_")
    val startLocation: Location = Location(),
    @Embedded(prefix = "end_")
    val endLocation: Location = Location(),

    // Stats
    @Embedded
    val speedStats: SpeedStats = SpeedStats(),

    // Settings
    @Embedded
    val settings: TripSettings = TripSettings(),

    // Sync status
    val syncStatus: String = "PENDING", // PENDING, SYNCED
    val createdAt: Long = System.currentTimeMillis()
)