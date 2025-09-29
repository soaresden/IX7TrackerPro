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

        // LOG BRUT
        val hex = data.joinToString(" ") { "%02X".format(it) }

        Log.d(TAG, "════════════════════════════════════════════════")
        Log.d(TAG, "FRAME #$frameNumber [${data.size} bytes]: $hex")
        Log.d(TAG, "════════════════════════════════════════════════")

        // Toujours mettre à jour
        onDataUpdate(ScooterData(isConnected = true, lastUpdate = Date()))
    }
}