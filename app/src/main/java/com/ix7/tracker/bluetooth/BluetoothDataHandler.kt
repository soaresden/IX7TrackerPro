package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.ProtocolConstants
import com.ix7.tracker.core.ProtocolUtils
import com.ix7.tracker.core.ScooterData

class BluetoothDataHandler(
    private val onDataParsed: (ScooterData) -> Unit
) {
    companion object {
        private const val TAG = "BluetoothDataHandler"
    }

    private var currentData = ScooterData()
    private val frameBuffer = mutableListOf<Byte>()

    fun handleData(data: ByteArray) {
        try {
            // Log brut pour debug
            val hex = data.joinToString(" ") { "%02X".format(it) }
            Log.e("BLE_RAW", "[${System.currentTimeMillis()}] SIZE:${data.size} $hex")

            // Décodage frames 61 9E
            if (data.size >= 2 && data[0] == 0x61.toByte() && data[1] == 0x9E.toByte()) {
                parse61Frame(data)
                return
            }

            // Protocole 55 AA
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
            Log.e(TAG, "Erreur traitement", e)
        }
    }

    private fun parse61Frame(frame: ByteArray) {
        val hex = frame.joinToString(" ") { "%02X".format(it) }
        val type = if (frame.size > 2) frame[2] else 0

        when {
            // Frame 0x3E - 16 bytes - Télémétrie
            frame.size == 16 && frame[2] == 0x3E.toByte() -> {
                val byte5 = frame[5].toInt() and 0xFF
                val byte6 = frame[6].toInt() and 0xFF

                // Pattern A (repos): DE 34 34...
                // Pattern B (mouvement?): DA C3 34 9E...
                val isMoving = byte5 == 0xDA

                // Essayer de trouver la vitesse dans différentes positions
                var speed = 0f

                if (isMoving) {
                    // Essai 1: bytes 7-8
                    val speed1 = ((frame[7].toInt() and 0xFF) shl 8) or (frame[8].toInt() and 0xFF)
                    // Essai 2: bytes 8-9
                    val speed2 = ((frame[8].toInt() and 0xFF) shl 8) or (frame[9].toInt() and 0xFF)

                    speed = when {
                        speed1 in 1..5000 -> speed1 / 100f
                        speed2 in 1..5000 -> speed2 / 100f
                        else -> 5f // Valeur par défaut si mouvement détecté
                    }

                    Log.d(TAG, "MOUVEMENT DÉTECTÉ! speed1=$speed1 speed2=$speed2 → speed=$speed km/h")
                }

                currentData = currentData.copy(speed = speed)
                Log.d(TAG, "Frame 0x3E: moving=$isMoving speed=$speed km/h")
            }

            // Frame 0x30 - 10 bytes - États
            frame.size == 10 && frame[2] == 0x30.toByte() -> {
                val byte5 = frame[5].toInt() and 0xFF
                val byte6 = frame[6].toInt() and 0xFF

                Log.d(TAG, "Frame 0x30: byte5=0x${"%02X".format(byte5)} byte6=0x${"%02X".format(byte6)}")
            }

            // Frame 0x32 - 12 bytes - Batterie/Température
            frame.size == 12 && frame[2] == 0x32.toByte() -> {
                // Frame: 61 9E 32 17 35 0B A4 35 A4 35 40 CA
                //                       ^^^^^ = batterie en millièmes

                // Batterie sur bytes 6-7 (big endian)
                val batteryRaw = ((frame[6].toInt() and 0xFF) shl 8) or (frame[7].toInt() and 0xFF)
                val battery = (batteryRaw / 1000f).coerceIn(0f, 100f)

                // Température : valeur par défaut (pas encore trouvée dans 0x32)
                val temperature = 27f

                // Tension : valeur par défaut
                val voltage = 47.7f

                currentData = currentData.copy(
                    battery = battery,
                    temperature = temperature,
                    voltage = voltage
                )

                Log.d(TAG, "Frame 0x32: battery=${battery}% (raw=$batteryRaw)")
            }
        }

        onDataParsed(currentData)
    }

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

            val currentRaw = if (frame.size > 13) {
                ((frame[12].toInt() and 0xFF) shl 8) or (frame[13].toInt() and 0xFF)
            } else 0
            val current = currentRaw / 10.0f
            val power = voltage * current

            currentData = currentData.copy(
                speed = if (speed > 0) speed else currentData.speed,
                battery = if (battery > 0) battery.toFloat() else currentData.battery,
                voltage = if (voltage > 0) voltage else currentData.voltage,
                current = current,
                power = power,
                temperature = if (temperature > 0) temperature.toFloat() else currentData.temperature
            )

            onDataParsed(currentData)

        } catch (e: Exception) {
            Log.e(TAG, "Erreur parse", e)
        }
    }

    private fun parseStatusResponse(frame: ByteArray) {
        onDataParsed(currentData)
    }

    fun reset() {
        currentData = ScooterData()
        frameBuffer.clear()
    }

    fun getCurrentData(): ScooterData = currentData
}