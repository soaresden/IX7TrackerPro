package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.ScooterData

class RawFrameLogger {
    private val TAG = "🔴 RAW"
    private var frameCounter = 0
    private val startTime = System.currentTimeMillis()

    fun logRawChunk(data: ByteArray) {
        val elapsed = System.currentTimeMillis() - startTime
        val hex = data.joinToString(" ") { "%02X".format(it) }
        Log.d(TAG, "CHUNK [$elapsed ms] (${data.size}b): $hex")
    }

    fun logCompleteFrame(frame: ByteArray) {
        frameCounter++
        val elapsed = System.currentTimeMillis() - startTime
        val hex = frame.joinToString(" ") { "%02X".format(it) }
        val type = if (frame.size >= 3) String.format("0x%02X", frame[2].toInt() and 0xFF) else "???"

        Log.d(TAG, "FRAME #$frameCounter [$elapsed ms] Type=$type Size=${frame.size}b: $hex")
    }

    fun logParsedData(speed: Float, battery: Float, temp: Float, odometer: Float, time: String = "") {
        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "DATA [$elapsed ms] Speed=$speed Battery=$battery Temp=$temp Odometer=$odometer Time=$time")
    }
}