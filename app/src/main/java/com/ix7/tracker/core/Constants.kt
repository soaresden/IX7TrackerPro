package com.ix7.tracker.core

/**
 * Constantes du protocole M0Robot - CORRIGÉ
 * Basé sur l'analyse approfondie des logs et du manuel
 */
object ProtocolConstants {

    // ===== HEADERS DE TRAME =====
    const val FRAME_HEADER_1 = 0x55.toByte()  // Premier byte: 0x55
    const val FRAME_HEADER_2 = 0xAA.toByte()  // Second byte: 0xAA

    // ===== TAILLES DE TRAMES =====
    const val MIN_FRAME_SIZE = 5        // Minimum: 55 AA length cmd checksum
    const val MAX_FRAME_SIZE = 100      // Maximum observé (avec marge)
    const val SHORT_FRAME_SIZE = 10     // Trame courte typique
    const val MEDIUM_FRAME_SIZE = 20    // Trame moyenne
    const val LONG_FRAME_SIZE = 60      // Trame longue complète (ODOMÈTRE ICI)

    // ===== COMMANDES =====
    const val CMD_REQUEST_DATA = 0x22.toByte()      // Demande données (commande principale)
    const val CMD_STATUS = 0x20.toByte()            // Demande statut
    const val CMD_SET_PARAMETER = 0x02.toByte()     // Modifier paramètre
    const val CMD_KEEP_ALIVE = 0x00.toByte()        // Keep-alive
    const val CMD_GET_VERSION = 0x03.toByte()       // Version firmware

    // ===== STRUCTURE DES TRAMES =====
    // Format: 55 AA [length] [command] [subcommand] [data...] [checksum]
    const val OFFSET_HEADER_1 = 0
    const val OFFSET_HEADER_2 = 1
    const val OFFSET_LENGTH = 2
    const val OFFSET_COMMAND = 3
    const val OFFSET_SUBCOMMAND = 4
    const val OFFSET_DATA_START = 5

    // ===== OFFSETS DANS LES TRAMES LONGUES (60 bytes) =====
    // Ces offsets sont CONFIRMÉS par l'analyse des logs
    const val OFFSET_TEMPERATURE = 5        // Température moteur en °C (byte unique)
    const val OFFSET_BATTERY = 22           // Batterie en % (byte unique)

    // Ces offsets sont À CONFIRMER lors des tests
    const val OFFSET_SPEED_START = 6        // Vitesse (2 bytes, little-endian, en cm/s)
    const val OFFSET_VOLTAGE_START = 14     // Voltage (2 bytes, little-endian, en dixièmes de volt)
    const val OFFSET_CURRENT_START = 16     // Courant (2 bytes, peut être signé)

    // ODOMÈTRE: Position à détecter dynamiquement (recherche 356km = 35600)
    // Généralement entre offsets 24 et 40 (4 bytes, little-endian, en décamètres)
    const val OFFSET_ODOMETER_SEARCH_START = 24
    const val OFFSET_ODOMETER_SEARCH_END = 45

    // TEMPS TOTAL: Position à détecter (recherche ~10898 minutes)
    const val OFFSET_TIME_SEARCH_START = 30
    const val OFFSET_TIME_SEARCH_END = 50

    // ===== VALEURS OBSERVÉES =====
    const val RESPONSE_ID1 = 0x7E.toByte()  // Identifiant dans réponses courtes
    const val RESPONSE_ID2 = 0x03.toByte()  // Second identifiant
}

/**
 * Seuils de température - BASÉ SUR LE MANUEL
 * Manuel page 7: E55 = Controller high temperature alarm
 */
object TemperatureThresholds {
    // Température moteur/contrôleur
    const val MOTOR_NORMAL = 50f        // °C - Température normale max
    const val MOTOR_WARNING = 60f       // °C - Seuil d'avertissement (voyant orange)
    const val MOTOR_CRITICAL = 70f      // °C - Seuil critique (voyant rouge + alerte sonore)
    const val MOTOR_SHUTDOWN = 80f      // °C - Arrêt automatique probable

    // Température batterie
    const val BATTERY_NORMAL = 40f      // °C - Température batterie normale max
    const val BATTERY_WARNING = 45f     // °C - Seuil d'avertissement batterie
    const val BATTERY_CRITICAL = 55f    // °C - Seuil critique batterie (E58)
    const val BATTERY_SHUTDOWN = 65f    // °C - Arrêt automatique
}

/**
 * Limites de sécurité
 */
object SafetyLimits {
    // Vitesse
    const val MAX_SPEED_KMH = 50f       // Vitesse max réaliste
    const val MIN_SPEED_KMH = 0f

    // Batterie
    const val MAX_BATTERY_PERCENT = 100f
    const val MIN_BATTERY_PERCENT = 0f
    const val LOW_BATTERY_THRESHOLD = 20f
    const val CRITICAL_BATTERY_THRESHOLD = 10f

