package com.ix7.tracker.protocol

import android.util.Log
import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.ScooterData

/**
 * 🔍 PARSER DE DEBUG ULTIME
 * Log TOUTES les trames avec TOUS les bytes pour déboguer
 */
object UltimateDebugParser {
    private const val TAG = "🔍DEBUG_PARSER"

    fun parseWithFullDebug(frame: ByteArray, currentData: ScooterData): ScooterData? {
        if (frame.size < 3) {
            Log.e(TAG, "❌ Trame trop courte: ${frame.size} bytes")
            return null
        }

        // Log header
        Log.d(TAG, "═══════════════════════════════════════════════")
        Log.d(TAG, "📥 TRAME REÇUE (${frame.size} bytes)")
        Log.d(TAG, "Header: ${hex(frame[0])} ${hex(frame[1])} ${hex(frame[2])}")

        if (frame[0] != 0x61.toByte() || frame[1] != 0x9E.toByte()) {
            Log.e(TAG, "❌ HEADERS INVALIDES! Attendu: 61 9E")
            logFullFrame(frame)
            return null
        }

        val type = frame[2].toInt() and 0xFF
        Log.d(TAG, "Type de trame: 0x${String.format("%02X", type)}")
        logFullFrame(frame)

        return when (type) {
            0x16 -> parse0x16(frame, currentData)
            0x1A -> parse0x1A(frame, currentData)
            0x30 -> parse0x30(frame, currentData)
            0x32 -> parse0x32(frame, currentData)
            0x37 -> parse0x37(frame, currentData)
            0x3E -> parse0x3E(frame, currentData)
            0xD3 -> parse0xD3(frame, currentData)
            else -> {
                Log.w(TAG, "⚠️ Type inconnu: 0x${String.format("%02X", type)}")
                null
            }
        }
    }

    // ==================== TRAME 0x16 - COMBINÉE ====================
    private fun parse0x16(frame: ByteArray, currentData: ScooterData): ScooterData? {
        Log.d(TAG, "🔋 Parsing 0x16 - BATTERIE + ODOMÈTRE")

        var battery = currentData.battery
        var odometer = currentData.odometer

        // Batterie à l'offset 7
        if (frame.size > 7) {
            val rawBattery = frame[7].toInt() and 0xFF
            battery = rawBattery.toFloat().coerceIn(0f, 100f)
            Log.d(TAG, "  [7] Batterie RAW = $rawBattery → ${battery.toInt()}%")
        }

        // Odomètre aux offsets 17-18 (LE, décamètres/100)
        if (frame.size >= 19) {
            val byte17 = frame[17].toInt() and 0xFF
            val byte18 = frame[18].toInt() and 0xFF
            val rawOdo = (byte18 shl 8) or byte17
            odometer = rawOdo / 100.0f
            Log.d(TAG, "  [17-18] Odomètre RAW = ${hex(frame[17])} ${hex(frame[18])} = $rawOdo → ${String.format("%.2f", odometer)} km")
        }

        val result = currentData.copy(battery = battery, odometer = odometer)
        Log.d(TAG, "✅ Résultat: Batterie=${battery.toInt()}% Odomètre=${String.format("%.2f", odometer)}km")
        return result
    }

    // ==================== TRAME 0x1A - ODOMÈTRE DÉTAILLÉ ====================
    private fun parse0x1A(frame: ByteArray, currentData: ScooterData): ScooterData? {
        Log.d(TAG, "🛣️ Parsing 0x1A - ODOMÈTRE DÉTAILLÉ")

        if (frame.size < 11) {
            Log.e(TAG, "❌ Trame trop courte pour odomètre")
            return null
        }

        val byte9 = frame[9].toInt() and 0xFF
        val byte10 = frame[10].toInt() and 0xFF
        val rawOdo = (byte10 shl 8) or byte9
        val odometer = rawOdo / 100.0f

        Log.d(TAG, "  [9-10] Odomètre RAW = ${hex(frame[9])} ${hex(frame[10])} = $rawOdo → ${String.format("%.2f", odometer)} km")

        val result = currentData.copy(odometer = odometer)
        Log.d(TAG, "✅ Résultat: Odomètre=${String.format("%.2f", odometer)}km")
        return result
    }

