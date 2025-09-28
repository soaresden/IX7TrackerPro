package com.ix7.tracker.bluetooth

import android.util.Log

/**
 * Outil de diagnostic pour analyser la communication Bluetooth avec le scooter M0Robot
 */
object BluetoothDiag {
    private const val TAG = "BluetoothDiagnostic"

    private val receivedFrames = mutableListOf<FrameInfo>()

    data class FrameInfo(
        val timestamp: Long,
        val data: ByteArray,
        val size: Int,
        val hexString: String,
        val pattern: String,
        val isValid: Boolean
    )

    /**
     * Analyse une frame reçue et fournit un diagnostic
     */
    fun analyzeFrame(data: ByteArray): DiagnosticResult {
        val timestamp = System.currentTimeMillis()
        val hexString = data.joinToString(" ") { "%02X".format(it) }
        val pattern = identifyPattern(data)
        val isValid = validateFrame(data)

        val frameInfo = FrameInfo(
            timestamp = timestamp,
            data = data,
            size = data.size,
            hexString = hexString,
            pattern = pattern,
            isValid = isValid
        )

        receivedFrames.add(frameInfo)

        // Garder seulement les 100 dernières frames
        if (receivedFrames.size > 100) {
            receivedFrames.removeAt(0)
        }

        Log.d(TAG, "Frame analysée: ${frameInfo.pattern} - Valid: ${frameInfo.isValid} - Data: ${frameInfo.hexString}")

        return DiagnosticResult(
            frameInfo = frameInfo,
            suggestions = generateSuggestions(frameInfo),
            possibleIssues = identifyIssues(frameInfo)
        )
    }

    private fun identifyPattern(data: ByteArray): String {
        return when {
            data.isEmpty() -> "EMPTY_FRAME"
            data.size == 2 && data.contentEquals(byteArrayOf(0x00, 0x01)) -> "KEEP_ALIVE"
            data.size == 2 && data.contentEquals(byteArrayOf(0x00, 0x00)) -> "NULL_RESPONSE"
            data.size == 4 && data[2] == 0xFF.toByte() && data[3] == 0xFF.toByte() -> "DIAGNOSTIC_FRAME"
            data.size == 8 && data[0] == 0x08.toByte() -> "M0ROBOT_MAIN_DATA"
            data.size == 16 && data[0] == 0x5A.toByte() -> "M0ROBOT_EXTENDED_DATA"
            data.all { it == 0.toByte() } -> "ALL_ZEROS"
            data.size in 1..3 -> "SHORT_RESPONSE"
            data.size > 20 -> "LONG_FRAME"
            else -> "UNKNOWN_PATTERN"
        }
    }

    private fun validateFrame(data: ByteArray): Boolean {
        return when {
            data.isEmpty() -> false
            data.size == 2 && data.contentEquals(byteArrayOf(0x00, 0x01)) -> true // Keep-alive valide
            data.size == 8 && data[0] == 0x08.toByte() -> validateM0RobotMainFrame(data)
            data.size == 16 && data[0] == 0x5A.toByte() -> validateM0RobotExtendedFrame(data)
            else -> data.any { it != 0.toByte() } // Au moins un byte non-nul
        }
    }

    private fun validateM0RobotMainFrame(data: ByteArray): Boolean {
        if (data.size != 8) return false

        // Vérifier que les valeurs sont dans des plages raisonnables
        val speed = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
        val battery = data[2].toUByte().toInt()

        return speed in 0..8000 && battery in 0..100
    }

    private fun validateM0RobotExtendedFrame(data: ByteArray): Boolean {
        if (data.size != 16 || data[0] != 0x5A.toByte()) return false

        // Vérifier checksum simple si présent
        val checksum = data[15].toUByte().toInt()
        return checksum != 0 // Checksum basique
    }

    private fun generateSuggestions(frameInfo: FrameInfo): List<String> {
        val suggestions = mutableListOf<String>()

        when (frameInfo.pattern) {
            "EMPTY_FRAME" -> suggestions.add("Frame vide - vérifier la connexion")
            "ALL_ZEROS" -> suggestions.add("Données nulles - scooter peut-être en veille")
            "KEEP_ALIVE" -> suggestions.add("Signal de maintien de connexion - normal")
            "UNKNOWN_PATTERN" -> suggestions.add("Pattern inconnu - analyser le protocole")
            "SHORT_RESPONSE" -> suggestions.add("Réponse courte - peut être un accusé de réception")
        }

        if (!frameInfo.isValid) {
            suggestions.add("Frame invalide - vérifier les commandes envoyées")
        }

        if (frameInfo.size > 16) {
            suggestions.add("Frame très longue - possiblement corrompue")
        }

        return suggestions
    }

    private fun identifyIssues(frameInfo: FrameInfo): List<String> {
        val issues = mutableListOf<String>()

        // Analyser les patterns récents
        val recentFrames = receivedFrames.takeLast(10)
        val emptyFrames = recentFrames.count { it.data.isEmpty() }
        val nullFrames = recentFrames.count { it.data.all { byte -> byte == 0.toByte() } }
        val validFrames = recentFrames.count { it.isValid }

        if (emptyFrames > 5) {
            issues.add("Trop de frames vides - problème de connexion")
        }

        if (nullFrames > 3) {
            issues.add("Trop de données nulles - scooter non réactif")
        }

        if (validFrames == 0 && recentFrames.size >= 5) {
            issues.add("Aucune frame valide récente - protocole incompatible")
        }

        return issues
    }

    /**
     * Génère un rapport de diagnostic complet
     */
    fun generateDiagnosticReport(): DiagnosticReport {
        val totalFrames = receivedFrames.size
        val validFrames = receivedFrames.count { it.isValid }
        val patternStats = receivedFrames.groupBy { it.pattern }.mapValues { it.value.size }

        val recommendations = mutableListOf<String>()

        if (totalFrames == 0) {
            recommendations.add("Aucune donnée reçue - vérifier la connexion Bluetooth")
        } else if (validFrames == 0) {
            recommendations.add("Aucune frame valide - protocole incompatible ou scooter non supporté")
        } else if (validFrames < totalFrames * 0.5) {
            recommendations.add("Faible taux de frames valides - connexion instable")
        }

        return DiagnosticReport(
            totalFrames = totalFrames,
            validFrames = validFrames,
            validityRate = if (totalFrames > 0) validFrames.toFloat() / totalFrames else 0f,
            patternStats = patternStats,
            recommendations = recommendations,
            lastFrames = receivedFrames.takeLast(5)
        )
    }

    data class DiagnosticResult(
        val frameInfo: FrameInfo,
        val suggestions: List<String>,
        val possibleIssues: List<String>
    )

    data class DiagnosticReport(
        val totalFrames: Int,
        val validFrames: Int,
        val validityRate: Float,
        val patternStats: Map<String, Int>,
        val recommendations: List<String>,
        val lastFrames: List<FrameInfo>
    )

    /**
     * Reset du diagnostic
     */
    fun reset() {
        receivedFrames.clear()
        Log.i(TAG, "Diagnostic reset")
    }
}