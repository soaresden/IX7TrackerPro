package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.ScooterData
import com.ix7.tracker.protocol.ProtocolConstants
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Handler pour décoder toutes les trames du protocole 61 9E
 *
 * CORRECTIONS APPORTÉES (Octobre 2025):
 * - ✅ Trame 0x3E: Batterie à [7] est CORRECTE
 * - ✅ Trame 0x1A: Odomètre trouvé aux offsets 9 et 11 (2 bytes LE, décamètres)
 * - ✅ Trame 0x16: Odomètre trouvé à l'offset 17 (2 bytes LE, décamètres)
 * - ❓ La vitesse n'est pas dans les trames actuelles (nécessite nouveaux logs)
 */
class BluetoothDataHandler {

    companion object {
        private const val TAG = "BLE_DATA"
        private val buffer = mutableListOf<Byte>()
        private var frameCount = 0
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
                // Pas de header trouvé, vider le buffer
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
                // Type inconnu, passer au prochain byte
                buffer.removeAt(0)
                continue
            }

            // Attendre d'avoir assez de bytes
            if (buffer.size < expectedSize) break

            // Extraire la trame
            val frame = buffer.take(expectedSize).toByteArray()

            // Vérifier le checksum
            if (ProtocolConstants.verifyChecksum(frame)) {
                // Traiter la trame
                val parsedData = parseFrame(frame)
                if (parsedData != null) {
                    result = parsedData
                }
            } else {
                Log.w(TAG, "❌ Checksum invalide pour trame type 0x${"%02X".format(frameType)}")
            }

