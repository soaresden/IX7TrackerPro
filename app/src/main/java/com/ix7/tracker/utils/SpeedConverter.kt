package com.ix7.tracker.utils

import com.ix7.tracker.core.SpeedUnit

/**
 * 🚀 CONVERTISSEUR DE VITESSE
 *
 * Centralise TOUTES les conversions KMH ↔ MPH
 * Élimine la duplication dans RideScreen
 *
 * AVANT: Logique dupliquée partout
 *   val displaySpeed = if (speedUnit == SpeedUnit.MPH) {
 *       (currentSpeed * 0.621371).toInt()
 *   } else {
 *       currentSpeed
 *   }
 *
 * APRÈS: Une fonction réutilisable
 *   val displaySpeed = SpeedConverter.convertCurrentSpeed(speed, speedUnit)
 */
object SpeedConverter {

    // ═══════════════════════════════════════════════════════════════
    // 📊 CONSTANTES DE CONVERSION
    // ═══════════════════════════════════════════════════════════════

    /** Facteur de conversion KMH → MPH */
    private const val KMH_TO_MPH = 0.621371

    /** Facteur de conversion MPH → KMH */
    private const val MPH_TO_KMH = 1.60934

    // ═══════════════════════════════════════════════════════════════
    // 🎯 CONVERSIONS DE VITESSE ACTUELLE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Convertit la vitesse actuelle du scooter
     *
     * @param speedKmh Vitesse en km/h depuis le scooter
     * @param unit L'unité désirée pour l'affichage
     * @return Vitesse convertie et arrondie
     *
     * Exemple:
     *   convertCurrentSpeed(30, KMH) = 30
     *   convertCurrentSpeed(30, MPH) = 18
     */
    fun convertCurrentSpeed(speedKmh: Int, unit: SpeedUnit): Int {
        return when (unit) {
            SpeedUnit.KMH -> speedKmh
            SpeedUnit.MPH -> (speedKmh * KMH_TO_MPH).toInt()
        }
    }

