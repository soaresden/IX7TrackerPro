package com.ix7.tracker.core

import com.ix7.tracker.ui.screens.RideMode
import java.util.Date

data class ScooterData(
    // Données de base
    val speed: Float = 0f,
    val battery: Float = 0f,
    val voltage: Float = 0f,
    val current: Float = 0f,
    val temperature: Float = 0f,
    val odometer: Float = 0f,
    val tripDistance: Float = 0f,
    val power: Float = 0f,
    val totalRideTime: String = "0H 0M 0S",

    // États décodés (nouveaux)
    val headlightsOn: Boolean = false,
    val neonOn: Boolean = false,
    val currentMode: RideMode = RideMode.ECO,
    val isLocked: Boolean = false,
    val leftBlinker: Boolean = false,
    val rightBlinker: Boolean = false,
    val isUnlocked: Boolean = false,  // Mode débridé
    val cruiseControl: Boolean = false,  // Régulateur
    val zeroStart: Boolean = false,  // Démarrage à zéro

    // Anciens champs (compatibilité)
    val lastUpdate: Date = Date(),
    val isConnected: Boolean = false,
    val batteryTemperature: Float = 0f,
    val errorCodes: List<Int> = emptyList(),
    val warningCodes: List<Int> = emptyList(),
    val firmwareVersion: String = "N/A",
    val bluetoothVersion: String = "N/A"
)