            // Retirer la trame traitée du buffer
            repeat(expectedSize) { buffer.removeAt(0) }
        }

        // Limiter la taille du buffer
        if (buffer.size > 200) {
            Log.w(TAG, "⚠️ Buffer trop grand (${buffer.size}), réinitialisation")
            buffer.clear()
        }

        return result
    }

    /**
     * Trouve l'index du header 61 9E dans le buffer
     */
    private fun findHeader(): Int {
        for (i in 0 until buffer.size - 1) {
            if (buffer[i] == ProtocolConstants.HEADER_1 &&
                buffer[i + 1] == ProtocolConstants.HEADER_2) {
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
            ProtocolConstants.FRAME_STATUS -> 10       // 0x30
            ProtocolConstants.FRAME_TEMP -> 12         // 0x32
            ProtocolConstants.FRAME_REALTIME -> 16     // 0x3E
            ProtocolConstants.FRAME_SPECIAL_2 -> 20    // 0x3A
            ProtocolConstants.FRAME_SPECIAL_1 -> 19    // 0x38
            ProtocolConstants.FRAME_SPECIAL_3 -> 18    // 0x3C
            ProtocolConstants.FRAME_COMBINED -> 47     // 0x16
            ProtocolConstants.FRAME_INFO_EXT -> 24     // 0x26
            ProtocolConstants.FRAME_DETAILED -> 49     // 0x1A
            ProtocolConstants.FRAME_EXTENDED -> 51     // 0x04
            ProtocolConstants.FRAME_INIT -> 67         // 0x02
            ProtocolConstants.FRAME_INFO -> 40         // 0x00 (variable)
            else -> -1 // Type inconnu
        }
    }

    /**
     * Parse une trame complète selon son type
     */
    private fun parseFrame(frame: ByteArray): ScooterData? {
        val frameType = frame[2]
        val hex = frame.joinToString(" ") { "%02X".format(it) }

        Log.i(TAG, "🔍 Parse trame type 0x${"%02X".format(frameType)} (${frame.size} bytes)")

        return when (frameType) {
            ProtocolConstants.FRAME_REALTIME -> parseRealtimeFrame(frame)
            ProtocolConstants.FRAME_STATUS -> parseStatusFrame(frame)
            ProtocolConstants.FRAME_TEMP -> parseTempFrame(frame)
            ProtocolConstants.FRAME_INIT -> parseInitFrame(frame)
            ProtocolConstants.FRAME_EXTENDED -> parseExtendedFrame(frame)
            ProtocolConstants.FRAME_DETAILED -> parseDetailedFrame(frame)
            ProtocolConstants.FRAME_COMBINED -> parseCombinedFrame(frame)
            else -> {
                Log.d(TAG, "   Type 0x${"%02X".format(frameType)}: $hex")
                null
            }
        }
    }

    /**
     * Parse trame 0x3E - Temps réel (BATTERIE SEULEMENT)
     * Format: 61 9E 3E 17 35 DA C3 34 9E 37 14 30 8B 36 6E C8
     *
     * ✅ CONFIRMÉ:
     * - Byte [7] = Batterie en % ✅ CORRECT
     * - La vitesse N'EST PAS dans cette trame !
     */
    private fun parseRealtimeFrame(frame: ByteArray): ScooterData {
        // Byte 7 : Batterie (pourcentage) ✅ CORRECT
        val battery = (frame[7].toInt() and 0xFF).toFloat()

        Log.i(TAG, "   🔋 Batterie: ${battery}%")

        return ScooterData(
            battery = battery
        )
    }

    /**
     * Parse trame 0x30 - État/Mode
     * Format: 61 9E 30 17 35 C3 E1 35 3E CA
     */
    private fun parseStatusFrame(frame: ByteArray): ScooterData {
        // Byte 6 : Mode actuel
        val modeId = frame[6]
        val mode = when (modeId) {
            0xE1.toByte() -> RideMode.PEDESTRIAN
            0x36.toByte() -> RideMode.ECO
            0x37.toByte() -> RideMode.SPORT
            else -> {
                Log.w(TAG, "   ⚠️ Mode inconnu: 0x${"%02X".format(modeId)}")
                RideMode.ECO
            }
        }

        Log.i(TAG, "   🏍️ Mode: $mode (byte: 0x${"%02X".format(modeId)})")

        return ScooterData(currentMode = mode)
    }

    /**
     * Parse trame 0x32 - Températures
     * Format: 61 9E 32 17 35 0B A4 35 A4 35 40 CA
     *
     * ATTENTION: Les valeurs brutes (164, 181, etc.) semblent aberrantes
     * À VÉRIFIER avec des captures pendant la conduite
     */
    private fun parseTempFrame(frame: ByteArray): ScooterData {
        // Températures dans les bytes 5 et 7
        // ATTENTION: Les valeurs semblent incorrectes (164°C, 181°C = impossible)
        val temp1Raw = (frame[5].toInt() and 0xFF)
        val temp2Raw = (frame[7].toInt() and 0xFF)

        // Pour l'instant, on les log sans correction
        // TODO: Trouver la vraie formule de conversion
        val temp1 = temp1Raw.toFloat()
        val temp2 = temp2Raw.toFloat()

        Log.i(TAG, "   🌡️ Températures: T1=${temp1}°C (raw:$temp1Raw) | T2=${temp2}°C (raw:$temp2Raw)")

        if (temp1 > 100 || temp2 > 100) {
            Log.w(TAG, "   ⚠️ Températures aberrantes ! Vérifier la formule de conversion")
        }

        return ScooterData(temperature = temp1)
    }

    /**
     * Parse trame 0x02 - Initialisation
     * Format: 61 9E 02 17 35 2E FE B0 ... (67 bytes)
     *
     * Cette trame contient beaucoup d'informations au démarrage
     */
    private fun parseInitFrame(frame: ByteArray): ScooterData? {
        Log.i(TAG, "   📋 Trame d'initialisation (${frame.size} bytes)")

        // TODO: Analyser le contenu pour extraire les infos utiles
        // Possiblement : versions firmware, capacité batterie, etc.

        return null
    }

    /**
     * Parse trame 0x04 - Données étendues
     * Format: 61 9E 04 15 35 20 E1 34 FB 30 78 35 60 67 F2 77 8D EF ... (51 bytes)
     *
     * NOTE: L'odomètre n'a PAS été trouvé dans cette trame
     * Il est dans les trames 0x1A (DETAILED) et 0x16 (COMBINED)
     */
    private fun parseExtendedFrame(frame: ByteArray): ScooterData? {
        Log.i(TAG, "   📊 Trame étendue (pas d'odomètre ici)")
        return null
    }

    /**
     * ✅ Parse trame 0x1A - Données détaillées (ODOMÈTRE ICI !)
     * Format: 61 9E 1A 17 35 F6 9E 37 ... (49 bytes)
     *
     * TROUVÉ ! Odomètre aux offsets 9 et 11
     * Format: 2 bytes Little Endian en DÉCAMÈTRES (1 dam = 10 m)
     */
    private fun parseDetailedFrame(frame: ByteArray): ScooterData? {
        Log.i(TAG, "   🔍 Trame détaillée (${frame.size} bytes)")

        if (frame.size < 12) {
            return null
        }

        // ✅ NOUVEAU : Extraction odomètre à l'offset 9
        // 2 bytes Little Endian = décamètres
        val odometerRaw = ((frame[10].toInt() and 0xFF) shl 8) or (frame[9].toInt() and 0xFF)
        val odometer = odometerRaw / 100.0f  // Conversion décamètres → km

        Log.i(TAG, "   🛣️ Odomètre: ${String.format("%.2f", odometer)}km (${odometerRaw} dam)")

        return ScooterData(odometer = odometer)
    }

    /**
     * ✅ Parse trame 0x16 - Combinée (BATTERIE + ODOMÈTRE)
     * Format: 61 9E 16 17 35 DE ... (47 bytes)
     *
     * TROUVÉ ! Odomètre à l'offset 17
     * Format: 2 bytes Little Endian en DÉCAMÈTRES
     */
    private fun parseCombinedFrame(frame: ByteArray): ScooterData? {
        Log.i(TAG, "   🔄 Trame combinée (${frame.size} bytes)")

        // Batterie à [7]
        val battery = if (frame.size > 7) (frame[7].toInt() and 0xFF).toFloat() else 0f

        // ✅ NOUVEAU : Odomètre à l'offset 17
        val odometer = if (frame.size >= 19) {
            val raw = ((frame[18].toInt() and 0xFF) shl 8) or (frame[17].toInt() and 0xFF)
            raw / 100.0f  // Conversion décamètres → km
        } else 0f

        Log.i(TAG, "   🔋 Batterie: ${battery}% | 🛣️ Odomètre: ${String.format("%.2f", odometer)}km")

        return ScooterData(
            battery = battery,
            odometer = odometer
        )
    }

    /**
     * Réinitialise le buffer
     */
    fun reset() {
        buffer.clear()
        frameCount = 0
        Log.i(TAG, "🔄 Handler réinitialisé")
    }
}