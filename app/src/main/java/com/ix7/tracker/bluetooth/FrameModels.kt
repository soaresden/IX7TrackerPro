package com.ix7.tracker.bluetooth

import androidx.compose.ui.graphics.Color

/**
 * Modèles de données pour le monitoring des trames Bluetooth
 * Version centralisée - 14 octobre 2025
 */

/**
 * Représente une trame Bluetooth monitorée
 */
data class FrameMonitor(
    val type: String,                           // Type de trame (ex: "0x02")
    val size: Int,                              // Taille en bytes
    val hex: String,                            // Représentation hexadécimale
    val count: Int,                             // Nombre de fois reçue
    val lastUpdate: Long,                       // Timestamp dernière mise à jour
    val decoded: Map<String, DecodedValue>      // Valeurs décodées
)

/**
 * Représente une valeur décodée d'une trame
 */
data class DecodedValue(
    val label: String,          // Label (ex: "Batterie")
    val value: String,          // Valeur formatée (ex: "66%")
    val validated: Boolean,     // Est-ce que la valeur est validée ?
    val color: Color            // Couleur pour l'affichage
)