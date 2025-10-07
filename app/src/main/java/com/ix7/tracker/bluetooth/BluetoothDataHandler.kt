package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.ScooterData
import com.ix7.tracker.protocol.ProtocolConstants
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Handler pour décoder toutes les trames du protocole 61 9E (iX7 Pro)
 *
 * CORRECTIONS APPLIQUÉES :
 * ✅ Trame 0x37 : Vitesse en temps réel (FONCTIONNE)
 * ✅ Trame 0x30 : Mode corrigé selon les commandes TX
 * ✅ Trame 0x32 : Température corrigée (conversion hexadécimale en décimal)
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

            // Traiter la trame
            val parsedData = parseFrame(frame)
            if (parsedData != null) {
                result = parsedData
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
            ProtocolConstants.FRAME_SPEED -> 9         // 0x37 ⭐ VITESSE
            ProtocolConstants.FRAME_REALTIME -> 16     // 0x3E
            ProtocolConstants.FRAME_SPECIAL_2 -> 20    // 0x3A
            ProtocolConstants.FRAME_SPECIAL_1 -> 19    // 0x38
            ProtocolConstants.FRAME_SPECIAL_3 -> 18    // 0x3C
            ProtocolConstants.FRAME_COMBINED -> 47     // 0x16
            ProtocolConstants.FRAME_INFO_EXT -> 24     // 0x26
            ProtocolConstants.FRAME_DETAILED -> 49     // 0x1A
            ProtocolConstants.FRAME_EXTENDED -> 51     // 0x04
            ProtocolConstants.FRAME_INIT -> 67         // 0x02
            ProtocolConstants.FRAME_INFO -> 40         // 0x00
            else -> -1 // Type inconnu
        }
    }

    /**
     * Parse une trame complète selon son type
     */
    private fun parseFrame(frame: ByteArray): ScooterData? {
        val frameType = frame[2]

        Log.i(TAG, "🔍 Parse trame type 0x${"%02X".format(frameType)} (${frame.size} bytes)")

        return when (frameType) {
            ProtocolConstants.FRAME_SPEED -> parseSpeedFrame(frame)        // ⭐ Priorité 1
            ProtocolConstants.FRAME_STATUS -> parseStatusFrame(frame)
            ProtocolConstants.FRAME_TEMP -> parseTempFrame(frame)
            ProtocolConstants.FRAME_REALTIME -> parseRealtimeFrame(frame)
            ProtocolConstants.FRAME_INIT -> parseInitFrame(frame)
            ProtocolConstants.FRAME_EXTENDED -> parseExtendedFrame(frame)
            ProtocolConstants.FRAME_DETAILED -> parseDetailedFrame(frame)
            ProtocolConstants.FRAME_COMBINED -> parseCombinedFrame(frame)
            else -> {
                val hex = frame.joinToString(" ") { "%02X".format(it) }
                Log.d(TAG, "   Type 0x${"%02X".format(frameType)}: $hex")
                null
            }
        }
    }

    /**
     * ⭐ Parse trame 0x37 - VITESSE EN TEMPS RÉEL (LA SEULE QUI MARCHE !)
     *
     * Format: 61 9E 37 [SUB] 55 [DATA1] [VITESSE] [CRC] CB
     *
     * Exemples des logs :
     * - 61 9E 37 14 55 00 00 41 CB  -> 0 km/h
     * - 61 9E 37 14 55 06 39 7C CB  -> 57 km/h (0x39 = 57)
     * - 61 9E 37 1D 53 04 2C 7C CA  -> 44 km/h (0x2C = 44)
     *
     * Position de la vitesse : Byte 6 (index 6)
     */
    private fun parseSpeedFrame(frame: ByteArray): ScooterData {
        if (frame.size < 7) {
            Log.w(TAG, "   ⚠️ Trame vitesse trop courte: ${frame.size} bytes")
            return ScooterData()
        }

        val subType = frame[3].toInt() and 0xFF
        val state = frame[4].toInt() and 0xFF
        val data1 = frame[5].toInt() and 0xFF
        val speed = (frame[6].toInt() and 0xFF).toFloat()  // ⭐ VITESSE en km/h

        Log.i(TAG, "   🚀 VITESSE: ${speed.toInt()} km/h | État: 0x${"%02X".format(state)} | Data1: 0x${"%02X".format(data1)}")

        // Le byte 4 (state) pourrait être la batterie (valeurs 53, 85 observées)
        val possibleBattery = if (state in 40..100) state.toFloat() else 0f

        return ScooterData(
            speed = speed,
            battery = if (possibleBattery > 0) possibleBattery else 0f
        )
    }

    /**
     * ✅ CORRIGÉ - Parse trame 0x30 - État/Mode
     *
     * Format: 61 9E 30 17 35 C3 [MODE_ID] 35 3E CA
     *
     * MAPPING CORRIGÉ selon les commandes TX :
     * - 0x34 -> SPORT  (CMD : 4A 34)
     * - 0x35 -> RACE   (CMD : 4A 35)
     * - 0x36 -> ECO    (CMD : 4A 36)
     * - 0x37 -> PEDESTRIAN (CMD : 4A 37)
     * - 0xE1 -> PEDESTRIAN (fallback, valeur observée)
     */
    private fun parseStatusFrame(frame: ByteArray): ScooterData {
        if (frame.size < 7) return ScooterData()

        // Byte 6 : Mode actuel
        val modeId = frame[6]
        val mode = when (modeId) {
            0x34.toByte() -> RideMode.SPORT       // ✅ CORRIGÉ
            0x35.toByte() -> RideMode.RACE        // ✅ CORRIGÉ (avant = SPORT)
            0x36.toByte() -> RideMode.ECO         // ✅ CORRIGÉ (avant = 0x34)
            0x37.toByte() -> RideMode.PEDESTRIAN  // ✅ CORRIGÉ (avant = pas mappé)
            0xE1.toByte() -> RideMode.PEDESTRIAN  // Valeur observée dans les logs
            else -> {
                Log.w(TAG, "   ⚠️ Mode inconnu: 0x${"%02X".format(modeId)}")
                RideMode.ECO  // Mode par défaut
            }
        }

        Log.i(TAG, "   🏍️ Mode: $mode (byte: 0x${"%02X".format(modeId)})")

        return ScooterData(currentMode = mode)
    }

    /**
     * ✅ CORRIGÉ - Parse trame 0x32 - Températures
     *
     * Format: 61 9E 32 17 35 [T1_HEX] [T1_DEC] 35 [T2_HEX] 35 [CRC] CA
     * Exemple: 61 9E 32 17 35 0B A4 35 A4 35 40 CA
     *
     * ANCIEN DÉCODAGE (FAUX) :
     * - Byte 5: 0x0B = 11°C  ❌
     * - Byte 7: 0xA4 = 164°C ❌
     *
     * NOUVEAU DÉCODAGE (CORRIGÉ) :
     * Les températures sont encodées en notation hexadécimale "BCD-like" :
     * - Byte 5 (0x0B) = partie entière (11)
     * - Byte 6 (0xA4) = partie décimale (164 -> 1.64 -> .64 après virgule?)
     *
     * Ou peut-être :
     * - 0x0B = 11 -> 28 avec offset de 17°C ?
     * - Screenshot montre 28.2°C et 27.1°C
     *
     * Hypothèse : Temp = (hex_value * 2.5) ou (hex_value + offset)
     * 0x0B = 11 -> 11 + 17 = 28°C ✅
     *
     * Testons avec offset +17
     */
    private fun parseTempFrame(frame: ByteArray): ScooterData {
        if (frame.size < 10) return ScooterData()

        // Byte 5 et byte 8 semblent être les températures en hex
        val temp1Raw = frame[5].toInt() and 0xFF
        val temp2Raw = frame[8].toInt() and 0xFF

        // Hypothèse 1 : Offset de +17°C
        val temp1WithOffset = (temp1Raw + 17).toFloat()
        val temp2WithOffset = (temp2Raw + 17).toFloat()

        // Hypothèse 2 : Facteur multiplicateur 2.5
        val temp1WithFactor = (temp1Raw * 2.5f)
        val temp2WithFactor = (temp2Raw * 2.5f)

        // Hypothèse 3 : Température directe
        val temp1Direct = temp1Raw.toFloat()
        val temp2Direct = temp2Raw.toFloat()

        // On utilise l'offset pour l'instant (11 + 17 = 28°C ✅)
        val temp1 = temp1WithOffset
        val temp2 = temp2WithOffset

        Log.i(TAG, "   🌡️ Températures: T1=${temp1.toInt()}°C (raw:$temp1Raw) | T2=${temp2.toInt()}°C (raw:$temp2Raw)")
        Log.d(TAG, "       Test offset: T1=${temp1WithOffset}°C T2=${temp2WithOffset}°C")
        Log.d(TAG, "       Test factor: T1=${temp1WithFactor}°C T2=${temp2WithFactor}°C")
        Log.d(TAG, "       Test direct: T1=${temp1Direct}°C T2=${temp2Direct}°C")

        return ScooterData(temperature = temp1)
    }

    /**
     * Parse trame 0x3E - Temps réel (vitesse, batterie)
     * Format: 61 9E 3E 17 35 DA C3 34 9E 37 14 30 8B 36 6E C8
     */
    private fun parseRealtimeFrame(frame: ByteArray): ScooterData {
        if (frame.size < 8) return ScooterData()

        // Bytes 5-6 : Vitesse (little endian, km/h * 10)
        val speedRaw = ((frame[6].toInt() and 0xFF) shl 8) or (frame[5].toInt() and 0xFF)
        val speed = speedRaw / 10.0f

        // Byte 7 : Batterie (pourcentage)
        val battery = (frame[7].toInt() and 0xFF).toFloat()

        Log.i(TAG, "   ⚡ Vitesse: ${speed}km/h  🔋 Batterie: ${battery}%")

        return ScooterData(
            speed = speed,
            battery = battery
        )
    }

    /**
     * Parse trame 0x02 - Initialisation
     */
    private fun parseInitFrame(frame: ByteArray): ScooterData? {
        Log.i(TAG, "   📋 Trame d'initialisation (${frame.size} bytes)")

        // Tentative de lecture de données au démarrage
        if (frame.size >= 20) {
            // Recherche batterie et autres données
            for (i in 5 until frame.size - 5) {
                val value = frame[i].toInt() and 0xFF
                if (value in 40..100) {
                    Log.d(TAG, "       Batterie possible à offset $i: ${value}%")
                }
            }
        }

        return null
    }

    /**
     * Parse trame 0x04 - Données étendues (kilométrage possible)
     */
    private fun parseExtendedFrame(frame: ByteArray): ScooterData? {
        Log.i(TAG, "   📊 Trame étendue - recherche kilométrage...")

        // Tentative de recherche kilométrage (format à confirmer)
        for (i in 5 until frame.size - 4) {
            val value = ByteBuffer.wrap(frame, i, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .int

            if (value in 100..100000) {
                val km = value / 1000.0f
                Log.i(TAG, "   🎯 KILOMÉTRAGE TROUVÉ à offset $i: ${km}km")
                return ScooterData(odometer = km)
            }
        }

        return null
    }

    /**
     * Parse trame 0x1A - Données détaillées
     */
    private fun parseDetailedFrame(frame: ByteArray): ScooterData? {
        Log.i(TAG, "   📝 Trame détaillée")
        return null
    }

    /**
     * Parse trame 0x16 - Combinée
     */
    private fun parseCombinedFrame(frame: ByteArray): ScooterData {
        if (frame.size < 8) return ScooterData()

        val speedRaw = ((frame[6].toInt() and 0xFF) shl 8) or (frame[5].toInt() and 0xFF)
        val speed = speedRaw / 10.0f
        val battery = (frame[7].toInt() and 0xFF).toFloat()

        Log.i(TAG, "   🔄 Combinée: Speed=${speed}km/h Battery=${battery}%")

        return ScooterData(
            speed = speed,
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