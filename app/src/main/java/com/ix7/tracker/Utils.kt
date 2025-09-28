package com.ix7.tracker

import java.text.SimpleDateFormat
import java.util.*

object Utils {

    /**
     * Estime la distance approximative basée sur la force du signal RSSI
     */
    fun estimateDistance(rssi: Int): String {
        return when {
            rssi > -50 -> "< 1m"
            rssi > -60 -> "1-3m"
            rssi > -70 -> "3-8m"
            rssi > -80 -> "8-15m"
            else -> "> 15m"
        }
    }

    /**
     * Vérifie si le nom de l'appareil correspond à un scooter M0Robot
     */
    fun isMQRobot(deviceName: String): Boolean {
        return deviceName.contains("MQRobot", ignoreCase = true) ||
                deviceName.contains("M0Robot", ignoreCase = true) ||
                deviceName.startsWith("M0", ignoreCase = true) ||
                deviceName.startsWith("H1", ignoreCase = true) ||
                deviceName.startsWith("M1", ignoreCase = true) ||
                deviceName.startsWith("Mini", ignoreCase = true) ||
                deviceName.startsWith("Plus", ignoreCase = true) ||
                deviceName.startsWith("X1", ignoreCase = true) ||
                deviceName.startsWith("X3", ignoreCase = true) ||
                deviceName.startsWith("M6", ignoreCase = true) ||
                deviceName.startsWith("GoKart", ignoreCase = true) ||
                deviceName.startsWith("A6", ignoreCase = true)
    }

    /**
     * Obtient l'heure actuelle formatée
     */
    fun getCurrentTime(): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }

    /**
     * Formate un message de log avec timestamp
     */
    fun formatLog(message: String): String {
        return "[${getCurrentTime()}] $message"
    }

    /**
     * Parse le temps total au format "164H 35M 0S" et le convertit en heures décimales
     */
    fun parseTotalHours(timeString: String): Float {
        return try {
            val hourMatch = Regex("""(\d+)H""").find(timeString)
            val minuteMatch = Regex("""(\d+)M""").find(timeString)
            val secondMatch = Regex("""(\d+)S""").find(timeString)

            val hours = hourMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
            val minutes = minuteMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
            val seconds = secondMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

            hours + (minutes / 60f) + (seconds / 3600f)
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * Détermine le type de scooter basé sur les données brutes du protocole
     */
    fun getScooterType(rawData: ByteArray?): String {
        return when {
            rawData == null -> "Inconnu"
            rawData.size >= 2 && rawData[0] == 0x55.toByte() -> "M0Robot v2"
            rawData.size >= 2 && rawData[0] == 0xAA.toByte() -> "M0Robot v1"
            else -> "Type non reconnu"
        }
    }

    /**
     * Formate une valeur en pourcentage avec une décimale
     */
    fun formatPercentage(value: Float): String {
        return "${String.format("%.1f", value)}%"
    }

    /**
     * Formate une valeur en volts avec une décimale
     */
    fun formatVoltage(value: Float): String {
        return "${String.format("%.1f", value)}V"
    }

    /**
     * Formate une valeur en ampères avec une décimale
     */
    fun formatCurrent(value: Float): String {
        return "${String.format("%.1f", value)}A"
    }

    /**
     * Formate une valeur en watts avec une décimale
     */
    fun formatPower(value: Float): String {
        return "${String.format("%.1f", value)}W"
    }

    /**
     * Formate une distance en kilomètres avec une décimale
     */
    fun formatDistance(value: Float): String {
        return "${String.format("%.1f", value)} km"
    }

    /**
     * Formate une vitesse en km/h avec une décimale
     */
    fun formatSpeed(value: Float): String {
        return "${String.format("%.1f", value)} km/h"
    }

    /**
     * Calcule l'économie estimée basée sur le kilométrage
     * @param odometer kilométrage total en km
     * @param costPerKm coût économisé par kilomètre (défaut 0.15€)
     * @return économie formatée en euros
     */
    fun calculateEstimatedSavings(odometer: Float, costPerKm: Float = 0.15f): String {
        val savings = odometer * costPerKm
        return "${String.format("%.2f", savings)}€"
    }

    /**
     * Calcule la vitesse moyenne basée sur la distance et le temps
     * @param distance distance totale en km
     * @param totalHours temps total en heures
     * @return vitesse moyenne en km/h
     */
    fun calculateAverageSpeed(distance: Float, totalHours: Float): Float {
        return if (totalHours > 0) distance / totalHours else 0f
    }

    /**
     * Calcule la consommation moyenne en W/km
     * @param power puissance actuelle en watts
     * @param distance distance totale en km
     * @return consommation en W/km
     */
    fun calculateConsumption(power: Float, distance: Float): Float {
        return if (distance > 0) power / distance else 0f
    }

    /**
     * Convertit un tableau de bytes en string hexadécimal pour le debug
     */
    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02x".format(it) }
    }

    /**
     * Vérifie si les données semblent être du texte
     */
    fun isTextData(data: ByteArray): Boolean {
        return data.all { it in 32..126 } && data.size > 4
    }

    /**
     * Formate une température en Celsius
     */
    fun formatTemperature(value: Int): String {
        return "${value}°C"
    }

    /**
     * Formate un temps de conduite depuis les heures décimales
     */
    fun formatRideTime(totalHours: Float): String {
        val hours = totalHours.toInt()
        val minutes = ((totalHours - hours) * 60).toInt()
        val seconds = ((((totalHours - hours) * 60) - minutes) * 60).toInt()
        return "${hours}H ${minutes}M ${seconds}S"
    }

    /**
     * Détermine la couleur de la batterie selon le niveau
     */
    fun getBatteryColor(batteryLevel: Int): String {
        return when {
            batteryLevel > 50 -> "#4CAF50" // Vert
            batteryLevel > 20 -> "#FF9800" // Orange
            else -> "#F44336" // Rouge
        }
    }

    /**
     * Calcule l'autonomie restante approximative
     * @param batteryLevel niveau de batterie en %
     * @param totalRange autonomie totale du scooter en km
     * @return autonomie restante en km
     */
    fun calculateRemainingRange(batteryLevel: Int, totalRange: Float = 30f): Float {
        return (batteryLevel / 100f) * totalRange
    }
}