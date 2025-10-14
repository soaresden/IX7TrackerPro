package com.ix7.tracker.protocol

import android.util.Log
import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.ScooterData

/**
 * Parser pour décoder les trames de données de la trottinette M0Robot
 * VERSION CORRIGÉE - Offsets validés par analyse des logs réels
 *
 *
 * Types de trames:
 * - 0x02: Données principales (batterie, température, puissance, tension)
 * - 0x04: Odomètre total
 * - 0x32: Vitesse actuelle
 * - 0x37: Temps réel complet (PRIORITAIRE - contient toutes les données!)
 */
object DebugParser {
    private const val TAG = "ScooterParser"

    // Headers
    private const val HEADER_1: Byte = 0x61
    private const val HEADER_2: Byte = 0x9E.toByte()

    // Types de trames
    private const val TYPE_MAIN: Byte = 0x02
    private const val TYPE_ODOMETER: Byte = 0x04
    private const val TYPE_SPEED: Byte = 0x32
    private const val TYPE_REALTIME: Byte = 0x37

    /**
     * Parse une trame complète et retourne les données mises à jour
     */
    fun parseFrame(frame: ByteArray, currentData: ScooterData = ScooterData()): ScooterData? {
        // Vérifier header
        if (frame.size < 3 || frame[0] != HEADER_1 || frame[1] != HEADER_2) {
            return null
        }

        val type = frame[2]

        return when (type) {
            TYPE_MAIN -> parseMainFrame(frame, currentData)
            TYPE_ODOMETER -> parseOdometerFrame(frame, currentData)
            TYPE_SPEED -> parseSpeedFrame(frame, currentData)
            TYPE_REALTIME -> parseRealtimeFrame(frame, currentData)
            else -> {
                Log.d(TAG, "Type de trame non géré: 0x${String.format("%02X", type)}")
                null
            }
        }
    }

    /**
     * Parse Type 0x02 - Données principales
     * Offsets validés:
     * [3-4]: Température BE16/1000
     * [10-11]: Puissance BE16
     * [12]: Batterie (byte - 40)
     * [15]: Tension (byte direct)
     */
    private fun parseMainFrame(frame: ByteArray, currentData: ScooterData): ScooterData? {
        if (frame.size < 16) return null

        try {
            // Température [3-4]: Big Endian / 1000
            val tempRaw = ((frame[3].toInt() and 0xFF) shl 8) or (frame[4].toInt() and 0xFF)
            val temperature = tempRaw / 1000.0f

            // Puissance [10-11]: Big Endian
            val powerRaw = ((frame[10].toInt() and 0xFF) shl 8) or (frame[11].toInt() and 0xFF)
            val power = powerRaw.toFloat()

            // Batterie [12]: byte - 40
            val battery = ((frame[12].toInt() and 0xFF) - 40).toFloat().coerceIn(0f, 100f)

            // Tension [15]: byte direct
            val voltage = (frame[15].toInt() and 0xFF).toFloat()

            Log.d(TAG, "Type 0x02: Bat=${battery.toInt()}% Temp=${String.format("%.1f", temperature)}°C " +
                    "Power=${String.format("%.0f", power)}W Volt=${String.format("%.0f", voltage)}V")

            return currentData.copy(
                battery = battery,
                temperature = temperature,
                power = power,
                voltage = voltage
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing 0x02: ${e.message}")
            return null
        }
    }

    /**
     * Parse Type 0x04 - Odomètre
     * Offsets validés:
     * [2-3]: Odomètre BE16/10 en km
     */
    private fun parseOdometerFrame(frame: ByteArray, currentData: ScooterData): ScooterData? {
        if (frame.size < 4) return null

        try {
            // Odomètre [2-3]: Big Endian / 10
            val odoRaw = ((frame[2].toInt() and 0xFF) shl 8) or (frame[3].toInt() and 0xFF)
            val odometer = odoRaw / 10.0f

            Log.d(TAG, "Type 0x04: Odomètre=${String.format("%.1f", odometer)}km")

            return currentData.copy(odometer = odometer)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing 0x04: ${e.message}")
            return null
        }
    }

    /**
     * Parse Type 0x32 - Vitesse
     * Offsets validés:
     * [5]: Vitesse en km/h (byte direct)
     */
    private fun parseSpeedFrame(frame: ByteArray, currentData: ScooterData): ScooterData? {
        if (frame.size < 6) return null

        try {
            // Vitesse [5]: byte direct
            val speed = (frame[5].toInt() and 0xFF).toFloat()

            Log.d(TAG, "Type 0x32: Vitesse=${speed.toInt()}km/h")

            return currentData.copy(speed = speed)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing 0x32: ${e.message}")
            return null
        }
    }

    /**
     * Parse Type 0x37 - Temps réel (TRAME LA PLUS COMPLETE)
     * Offsets validés:
     * [5-6]: Vitesse LE16/10
     * [7]: Batterie %
     * [8-9]: Tension LE16/10
     * [10-11]: Courant LE16/10
     * [12]: Température (byte - 40)
     * [20]: Mode (nibble bas)
     * [21]: États (bits)
     */
    private fun parseRealtimeFrame(frame: ByteArray, currentData: ScooterData): ScooterData? {
        if (frame.size < 22) return null

        try {
            // Vitesse [5-6]: Little Endian / 10
            val speedRaw = ((frame[6].toInt() and 0xFF) shl 8) or (frame[5].toInt() and 0xFF)
            val speed = speedRaw / 10.0f

            // Batterie [7]: % direct
            val battery = (frame[7].toInt() and 0xFF).toFloat().coerceIn(0f, 100f)

            // Tension [8-9]: Little Endian / 10
            val voltageRaw = ((frame[9].toInt() and 0xFF) shl 8) or (frame[8].toInt() and 0xFF)
            val voltage = voltageRaw / 10.0f

            // Courant [10-11]: Little Endian / 10
            val currentRaw = ((frame[11].toInt() and 0xFF) shl 8) or (frame[10].toInt() and 0xFF)
            val current = currentRaw / 10.0f

            // Température [12]: byte - 40
            val temperature = ((frame[12].toInt() and 0xFF) - 40).toFloat()

            // Puissance = Tension × Courant
            val power = voltage * current

            // Mode [20]: nibble bas
            val modeId = frame[20].toInt() and 0x0F
            val mode = when (modeId) {
                0x01 -> RideMode.PEDESTRIAN
                0x02 -> RideMode.ECO
                0x03 -> RideMode.SPORT
                0x04 -> RideMode.RACE
                else -> currentData.currentMode
            }

            // États [21]
            val stateByte = frame[21].toInt() and 0xFF
            val headlightsOn = (stateByte and 0x01) != 0
            val isLocked = (stateByte and 0x02) != 0

            Log.d(TAG, "Type 0x37: Vitesse=${String.format("%.1f", speed)}km/h " +
                    "Bat=${battery.toInt()}% Volt=${String.format("%.1f", voltage)}V " +
                    "Curr=${String.format("%.1f", current)}A Temp=${String.format("%.1f", temperature)}°C " +
                    "Mode=${mode.name} Lumières=$headlightsOn Verrouillé=$isLocked")

            return currentData.copy(
                speed = speed,
                battery = battery,
                voltage = voltage,
                current = current,
                temperature = temperature,
                power = power,
                currentMode = mode,
                headlightsOn = headlightsOn,
                isLocked = isLocked
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing 0x37: ${e.message}")
            return null
        }
    }
}