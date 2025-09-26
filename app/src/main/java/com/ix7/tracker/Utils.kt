package com.ix7.tracker

import java.text.SimpleDateFormat
import java.util.*

object Utils {
    fun estimateDistance(rssi: Int): String {
        return when {
            rssi > -50 -> "< 1m"
            rssi > -60 -> "1-3m"
            rssi > -70 -> "3-8m"
            rssi > -80 -> "8-15m"
            else -> "> 15m"
        }
    }

    fun isMQRobot(deviceName: String): Boolean {
        return deviceName.contains("MQRobot", ignoreCase = true) ||
                deviceName.contains("M0Robot", ignoreCase = true)
    }

    fun getCurrentTime(): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }

    fun formatLog(message: String): String {
        return "[${getCurrentTime()}] $message"
    }
}