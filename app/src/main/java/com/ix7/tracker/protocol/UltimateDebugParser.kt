package com.ix7.tracker.protocol

import android.util.Log
import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.ScooterData
import java.util.Date

/**
 * PARSER ULTIME CONSOLIDÉ
 * Fusionne UltimateDebugParser (bonne structure) + ScooterDataParser (vraies formules)
 *
 * PROTOCOLE M0ROBOT:
 * - Header: 0x61 0x9E (bytes 0-1)
 * - Type: byte[2] (0x16, 0x1A, 0x30, 0x32, 0x37, 0x3E, 0xD3, etc.)
 * - Subtype: bytes[3-4] (0x1455, 0x1735, 0x1437, etc.)
 * - Tailles variables: 10 à 44+ bytes
 *
 * FORMULES VALIDÉES (de ScooterDataParser):
 * ✓ Batterie: byte[30] direct (50-100%) OU byte[7] OU byte[43]
 * ✓ Vitesse: (byte[30] * 45) / 253 km/h OU byte[5] direct
 * ✓ Odomètre: ((byte[30] << 8) | byte[29]) / 10 km
 * ✓ Temps total: bytes[5-6] big-endian = minutes (0x1735 only)
 */
object UltimateDebugParser {
    private const val TAG = "🔍PARSER"

    fun parseWithFullDebug(frame: ByteArray, currentData: ScooterData): ScooterData? {
        if (frame.size < 3) {
            Log.e(TAG, "❌ Trame trop courte: ${frame.size} bytes")
            return null
        }

        if (frame[0] != 0x61.toByte() || frame[1] != 0x9E.toByte()) {
            Log.e(TAG, "❌ HEADERS INVALIDES! Attendu: 61 9E")
            return null
        }

        val type = frame[2].toInt() and 0xFF
        val subtype = if (frame.size >= 5) {
            ((frame[3].toInt() and 0xFF) shl 8) or (frame[4].toInt() and 0xFF)
        } else {
            0
        }

        Log.d(TAG, "📥 Type=0x${String.format("%02X", type)} Subtype=0x${String.format("%04X", subtype)} Size=${frame.size}")
        logFullFrame(frame)

        // D'abord essayer par TYPE (0x30, 0x32, 0x37, etc.)
        val resultByType = when (type) {
            0x16 -> parse0x16(frame, currentData)
            0x1A -> parse0x1A(frame, currentData)
            0x30 -> parse0x30(frame, currentData)
            0x32 -> parse0x32(frame, currentData)
            0x37 -> parse0x37(frame, currentData)
            0x3E -> parse0x3E(frame, currentData)
            0xD3 -> parse0xD3(frame, currentData)
            else -> null
        }

        if (resultByType != null) return resultByType

        // Sinon essayer par SUBTYPE (0x1455, 0x1735 - anciennes trames)
        return when (subtype) {
            0x1455 -> parseSubtype1455(frame, currentData)  // Mouvement
            0x1735 -> parseSubtype1735(frame, currentData)  // Système
            0x1437 -> null
            else -> {
                Log.w(TAG, "⚠️ Type/Subtype inconnu")
                null
            }
        }
    }

