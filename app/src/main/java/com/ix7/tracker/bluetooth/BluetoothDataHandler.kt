package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.ScooterData
import com.ix7.tracker.core.RideMode

class BluetoothDataHandler(
    private val onDataParsed: (ScooterData) -> Unit
) {
    companion object {
        private const val TAG = "BLE"
        private const val ENABLE_DETAILED_LOGS = true

        private const val HEADER_1: Byte = 0x61
        private const val HEADER_2: Byte = 0x9E.toByte()

        private const val TYPE_TELEMETRY: Byte = 0x3E
        private const val TYPE_BATTERY: Byte = 0x32
        private const val TYPE_MODE: Byte = 0x30

        private const val SIZE_TELEMETRY = 16
        private const val SIZE_BATTERY = 12
        private const val SIZE_MODE = 10
    }

    private val frameBuffer = mutableListOf<Byte>()
    private var currentData = ScooterData()

    fun handleData(data: ByteArray) {
        try {
            if (ENABLE_DETAILED_LOGS) {
                val hex = data.joinToString(" ") { "%02X".format(it) }
                Log.e(TAG, "[RX] SIZE:${data.size} $hex")
            }

            if (data.size >= 3 && data[0] == HEADER_1 && data[1] == HEADER_2) {
                parse61Frame(data)
                return
            }

            frameBuffer.addAll(data.toList())
            processBuffer()

            if (frameBuffer.size > 100) {
                frameBuffer.clear()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur: ${e.message}")
        }
    }

    private fun processBuffer() {
        while (frameBuffer.size >= 10) {
            val headerIndex = findHeaderIndex()
            if (headerIndex == -1) {
                frameBuffer.clear()
                return
            }

            if (headerIndex > 0) {
                repeat(headerIndex) { frameBuffer.removeAt(0) }
            }

            if (frameBuffer.size < 3) return

            val type = frameBuffer[2]
            val expectedSize = when (type) {
                TYPE_TELEMETRY -> SIZE_TELEMETRY
                TYPE_BATTERY -> SIZE_BATTERY
                TYPE_MODE -> SIZE_MODE
                else -> {
                    frameBuffer.removeAt(0)
                    return
                }
            }

            if (frameBuffer.size < expectedSize) return

            val frame = frameBuffer.take(expectedSize).toByteArray()
            repeat(expectedSize) { frameBuffer.removeAt(0) }

            parse61Frame(frame)
        }
    }

    private fun findHeaderIndex(): Int {
        for (i in 0 until frameBuffer.size - 1) {
            if (frameBuffer[i] == HEADER_1 && frameBuffer[i + 1] == HEADER_2) {
                return i
            }
        }
        return -1
    }

    private fun parse61Frame(frame: ByteArray) {
        if (frame.size < 3) return

        val type = frame[2]

        if (ENABLE_DETAILED_LOGS) {
            val typeName = when (type) {
                TYPE_TELEMETRY -> "TELEMETRY"
                TYPE_BATTERY -> "BATTERY"
                TYPE_MODE -> "MODE"
                else -> "UNKNOWN"
            }
            val hex = frame.joinToString(" ") { "%02X".format(it) }
            Log.e(TAG, "[$typeName] $hex")
        }

        when (type) {
            TYPE_TELEMETRY -> if (frame.size == SIZE_TELEMETRY) decodeTelemetry(frame)
            TYPE_BATTERY -> if (frame.size == SIZE_BATTERY) decodeBattery(frame)
            TYPE_MODE -> if (frame.size == SIZE_MODE) decodeMode(frame)
        }

        onDataParsed(currentData)
    }

    private fun decodeTelemetry(frame: ByteArray) {
        val subType = frame[5].toInt() and 0xFF

        when (subType) {
            0xDE -> {
                currentData = currentData.copy(speed = 0f)
            }

            0xDA -> {
                val byte8 = frame[8].toInt() and 0xFF
                val byte9 = frame[9].toInt() and 0xFF

                val rawSpeed = (byte8 shl 8) or byte9
                val speed = rawSpeed / 100.0f

                if (speed in 0f..50f) {
                    currentData = currentData.copy(speed = speed)
                    if (ENABLE_DETAILED_LOGS) {
                        Log.d(TAG, "⚡ Vitesse: $speed km/h")
                    }
                }
            }
        }
    }

    private fun decodeBattery(frame: ByteArray) {
        val temp = frame[5].toInt() and 0xFF
        val battHigh = frame[6].toInt() and 0xFF
        val battLow = frame[7].toInt() and 0xFF

        val battRaw = (battHigh shl 8) or battLow
        val batteryPercent = (battRaw / 1000.0f).coerceIn(0f, 100f)

        currentData = currentData.copy(
            battery = batteryPercent,
            temperature = temp.toFloat()
        )

        if (ENABLE_DETAILED_LOGS) {
            Log.d(TAG, "🔋 Batterie: ${batteryPercent.toInt()}% Temp: ${temp}°C")
        }
    }

    /**
     * Décode MODE (0x30) - États (néon, lumières, débridage)
     *
     * CORRECTION BASÉE SUR LES LOGS RÉELS:
     * Byte[5] FLAGS (LOGIQUE INVERSÉE):
     *   Bit 7: Débridage (1=débridé)
     *   Bit 1: Néon (0=ON, 1=OFF)  ← INVERSÉ
     *   Bit 0: Lumières (0=ON, 1=OFF)  ← INVERSÉ
     *
     * Byte[6]: MODE (IGNORÉ car change aléatoirement)
     */
    private fun decodeMode(frame: ByteArray) {
        val flags = frame[5].toInt() and 0xFF

        // LOGIQUE INVERSÉE: 0=ON, 1=OFF
        val isUnlocked = (flags and 0x80) != 0  // Bit 7
        val neonOn = (flags and 0x02) == 0      // Bit 1 INVERSÉ
        val lightsOn = (flags and 0x01) == 0    // Bit 0 INVERSÉ

        // NE PAS changer le mode de conduite depuis les trames MODE
        // (byte[6] change aléatoirement, non fiable)

        val hasChanged = currentData.headlightsOn != lightsOn ||
                currentData.neonOn != neonOn ||
                currentData.isLocked != !isUnlocked

        if (hasChanged) {
            if (ENABLE_DETAILED_LOGS) {
                Log.i(TAG, "═══ ÉTAT CHANGÉ ═══")
                Log.i(TAG, "🔓 Débridé: ${if (isUnlocked) "OUI" else "NON"}")
                Log.i(TAG, "💡 Néon: ${if (neonOn) "ON" else "OFF"} [bit=${flags and 0x02}]")
                Log.i(TAG, "🔦 Lumières: ${if (lightsOn) "ON" else "OFF"} [bit=${flags and 0x01}]")
                Log.i(TAG, "🏍️ Mode: ${currentData.currentMode} (inchangé)")
                Log.i(TAG, "═══════════════════")
            }

            currentData = currentData.copy(
                headlightsOn = lightsOn,
                neonOn = neonOn,
                isLocked = !isUnlocked
                // currentMode reste inchangé
            )
        }
    }

    /**
     * Permet de changer le mode manuellement (depuis les commandes)
     */
    fun setRideMode(mode: RideMode) {
        currentData = currentData.copy(currentMode = mode)
        if (ENABLE_DETAILED_LOGS) {
            Log.i(TAG, "🏍️ Mode changé manuellement: $mode")
        }
    }

    fun reset() {
        frameBuffer.clear()
        currentData = ScooterData()
    }

    fun getCurrentData(): ScooterData = currentData
}