package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.ProtocolConstants
import com.ix7.tracker.core.ProtocolUtils
import com.ix7.tracker.core.ScooterData

/**
 * Handler Bluetooth FINAL avec parsing des trames d'initialisation
 * Types découverts: 0x02 et 0x1A (envoyés au début uniquement)
 */
class BluetoothDataHandler(
    private val onDataParsed: (ScooterData) -> Unit
) {
    companion object {
        private const val TAG = "BLE"
        private const val ENABLE_DETAILED_LOGS = true // Pour voir les nouvelles trames
    }

    private var currentData = ScooterData()
    private val frameBuffer = mutableListOf<Byte>()
    private var frameCount = 0
    private var lastFrameType = 0.toByte()
    private var initFramesReceived = false

    fun handleData(data: ByteArray) {
        try {
            frameCount++

            // Log raccourci
            if (ENABLE_DETAILED_LOGS && frameCount <= 200) {
                val hex = data.joinToString(" ") { "%02X".format(it) }
                Log.e(TAG, "[$frameCount] $hex")
            }

            // Recherche kilométrage dans les 100 premières trames
            if (frameCount < 100) {
                searchForOdometer(data)
            }

            // Décodage frames 61 9E
            if (data.size >= 3 && data[0] == 0x61.toByte() && data[1] == 0x9E.toByte()) {
                parse61Frame(data)
                return
            }

            // Protocole 55 AA (legacy)
            frameBuffer.addAll(data.toList())
            while (frameBuffer.size >= 5) {
                if (!tryParseFrame()) {
                    frameBuffer.removeAt(0)
                }
            }

            if (frameBuffer.size > 100) {
                frameBuffer.clear()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur: ${e.message}")
        }
    }

    /**
     * Recherche kilométrage 34.7 km
     */
    private fun searchForOdometer(data: ByteArray) {
        // Cherche 347 (0x015B) - dixièmes
        for (i in 0 until data.size - 1) {
            val value = ((data[i].toInt() and 0xFF) shl 8) or (data[i+1].toInt() and 0xFF)
            if (value == 347 || value == 0x015B) {
                Log.e(TAG, "🎯 ODOMETER! 34.7km aux bytes[$i-${i+1}] = 0x${"%04X".format(value)}")
            }
        }

        // Cherche 3470 (0x0D8E) - centièmes
        for (i in 0 until data.size - 1) {
            val value = ((data[i].toInt() and 0xFF) shl 8) or (data[i+1].toInt() and 0xFF)
            if (value == 3470 || value == 0x0D8E) {
                Log.e(TAG, "🎯 ODOMETER! 34.7km aux bytes[$i-${i+1}] = 0x${"%04X".format(value)}")
            }
        }

        // Cherche ~3470 (entre 3400 et 3500)
        for (i in 0 until data.size - 1) {
            val value = ((data[i].toInt() and 0xFF) shl 8) or (data[i+1].toInt() and 0xFF)
            if (value in 3400..3500) {
                val km = value / 100f
                Log.w(TAG, "⚠️ POSSIBLE ODO: ${km}km aux bytes[$i-${i+1}] = 0x${"%04X".format(value)}")
            }
        }
    }

    private fun parse61Frame(frame: ByteArray) {
        val type = if (frame.size > 2) frame[2] else 0

        // Log nouveaux types
        if (type != lastFrameType) {
            Log.w(TAG, "NEW TYPE: 0x${"%02X".format(type)} size=${frame.size}")
            lastFrameType = type
        }

        when {
            // TYPE 0x02 - TRAME D'INITIALISATION (48 bytes)
            frame.size >= 40 && frame[2] == 0x02.toByte() -> {
                parseFrame02_Init(frame)
            }

            // TYPE 0x1A - TRAME DE DONNÉES ÉTENDUES (58 bytes)
            frame.size >= 50 && frame[2] == 0x1A.toByte() -> {
                parseFrame1A_Extended(frame)
            }

            // TYPE 0x3E - VITESSE (16 bytes)
            frame.size == 16 && frame[2] == 0x3E.toByte() -> {
                parseFrame3E(frame)
            }

            // TYPE 0x30 - États (10 bytes)
            frame.size == 10 && frame[2] == 0x30.toByte() -> {
                // États secondaires
            }

            // TYPE 0x32 - BATTERIE (12 bytes)
            frame.size == 12 && frame[2] == 0x32.toByte() -> {
                parseFrame32(frame)
            }

            else -> {
                if (ENABLE_DETAILED_LOGS && frameCount <= 100) {
                    Log.w(TAG, "UNKNOWN: type=0x${"%02X".format(type)} size=${frame.size}")
                }
            }
        }

        onDataParsed(currentData)
    }

    /**
     * Parse TYPE 0x02 - TRAME D'INITIALISATION
     * Contient probablement: kilométrage, temps total, versions
     */
    private fun parseFrame02_Init(frame: ByteArray) {
        if (initFramesReceived) return // Déjà parsé

        val hex = frame.joinToString(" ") { "%02X".format(it) }
        Log.e(TAG, "📦 INIT 0x02 (${ frame.size}b): $hex")

        // Analyse toutes les paires de bytes
        Log.e(TAG, "=== ANALYSE TRAME 0x02 ===")
        for (i in 5 until minOf(frame.size - 1, 45)) {
            val value16 = ((frame[i].toInt() and 0xFF) shl 8) or (frame[i+1].toInt() and 0xFF)
            val valueLE = ((frame[i+1].toInt() and 0xFF) shl 8) or (frame[i].toInt() and 0xFF)

            if (value16 > 100 && value16 < 50000) {
                Log.d(TAG, "  [$i-${i+1}] BE=0x${"%04X".format(value16)}=$value16 LE=$valueLE → ${value16/100f}km ${value16/10f}°C")
            }
        }

        // Bytes spécifiques identifiés
        val byte14_15 = ((frame[14].toInt() and 0xFF) shl 8) or (frame[15].toInt() and 0xFF)
        Log.e(TAG, "Bytes[14-15] = 0x${"%04X".format(byte14_15)} = ${byte14_15/100f} km")

        initFramesReceived = true
    }

    /**
     * Parse TYPE 0x1A - TRAME DONNÉES ÉTENDUES
     * Contient probablement aussi des infos importantes
     */
    private fun parseFrame1A_Extended(frame: ByteArray) {
        val hex = frame.joinToString(" ") { "%02X".format(it) }
        Log.e(TAG, "📦 EXTENDED 0x1A (${frame.size}b): $hex")

        // Analyse toutes les paires de bytes
        Log.e(TAG, "=== ANALYSE TRAME 0x1A ===")
        for (i in 5 until minOf(frame.size - 1, 50)) {
            val value16 = ((frame[i].toInt() and 0xFF) shl 8) or (frame[i+1].toInt() and 0xFF)
            val valueLE = ((frame[i+1].toInt() and 0xFF) shl 8) or (frame[i].toInt() and 0xFF)

            if (value16 > 100 && value16 < 50000) {
                Log.d(TAG, "  [$i-${i+1}] BE=0x${"%04X".format(value16)}=$value16 LE=$valueLE → ${value16/100f}km")
            }
        }

        // Cherche temps total (>10h15m = >615 minutes = 36900 secondes)
        for (i in 5 until minOf(frame.size - 3, 50)) {
            val value32 = ((frame[i].toInt() and 0xFF) shl 24) or
                    ((frame[i+1].toInt() and 0xFF) shl 16) or
                    ((frame[i+2].toInt() and 0xFF) shl 8) or
                    (frame[i+3].toInt() and 0xFF)

            if (value32 in 30000..50000) {
                val hours = value32 / 3600
                val minutes = (value32 % 3600) / 60
                Log.e(TAG, "🕐 TEMPS? bytes[$i-${i+3}] = ${value32}s = ${hours}h${minutes}m")
            }
        }
    }

    /**
     * Frame 0x3E - VITESSE
     */
    private fun parseFrame3E(frame: ByteArray) {
        val byte8 = frame[8].toInt() and 0xFF
        val speed = byte8 / 100f

        if (Math.abs(currentData.speed - speed) > 0.05f) {
            currentData = currentData.copy(speed = speed)
            if (ENABLE_DETAILED_LOGS && frameCount <= 100) {
                Log.d(TAG, "Vitesse: ${speed} km/h")
            }
        }
    }

    /**
     * Frame 0x32 - BATTERIE
     */
    private fun parseFrame32(frame: ByteArray) {
        val batteryRaw = ((frame[6].toInt() and 0xFF) shl 8) or (frame[7].toInt() and 0xFF)
        val battery = (batteryRaw / 1000f).coerceIn(0f, 100f)

        if (Math.abs(currentData.battery - battery) > 0.5f) {
            currentData = currentData.copy(battery = battery)
            if (ENABLE_DETAILED_LOGS && frameCount <= 100) {
                Log.d(TAG, "Batterie: ${battery}%")
            }
        }

        val temp1 = (frame[6].toInt() and 0xFF) / 10f + 10f
        currentData = currentData.copy(
            temperature = temp1,
            voltage = 47.7f
        )
    }

    // ========== PROTOCOLE 55 AA (Legacy) ==========

    private fun tryParseFrame(): Boolean {
        if (frameBuffer.size < 2) return false

        if (frameBuffer[0] != ProtocolConstants.FRAME_HEADER_1 ||
            frameBuffer[1] != ProtocolConstants.FRAME_HEADER_2) {
            return false
        }

        if (frameBuffer.size < 3) return false
        val length = frameBuffer[2].toInt() and 0xFF
        val totalLength = 2 + 1 + length + 1

        if (frameBuffer.size < totalLength) return false

        val frame = frameBuffer.take(totalLength).toByteArray()
        val calculatedChecksum = ProtocolUtils.calculateChecksum(frame)
        val receivedChecksum = frame[totalLength - 1]

        if (calculatedChecksum != receivedChecksum) {
            repeat(totalLength) { if (frameBuffer.isNotEmpty()) frameBuffer.removeAt(0) }
            return true
        }

        parseValidFrame(frame)
        repeat(totalLength) { if (frameBuffer.isNotEmpty()) frameBuffer.removeAt(0) }
        return true
    }

    private fun parseValidFrame(frame: ByteArray) {
        if (frame.size < 5) return
        val command = frame[3]
        when {
            command == 0x23.toByte() && frame.size >= 20 -> parseLongStatusFrame(frame)
            command == 0x23.toByte() -> parseDataFrame(frame)
            command == 0x20.toByte() -> parseStatusResponse(frame)
        }
    }

    private fun parseDataFrame(frame: ByteArray) {
        onDataParsed(currentData)
    }

    private fun parseLongStatusFrame(frame: ByteArray) {
        try {
            if (frame.size < 20) return

            val temperature = frame[5].toInt() and 0xFF
            val battery = if (frame.size > 22) frame[22].toInt() and 0xFF else 0
            val voltageRaw = if (frame.size > 11) {
                ((frame[10].toInt() and 0xFF) shl 8) or (frame[11].toInt() and 0xFF)
            } else 0
            val voltage = voltageRaw / 10.0f

            val speedRaw = if (frame.size > 7) {
                ((frame[6].toInt() and 0xFF) shl 8) or (frame[7].toInt() and 0xFF)
            } else 0
            val speed = speedRaw / 100.0f

            currentData = currentData.copy(
                speed = if (speed > 0) speed else currentData.speed,
                battery = if (battery > 0) battery.toFloat() else currentData.battery,
                voltage = if (voltage > 0) voltage else currentData.voltage,
                temperature = if (temperature > 0) temperature.toFloat() else currentData.temperature
            )

            onDataParsed(currentData)

        } catch (e: Exception) {
            Log.e(TAG, "Erreur parse 55AA: ${e.message}")
        }
    }

    private fun parseStatusResponse(frame: ByteArray) {
        onDataParsed(currentData)
    }

    fun reset() {
        currentData = ScooterData()
        frameBuffer.clear()
        frameCount = 0
        lastFrameType = 0
        initFramesReceived = false
    }

    fun getCurrentData(): ScooterData = currentData
}