    // ==================== TRAME 0x30 - STATUS/MODE ====================
    private fun parse0x30(frame: ByteArray, currentData: ScooterData): ScooterData? {
        Log.d(TAG, "⚙️ Parsing 0x30 - STATUS/MODE")

        if (frame.size < 10) {
            Log.e(TAG, "❌ Trame trop courte")
            return null
        }

        // Analyser tous les bytes pertinents
        Log.d(TAG, "  [5] = ${hex(frame[5])} (CMD)")
        Log.d(TAG, "  [6] = ${hex(frame[6])} (VAL)")
        Log.d(TAG, "  [7] = ${hex(frame[7])}")

        // Détection du mode
        val cmd = frame[5].toInt() and 0xFF
        val val6 = frame[6].toInt() and 0xFF

        var mode: RideMode? = null
        var unlocked: Boolean? = null
        var headlightsOn: Boolean? = null

        when (cmd) {
            0x4A -> { // Mode
                mode = when (val6) {
                    0x37 -> RideMode.PEDESTRIAN
                    0x36 -> RideMode.ECO
                    0x35 -> RideMode.RACE
                    0x34 -> RideMode.SPORT
                    else -> null
                }
                if (mode != null) {
                    Log.d(TAG, "  ✅ Mode détecté: ${mode.name}")
                }
            }
            0x4B -> { // Lock/Unlock
                unlocked = val6 == 0x34
                Log.d(TAG, "  ✅ Verrouillage: ${if (unlocked!!) "DÉVERROUILLÉ" else "VERROUILLÉ"}")
            }
            0xC6 -> { // Lumières
                headlightsOn = val6 == 0x35
                Log.d(TAG, "  ✅ Lumières: ${if (headlightsOn!!) "ON" else "OFF"}")
            }
        }

        var result = currentData
        if (mode != null) result = result.copy(currentMode = mode)
        if (unlocked != null) result = result.copy(isLocked = !unlocked)
        if (headlightsOn != null) result = result.copy(headlightsOn = headlightsOn)

        return result
    }

    // ==================== TRAME 0x32 - VITESSE + TEMPÉRATURE ====================
    private fun parse0x32(frame: ByteArray, currentData: ScooterData): ScooterData? {
        Log.d(TAG, "🏃 Parsing 0x32 - VITESSE + TEMPÉRATURE")

        if (frame.size < 8) {
            Log.e(TAG, "❌ Trame trop courte")
            return null
        }

        // Vitesse à l'offset 5
        val rawSpeed = frame[5].toInt() and 0xFF
        val speed = rawSpeed.toFloat()
        Log.d(TAG, "  [5] Vitesse RAW = $rawSpeed → ${speed.toInt()} km/h")

        // Température à l'offset 6 (avec offset -40)
        val rawTemp = frame[6].toInt() and 0xFF
        val temperature = (rawTemp - 40).toFloat()
        Log.d(TAG, "  [6] Température RAW = $rawTemp → ${String.format("%.1f", temperature)}°C")

        val result = currentData.copy(speed = speed, temperature = temperature)
        Log.d(TAG, "✅ Résultat: Vitesse=${speed.toInt()}km/h Temp=${String.format("%.1f", temperature)}°C")
        return result
    }

