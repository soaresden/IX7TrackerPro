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
 * CORRECTIONS APPORTÉES (Janvier 2025):
 * - Trame 0x3E: Les bytes [5-6] NE SONT PAS la vitesse !
 * - La batterie à [7] est CORRECTE
 * - La vitesse réelle doit être cherchée ailleurs ou dans les métadonnées BLE
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
     * CORRECTION CRITIQUE:
     * - Les bytes [5-6] NE SONT PAS la vitesse !
     * - Byte [7] = Batterie en % ✓ CORRECT
     * - La vitesse est probablement dans une autre trame ou dans les métadonnées BLE
     */
    private fun parseRealtimeFrame(frame: ByteArray): ScooterData {
        // ❌ ANCIEN CODE (FAUX):
        // val speedRaw = ((frame[6].toInt() and 0xFF) shl 8) or (frame[5].toInt() and 0xFF)
        // val speed = speedRaw / 10.0f
        // → Donnait 5013.8 km/h (aberrant !)

        // ✓ NOUVEAU CODE (CORRIGÉ):
        // La vitesse n'est PAS dans cette trame de 16 bytes
        // Pour l'instant, on met 0.0f
        // TODO: Chercher la vitesse dans les trames 0x16 (Combined) ou 0x1A (Detailed)
        val speed = 0.0f

        // Byte 7 : Batterie (pourcentage) ✓ CORRECT
        val battery = (frame[7].toInt() and 0xFF).toFloat()

        Log.i(TAG, "   🔋 Batterie: ${battery}% (vitesse non disponible dans cette trame)")
        Log.d(TAG, "   ⚠️ Bytes [5-6] = 0x${"%02X".format(frame[5])}${"%02X".format(frame[6])} (ne sont PAS la vitesse)")

        return ScooterData(
            speed = speed,      // 0.0f pour l'instant
            battery = battery   // CORRECT ✓
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
     * Parse trame 0x04 - Données étendues (KILOMÉTRAGE TOTAL ICI !)
     * Format: 61 9E 04 15 35 20 E1 34 FB 30 78 35 60 67 F2 77 8D EF ... (51 bytes)
     *
     * IMPORTANT: Cette trame contient le KILOMÉTRAGE TOTAL et le TEMPS DE CONDUITE
     */
    private fun parseExtendedFrame(frame: ByteArray): ScooterData? {
        Log.i(TAG, "   📊 Trame étendue - recherche kilométrage...")

        // Le kilométrage est probablement encodé dans les bytes 5-20
        // Chercher une valeur raisonnable (0-1000 km)

        // Tentative 1: Little endian 4 bytes (mètres)
        for (i in 5 until frame.size - 4) {
            val value = ByteBuffer.wrap(frame, i, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .int

            // Chercher une valeur entre 0 et 1000000 mètres (0-1000 km)
            if (value in 100..1000000) {
                val km = value / 1000.0f
                Log.i(TAG, "   🎯 KILOMÉTRAGE TROUVÉ à offset $i: ${km}km (${value}m)")
                return ScooterData(odometer = km)
            }
        }

        // Tentative 2: Little endian 2 bytes (décamètres)
        for (i in 5 until frame.size - 2) {
            val value = ((frame[i+1].toInt() and 0xFF) shl 8) or (frame[i].toInt() and 0xFF)

            // Si c'est en décamètres: 400km = 40000 décamètres = 0x9C40
            if (value in 100..10000) {
                val km = value / 100.0f
                Log.i(TAG, "   🎯 KILOMÉTRAGE TROUVÉ (décam) à offset $i: ${km}km")
                return ScooterData(odometer = km)
            }
        }

        // Log pour analyse manuelle
        val hex = frame.joinToString(" ") { "%02X".format(it) }
        Log.w(TAG, "   ⚠️ Kilométrage non trouvé dans: $hex")

        return null
    }

    /**
     * Parse trame 0x1A - Données détaillées
     * Format: 61 9E 1A 17 35 F6 9E 37 ... (49 bytes)
     *
     * HYPOTHÈSE: Cette trame pourrait contenir la vitesse !
     */
    private fun parseDetailedFrame(frame: ByteArray): ScooterData? {
        Log.i(TAG, "   📝 Trame détaillée (${frame.size} bytes)")

        // TODO: Analyser cette trame pour trouver la vitesse
        // Chercher des bytes qui varient selon la vitesse réelle

        return null
    }

    /**
     * Parse trame 0x16 - Combinée
     * Format: 61 9E 16 17 35 DE ... (47 bytes)
     *
     * HYPOTHÈSE: Cette trame pourrait contenir la vitesse !
     */
    private fun parseCombinedFrame(frame: ByteArray): ScooterData? {
        Log.i(TAG, "   🔄 Trame combinée (${frame.size} bytes)")

        // TODO: Analyser cette trame pour trouver la vitesse
        // La batterie semble être aussi à [7] = 52%

        val battery = if (frame.size > 7) (frame[7].toInt() and 0xFF).toFloat() else 0f

        Log.i(TAG, "   🔋 Batterie: ${battery}%")

        return ScooterData(
            speed = 0.0f,       // À TROUVER
            battery = battery
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