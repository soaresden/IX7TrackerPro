package com.ix7.tracker

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Gestionnaire de logs pour l'application IX7 Tracker
 */
class LogManager {
    private val TAG = "IX7Tracker"
    private val MAX_LOGS = 100

    // StateFlow pour observer les logs dans l'UI
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    // Instance singleton
    companion object {
        @Volatile
        private var INSTANCE: LogManager? = null

        fun getInstance(): LogManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LogManager().also { INSTANCE = it }
            }
        }
    }

    /**
     * Ajoute un log d'information
     */
    fun logInfo(tag: String, message: String) {
        addLog(LogLevel.INFO, tag, message)
    }

    /**
     * Ajoute un log de debug
     */
    fun logDebug(tag: String, message: String) {
        addLog(LogLevel.DEBUG, tag, message)
    }

    /**
     * Ajoute un log d'avertissement
     */
    fun logWarning(tag: String, message: String) {
        addLog(LogLevel.WARNING, tag, message)
    }

    /**
     * Ajoute un log d'erreur
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        addLog(LogLevel.ERROR, tag, fullMessage)
    }

    /**
     * Ajoute un log de données Bluetooth
     */
    fun logBluetoothData(direction: String, data: ByteArray, description: String = "") {
        val hexData = data.joinToString(" ") { "%02x".format(it) }
        val message = "$direction: $hexData ${if (description.isNotEmpty()) "($description)" else ""}"
        addLog(LogLevel.BLUETOOTH, "BLE_DATA", message)
    }

    /**
     * Ajoute un log de connexion Bluetooth
     */
    fun logBluetoothConnection(event: String, deviceName: String?, deviceAddress: String?) {
        val message = "$event - Device: ${deviceName ?: "Unknown"} (${deviceAddress ?: "Unknown"})"
        addLog(LogLevel.BLUETOOTH, "BLE_CONNECTION", message)
    }

    /**
     * Méthode privée pour ajouter un log
     */
    private fun addLog(level: LogLevel, tag: String, message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logEntry = LogEntry(
            timestamp = timestamp,
            level = level,
            tag = tag,
            message = message
        )

        // Ajouter au Logcat Android
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARNING -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
            LogLevel.BLUETOOTH -> Log.v(tag, message)
        }

        // Ajouter à la liste interne
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, logEntry) // Ajouter en haut de la liste

        // Garder seulement les MAX_LOGS derniers
        if (currentLogs.size > MAX_LOGS) {
            currentLogs.removeAt(currentLogs.size - 1)
        }

        _logs.value = currentLogs
    }

    /**
     * Efface tous les logs
     */
    fun clearLogs() {
        _logs.value = emptyList()
        Log.i(TAG, "Logs cleared")
    }

    /**
     * Exporte les logs sous forme de texte
     */
    fun exportLogs(): String {
        return _logs.value.joinToString("\n") { logEntry ->
            "[${logEntry.timestamp}] ${logEntry.level.displayName}/${logEntry.tag}: ${logEntry.message}"
        }
    }

    /**
     * Filtre les logs par niveau
     */
    fun getLogsByLevel(level: LogLevel): List<LogEntry> {
        return _logs.value.filter { it.level == level }
    }

    /**
     * Filtre les logs par tag
     */
    fun getLogsByTag(tag: String): List<LogEntry> {
        return _logs.value.filter { it.tag == tag }
    }
}

/**
 * Représente une entrée de log
 */
data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

/**
 * Niveaux de log disponibles
 */
enum class LogLevel(val displayName: String, val priority: Int) {
    DEBUG("DEBUG", 0),
    INFO("INFO", 1),
    WARNING("WARN", 2),
    ERROR("ERROR", 3),
    BLUETOOTH("BLE", 1)
}