package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.ScooterData
import java.util.*

class BluetoothDataHandler(
    private val onDataUpdate: (ScooterData) -> Unit
) {
    companion object {
        private const val TAG = "BT_RAW_DATA"
    }

    private var frameNumber = 0

    fun handleData(data: ByteArray) {
        frameNumber++

        // LOG BRUT - Ne rien parser, juste tout afficher
        val hex = data.joinToString(" ") { "%02X".format(it) }
        val dec = data.joinToString(" ") { "${it.toInt() and 0xFF}" }

        Log.d(TAG, "═══════════════════════════════════════════════════════════")
        Log.d(TAG, "FRAME #$frameNumber - Taille: ${data.size} bytes")
        Log.d(TAG, "HEX: $hex")
        Log.d(TAG, "DEC: $dec")
        Log.d(TAG, "═══════════════════════════════════════════════════════════")

        // Toujours mettre à jour avec des données vides pour garder la connexion active
        onDataUpdate(ScooterData(isConnected = true, lastUpdate = Date()))
    }
}