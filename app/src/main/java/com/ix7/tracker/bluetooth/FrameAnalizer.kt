package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.ProtocolUtils

/**
 * Analyseur automatique de trames M0Robot
 * Détecte les positions des données critiques comme l'odomètre à 356km
 */
object FrameAnalyzer {
    private const val TAG = "FrameAnalyzer"

    // Valeurs cibles de référence (à ajuster selon tes valeurs réelles)
    private var TARGET_ODOMETER = 0    // 356 km en décamètres
    private var TARGET_BATTERY = 49        // Batterie en %
    private var TARGET_VOLTAGE = 473       // 47.3V en dixièmes
    private var TARGET_TEMPERATURE = 26    // Température en °C

    /**
     * Configure les valeurs cibles pour la détection
     */
    fun setTargetValues(
        odometerKm: Float? = null,
        batteryPercent: Int? = null,
        voltageV: Float? = null,
        temperatureC: Int? = null
    ) {
        odometerKm?.let { TARGET_ODOMETER = (it * 100).toInt() }
        batteryPercent?.let { TARGET_BATTERY = it }
        voltageV?.let { TARGET_VOLTAGE = (it * 10).toInt() }
        temperatureC?.let { TARGET_TEMPERATURE = it }

        Log.i(TAG, "Valeurs cibles configurées:")
        Log.i(TAG, "  Odomètre: ${TARGET_ODOMETER/100f} km")
        Log.i(TAG, "  Batterie: $TARGET_BATTERY%")
        Log.i(TAG, "  Voltage: ${TARGET_VOLTAGE/10f}V")
        Log.i(TAG, "  Température: ${TARGET_TEMPERATURE}°C")
    }

    data class DetectionResult(
        val odometerOffsets: List<Pair<Int, Float>>,
        val batteryOffsets: List<Pair<Int, Int>>,
        val voltageOffsets: List<Pair<Int, Float>>,
        val temperatureOffsets: List<Pair<Int, Int>>,
        val recommendations: List<String>
    )

