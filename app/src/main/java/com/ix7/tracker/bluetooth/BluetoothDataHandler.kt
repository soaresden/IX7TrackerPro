package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.ProtocolConstants
import com.ix7.tracker.core.ProtocolUtils
import com.ix7.tracker.core.ScooterData
import java.util.*

/**
 * Gestionnaire de données Bluetooth AMÉLIORÉ
 * Parse les trames 55 AA correctement
 */
class BluetoothDataHandler(
    private val onDataParsed: (ScooterData) -> Unit
) {
    companion object {
        private const val TAG = "BluetoothDataHandler"
    }

    private var currentData = ScooterData()
    private val frameBuffer = mutableListOf<Byte>()

    /**
     * Traite les données reçues
     */
    fun handleData(data: ByteArray) {
        try {
            // Ajouter au buffer
            frameBuffer.addAll(data.toList())

            // Parser toutes les trames disponibles
            while (frameBuffer.size >= 5) {
                if (!tryParseFrame()) {
                    // Pas de trame valide, supprimer premier byte
                    frameBuffer.removeAt(0)
                }
            }

            // Nettoyer si buffer trop grand
            if (frameBuffer.size > 100) {
                Log.w(TAG, "⚠️ Buffer trop grand, nettoyage")
                frameBuffer.clear()
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur traitement", e)
        }
    }

    /**
     * Essaie de parser une trame
     */
    private fun tryParseFrame(): Boolean {
        // Vérifier header 55 AA
        if (frameBuffer.size < 2) return false

        if (frameBuffer[0] != ProtocolConstants.FRAME_HEADER_1 ||
            frameBuffer[1] != ProtocolConstants.FRAME_HEADER_2) {
            return false
        }

        // Lire la longueur
        if (frameBuffer.size < 3) return false
        val length = frameBuffer[2].toInt() and 0xFF

        // Calculer taille totale
        val totalLength = 2 + 1 + length + 1 // header + length + payload + checksum

        // Vérifier si on a assez de données
        if (frameBuffer.size < totalLength) return false

        // Extraire la trame
        val frame = frameBuffer.take(totalLength).toByteArray()

        // Vérifier checksum
        val calculatedChecksum = ProtocolUtils.calculateChecksum(frame)
        val receivedChecksum = frame[totalLength - 1]

        if (calculatedChecksum != receivedChecksum) {
            Log.w(TAG, "⚠️ Checksum invalide")
            // Supprimer cette trame du buffer
            repeat(totalLength) { if (frameBuffer.isNotEmpty()) frameBuffer.removeAt(0) }
            return true
        }

        // Checksum OK, parser la trame
        ProtocolUtils.logFrame("✓ Trame valide", frame)
        parseValidFrame(frame)

        // Supprimer du buffer
        repeat(totalLength) { if (frameBuffer.isNotEmpty()) frameBuffer.removeAt(0) }

        return true
    }

    /**
     * Parse une trame validée
     */
    private fun parseValidFrame(frame: ByteArray) {
        if (frame.size < 5) return

        val command = frame[3]
        val length = frame[2].toInt() and 0xFF

        when {
            // Trame de données complète (60 bytes)
            command == 0x23.toByte() && frame.size >= 20 -> {
                parseLongStatusFrame(frame)
            }

            // Trame de données courte
            command == 0x23.toByte() -> {
                parseDataFrame(frame)
            }

            // Trame de statut
            command == 0x20.toByte() -> {
                parseStatusResponse(frame)
            }

            // Keep-alive ou autre
            else -> {
                Log.d(TAG, "ℹ️ Trame non gérée: cmd=${"%02X".format(command)}")
            }
        }
    }

    /**
     * Parse trame de données courte
     */
    private fun parseDataFrame(frame: ByteArray) {
        Log.d(TAG, "📊 Trame de données courte")

        try {
            currentData = currentData.copy(
                lastUpdate = Date(),
                isConnected = true
            )

            onDataParsed(currentData)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur parse data", e)
        }
    }

    /**
     * Parse trame de statut longue (60 bytes)
     * Contient toutes les infos importantes
     */
    private fun parseLongStatusFrame(frame: ByteArray) {
        Log.d(TAG, "📊 Trame longue (${frame.size} bytes)")

        try {
            if (frame.size < 20) return

            // OFFSETS BASÉS SUR LES LOGS RÉELS
            val temperature = frame[5].toInt() and 0xFF  // Température en °C
            val battery = if (frame.size > 22) frame[22].toInt() and 0xFF else 0  // Batterie %

            // Voltage (bytes 10-11, big endian, en dixièmes de volt)
            val voltageRaw = if (frame.size > 11) {
                ((frame[10].toInt() and 0xFF) shl 8) or (frame[11].toInt() and 0xFF)
            } else 0
            val voltage = voltageRaw / 10.0f

            // Vitesse (bytes 6-7, en centièmes de km/h)
            val speedRaw = if (frame.size > 7) {
                ((frame[6].toInt() and 0xFF) shl 8) or (frame[7].toInt() and 0xFF)
            } else 0
            val speed = speedRaw / 100.0f

            // Courant (bytes 12-13)
            val currentRaw = if (frame.size > 13) {
                ((frame[12].toInt() and 0xFF) shl 8) or (frame[13].toInt() and 0xFF)
            } else 0
            val current = currentRaw / 10.0f

            // Puissance = Voltage × Courant
            val power = voltage * current

            Log.d(TAG, "  🌡️ Température: $temperature °C")
            Log.d(TAG, "  🔋 Batterie: $battery %")
            Log.d(TAG, "  ⚡ Voltage: $voltage V")
            Log.d(TAG, "  🏃 Vitesse: $speed km/h")
            Log.d(TAG, "  💪 Puissance: $power W")

            currentData = currentData.copy(
                speed = speed,
                battery = battery.toFloat(),
                voltage = voltage,
                current = current,
                power = power,
                temperature = temperature.toFloat(),
                lastUpdate = Date(),
                isConnected = true
            )

            onDataParsed(currentData)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur parse", e)
        }
    }

    /**
     * Parse réponse de statut simple
     */
    private fun parseStatusResponse(frame: ByteArray) {
        Log.d(TAG, "📊 Réponse statut")

        try {
            currentData = currentData.copy(
                lastUpdate = Date(),
                isConnected = true
            )

            onDataParsed(currentData)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur parse status", e)
        }
    }

    /**
     * Réinitialise les données
     */
    fun reset() {
        currentData = ScooterData()
        frameBuffer.clear()
        Log.d(TAG, "🔄 Reset")
    }

    /**
     * Obtient les données actuelles
     */
    fun getCurrentData(): ScooterData = currentData
}