    /**
     * Variant Float (pour plus de précision)
     */
    fun convertCurrentSpeedFloat(speedKmh: Float, unit: SpeedUnit): Float {
        return when (unit) {
            SpeedUnit.KMH -> speedKmh
            SpeedUnit.MPH -> speedKmh * KMH_TO_MPH.toFloat()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎯 CONVERSIONS DE VITESSE MAXIMALE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Convertit la vitesse maximale
     *
     * @param maxSpeedKmh Vitesse maximale en km/h
     * @param unit L'unité désirée pour l'affichage
     * @return Vitesse maximale convertie
     *
     * Exemple:
     *   convertMaxSpeed(50, KMH) = 50
     *   convertMaxSpeed(50, MPH) = 31
     */
    fun convertMaxSpeed(maxSpeedKmh: Int, unit: SpeedUnit): Int {
        return when (unit) {
            SpeedUnit.KMH -> maxSpeedKmh
            SpeedUnit.MPH -> (maxSpeedKmh * KMH_TO_MPH).toInt()
        }
    }

    /**
     * Variant Float
     */
    fun convertMaxSpeedFloat(maxSpeedKmh: Float, unit: SpeedUnit): Float {
        return when (unit) {
            SpeedUnit.KMH -> maxSpeedKmh
            SpeedUnit.MPH -> maxSpeedKmh * KMH_TO_MPH.toFloat()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎯 CONVERSIONS DE SEUIL DE RÉGULATEUR
    // ═══════════════════════════════════════════════════════════════

    /**
     * Convertit le seuil du régulateur
     *
     * Le régulateur est toujours en KMH en backend,
     * mais l'affichage peut être en MPH
     *
     * @param thresholdKmh Seuil en km/h
     * @param unit L'unité désirée pour l'affichage
     * @return Seuil converti
     *
     * Exemple:
     *   convertCruiseThreshold(25, KMH) = 25
     *   convertCruiseThreshold(25, MPH) = 15.5
     */
    fun convertCruiseThreshold(thresholdKmh: Float, unit: SpeedUnit): Float {
        return when (unit) {
            SpeedUnit.KMH -> thresholdKmh
            SpeedUnit.MPH -> thresholdKmh * KMH_TO_MPH.toFloat()
        }
    }

    /**
     * Inverse: Convertit de l'unité affichée vers KMH pour l'envoi au BT
     *
     * @param displayValue Valeur affichée à l'utilisateur
     * @param unit L'unité actuelle
     * @return Valeur en KMH pour l'envoi Bluetooth
     *
     * Exemple:
     *   convertToKmh(15.5, MPH) = 25
     *   convertToKmh(25, KMH) = 25
     */
    fun convertToKmh(displayValue: Float, unit: SpeedUnit): Float {
        return when (unit) {
            SpeedUnit.KMH -> displayValue
            SpeedUnit.MPH -> displayValue * MPH_TO_KMH.toFloat()
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎯 UTILITAIRES D'AFFICHAGE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Crée une chaîne formatée pour l'affichage
     *
     * @param speedKmh Vitesse en km/h
     * @param unit L'unité désirée
     * @return String au format "30 km/h" ou "18 mph"
     *
     * Exemple:
     *   formatSpeed(30, KMH) = "30 km/h"
     *   formatSpeed(30, MPH) = "18 mph"
     */
    fun formatSpeed(speedKmh: Int, unit: SpeedUnit): String {
        val converted = convertCurrentSpeed(speedKmh, unit)
        val suffix = when (unit) {
            SpeedUnit.KMH -> "km/h"
            SpeedUnit.MPH -> "mph"
        }
        return "$converted $suffix"
    }

    /**
     * Crée une chaîne formatée pour l'affichage (Float version)
     */
    fun formatSpeedFloat(speedKmh: Float, unit: SpeedUnit, decimals: Int = 1): String {
        val converted = convertCurrentSpeedFloat(speedKmh, unit)
        val formatted = String.format("%.${decimals}f", converted)
        val suffix = when (unit) {
            SpeedUnit.KMH -> "km/h"
            SpeedUnit.MPH -> "mph"
        }
        return "$formatted $suffix"
    }

    /**
     * Obtient le symbole de l'unité
     */
    fun getUnitSymbol(unit: SpeedUnit): String = when (unit) {
        SpeedUnit.KMH -> "km/h"
        SpeedUnit.MPH -> "mph"
    }

    /**
     * Obtient le nom complet de l'unité
     */
    fun getUnitName(unit: SpeedUnit): String = when (unit) {
        SpeedUnit.KMH -> "Kilomètres/heure"
        SpeedUnit.MPH -> "Miles/heure"
    }

    // ═══════════════════════════════════════════════════════════════
    // 🆘 UTILITAIRES AVANCÉS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Formate une autonomie estimée
     *
     * @param kmhRange Autonomie en km
     * @param unit L'unité désirée
     * @return "50 km" ou "31 miles"
     */
    fun formatRange(kmhRange: Int, unit: SpeedUnit): String {
        val converted = when (unit) {
            SpeedUnit.KMH -> kmhRange
            SpeedUnit.MPH -> (kmhRange * KMH_TO_MPH).toInt()
        }
        val suffix = when (unit) {
            SpeedUnit.KMH -> "km"
            SpeedUnit.MPH -> "miles"
        }
        return "$converted $suffix"
    }

    /**
     * Estime le temps restant
     *
     * @param autonomyKm Autonomie en km
     * @param speedKmh Vitesse actuelle en km/h
     * @return Temps en minutes
     */
    fun estimateTimeRemaining(autonomyKm: Int, speedKmh: Int): Int {
        return if (speedKmh > 0) {
            ((autonomyKm.toFloat() / speedKmh) * 60).toInt()
        } else {
            0
        }
    }

    /**
     * Estime la consommation énergétique
     * (Watts par kilomètre)
     *
     * @param powerW Puissance en Watts
     * @param speedKmh Vitesse en km/h
     * @return Consommation en W/km
     */
    fun estimateConsumption(powerW: Int, speedKmh: Int): Float {
        return if (speedKmh > 0) {
            powerW.toFloat() / speedKmh
        } else {
            0f
        }
    }
}