    /**
     * Analyse complète d'une trame pour détecter toutes les données
     */
    fun analyzeFrame(frame: ByteArray): DetectionResult {
        Log.d(TAG, "═══════════════════════════════════════════════")
        Log.d(TAG, "ANALYSE AUTOMATIQUE DE TRAME (${frame.size} bytes)")
        Log.d(TAG, "HEX: ${ProtocolUtils.toHexString(frame)}")
        Log.d(TAG, "═══════════════════════════════════════════════")

        val odometerOffsets = mutableListOf<Pair<Int, Float>>()
        val batteryOffsets = mutableListOf<Pair<Int, Int>>()
        val voltageOffsets = mutableListOf<Pair<Int, Float>>()
        val temperatureOffsets = mutableListOf<Pair<Int, Int>>()

        // RECHERCHE ODOMÈTRE (356 km = 35600 décamètres en 32-bit little-endian)
        Log.d(TAG, "")
        Log.d(TAG, "🔍 RECHERCHE ODOMÈTRE (cible: ${TARGET_ODOMETER/100f} km)...")
        for (i in 0 until frame.size - 3) {
            val value = ProtocolUtils.parseUInt32LE(frame, i)
            val km = value / 100f

            // Exacte correspondance
            if (value == TARGET_ODOMETER) {
                odometerOffsets.add(i to km)
                Log.i(TAG, "  ✓✓✓ ODOMÈTRE EXACT trouvé à offset $i: $km km")
                Log.i(TAG, "      Bytes: ${frame[i].toHex()} ${frame[i+1].toHex()} ${frame[i+2].toHex()} ${frame[i+3].toHex()}")
            }
            // Valeurs proches (±10 km)
            else if (value in (TARGET_ODOMETER-1000)..(TARGET_ODOMETER+1000)) {
                odometerOffsets.add(i to km)
                Log.d(TAG, "  ✓ Odomètre proche à offset $i: $km km (diff: ${kotlin.math.abs(TARGET_ODOMETER - value)/100f} km)")
            }
        }

        // RECHERCHE BATTERIE (84%)
        Log.d(TAG, "")
        Log.d(TAG, "🔋 RECHERCHE BATTERIE (cible: $TARGET_BATTERY%)...")
        for (i in frame.indices) {
            val value = frame[i].toInt() and 0xFF
            if (value == TARGET_BATTERY) {
                batteryOffsets.add(i to value)
                Log.i(TAG, "  ✓ BATTERIE trouvée à offset $i: $value%")
            } else if (value in (TARGET_BATTERY-5)..(TARGET_BATTERY+5) && value in 0..100) {
                Log.d(TAG, "  ~ Batterie possible à offset $i: $value%")
            }
        }

        // RECHERCHE VOLTAGE (47.3V = 473 en dixièmes)
        Log.d(TAG, "")
        Log.d(TAG, "⚡ RECHERCHE VOLTAGE (cible: ${TARGET_VOLTAGE/10f}V)...")
        for (i in 0 until frame.size - 1) {
            val value = ProtocolUtils.parseUInt16LE(frame, i)
            val volts = value / 10f

            if (value == TARGET_VOLTAGE) {
                voltageOffsets.add(i to volts)
                Log.i(TAG, "  ✓ VOLTAGE trouvé à offset $i: ${volts}V")
            } else if (value in 300..600) {
                voltageOffsets.add(i to volts)
                Log.d(TAG, "  ~ Voltage possible à offset $i: ${volts}V")
            }
        }

        // RECHERCHE TEMPÉRATURE (26°C)
        Log.d(TAG, "")
        Log.d(TAG, "🌡️ RECHERCHE TEMPÉRATURE (cible: ${TARGET_TEMPERATURE}°C)...")
        for (i in frame.indices) {
            val value = frame[i].toInt() and 0xFF
            if (value == TARGET_TEMPERATURE) {
                temperatureOffsets.add(i to value)
                Log.i(TAG, "  ✓ TEMPÉRATURE trouvée à offset $i: ${value}°C")
            } else if (value in 15..80) {
                Log.d(TAG, "  ~ Température possible à offset $i: ${value}°C")
            }
        }

        // GÉNÉRER RECOMMANDATIONS
        val recommendations = generateRecommendations(
            odometerOffsets,
            batteryOffsets,
            voltageOffsets,
            temperatureOffsets
        )

        Log.d(TAG, "═══════════════════════════════════════════════")
        Log.i(TAG, "📊 RÉSUMÉ DES DÉTECTIONS:")
        Log.i(TAG, "  Odomètre: ${odometerOffsets.size} position(s) détectée(s)")
        Log.i(TAG, "  Batterie: ${batteryOffsets.size} position(s) détectée(s)")
        Log.i(TAG, "  Voltage: ${voltageOffsets.size} position(s) détectée(s)")
        Log.i(TAG, "  Température: ${temperatureOffsets.size} position(s) détectée(s)")
        Log.d(TAG, "═══════════════════════════════════════════════")

        recommendations.forEach { Log.i(TAG, "💡 $it") }

        return DetectionResult(
            odometerOffsets = odometerOffsets,
            batteryOffsets = batteryOffsets,
            voltageOffsets = voltageOffsets,
            temperatureOffsets = temperatureOffsets,
            recommendations = recommendations
        )
    }

    /**
     * Génère des recommandations basées sur les détections
     */
    private fun generateRecommendations(
        odometerOffsets: List<Pair<Int, Float>>,
        batteryOffsets: List<Pair<Int, Int>>,
        voltageOffsets: List<Pair<Int, Float>>,
        temperatureOffsets: List<Pair<Int, Int>>
    ): List<String> {
        val recommendations = mutableListOf<String>()

        // Odomètre
        if (odometerOffsets.isNotEmpty()) {
            val bestOdometer = odometerOffsets.first()
            recommendations.add("✓ Odomètre détecté à l'offset ${bestOdometer.first}")
            recommendations.add("  → Dans BluetoothDataHandler, utiliser: val odometerRaw = ProtocolUtils.parseUInt32LE(frame, ${bestOdometer.first})")
            recommendations.add("  → Conversion: odometer = odometerRaw / 100f  // ${bestOdometer.second} km")
        } else {
            recommendations.add("✗ Odomètre NON trouvé")
            recommendations.add("  → Vérifier que la valeur cible est correcte: ${TARGET_ODOMETER/100f} km")
            recommendations.add("  → Essayer de capturer plusieurs trames différentes")
            recommendations.add("  → L'odomètre peut être dans une trame différente")
        }

        // Batterie
        if (batteryOffsets.isNotEmpty()) {
            val bestBattery = batteryOffsets.first()
            recommendations.add("✓ Batterie détectée à l'offset ${bestBattery.first}")
            recommendations.add("  → battery = frame[${bestBattery.first}].toInt() and 0xFF")
        }

        // Voltage
        if (voltageOffsets.isNotEmpty()) {
            val bestVoltage = voltageOffsets.first()
            recommendations.add("✓ Voltage détecté à l'offset ${bestVoltage.first}")
            recommendations.add("  → voltage = ProtocolUtils.parseUInt16LE(frame, ${bestVoltage.first}) / 10f")
        }

        // Température
        if (temperatureOffsets.isNotEmpty()) {
            val bestTemp = temperatureOffsets.first()
            recommendations.add("✓ Température détectée à l'offset ${bestTemp.first}")
            recommendations.add("  → temperature = frame[${bestTemp.first}].toInt() and 0xFF")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("⚠ AUCUNE donnée détectée automatiquement")
            recommendations.add("  → Les valeurs cibles sont peut-être incorrectes")
            recommendations.add("  → Appeler setTargetValues() avec les vraies valeurs")
        }

        return recommendations
    }

