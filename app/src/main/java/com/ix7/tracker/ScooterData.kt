package com.ix7.tracker

data class ScooterData(
    // État de connexion
    val isConnected: Boolean = false,

    // Informations générales
    val model: String = "IX7 Pro",
    val firmwareVersion: String = "N/A",
    val bluetoothVersion: String = "N/A",
    val appVersion: String = "11.2.9",

    // Distance et temps
    val totalDistance: Double = 0.0, // Kilométrage total en km
    val ridingTime: String = "0H 0M 0S", // Temps de conduite total

    // Vitesse
    val currentSpeed: Double = 0.0, // Vitesse actuelle en km/h

    // Batterie et puissance
    val batteryLevel: Int = 0, // Puissance restante en %
    val batteryTemperature: Int = 0, // Température de la batterie en °C
    val batteryStatus: Int = 0, // État de la batterie
    val current: Double = 0.0, // Courant en A
    val voltage: Double = 0.0, // Tension en V
    val power: Double = 0.0, // Puissance en W

    // Températures
    val scooterTemperature: Double = 0.0, // Température du scooter en °C

    // Codes d'erreur
    val errorCode: Int = 0, // Codes d'erreur
    val warningCode: Int = 0, // Code d'avertissement

    // Versions
    val electricVersion: String = "N/A", // Version électrique

    // Timestamp
    val timestamp: Long = System.currentTimeMillis()
)