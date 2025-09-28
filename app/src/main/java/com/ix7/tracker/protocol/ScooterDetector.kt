package com.ix7.tracker.protocol

import com.ix7.tracker.core.ScooterType
import com.ix7.tracker.core.ScooterPrefixes
import android.util.Log

/**
 * Détecteur intelligent de scooters M0Robot et similaires
 */
object ScooterDetector {

    private const val TAG = "ScooterDetector"

    /**
     * Vérifie si un nom d'appareil correspond à un scooter supporté
     */
    fun isScooterDevice(deviceName: String?): Boolean {
        if (deviceName.isNullOrBlank()) return false

        val isScooter = ScooterPrefixes.SUPPORTED_PREFIXES.any { prefix ->
            deviceName.startsWith(prefix, ignoreCase = true)
        }

        if (isScooter) {
            Log.d(TAG, "Scooter détecté: $deviceName")
        }

        return isScooter
    }

    /**
     * Détermine le type de protocole selon le nom du scooter
     */
    fun detectScooterType(deviceName: String?): ScooterType {
        if (deviceName.isNullOrBlank()) return ScooterType.UNKNOWN

        val type = when {
            // Type 2 - Protocole Nordic UART
            deviceName.startsWith("MQRobot", ignoreCase = true) -> ScooterType.TYPE_2
            deviceName.startsWith("Nordic", ignoreCase = true) -> ScooterType.TYPE_2

            // Type 3 - Protocole AE00 (certains M6)
            deviceName.startsWith("M6", ignoreCase = true) -> ScooterType.TYPE_3
            deviceName.startsWith("A6", ignoreCase = true) -> ScooterType.TYPE_3

            // Type 4 - Protocole FFF0
            deviceName.startsWith("MAX", ignoreCase = true) -> ScooterType.TYPE_4
            deviceName.startsWith("NEXRIDE", ignoreCase = true) -> ScooterType.TYPE_4

            // Type 1 - Protocole standard (FFE0) - Par défaut pour M0, H1, etc.
            deviceName.startsWith("M0", ignoreCase = true) -> ScooterType.TYPE_1
            deviceName.startsWith("H1", ignoreCase = true) -> ScooterType.TYPE_1
            deviceName.startsWith("Mini", ignoreCase = true) -> ScooterType.TYPE_1
            deviceName.startsWith("Plus", ignoreCase = true) -> ScooterType.TYPE_1

            // Défaut pour tous les autres scooters reconnus
            isScooterDevice(deviceName) -> ScooterType.TYPE_1

            else -> ScooterType.UNKNOWN
        }

        Log.d(TAG, "Type détecté pour '$deviceName': ${type.description}")
        return type
    }

    /**
     * Estime la distance basée sur le RSSI
     */
    fun estimateDistance(rssi: Int): String {
        return when {
            rssi > -40 -> "< 0.5m"
            rssi > -50 -> "< 1m"
            rssi > -60 -> "1-3m"
            rssi > -70 -> "3-8m"
            rssi > -80 -> "8-15m"
            rssi > -90 -> "15-30m"
            else -> "> 30m"
        }
    }

    /**
     * Évalue la qualité du signal
     */
    fun getSignalQuality(rssi: Int): String {
        return when {
            rssi > -50 -> "Excellent"
            rssi > -60 -> "Bon"
            rssi > -70 -> "Moyen"
            rssi > -80 -> "Faible"
            else -> "Très faible"
        }
    }

    /**
     * Détermine si le RSSI est suffisant pour une connexion stable
     */
    fun isSignalStrongEnough(rssi: Int): Boolean {
        return rssi > -85 // Seuil conservateur pour une connexion stable
    }
}