    /**
     * Analyse multiple de trames pour affiner la détection
     */
    fun analyzeMultipleFrames(frames: List<ByteArray>): Map<String, List<Int>> {
        val odometerPositions = mutableMapOf<Int, Int>()
        val batteryPositions = mutableMapOf<Int, Int>()
        val voltagePositions = mutableMapOf<Int, Int>()
        val temperaturePositions = mutableMapOf<Int, Int>()

        Log.i(TAG, "════════════════════════════════════════════════════")
        Log.i(TAG, "ANALYSE DE ${frames.size} TRAMES POUR DÉTECTION CROISÉE")
        Log.i(TAG, "════════════════════════════════════════════════════")

        frames.forEachIndexed { index, frame ->
            val result = analyzeFrame(frame)

            // Compter les occurrences de chaque position
            result.odometerOffsets.forEach { (offset, _) ->
                odometerPositions[offset] = odometerPositions.getOrDefault(offset, 0) + 1
            }
            result.batteryOffsets.forEach { (offset, _) ->
                batteryPositions[offset] = batteryPositions.getOrDefault(offset, 0) + 1
            }
            result.voltageOffsets.forEach { (offset, _) ->
                voltagePositions[offset] = voltagePositions.getOrDefault(offset, 0) + 1
            }
            result.temperatureOffsets.forEach { (offset, _) ->
                temperaturePositions[offset] = temperaturePositions.getOrDefault(offset, 0) + 1
            }
        }

        // Trier par nombre d'occurrences
        val sortedOdometer = odometerPositions.entries.sortedByDescending { it.value }.map { it.key }
        val sortedBattery = batteryPositions.entries.sortedByDescending { it.value }.map { it.key }
        val sortedVoltage = voltagePositions.entries.sortedByDescending { it.value }.map { it.key }
        val sortedTemperature = temperaturePositions.entries.sortedByDescending { it.value }.map { it.key }

        Log.i(TAG, "")
        Log.i(TAG, "🎯 POSITIONS LES PLUS PROBABLES (par fréquence):")
        Log.i(TAG, "  Odomètre: ${sortedOdometer.take(3)}")
        Log.i(TAG, "  Batterie: ${sortedBattery.take(3)}")
        Log.i(TAG, "  Voltage: ${sortedVoltage.take(3)}")
        Log.i(TAG, "  Température: ${sortedTemperature.take(3)}")
        Log.i(TAG, "════════════════════════════════════════════════════")

        return mapOf(
            "odometer" to sortedOdometer,
            "battery" to sortedBattery,
            "voltage" to sortedVoltage,
            "temperature" to sortedTemperature
        )
    }

    /**
     * Extension pour formatter un byte en hex
     */
    private fun Byte.toHex(): String = "%02X".format(this)
}

/**
 * Extension pour faciliter l'utilisation dans BluetoothDataHandler
 */
fun ByteArray.analyze(): FrameAnalyzer.DetectionResult {
    return FrameAnalyzer.analyzeFrame(this)
}

/**
 * Extension pour configurer les valeurs cibles
 */
fun ByteArray.analyzeWith(odometerKm: Float, batteryPercent: Int, voltageV: Float, temperatureC: Int): FrameAnalyzer.DetectionResult {
    FrameAnalyzer.setTargetValues(odometerKm, batteryPercent, voltageV, temperatureC)
    return FrameAnalyzer.analyzeFrame(this)
}