package com.ix7.tracker.protocol

import android.util.Log
import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.ScooterData
import com.ix7.tracker.core.SpeedLimitMode

/**
 * Parser pour décoder les trames de données de la trottinette M0Robot
 *
 * Basé sur l'analyse des logs Bluetooth et du protocole M0Robot
 *
 * Types de trames principaux:
 * - 0x37: Données en temps réel (vitesse, batterie, tension, température)
 * - 0x3E: Batterie uniquement
 * - 0x30: État/Mode de conduite
 * - 0x32: Températures
 * - 0x1A: Données détaillées (odomètre)
 * - 0x16: Trame combinée (batterie + odomètre)
 */
object ScooterDataParser {
    private const val TAG = "ScooterDataParser"

    // Headers de trame
    private const val HEADER_1: Byte = 0x61
    private const val HEADER_2: Byte = 0x9E.toByte()

    // Types de trames
    private const val FRAME_REALTIME: Byte = 0x37      // Données temps réel complètes
    private const val FRAME_BATTERY: Byte = 0x3E.toByte()  // Batterie seulement
    private const val FRAME_STATUS: Byte = 0x30        // État/Mode
    private const val FRAME_TEMP: Byte = 0x32          // Températures
    private const val FRAME_DETAILED: Byte = 0x1A      // Détails (odomètre)
    private const val FRAME_COMBINED: Byte = 0x16      // Combinée (batterie + odomètre)

    /**
     * Parse une trame complète et retourne les données mises à jour
     *
     * @param frame Trame complète reçue
     * @param currentData Données actuelles (pour mise à jour incrémentale)
     * @return Nouvelles données mises à jour, ou null si parsing échoué
     */
    fun parseFrame(frame: ByteArray, currentData: ScooterData = ScooterData()): ScooterData? {
        if (frame.size < 5) {
            Log.w(TAG, "⚠️ Trame trop courte: ${frame.size} bytes")
            return null
        }

        // Vérifier les headers
        if (frame[0] != HEADER_1 || frame[1] != HEADER_2) {
            Log.w(TAG, "⚠️ Headers invalides: ${toHex(frame[0])} ${toHex(frame[1])}")
            return null
        }

        val frameType = frame[2]
        val hex = frame.joinToString(" ") { "%02X".format(it) }

        Log.d(TAG, "📥 Parse type ${toHex(frameType)} (${frame.size} bytes)")

        return when (frameType) {
            FRAME_REALTIME -> parseRealtimeFrame(frame, currentData)
            FRAME_BATTERY -> parseBatteryFrame(frame, currentData)
            FRAME_STATUS -> parseStatusFrame(frame, currentData)
            FRAME_TEMP -> parseTempFrame(frame, currentData)
            FRAME_DETAILED -> parseDetailedFrame(frame, currentData)
            FRAME_COMBINED -> parseCombinedFrame(frame, currentData)
            else -> {
                Log.d(TAG, "   Type inconnu: $hex")
                null
            }
        }
    }

