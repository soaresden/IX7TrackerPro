package com.ix7.tracker.utils

import com.ix7.tracker.core.SpeedUnit

/**
 * 🏃 CONVERTISSEUR DE VITESSES
 *
 * Centralise toutes les conversions KMH ↔ MPH
 * pour éliminer la duplication dans les composants
 */
object SpeedConverter {

    private const val KMH_TO_MPH = 0.621371f
    private const val MPH_TO_KMH = 1.60934f

    /**
     * Convertir la vitesse actuelle en l'unité demandée
     */
    fun convertCurrentSpeed(speedKmh: Float, targetUnit: SpeedUnit): Float {
        return when (targetUnit) {
            SpeedUnit.KMH -> speedKmh
            SpeedUnit.MPH -> speedKmh * KMH_TO_MPH
        }
    }

    /**
     * Convertir la vitesse maximale en l'unité demandée
     */
    fun convertMaxSpeed(maxSpeedKmh: Int, targetUnit: SpeedUnit): Int {
        return when (targetUnit) {
            SpeedUnit.KMH -> maxSpeedKmh
            SpeedUnit.MPH -> (maxSpeedKmh * KMH_TO_MPH).toInt()
        }
    }

    /**
     * Format de la vitesse pour l'affichage (ex: "42.3 km/h")
     */
    fun formatSpeed(speed: Float, unit: SpeedUnit): String {
        val unitStr = when (unit) {
            SpeedUnit.KMH -> "km/h"
            SpeedUnit.MPH -> "mph"
        }
        return "%.1f %s".format(speed, unitStr)
    }

    /**
     * Format de la vitesse max pour l'affichage (ex: "50 km/h")
     */
    fun formatMaxSpeed(speed: Int, unit: SpeedUnit): String {
        val unitStr = when (unit) {
            SpeedUnit.KMH -> "km/h"
            SpeedUnit.MPH -> "mph"
        }
        return "%d %s".format(speed, unitStr)
    }

    /**
     * Obtenir le symbole de l'unité
     */
    fun getUnitSymbol(unit: SpeedUnit): String {
        return when (unit) {
            SpeedUnit.KMH -> "km/h"
            SpeedUnit.MPH -> "mph"
        }
    }
}