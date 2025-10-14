package com.ix7.tracker.core

import java.util.Date

/**
 * Modèle de données du scooter M0Robot
 * VERSION COMPLÈTE avec TOUS les états possibles
 */
data class ScooterData(
    // ===== DONNÉES DE BASE =====
    val speed: Float = 0f,                      // Vitesse actuelle (km/h ou mph selon speedUnit)
    val battery: Float = 0f,                    // Batterie (%)
    val voltage: Float = 0f,                    // Tension (V)
    val current: Float = 0f,                    // Courant (A)
    val temperature: Float = 0f,                // Température (°C)
    val odometer: Float = 0f,                   // Odomètre total (km)
    val tripDistance: Float = 0f,               // Distance du trajet (km)
    val power: Float = 0f,                      // Puissance (W)
    val totalRideTime: String = "0H 0M 0S",    // Temps total de conduite

    // ===== MODE ET CONFIGURATION =====
    val currentMode: RideMode = RideMode.ECO,   // Mode de conduite actuel
    val speedLimitMode: SpeedLimitMode = SpeedLimitMode.LIMITED,  // Bridé ou débridé
    val speedUnit: SpeedUnit = SpeedUnit.KMH,   // Unité de vitesse

    // ===== ÉTATS LUMINEUX =====
    val headlightsOn: Boolean = false,          // Phare avant
    val neonOn: Boolean = false,                // Néons/LEDs décoratives
    val leftBlinker: Boolean = false,           // Clignotant gauche
    val rightBlinker: Boolean = false,          // Clignotant droite

    // ===== ÉTATS DE SÉCURITÉ =====
    val isLocked: Boolean = false,              // Verrouillage électronique
    val cruiseControl: Boolean = false,         // Régulateur de vitesse actif
    val cruiseSpeed: Float? = null,             // Vitesse du régulateur (si actif)
    val zeroStart: Boolean = false,             // Démarrage à zéro (sans coup de pied)

    // ===== MÉTADONNÉES =====
    val lastUpdate: Date = Date(),              // Dernière mise à jour
    val isConnected: Boolean = false,           // État de connexion

    // ===== DIAGNOSTICS =====
    val batteryTemperature: Float = 0f,         // Température batterie (°C)
    val errorCodes: List<Int> = emptyList(),    // Codes d'erreur actifs
    val warningCodes: List<Int> = emptyList(),  // Codes d'avertissement

    // ===== VERSIONS =====
    val firmwareVersion: String = "N/A",        // Version firmware
    val bluetoothVersion: String = "N/A"        // Version Bluetooth
)

/**
 * Modes de conduite disponibles
 */
enum class RideMode(val displayName: String, val maxSpeed: Int) {
    PEDESTRIAN("🚶 Piéton", 5),
    ECO("🌱 Eco", 15),
    RACE("🏎️ Race", 45),
    SPORT("⚡ Sport", 50);

    companion object {
        fun fromId(id: Int): RideMode? = when(id) {
            0x01 -> PEDESTRIAN
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
    LIMITED("🚧 Bridé"),
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