    /**
     * ✅ Parse trame 0x37 - Données temps réel complètes
     *
     * C'est LA trame la plus importante ! Contient :
     * - Vitesse (bytes 5-6)
     * - Batterie % (byte 7)
     * - Tension (bytes 8-9)
     * - Courant (bytes 10-11)
     * - Température (byte 12)
     * - Mode de conduite (byte 20)
     * - États (phares, verrouillage, etc.) (byte 21)
     * - Temps de trajet total (bytes 26-29)
     *
     */
    private fun parseRealtimeFrame(frame: ByteArray, current: ScooterData): ScooterData {
        if (frame.size < 30) {
            Log.w(TAG, "⚠️ Trame 0x37 incomplète: ${frame.size} bytes")
            return current
        }

        try {
            // VITESSE (bytes 5-6) - Little Endian, en dixièmes de km/h
            val speedRaw = ((frame[6].toInt() and 0xFF) shl 8) or (frame[5].toInt() and 0xFF)
            val speed = speedRaw / 10.0f

            // BATTERIE (byte 7) - Pourcentage direct
            val battery = (frame[7].toInt() and 0xFF).toFloat().coerceIn(0f, 100f)

            // TENSION (bytes 8-9) - Little Endian, en dixièmes de volts
            val voltageRaw = ((frame[9].toInt() and 0xFF) shl 8) or (frame[8].toInt() and 0xFF)
            val voltage = voltageRaw / 10.0f

            // COURANT (bytes 10-11) - Little Endian, en dixièmes d'ampères
            val currentRaw = ((frame[11].toInt() and 0xFF) shl 8) or (frame[10].toInt() and 0xFF)
            val currentAmp = currentRaw / 10.0f

            // TEMPÉRATURE (byte 12) - Offset de -40°C (standard)
            val temperature = (frame[12].toInt() and 0xFF) - 40.0f

            // PUISSANCE = Tension × Courant
            val power = voltage * currentAmp

            // MODE DE CONDUITE (byte 20 - nibble bas)
            val modeId = frame[20].toInt() and 0x0F
            val mode = when (modeId) {
                0x01 -> RideMode.PEDESTRIAN
                0x02 -> RideMode.ECO
                0x03 -> RideMode.SPORT
                0x04 -> RideMode.RACE
                else -> {
                    Log.w(TAG, "   ⚠️ Mode inconnu: 0x${modeId.toString(16)}")
                    current.currentMode
                }
            }

            // ÉTATS (byte 21)
            val stateByte = frame[21].toInt() and 0xFF
            val headlightsOn = (stateByte and 0x01) != 0
            val isLocked = (stateByte and 0x02) != 0
            val neonOn = (stateByte and 0x04) != 0
            val cruiseControl = (stateByte and 0x08) != 0

            // LIMITEUR DE VITESSE (byte 21 - bit 4)
            val speedLimitMode = if ((stateByte and 0x10) != 0) {
                SpeedLimitMode.UNLIMITED
            } else {
                SpeedLimitMode.LIMITED
            }

            // TEMPS TOTAL DE TRAJET (bytes 26-29) - Big Endian, en secondes
            val totalSeconds = if (frame.size >= 30) {
                ((frame[26].toInt() and 0xFF) shl 24) or
                        ((frame[27].toInt() and 0xFF) shl 16) or
                        ((frame[28].toInt() and 0xFF) shl 8) or
                        (frame[29].toInt() and 0xFF)
            } else 0

            val totalRideTime = formatTime(totalSeconds)

            Log.d(TAG, "   🏍️ Vitesse: ${String.format("%.1f", speed)} km/h")
            Log.d(TAG, "   🔋 Batterie: ${battery.toInt()}% | ${String.format("%.1f", voltage)}V")
            Log.d(TAG, "   ⚡ Courant: ${String.format("%.1f", currentAmp)}A | ${String.format("%.0f", power)}W")
            Log.d(TAG, "   🌡️ Température: ${String.format("%.1f", temperature)}°C")
            Log.d(TAG, "   🎮 Mode: $mode | Phares: $headlightsOn | Verrouillé: $isLocked")

            return current.copy(
                speed = speed,
                battery = battery,
                voltage = voltage,
                current = currentAmp,
                temperature = temperature,
                power = power,
                currentMode = mode,
                headlightsOn = headlightsOn,
                isLocked = isLocked,
                neonOn = neonOn,
                cruiseControl = cruiseControl,
                speedLimitMode = speedLimitMode,
                totalRideTime = totalRideTime
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur parsing 0x37", e)
            return current
        }
    }

    /**
     * Parse trame 0x3E - Batterie uniquement
     * Format: 61 9E 3E 17 35 DA C3 34 9E 37 14 30 8B 36 6E C8
     */
    private fun parseBatteryFrame(frame: ByteArray, current: ScooterData): ScooterData {
        if (frame.size < 8) return current

        val battery = (frame[7].toInt() and 0xFF).toFloat().coerceIn(0f, 100f)

        Log.d(TAG, "   🔋 Batterie: ${battery.toInt()}%")

        return current.copy(battery = battery)
    }

    /**
     * Parse trame 0x30 - État/Mode
     * Format: 61 9E 30 17 35 C3 E1 35 3E CA
     */
    private fun parseStatusFrame(frame: ByteArray, current: ScooterData): ScooterData {
        if (frame.size < 7) return current

        val modeId = frame[6]
        val mode = when (modeId) {
            0xE1.toByte() -> RideMode.PEDESTRIAN
            0x36.toByte() -> RideMode.ECO
            0x37.toByte() -> RideMode.SPORT
            0x35.toByte() -> RideMode.RACE
            else -> {
                Log.w(TAG, "   ⚠️ Mode inconnu: ${toHex(modeId)}")
                current.currentMode
            }
        }

        Log.d(TAG, "   🎮 Mode: $mode")

        return current.copy(currentMode = mode)
    }

    /**
     * Parse trame 0x32 - Températures
     * Format: 61 9E 32 17 35 0B A4 35 A4 35 40 CA
     *
     * ATTENTION: Les valeurs brutes peuvent sembler aberrantes (164°C)
     * Une correction peut être nécessaire
     */
    private fun parseTempFrame(frame: ByteArray, current: ScooterData): ScooterData {
        if (frame.size < 8) return current

        // Température 1 (byte 6) - Peut nécessiter offset
        val temp1Raw = frame[6].toInt() and 0xFF
        val temperature = (temp1Raw - 40).toFloat() // Offset standard

        Log.d(TAG, "   🌡️ Température: ${String.format("%.1f", temperature)}°C (raw: $temp1Raw)")

        if (temperature > 100 || temperature < -20) {
            Log.w(TAG, "   ⚠️ Température aberrante - vérifier formule")
        }

        return current.copy(temperature = temperature)
    }

    /**
     * ✅ Parse trame 0x1A - Données détaillées (ODOMÈTRE)
     * Format: 61 9E 1A 17 35 F6 9E 37 ... (49 bytes)
     *
     * Odomètre aux offsets 9-10: 2 bytes Little Endian en DÉCAMÈTRES
     */
    private fun parseDetailedFrame(frame: ByteArray, current: ScooterData): ScooterData {
        if (frame.size < 11) return current

        // Odomètre (bytes 9-10) - Little Endian, en décamètres (1 dam = 10 m)
        val odometerRaw = ((frame[10].toInt() and 0xFF) shl 8) or (frame[9].toInt() and 0xFF)
        val odometer = odometerRaw / 100.0f  // Conversion décamètres → km

        Log.d(TAG, "   🛣️ Odomètre: ${String.format("%.2f", odometer)} km")

        return current.copy(odometer = odometer)
    }

    /**
     * ✅ Parse trame 0x16 - Combinée (BATTERIE + ODOMÈTRE)
     * Format: 61 9E 16 17 35 DE ... (47 bytes)
     *
     * - Batterie à l'offset 7
     * - Odomètre aux offsets 17-18
     */
    private fun parseCombinedFrame(frame: ByteArray, current: ScooterData): ScooterData {
        if (frame.size < 19) return current

        // Batterie (byte 7)
        val battery = (frame[7].toInt() and 0xFF).toFloat().coerceIn(0f, 100f)

        // Odomètre (bytes 17-18) - Little Endian, en décamètres
        val odometerRaw = ((frame[18].toInt() and 0xFF) shl 8) or (frame[17].toInt() and 0xFF)
        val odometer = odometerRaw / 100.0f

        Log.d(TAG, "   🔋 Batterie: ${battery.toInt()}% | 🛣️ Odomètre: ${String.format("%.2f", odometer)} km")

        return current.copy(
            battery = battery,
            odometer = odometer
        )
    }

    /**
     * Formate un temps en secondes vers "HH:MM:SS"
     */
    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%dH %dM %dS", hours, minutes, secs)
    }

    /**
     * Convertit un byte en hex pour les logs
     */
    private fun toHex(byte: Byte): String = "0x%02X".format(byte)

    /**
     * Analyse rapide d'une trame pour déterminer son type
     */
    fun analyzeFrame(frame: ByteArray): String {
        if (frame.size < 3) return "Trame invalide"

        return when (frame[2]) {
            FRAME_REALTIME -> "Données temps réel (0x37)"
            FRAME_BATTERY -> "Batterie (0x3E)"
            FRAME_STATUS -> "État/Mode (0x30)"
            FRAME_TEMP -> "Températures (0x32)"
            FRAME_DETAILED -> "Détails/Odomètre (0x1A)"
            FRAME_COMBINED -> "Combinée (0x16)"
            else -> "Type inconnu (${toHex(frame[2])})"
        }
    }

    /**
     * Valide qu'une trame est bien formée
     */
    fun validateFrame(frame: ByteArray): Boolean {
        if (frame.size < 5) return false
        if (frame[0] != HEADER_1 || frame[1] != HEADER_2) return false

        // TODO: Vérifier checksum si nécessaire

        return true
    }
}