    // ==================== TRAME 0x37 - TEMPS RÉEL COMPLET ====================
    private fun parse0x37(frame: ByteArray, currentData: ScooterData): ScooterData? {
        Log.d(TAG, "⚡ Parsing 0x37 - TEMPS RÉEL COMPLET")

        if (frame.size < 22) {
            Log.e(TAG, "❌ Trame trop courte")
            return null
        }

        // Vitesse [5-6] LE / 10
        val byte5 = frame[5].toInt() and 0xFF
        val byte6 = frame[6].toInt() and 0xFF
        val rawSpeed = (byte6 shl 8) or byte5
        val speed = rawSpeed / 10.0f
        Log.d(TAG, "  [5-6] Vitesse RAW = ${hex(frame[5])} ${hex(frame[6])} = $rawSpeed → ${String.format("%.1f", speed)} km/h")

        // Batterie [7]
        val battery = (frame[7].toInt() and 0xFF).toFloat().coerceIn(0f, 100f)
        Log.d(TAG, "  [7] Batterie = ${battery.toInt()}%")

        // Tension [8-9] LE / 10
        val byte8 = frame[8].toInt() and 0xFF
        val byte9 = frame[9].toInt() and 0xFF
        val rawVoltage = (byte9 shl 8) or byte8
        val voltage = rawVoltage / 10.0f
        Log.d(TAG, "  [8-9] Tension RAW = ${hex(frame[8])} ${hex(frame[9])} = $rawVoltage → ${String.format("%.1f", voltage)} V")

        // Courant [10-11] LE / 10
        val byte10 = frame[10].toInt() and 0xFF
        val byte11 = frame[11].toInt() and 0xFF
        val rawCurrent = (byte11 shl 8) or byte10
        val current = rawCurrent / 10.0f
        Log.d(TAG, "  [10-11] Courant RAW = ${hex(frame[10])} ${hex(frame[11])} = $rawCurrent → ${String.format("%.1f", current)} A")

        // Température [12] - 40
        val rawTemp = frame[12].toInt() and 0xFF
        val temperature = (rawTemp - 40).toFloat()
        Log.d(TAG, "  [12] Température RAW = $rawTemp → ${String.format("%.1f", temperature)}°C")

        // Puissance
        val power = voltage * current
        Log.d(TAG, "  Puissance calculée = ${String.format("%.0f", power)} W")

        // Mode [20]
        val modeId = frame[20].toInt() and 0x0F
        val mode = when (modeId) {
            0x01 -> RideMode.PEDESTRIAN
            0x02 -> RideMode.ECO
            0x03 -> RideMode.SPORT
            0x04 -> RideMode.RACE
            else -> currentData.currentMode
        }
        Log.d(TAG, "  [20] Mode RAW = ${hex(frame[20])} → ${mode.name}")

        // États [21]
        val stateByte = frame[21].toInt() and 0xFF
        val headlightsOn = (stateByte and 0x01) != 0
        val isLocked = (stateByte and 0x02) != 0
        Log.d(TAG, "  [21] États RAW = ${hex(frame[21])} → Lumières=$headlightsOn Verrouillé=$isLocked")

        val result = currentData.copy(
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

        Log.d(TAG, "✅ Résultat complet loggé ci-dessus")
        return result
    }

    // ==================== TRAME 0x3E - BATTERIE ====================
    private fun parse0x3E(frame: ByteArray, currentData: ScooterData): ScooterData? {
        Log.d(TAG, "🔋 Parsing 0x3E - BATTERIE")

        if (frame.size < 8) {
            Log.e(TAG, "❌ Trame trop courte")
            return null
        }

        val battery = (frame[7].toInt() and 0xFF).toFloat().coerceIn(0f, 100f)
        Log.d(TAG, "  [7] Batterie = ${battery.toInt()}%")

        val result = currentData.copy(battery = battery)
        Log.d(TAG, "✅ Résultat: Batterie=${battery.toInt()}%")
        return result
    }

    // ==================== TRAME 0xD3 - SYSTÈME ====================
    private fun parse0xD3(frame: ByteArray, currentData: ScooterData): ScooterData? {
        Log.d(TAG, "💻 Parsing 0xD3 - SYSTÈME")

        if (frame.size < 44) {
            Log.e(TAG, "❌ Trame trop courte")
            return null
        }

        // Batterie [43]
        val battery = (frame[43].toInt() and 0xFF).toFloat().coerceIn(0f, 100f)
        Log.d(TAG, "  [43] Batterie = ${battery.toInt()}%")

        // Température [17]
        val rawTemp = frame[17].toInt() and 0xFF
        val temperature = rawTemp.toFloat()
        Log.d(TAG, "  [17] Température RAW = $rawTemp → ${temperature.toInt()}°C")

        val result = currentData.copy(battery = battery, temperature = temperature)
        Log.d(TAG, "✅ Résultat: Batterie=${battery.toInt()}% Temp=${temperature.toInt()}°C")
        return result
    }

    // ==================== UTILITAIRES ====================
    private fun hex(byte: Byte): String = String.format("%02X", byte.toInt() and 0xFF)

    private fun logFullFrame(frame: ByteArray) {
        val hex = frame.joinToString(" ") { hex(it) }
        Log.d(TAG, "TRAME COMPLÈTE: $hex")

        // Log en groupes de 10 bytes pour lisibilité
        for (i in frame.indices step 10) {
            val end = minOf(i + 10, frame.size)
            val chunk = frame.slice(i until end).toByteArray()
            val chunkHex = chunk.joinToString(" ") { hex(it) }
            val chunkDec = chunk.joinToString(" ") { String.format("%3d", it.toInt() and 0xFF) }
            Log.d(TAG, "  [$i-${end-1}] HEX: $chunkHex")
            Log.d(TAG, "  [$i-${end-1}] DEC: $chunkDec")
        }
    }
}