    // ==================== SUBTYPE 0x1455 - MOUVEMENT (ScooterDataParser) ====================
    private fun parseSubtype1455(frame: ByteArray, currentData: ScooterData): ScooterData? {
        Log.d(TAG, "⚡ Parsing Subtype 0x1455 - MOUVEMENT")
        if (frame.size < 31) return null

        try {
            val byte29 = frame[29].toInt() and 0xFF
            val byte30 = frame[30].toInt() and 0xFF

            // VITESSE: (byte[30] * 45) / 253
            val speed = (byte30 * 45.0f) / 253.0f

            // TRIP DISTANCE: ((byte[13] << 8) | byte[12]) / 10
            val byte12 = frame[12].toInt() and 0xFF
            val byte13 = frame[13].toInt() and 0xFF
            val trip = ((byte13 shl 8) or byte12) / 10.0f

            // ODOMÈTRE: ((byte[30] << 8) | byte[29]) / 10
            val odometer = ((byte30 shl 8) or byte29) / 10.0f

            // BATTERIE: Détection automatique
            val battery: Float? = if (byte30 >= 50 && speed > 100) {
                byte30.toFloat().coerceIn(50f, 100f)
            } else {
                null
            }

            Log.d(TAG, "✅ Speed=${String.format("%.1f", speed)}km/h Trip=${String.format("%.1f", trip)}km Odometer=${String.format("%.1f", odometer)}km Battery=${battery?.toInt() ?: "N/A"}%")

            var result = currentData.copy(
                speed = speed,
                odometer = odometer,
                tripDistance = trip
            )
            if (battery != null) {
                result = result.copy(battery = battery)
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur parsing 0x1455: ${e.message}")
            return null
        }
    }

    // ==================== SUBTYPE 0x1735 - SYSTÈME (ScooterDataParser) ====================
    private fun parseSubtype1735(frame: ByteArray, currentData: ScooterData): ScooterData? {
        Log.d(TAG, "💻 Parsing Subtype 0x1735 - SYSTÈME")
        if (frame.size < 31) return null

        try {
            // Extraire le temps total depuis bytes [5:6] (big-endian = minutes)
            val byte5 = frame[5].toInt() and 0xFF
            val byte6 = frame[6].toInt() and 0xFF
            val totalMinutes = (byte5 shl 8) or byte6
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            val recordingTime = "${hours}H ${minutes}M 0S"

            Log.d(TAG, "✅ RecordingTime=$recordingTime (${totalMinutes}min)")

            return currentData.copy(
                totalRideTime = recordingTime,
                lastUpdate = Date()
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur parsing 0x1735: ${e.message}")
            return null
        }
    }

    // ==================== TYPE 0x16 - COMBINÉE ====================
    private fun parse0x16(frame: ByteArray, currentData: ScooterData): ScooterData? {
        Log.d(TAG, "🔋 0x16 - BATTERIE + ODOMÈTRE")
        if (frame.size < 8) return null

        var battery = currentData.battery
        var odometer = currentData.odometer

        if (frame.size > 7) {
            battery = (frame[7].toInt() and 0xFF).toFloat().coerceIn(0f, 100f)
            Log.d(TAG, "  [7] Batterie=${battery.toInt()}%")
        }

        if (frame.size >= 19) {
            val byte17 = frame[17].toInt() and 0xFF
            val byte18 = frame[18].toInt() and 0xFF
            val rawOdo = (byte18 shl 8) or byte17
            odometer = rawOdo / 100.0f
            Log.d(TAG, "  [17-18] Odomètre=${String.format("%.2f", odometer)}km")
        }

        return currentData.copy(battery = battery, odometer = odometer)
    }

    // ==================== TYPE 0x1A - ODOMÈTRE DÉTAILLÉ ====================
    private fun parse0x1A(frame: ByteArray, currentData: ScooterData): ScooterData? {
        Log.d(TAG, "🛣️ 0x1A - ODOMÈTRE")
        if (frame.size < 11) return null

        val byte9 = frame[9].toInt() and 0xFF
        val byte10 = frame[10].toInt() and 0xFF
        val odometer = ((byte10 shl 8) or byte9) / 100.0f

        Log.d(TAG, "  Odomètre=${String.format("%.2f", odometer)}km")
        return currentData.copy(odometer = odometer)
    }

    // ==================== TYPE 0x30 - STATUS/MODE ====================
    private fun parse0x30(frame: ByteArray, currentData: ScooterData): ScooterData? {
        Log.d(TAG, "⚙️ 0x30 - STATUS/MODE")
        if (frame.size < 10) return null

        val cmd = frame[5].toInt() and 0xFF
        val val6 = frame[6].toInt() and 0xFF

        var result = currentData

        when (cmd) {
            0x4A -> {
                val mode = when (val6) {
                    0x37 -> RideMode.PIETON
                    0x36 -> RideMode.ECO
                    0x35 -> RideMode.RACE
                    0x34 -> RideMode.SPORT
                    else -> null
                }
                if (mode != null) {
                    Log.d(TAG, "  Mode=${mode.name}")
                    result = result.copy(currentMode = mode)
                }
            }
            0x4B -> {
                val unlocked = val6 == 0x34
                Log.d(TAG, "  Verrouillé=${!unlocked}")
                result = result.copy(isLocked = !unlocked)
            }
            0xC6 -> {
                val headlightsOn = val6 == 0x35
                Log.d(TAG, "  Lumières=$headlightsOn")
                result = result.copy(headlightsOn = headlightsOn)
            }
        }

        return result
    }

    // ==================== TYPE 0x32 - VITESSE + TEMPÉRATURE ====================
    private fun parse0x32(frame: ByteArray, currentData: ScooterData): ScooterData? {
        Log.d(TAG, "🏃 0x32 - VITESSE + TEMPÉRATURE")
        if (frame.size < 8) return null

        val speed = (frame[5].toInt() and 0xFF).toFloat()

        // ✅ ESSAYER DIFFÉRENTS OFFSETS POUR LA TEMPÉRATURE
        // Offset [7] au lieu de [6]
        val temperature = ((frame[7].toInt() and 0xFF) - 40).toFloat()

        Log.d(TAG, "  Speed=${speed.toInt()}km/h Temp=${String.format("%.1f", temperature)}°C")
        Log.d(TAG, "  DEBUG: frame[5]=${frame[5]} frame[6]=${frame[6]} frame[7]=${frame[7]}")  // Pour déboguer

        return currentData.copy(speed = speed, temperature = temperature)
    }

    // ==================== TYPE 0x37 - TEMPS RÉEL COMPLET ====================
    private fun parse0x37(frame: ByteArray, currentData: ScooterData): ScooterData? {
        Log.d(TAG, "⚡ 0x37 - TEMPS RÉEL COMPLET")
        if (frame.size < 22) return null

        val byte5 = frame[5].toInt() and 0xFF
        val byte6 = frame[6].toInt() and 0xFF
        val speed = ((byte6 shl 8) or byte5) / 10.0f

        val battery = (frame[7].toInt() and 0xFF).toFloat().coerceIn(0f, 100f)

        val byte8 = frame[8].toInt() and 0xFF
        val byte9 = frame[9].toInt() and 0xFF
        val voltage = ((byte9 shl 8) or byte8) / 10.0f

        val byte10 = frame[10].toInt() and 0xFF
        val byte11 = frame[11].toInt() and 0xFF
        val current = ((byte11 shl 8) or byte10) / 10.0f

        val temperature = ((frame[12].toInt() and 0xFF) - 40).toFloat()
        val power = voltage * current

        val modeId = frame[20].toInt() and 0x0F
        val mode = when (modeId) {
            0x01 -> RideMode.PIETON
            0x02 -> RideMode.ECO
            0x03 -> RideMode.SPORT
            0x04 -> RideMode.RACE
            else -> currentData.currentMode
        }

        val stateByte = frame[21].toInt() and 0xFF
        val headlightsOn = (stateByte and 0x01) != 0
        val isLocked = (stateByte and 0x02) != 0

        Log.d(TAG, "  Speed=${String.format("%.1f", speed)}km/h Battery=${battery.toInt()}% Voltage=${String.format("%.1f", voltage)}V Current=${String.format("%.1f", current)}A Temp=${String.format("%.1f", temperature)}°C Power=${String.format("%.0f", power)}W")

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
    }

    // ==================== TYPE 0x3E - BATTERIE ====================
    private fun parse0x3E(frame: ByteArray, currentData: ScooterData): ScooterData? {
        Log.d(TAG, "🔋 0x3E - BATTERIE (${frame.size} bytes)")
        if (frame.size < 16) return null

        // DEBUG: Show all bytes as 16-bit values
        for (i in 5 until frame.size - 2 step 2) {
            val value16 = ((frame[i].toInt() and 0xFF) shl 8) or (frame[i+1].toInt() and 0xFF)
            Log.d(TAG, "  [{$i}-${i+1}] = 0x${String.format("%04X", value16)} (${value16})")
        }

        val battery = (frame[7].toInt() and 0xFF).toFloat()
        Log.d(TAG, "  Battery=${battery.toInt()}%")

        return currentData.copy(battery = battery)
    }

    // ==================== TYPE 0xD3 - SYSTÈME ====================
    private fun parse0xD3(frame: ByteArray, currentData: ScooterData): ScooterData? {
        Log.d(TAG, "💻 0xD3 - SYSTÈME")
        if (frame.size < 44) return null

        val battery = (frame[43].toInt() and 0xFF).toFloat().coerceIn(0f, 100f)
        val temperature = (frame[17].toInt() and 0xFF).toFloat()

        Log.d(TAG, "  Battery=${battery.toInt()}% Temp=${temperature.toInt()}°C")
        return currentData.copy(battery = battery, temperature = temperature)
    }

    // ==================== UTILITAIRES ====================
    private fun hex(byte: Byte): String = String.format("%02X", byte.toInt() and 0xFF)

    private fun logFullFrame(frame: ByteArray) {
        val hex = frame.joinToString(" ") { hex(it) }
        Log.d(TAG, "FRAME: $hex")
    }
}