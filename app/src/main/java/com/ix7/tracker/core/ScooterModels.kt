package com.ix7.tracker.core

import java.util.Date

/**
 * Modèle de données du scooter M0Robot
 * VERSION COMPLÈTE avec TOUS les états possibles
 */
data class ScooterData(
    // ===== DONNÉES DE BASE =====
    val speed: Float = 0f,
    val battery: Float = 0f,
    val voltage: Float = 0f,
    val current: Float = 0f,
    val temperature: Float = 0f,
    val odometer: Float = 0f,
    val tripDistance: Float = 0f,
    val power: Float = 0f,
    val totalRideTime: String = "0H 0M 0S",

    // ===== MODE ET CONFIGURATION =====
    val currentMode: RideMode = RideMode.ECO,
    val speedLimitMode: SpeedLimitMode = SpeedLimitMode.NORMAL,
    val speedUnit: SpeedUnit = SpeedUnit.KMH,
    val wheelMode: WheelMode = WheelMode.TWO_WHEELS,

    // ===== ÉTATS LUMINEUX =====
    val headlightsOn: Boolean = false,
    val neonOn: Boolean = false,
    val leftBlinker: Boolean = false,
    val rightBlinker: Boolean = false,

    // ===== ÉTATS DE SÉCURITÉ =====
    val isLocked: Boolean = false,
    val cruiseControl: Boolean = false,
    val cruiseSpeed: Float? = null,
    val zeroStart: Boolean = false,

    // ===== MÉTADONNÉES =====
    val lastUpdate: Date = Date(),
    val isConnected: Boolean = false,

    // ===== DIAGNOSTICS =====
    val batteryTemperature: Float = 0f,
    val errorCodes: List<Int> = emptyList(),
    val warningCodes: List<Int> = emptyList(),

    // ===== VERSIONS =====
    val firmwareVersion: String = "N/A",
    val bluetoothVersion: String = "N/A"
) {
    val blinkerState: BlinkerState
        get() = BlinkerState.from(leftBlinker, rightBlinker)
}

data class SpeedLimits(
    val PIETON: Int,
    val eco: Int,
    val race: Int,
    val sport: Int
)

// ============= ÉNUMÉRATIONS =============

/**
 * Modes de conduite disponibles
 */
enum class RideMode(val displayName: String, val maxSpeed: Int) {
    PIETON("🚶 Piéton", 5),
    ECO("🌱 Eco", 15),
    SPORT("⚡ Sport", 50),
    RACE("🏎️ Race", 45);

    companion object {
        fun fromId(id: Int): RideMode? = when(id) {
            0x01 -> PIETON
            0x02 -> ECO
            0x03 -> SPORT
            0x04 -> RACE
            else -> null
        }
    }
}

/**
 * État du bridage de vitesse
 */
enum class SpeedLimitMode(val displayName: String) {
    NORMAL("🚧 Bridé"),
    UNLIMITED("⚡ Débridé");
}

/**
 * Unité de mesure de la vitesse
 */
enum class SpeedUnit(val displayName: String, val symbol: String) {
    KMH("Kilomètres/heure", "km/h"),
    MPH("Miles/heure", "mph");

    fun convert(speedKmh: Float): Float {
        return when (this) {
            KMH -> speedKmh
            MPH -> speedKmh * 0.621371f
        }
    }
}

/**
 * État des clignotants
 */
enum class BlinkerState {
    OFF,
    LEFT,
    RIGHT,
    BOTH;  // Warnings/feux de détresse

    companion object {
        fun from(left: Boolean, right: Boolean): BlinkerState = when {
            left && right -> BOTH
            left -> LEFT
            right -> RIGHT
            else -> OFF
        }
    }
}

/**
 * Mode de roues (1 ou 2 roues)
 */
enum class WheelMode(val emoji: String, val label: String) {
    ONE_WHEEL("🛴", "1 roue"),
    TWO_WHEELS("🏍️", "2 roues")
}