    // Voltage (pour hoverboard 36V nominal)
    const val NOMINAL_VOLTAGE = 36f
    const val MAX_VOLTAGE = 42f         // Batterie pleine
    const val MIN_VOLTAGE = 30f         // Batterie vide
    const val CRITICAL_VOLTAGE = 32f    // Seuil critique

    // Courant
    const val MAX_CURRENT_AMPS = 20f    // Courant max en charge

    // Odomètre
    const val MAX_ODOMETER_KM = 100000f // Limite réaliste
}

/**
 * Préfixes des noms de scooters supportés
 */
object ScooterPrefixes {
    val SUPPORTED_PREFIXES = listOf(
        // Hoverboards/Balance cars
        "Loby", "Balance", "Car",

        // Scooters M0Robot
        "M0", "H1", "M1", "Mini", "Plus", "X1", "X3", "M6",
        "GoKart", "A6", "MI", "N3MTenbot", "miniPLUS_",
        "MiniPro", "SFSO", "V5Robot", "EO STREET", "XRIDER",
        "TECAR", "MAX", "i10", "NEXRIDE", "E-WHEELS", "E12",
        "E9PRO", "T10", "MQRobot", "KING", "oP9G", "Mentor", "E9G"
    )

    /**
     * Vérifie si un nom de device correspond aux préfixes supportés
     */
    fun isSupported(deviceName: String?): Boolean {
        if (deviceName == null) return false
        return SUPPORTED_PREFIXES.any { deviceName.startsWith(it, ignoreCase = true) }
    }

    /**
     * Détecte le type de scooter
     */
    fun detectType(deviceName: String?): ScooterType {
        if (deviceName == null) return ScooterType.UNKNOWN

        return when {
            deviceName.startsWith("M0", ignoreCase = true) -> ScooterType.M0ROBOT
            deviceName.contains("Loby", ignoreCase = true) -> ScooterType.HOVERBOARD
            deviceName.contains("Balance", ignoreCase = true) -> ScooterType.HOVERBOARD
            deviceName.startsWith("M1", ignoreCase = true) -> ScooterType.SCOOTER_M1
            deviceName.startsWith("X", ignoreCase = true) -> ScooterType.SCOOTER_X_SERIES
            else -> ScooterType.GENERIC
        }
    }
}

/**
 * Types de scooters supportés
 */
// Dans ScooterType enum
enum class ScooterType(val description: String) {
    UNKNOWN("Appareil inconnu"),
    M0ROBOT("M0Robot Scooter"),
    HOVERBOARD("Hoverboard/Balance Car"),
    SCOOTER_M1("Scooter M1"),
    SCOOTER_X_SERIES("Scooter Série X"),
    GENERIC("Scooter générique"),
    TYPE_NORDIC("Nordic UART (6E40)"),
    TYPE_AE00("Type AE00"),
    TYPE_FFF0("Type FFF0"),
    TYPE_FFE0("Type FFE0/FFE1 - Protocole 55 AA")
}

/**
 * Codes d'erreur du manuel (page 7)
 */
object ErrorCodes {
    const val E53_OVERVOLTAGE = 0x53            // Over-voltage alarm
    const val E54_UNDERVOLTAGE = 0x54           // Under-voltage alarm
    const val E55_CONTROLLER_TEMP = 0x55        // Controller high temperature
    const val E56_SHORT_CIRCUIT = 0x56          // Short circuit protection
    const val E57_BRAKE_HANDLE = 0x57           // Brake handle malfunction
    const val E58_BATTERY_TEMP = 0x58           // Battery high temperature
    const val E59_MOTOR_FAULT = 0x59            // Motor malfunction

    /**
     * Retourne le message d'erreur lisible
     */
    fun getMessage(code: Int): String {
        return when (code) {
            E53_OVERVOLTAGE -> "Surtension détectée"
            E54_UNDERVOLTAGE -> "Sous-tension détectée"
            E55_CONTROLLER_TEMP -> "Contrôleur en surchauffe"
            E56_SHORT_CIRCUIT -> "Court-circuit détecté"
            E57_BRAKE_HANDLE -> "Dysfonctionnement frein"
            E58_BATTERY_TEMP -> "Batterie en surchauffe"
            E59_MOTOR_FAULT -> "Dysfonctionnement moteur"
            else -> "Erreur inconnue: $code"
        }
    }
}

/**
 * Utilitaires pour les calculs
 */
object ConversionUtils {
    /**
     * Convertit vitesse de cm/s en km/h
     */
    fun cmPerSecToKmPerHour(cmPerSec: Int): Float {
        return cmPerSec / 100f * 3.6f
    }

    /**
     * Convertit décamètres en kilomètres
     */
    fun decametersToKilometers(decameters: Int): Float {
        return decameters / 100f
    }

    /**
     * Convertit dixièmes de volt en volts
     */
    fun decivoltsToVolts(decivolts: Int): Float {
        return decivolts / 10f
    }

    /**
     * Convertit minutes en format "XH YM ZS"
     */
    fun minutesToTimeString(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}H ${minutes}M 0S"
    }
}