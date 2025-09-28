package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.ScooterData
import java.util.*

class BluetoothDataHandler(
    private val onDataUpdate: (ScooterData) -> Unit
) {
    companion object {
        private const val TAG = "BluetoothDataHandler"
    }

    private var currentData = ScooterData()

    fun handleData(data: ByteArray) {
        try {
            val parsedData = parseData(data)
            currentData = currentData.copy(
                speed = parsedData.speed.takeIf { it > 0 } ?: currentData.speed,
                battery = parsedData.battery.takeIf { it > 0 } ?: currentData.battery,
                voltage = parsedData.voltage.takeIf { it > 0 } ?: currentData.voltage,
                temperature = parsedData.temperature.takeIf { it != 0f } ?: currentData.temperature,
                lastUpdate = Date(),
                isConnected = true
            )
            onDataUpdate(currentData)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing data", e)
        }
    }

    private fun parseData(data: ByteArray): ScooterData {
        // Parsing simplifié - à adapter selon votre protocole
        var speed = 0f
        var battery = 0f
        var voltage = 0f

        for (i in 0 until minOf(data.size - 1, 10)) {
            val value16 = getShort(data, i)
            val value8 = data[i].toUByte().toInt()

            when {
                value16 in 0..5000 && i in 2..6 -> speed = value16 / 100f
                value8 in 0..100 && i in 4..8 -> battery = value8.toFloat()
                value16 in 3000..6000 && i in 6..10 -> voltage = value16 / 100f
            }
        }

        return ScooterData(speed = speed, battery = battery, voltage = voltage)
    }

    private fun getShort(data: ByteArray, offset: Int): Int {
        return if (offset + 1 < data.size) {
            ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
        } else 0
    }
}