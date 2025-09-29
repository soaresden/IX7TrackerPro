package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.ProtocolConstants
import com.ix7.tracker.core.ProtocolUtils
import com.ix7.tracker.core.ScooterData
import java.util.*

/**
 * Gestionnaire de données Bluetooth CORRIGÉ pour protocole 55 AA
 * Basé sur l'analyse des logs de l'application officielle
 */
class BluetoothDataHandler(
    private val onDataParsed: (ScooterData) -> Unit
) {
    companion object {
        private const val TAG = "BluetoothDataHandler"
    }

    private var currentData = ScooterData()
    private val frameBuffer = mutableListOf<Byte>()
    private var lastUpdateTime = System.currentTimeMillis()

    /**
     * Traite les données reçues via Bluetooth
     */
    fun handleData(data: ByteArray) {
        try {
            val hex = data.joinToString(" ") { "%02X".format(it) }
            Log.d(TAG, "📦 Données reçues (${data.size} bytes): $hex")

            // Ajouter au buffer
            frameBuffer.addAll(data.toList())

            // Chercher et parser les trames complètes
            while (frameBuffer.size >= 5) {  // Minimum: 55 AA length cmd checksum
                if (tryParseFrame()) {
                    // Frame parsée avec succès
                } else {
                    // Pas de frame valide, supprimer le premier byte
                    frameBuffer.removeAt(0)
                }
            }

            // Nettoyer le buffer s'il devient trop grand
            if (frameBuffer.size > 100) {
                Log.w(TAG, "⚠ Buffer trop grand, nettoyage")
                frameBuffer.clear()
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur traitement données", e)
        }
    }

    /**
     * Essaie de parser une trame depuis le début du buffer
     * Retourne true si une trame valide a été trouvée et supprimée du buffer
     */
    private fun tryParseFrame(): Boolean {
        // Vérifier le header 55 AA
        if (frameBuffer.size < 2) return false

        if (frameBuffer[0] != ProtocolConstants.FRAME_HEADER_1 ||
            frameBuffer[1] != ProtocolConstants.FRAME_HEADER_2) {
            return false
        }

        // Lire la longueur
        if (frameBuffer.size < 3) return false
        val length = frameBuffer[2].toInt() and 0xFF

        // Calculer la taille totale de la trame
        // Format: 55 AA length command [data...] checksum
        val totalLength = 2 + 1 + length + 1  // header(2) + length(1) + payload(length) + checksum(1)

        // Vérifier si on a assez de données
        if (frameBuffer.size < totalLength) {
            return false  // Pas encore assez de données
        }

        // Extraire la trame
        val frame = frameBuffer.take(totalLength).toByteArray()

        // Vérifier le checksum
        val calculatedChecksum = ProtocolUtils.calculateChecksum(frame)
        val receivedChecksum = frame[totalLength - 1]

        if (calculatedChecksum != receivedChecksum) {
            Log.w(TAG, "⚠ Checksum invalide: calculé=${"%02X".format(calculatedChecksum)} reçu=${"%02X".format(receivedChecksum)}")
            // On supprime quand même cette trame du buffer
            repeat(totalLength) { if (frameBuffer.isNotEmpty()) frameBuffer.removeAt(0) }
            return true
        }

        // Checksum OK, parser la trame
        val hex = frame.joinToString(" ") { "%02X".format(it) }
        Log.i(TAG, "✓ Trame valide (${frame.size} bytes): $hex")

        parseValidFrame(frame)

        // Supprimer la trame du buffer
        repeat(totalLength) { if (frameBuffer.isNotEmpty()) frameBuffer.removeAt(0) }

        return true
    }

    /**
     * Parse une trame validée
     */
    private fun parseValidFrame(frame: ByteArray) {
        if (frame.size < 5) {
            Log.w(TAG, "⚠ Trame trop courte")
            return
        }

        val command = frame[3]
        val length = frame[2].toInt() and 0xFF

        Log.d(TAG, "→ Command: ${"%02X".format(command)}, Length: $length")

        when {
            // Trame de données (comme dans le log: 55 AA 04 23 01 7E 03 00 56 FF)
            command == 0x23.toByte() && frame.size >= 10 -> {
                parseDataFrame(frame)
            }

            // Trame longue de statut (comme dans le log: 55 AA 36 23 01 1A CA 84...)
            command == 0x23.toByte() && frame.size >= 20 -> {
                parseLongStatusFrame(frame)
            }

            // Trame de réponse simple
            command == 0x20.toByte() -> {
                parseStatusResponse(frame)
            }

            // Keep-alive ou autre
            else -> {
                Log.d(TAG, "ℹ Trame non gérée: cmd=${"%02X".format(command)}")
                logFrameDetails(frame)
            }
        }
    }

    /**
     * Parse une trame de données courte (type SET4)
     * Exemple du log: 55 AA 04 23 01 7E 03 00 56 FF
     */
    private fun parseDataFrame(frame: ByteArray) {
        Log.d(TAG, "📊 Parse trame de données")

        try {
            if (frame.size < 10) return

            // Offset 5: SET4_ID1 (7E dans l'exemple)
            val id1 = frame[5].toInt() and 0xFF

            // Offset 6: Valeur (03 dans l'exemple)
            val value = frame[6].toInt() and 0xFF

            Log.d(TAG, "  ID1=${"%02X".format(id1)}, Value=$value")

            // Dans le log on voit: SET4_ID1 = 3, SET4_ID2 = 0
            // et getCurMode type:2 typeLow:2 typeHide:0

            // Pour l'instant, mettre à jour les données basiques
            currentData = currentData.copy(
                lastUpdate = Date(),
                isConnected = true
            )

            onDataParsed(currentData)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur parse data frame", e)
        }
    }

    /**
     * Parse une trame longue de statut (60 bytes)
     * Exemple du log: 55 AA 36 23 01 1A CA 84 00 00 00 00 5E 08 39 04...
     */
    private fun parseLongStatusFrame(frame: ByteArray) {
        Log.d(TAG, "📊 Parse trame longue de statut (${frame.size} bytes)")

        try {
            if (frame.size < 20) return

            // Basé sur l'analyse des offsets dans le log
            // Offset 5-6: Peut-être la vitesse (1A CA = 6858 en big endian)
            val speedRaw = ((frame[5].toInt() and 0xFF) shl 8) or (frame[6].toInt() and 0xFF)

            // Offset 7: Peut-être la batterie (84 = 132 en décimal, ou 84% ?)
            val battery = frame[7].toInt() and 0xFF

            // Offset 12-13: Peut-être température (5E 08)
            val tempRaw = ((frame[12].toInt() and 0xFF) shl 8) or (frame[13].toInt() and 0xFF)

            // Offset 14-15: Peut-être voltage (39 04)
            val voltageRaw = ((frame[14].toInt() and 0xFF) shl 8) or (frame[15].toInt() and 0xFF)

            // Conversion des valeurs (à ajuster selon tests réels)
            val speed = speedRaw / 100.0f  // Diviser par 100 pour avoir km/h
            val batteryPercent = if (battery <= 100) battery.toFloat() else 100f
            val temperature = tempRaw / 10.0f  // Diviser par 10 pour avoir °C
            val voltage = voltageRaw / 100.0f  // Diviser par 100 pour avoir V

            Log.d(TAG, "  Vitesse: $speed km/h")
            Log.d(TAG, "  Batterie: $batteryPercent %")
            Log.d(TAG, "  Température: $temperature °C")
            Log.d(TAG, "  Voltage: $voltage V")

            currentData = currentData.copy(
                speed = speed,
                battery = batteryPercent,
                voltage = voltage,
                temperature = temperature,
                lastUpdate = Date(),
                isConnected = true
            )

            onDataParsed(currentData)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur parse long status frame", e)
        }
    }

    /**
     * Parse une réponse de statut simple
     */
    private fun parseStatusResponse(frame: ByteArray) {
        Log.d(TAG, "📊 Parse réponse statut")

        try {
            // Mettre à jour juste le timestamp pour montrer qu'on reçoit des données
            currentData = currentData.copy(
                lastUpdate = Date(),
                isConnected = true
            )

            onDataParsed(currentData)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur parse status response", e)
        }
    }

    /**
     * Log les détails d'une trame pour debug
     */
    private fun logFrameDetails(frame: ByteArray) {
        if (frame.size < 5) return

        val hex = frame.joinToString(" ") { "%02X".format(it) }
        Log.d(TAG, "🔍 Détails trame:")
        Log.d(TAG, "  Hex: $hex")
        Log.d(TAG, "  Header: ${"%02X".format(frame[0])} ${"%02X".format(frame[1])}")
        Log.d(TAG, "  Length: ${frame[2].toInt() and 0xFF}")
        Log.d(TAG, "  Command: ${"%02X".format(frame[3])}")

        if (frame.size > 4) {
            Log.d(TAG, "  Data bytes:")
            for (i in 4 until minOf(frame.size - 1, 20)) {
                Log.d(TAG, "    [${i-4}]: ${"%02X".format(frame[i])} (${frame[i].toInt() and 0xFF})")
            }
        }

        Log.d(TAG, "  Checksum: ${"%02X".format(frame[frame.size - 1])}")
    }

    /**
     * Réinitialise les données
     */
    fun reset() {
        currentData = ScooterData()
        frameBuffer.clear()
        Log.d(TAG, "🔄 Données réinitialisées")
    }

    /**
     * Obtient les données actuelles
     */
    fun getCurrentData(): ScooterData = currentData
}