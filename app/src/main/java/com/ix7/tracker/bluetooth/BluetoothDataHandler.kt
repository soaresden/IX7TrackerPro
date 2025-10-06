package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.ScooterData
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Handler pour décoder toutes les trames du protocole 61 9E
 * VERSION SIMPLIFIÉE - Constantes en dur
 */
class BluetoothDataHandler {

    companion object {
        private const val TAG = "BLE_DATA"
        private val buffer = mutableListOf<Byte>()
        private var frameCount = 0

        // Constantes du protocole (en dur)
        private const val HEADER_1: Byte = 0x61
        private const val HEADER_2: Byte = 0x9E.toByte()

        // Types de trames
        private const val FRAME_STATUS: Byte = 0x30
        private const val FRAME_TEMP: Byte = 0x32
        private const val FRAME_REALTIME: Byte = 0x3E
    }

    /**
     * Traite les données brutes reçues du scooter
     */
    fun handleData(data: ByteArray): ScooterData? {
        frameCount++

        // Log brut
        val hex = data.joinToString(" ") { "%02X".format(it) }
        Log.d(TAG, "📥 [$frameCount] RAW(${data.size}): $hex")

        // Ajouter au buffer
        buffer.addAll(data.toList())

        // Chercher et traiter les trames complètes
        return processBuffer()
    }

    /**
     * Traite le buffer pour extraire les trames complètes
     */
    private fun processBuffer(): ScooterData? {
        var result: ScooterData? = null

        while (buffer.size >= 3) {
            // Chercher le header 61 9E
            val headerIndex = findHeader()

            if (headerIndex == -1) {
                buffer.clear()
                break
            }

            // Retirer les bytes avant le header
            if (headerIndex > 0) {
                repeat(headerIndex) { buffer.removeAt(0) }
            }

            // Vérifier qu'on a assez de bytes pour le type
            if (buffer.size < 3) break

            val frameType = buffer[2]

            // Déterminer la taille attendue selon le type
            val expectedSize = getExpectedFrameSize(frameType)

            if (expectedSize == -1) {
                buffer.removeAt(0)
                continue
            }

            // Attendre d'avoir assez de bytes
            if (buffer.size < expectedSize) break

            // Extraire la trame
            val frame = buffer.take(expectedSize).toByteArray()

            // Vérifier le checksum
            if (verifyChecksum(frame)) {
                val parsedData = parseFrame(frame)
                if (parsedData != null) {
                    result = parsedData
                }
            } else {
                Log.w(TAG, "❌ Checksum invalide")
            }

            // Retirer la trame traitée du buffer
            repeat(expectedSize) { buffer.removeAt(0) }
        }

        // Limiter la taille du buffer
        if (buffer.size > 200) {
            Log.w(TAG, "⚠️ Buffer trop grand, réinitialisation")
            buffer.clear()
        }

        return result
    }

    /**
     * Trouve l'index du header 61 9E dans le buffer
     */
    private fun findHeader(): Int {
        for (i in 0 until buffer.size - 1) {
            if (buffer[i] == HEADER_1 && buffer[i + 1] == HEADER_2) {
                return i
            }
        }
        return -1
    }

    /**
     * Retourne la taille attendue d'une trame selon son type
     */
    private fun getExpectedFrameSize(frameType: Byte): Int {
        return when (frameType) {
            FRAME_STATUS -> 10      // 0x30
            FRAME_TEMP -> 12        // 0x32
            FRAME_REALTIME -> 16    // 0x3E
            0x3A.toByte() -> 20     // FRAME_SPECIAL_2
            0x38.toByte() -> 19     // FRAME_SPECIAL_1
            0x3C.toByte() -> 18     // FRAME_SPECIAL_3
            0x16.toByte() -> 47     // FRAME_COMBINED
            0x26.toByte() -> 24     // FRAME_INFO_EXT
            0x1A.toByte() -> 49     // FRAME_DETAILED
            0x04.toByte() -> 51     // FRAME_EXTENDED
            0x02.toByte() -> 67     // FRAME_INIT
            0x00.toByte() -> 40     // FRAME_INFO
            else -> -1              // Type inconnu
        }
    }

    /**
     * Vérifie le checksum d'une trame
     */
    private fun verifyChecksum(frame: ByteArray): Boolean {
        if (frame.size < 3) return false

        var xor: Byte = 0
        for (i in 2 until frame.size - 1) {
            xor = (xor.toInt() xor frame[i].toInt()).toByte()
        }

        val expectedChecksum = frame[frame.size - 1]
        return xor == expectedChecksum
    }

    /**
     * Parse une trame complète selon son type
     */
    private fun parseFrame(frame: ByteArray): ScooterData? {
        val frameType = frame[2]

        return when (frameType) {
            FRAME_STATUS -> parseStatusFrame(frame)
            FRAME_REALTIME -> parseRealtimeFrame(frame)
            else -> {
                Log.d(TAG, "Type de trame non géré: 0x${"%02X".format(frameType)}")
                null
            }
        }
    }

    /**
     * Parse une trame de statut (0x30 - 10 bytes)
     */
    private fun parseStatusFrame(frame: ByteArray): ScooterData {
        // Exemple de parsing basique
        val battery = if (frame.size > 6) frame[6].toInt() and 0xFF else 0
        val speed = if (frame.size > 7) (frame[7].toInt() and 0xFF) / 10f else 0f

        Log.d(TAG, "📊 Status - Batterie: $battery%, Vitesse: ${speed}km/h")

        return ScooterData(
            battery = battery.toFloat(),
            speed = speed
        )
    }

    /**
     * Parse une trame temps réel (0x3E - 16 bytes)
     */
    private fun parseRealtimeFrame(frame: ByteArray): ScooterData {
        if (frame.size < 16) return ScooterData()

        // Vitesse (bytes 6-7, little-endian, en cm/s)
        val speedRaw = ((frame[7].toInt() and 0xFF) shl 8) or (frame[6].toInt() and 0xFF)
        val speed = speedRaw / 100f // Convertir cm/s en m/s puis en km/h

        // Batterie (byte 8)
        val battery = frame[8].toInt() and 0xFF

        // Tension (bytes 9-10, little-endian, en dixièmes de volt)
        val voltageRaw = ((frame[10].toInt() and 0xFF) shl 8) or (frame[9].toInt() and 0xFF)
        val voltage = voltageRaw / 10f

        // Courant (bytes 11-12, little-endian, signé)
        val currentRaw = ((frame[12].toInt() and 0xFF) shl 8) or (frame[11].toInt() and 0xFF)
        val current = currentRaw / 100f

        // Température (byte 13)
        val temperature = frame[13].toInt() and 0xFF

        Log.d(TAG, "📊 Realtime - V:${speed}km/h B:${battery}% U:${voltage}V I:${current}A T:${temperature}°C")

        return ScooterData(
            speed = speed,
            battery = battery.toFloat(),
            voltage = voltage,
            current = current,
            temperature = temperature.toFloat()
        )
    }

    /**
     * Réinitialise le handler
     */
    fun reset() {
        buffer.clear()
        frameCount = 0
        Log.d(TAG, "🔄 Handler réinitialisé")
    }
}