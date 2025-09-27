package com.ix7.tracker

/**
 * Fonctions utilitaires pour l'écran d'informations
 */

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
 */
fun calculateEstimatedSavings(odometer: Float, costPerKm: Float = 0.15f): String {
    val savings = odometer * costPerKm
    return "${String.format("%.2f", savings)}€"
}

/**
 * Calcule la vitesse moyenne basée sur la distance et le temps
 */
fun calculateAverageSpeed(distance: Float, totalHours: Float): Float {
    return if (totalHours > 0) distance / totalHours else 0f
}

/**
 * Calcule la consommation moyenne en W/km
 */
fun calculateConsumption(power: Float, distance: Float): Float {
    return if (distance > 0) power / distance else 0f
}