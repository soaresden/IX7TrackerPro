package com.ix7.tracker.utils

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Utilitaires de formatage pour l'affichage des données
 */
object FormatUtils {

    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // Formatage des valeurs principales
    fun formatSpeed(speed: Float): String = "%.1f km/h".format(speed)
    fun formatBattery(battery: Float): String = "%.1f%%".format(battery)
    fun formatVoltage(voltage: Float): String = "%.1fV".format(voltage)
    fun formatCurrent(current: Float): String = "%.1fA".format(abs(current))
    fun formatPower(power: Float): String = "%.1fW".format(abs(power))
    fun formatTemperature(temp: Float): String = "%.1f°C".format(temp)
    fun formatDistance(distance: Float): String = "%.1f km".format(distance)

    /**
     * Formate le temps total de conduite
     */
    fun formatRideTime(timeString: String): String {
        return try {
            val hourMatch = Regex("""(\d+)H""").find(timeString)
            val minuteMatch = Regex("""(\d+)M""").find(timeString)
            val secondMatch = Regex("""(\d+)S""").find(timeString)

            val hours = hourMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val minutes = minuteMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val seconds = secondMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

            "${hours}H ${minutes}M ${seconds}S"
        } catch (e: Exception) {
            timeString
        }
    }

    /**
     * Convertit le temps total en heures décimales
     */
    fun parseTimeToHours(timeString: String): Float {
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
     * Calcule la vitesse moyenne
     */
    fun calculateAverageSpeed(distance: Float, totalHours: Float): Float {
        return if (totalHours > 0) distance / totalHours else 0f
    }

    /**
     * Calcule l'économie estimée
     */
    fun calculateEstimatedSavings(odometer: Float, costPerKm: Float = 0.15f): String {
        val savings = odometer * costPerKm
        return "${String.format("%.2f", savings)}€"
    }

    /**
     * Calcule la consommation moyenne en W/km
     */
    fun calculateConsumption(power: Float, distance: Float): Float {
        return if (distance > 0) power / distance else 0f
    }

    /**
     * Formate l'horodatage actuel
     */
    fun getCurrentTimestamp(): String = timeFormatter.format(Date())

    /**
     * Formate une date complète
     */
    fun formatDate(date: Date): String = dateFormatter.format(date)

    /**
     * Formate la qualité du signal
     */
    fun formatSignalStrength(rssi: Int): String {
        return when {
            rssi > -50 -> "Excellent"
            rssi > -60 -> "Bon"
            rssi > -70 -> "Moyen"
            rssi > -80 -> "Faible"
            else -> "Très faible"
        }
    }

    /**
     * Formate les codes d'erreur en texte lisible
     */
    fun formatErrorCodes(errorCode: Int): String {
        return when (errorCode) {
            0 -> "Aucune erreur"
            1 -> "Erreur batterie"
            2 -> "Erreur moteur"
            3 -> "Erreur température"
            4 -> "Erreur communication"
            else -> "Erreur inconnue ($errorCode)"
        }
    }

    /**
     * Formate les codes d'avertissement
     */
    fun formatWarningCodes(warningCode: Int): String {
        return when (warningCode) {
            0 -> "Aucun avertissement"
            1 -> "Batterie faible"
            2 -> "Température élevée"
            3 -> "Vitesse limite"
            else -> "Avertissement ($warningCode)"
        }
    }
}