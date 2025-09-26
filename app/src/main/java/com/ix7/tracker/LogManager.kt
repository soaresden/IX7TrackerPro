package com.ix7.tracker

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.*

class LogManager {
    private val TAG = "IX7Tracker"

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "[$timestamp] $message"

        Log.d(TAG, message)

        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, logEntry)

        if (currentLogs.size > 50) {
            currentLogs.removeAt(currentLogs.size - 1)
        }

        _logs.value = currentLogs
    }

    fun clearLogs() {
        _logs.value = emptyList()
        Log.d(TAG, "Logs effacés")
    }
}