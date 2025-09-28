package com.ix7.tracker.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gestionnaire de logs pour debugging et monitoring
 */
class LogManager {

    private val TAG = "IX7Tracker"
    private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    data class LogEntry(
        val timestamp: String,
        val level: LogLevel,
        val message: String,
        val tag: String = ""
    )

    enum class LogLevel(val symbol: String, val color: Int) {
        DEBUG("D", 0xFF4CAF50.toInt()),    // Vert
        INFO("I", 0xFF2196F3.toInt()),     // Bleu
        WARNING("W", 0xFFFF9800.toInt()),  // Orange
        ERROR("E", 0xFFF44336.toInt())     // Rouge
    }

    /**
     * Log de debug
     */
    fun debug(message: String, tag: String = "") {
        addLog(LogLevel.DEBUG, message, tag)
        Log.d(TAG, "[$tag] $message")
    }

    /**
     * Log d'information
     */
    fun info(message: String, tag: String = "") {
        addLog(LogLevel.INFO, message, tag)
        Log.i(TAG, "[$tag] $message")
    }

    /**
     * Log d'avertissement
     */
    fun warning(message: String, tag: String = "") {
        addLog(LogLevel.WARNING, message, tag)
        Log.w(TAG, "[$tag] $message")
    }

    /**
     * Log d'erreur
     */
    fun error(message: String, tag: String = "", throwable: Throwable? = null) {
        addLog(LogLevel.ERROR, message, tag)
        if (throwable != null) {
            Log.e(TAG, "[$tag] $message", throwable)
        } else {
            Log.e(TAG, "[$tag] $message")
        }
    }

    /**
     * Log pour les données Bluetooth
     */
    fun logBluetoothData(direction: String, data: ByteArray, deviceName: String = "") {
        val hexString = data.joinToString(" ") { "%02X".format(it) }
        val message = "$direction: [$deviceName] ${data.size} bytes: $hexString"
        debug(message, "BLE")
    }

    /**
     * Log pour les événements de connexion
     */
    fun logConnectionEvent(event: String, deviceAddress: String = "") {
        val message = if (deviceAddress.isNotEmpty()) {
            "$event - $deviceAddress"
        } else {
            event
        }
        info(message, "CONNECTION")
    }

    /**
     * Log pour les données du scooter
     */
    fun logScooterData(speed: Float, battery: Float, voltage: Float) {
        val message = "Données reçues - Vitesse: ${speed}km/h, Batterie: ${battery}%, Tension: ${voltage}V"
        debug(message, "SCOOTER")
    }

    /**
     * Ajoute un log à la liste
     */
    private fun addLog(level: LogLevel, message: String, tag: String) {
        val timestamp = timeFormatter.format(Date())
        val entry = LogEntry(timestamp, level, message, tag)

        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, entry) // Ajouter en tête

        // Limiter à 100 entrées
        if (currentLogs.size > 100) {
            currentLogs.removeAt(currentLogs.size - 1)
        }

        _logs.value = currentLogs
    }

    /**
     * Efface tous les logs
     */
    fun clearLogs() {
        _logs.value = emptyList()
        Log.d(TAG, "Logs effacés")
    }

    /**
     * Exporte les logs en format texte
     */
    fun exportLogs(): String {
        val logs = _logs.value
        if (logs.isEmpty()) return "Aucun log disponible"

        val builder = StringBuilder()
        builder.appendLine("=== IX7TrackerPro - Export des logs ===")
        builder.appendLine("Date d'export: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}")
        builder.appendLine("Nombre d'entrées: ${logs.size}")
        builder.appendLine()

        logs.reversed().forEach { entry ->
            val tagPart = if (entry.tag.isNotEmpty()) "[${entry.tag}] " else ""
            builder.appendLine("${entry.timestamp} ${entry.level.symbol} $tagPart${entry.message}")
        }

        return builder.toString()
    }

    companion object {
        @Volatile
        private var INSTANCE: LogManager? = null

        fun getInstance(): LogManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LogManager().also { INSTANCE = it }
            }
        }
    }
}