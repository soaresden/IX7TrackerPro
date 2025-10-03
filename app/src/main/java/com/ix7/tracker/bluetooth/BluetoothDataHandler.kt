package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.ProtocolConstants
import com.ix7.tracker.core.ProtocolUtils
import com.ix7.tracker.ui.screens.RideMode
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
            // Essayer de décoder avec le protocole 61 9E (états)
            ProtocolDecoder.decode(data)?.let { hoverData ->
                currentData = currentData.copy(
                    headlightsOn = hoverData.headlightsOn,
                    neonOn = hoverData.neonOn,
                    currentMode = when(hoverData.mode) {
                        0 -> RideMode.ECO
                        1 -> RideMode.PEDESTRIAN
                        2 -> RideMode.RACE
                        3 -> RideMode.POWER
                        else -> RideMode.ECO
                    },
                    battery = if (hoverData.battery > 0) hoverData.battery.toFloat() else currentData.battery,
                    voltage = if (hoverData.voltage > 0) hoverData.voltage else currentData.voltage,
                    temperature = if (hoverData.temperature > 0) hoverData.temperature.toFloat() else currentData.temperature
                )
                onDataParsed(currentData)
                return
            }

            // Si pas 61 9E, essayer le protocole 55 AA
            frameBuffer.addAll(data.toList())

            while (frameBuffer.size >= 5) {
                if (!tryParseFrame()) {
                    frameBuffer.removeAt(0)
                }
            }

            if (frameBuffer.size > 100) {
                Log.w(TAG, "Buffer trop grand, nettoyage")
                frameBuffer.clear()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur traitement", e)
        }
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
            Log.w(TAG, "Checksum invalide")
            repeat(totalLength) { if (frameBuffer.isNotEmpty()) frameBuffer.removeAt(0) }
            return true
        }

        ProtocolUtils.logFrame("Trame valide", frame)
        parseValidFrame(frame)

        repeat(totalLength) { if (frameBuffer.isNotEmpty()) frameBuffer.removeAt(0) }

        return true
    }

    private fun parseValidFrame(frame: ByteArray) {
        if (frame.size < 5) return

        val command = frame[3]

        when {
            command == 0x23.toByte() && frame.size >= 20 -> {
                parseLongStatusFrame(frame)
            }
            command == 0x23.toByte() -> {
                parseDataFrame(frame)
            }
            command == 0x20.toByte() -> {
                parseStatusResponse(frame)
            }
            else -> {
                Log.d(TAG, "Trame non gérée: cmd=${"%02X".format(command)}")
            }
        }
    }

    private fun parseDataFrame(frame: ByteArray) {
        Log.d(TAG, "Trame de données courte")
        onDataParsed(currentData)
    }

    private fun parseLongStatusFrame(frame: ByteArray) {
        Log.d(TAG, "Trame longue (${frame.size} bytes)")

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

            Log.d(TAG, "Température: $temperature °C")
            Log.d(TAG, "Batterie: $battery %")
            Log.d(TAG, "Voltage: $voltage V")
            Log.d(TAG, "Vitesse: $speed km/h")
            Log.d(TAG, "Puissance: $power W")

            currentData = currentData.copy(
                speed = speed,
                battery = battery.toFloat(),
                voltage = voltage,
                current = current,
                power = power,
                temperature = temperature.toFloat()
            )

            onDataParsed(currentData)

        } catch (e: Exception) {
            Log.e(TAG, "Erreur parse", e)
        }
    }

    private fun parseStatusResponse(frame: ByteArray) {
        Log.d(TAG, "Réponse statut")
        onDataParsed(currentData)
    }

    fun reset() {
        currentData = ScooterData()
        frameBuffer.clear()
        Log.d(TAG, "Reset")
    }

    fun getCurrentData(): ScooterData = currentData
}