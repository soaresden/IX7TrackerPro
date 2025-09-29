package com.ix7.tracker.bluetooth

import android.util.Log

/**
 * Analyseur de trames pour identifier les positions exactes des données
 * Basé sur les valeurs réelles : Batterie=84%, Odomètre=324.8km, Voltage=50.8V
 */
object FrameAnalyzer {
    private const val TAG = "FrameAnalyzer"

    // Valeurs cibles connues
    private const val TARGET_BATTERY = 84        // 84%
    private const val TARGET_ODOMETER = 32480    // 324.8 km en décamètres
    private const val TARGET_VOLTAGE = 5080      // 50.8 V en centièmes
    private const val TARGET_TIME_MIN = 10898    // 181h38min en minutes

    data class FrameAnalysis(
        val batteryPositions: List<Int>,
        val odometerPositions: List<Int>,
        val voltagePositions: List<Int>,
        val timePositions: List<Int>,
        val recommendations: List<String>
    )

    /**
     * Analyse complète d'une trame pour localiser toutes les données
     */
    fun analyzeFrame(data: ByteArray): FrameAnalysis {
        val batteryPos = mutableListOf<Int>()
        val odometerPos = mutableListOf<Int>()
        val voltagePos = mutableListOf<Int>()
        val timePos = mutableListOf<Int>()

        Log.d(TAG, "═══════════════════════════════════════════════")
        Log.d(TAG, "ANALYSE DE TRAME : ${data.size} bytes")
        Log.d(TAG, "HEX: ${bytesToHex(data)}")
        Log.d(TAG, "═══════════════════════════════════════════════")

        // Scanner tous les offsets possibles
        for (i in data.indices) {
            // Vérifier byte unique (batterie potentielle)
            if (i < data.size) {
                val byte8 = parseUInt8(data, i)
                if (byte8 == TARGET_BATTERY) {
                    batteryPos.add(i)
                    Log.d(TAG, "✓ BATTERIE trouvée à offset $i: $byte8%")
                }
            }

            // Vérifier 16 bits (voltage)
            if (i + 1 < data.size) {
                val short16 = parseUInt16(data, i)
                if (short16 == TARGET_VOLTAGE) {
                    voltagePos.add(i)
                    Log.d(TAG, "✓ VOLTAGE trouvé à offset $i: ${short16/100f}V")
                }
            }

            // Vérifier 32 bits (odomètre, temps)
            if (i + 3 < data.size) {
                val int32 = parseUInt32(data, i)

                if (int32 == TARGET_ODOMETER) {
                    odometerPos.add(i)
                    Log.d(TAG, "✓ ODOMÈTRE trouvé à offset $i: ${int32/100f}km")
                }

                if (int32 == TARGET_TIME_MIN) {
                    timePos.add(i)
                    Log.d(TAG, "✓ TEMPS trouvé à offset $i: ${int32/60}h ${int32%60}min")
                }

                // Vérifications approximatives (±5%)
                if (int32 in (TARGET_ODOMETER-1600)..(TARGET_ODOMETER+1600)) {
                    Log.d(TAG, "  ~ Odomètre proche à offset $i: ${int32/100f}km")
                }
            }
        }

        Log.d(TAG, "═══════════════════════════════════════════════")
        printDetailedAnalysis(data)

        val recommendations = generateRecommendations(
            batteryPos, odometerPos, voltagePos, timePos
        )

        return FrameAnalysis(
            batteryPositions = batteryPos,
            odometerPositions = odometerPos,
            voltagePositions = voltagePos,
            timePositions = timePos,
            recommendations = recommendations
        )
    }

    /**
     * Affiche une analyse détaillée byte par byte
     */
    private fun printDetailedAnalysis(data: ByteArray) {
        Log.d(TAG, "ANALYSE DÉTAILLÉE :")
        for (i in data.indices) {
            val byte = data[i].toInt() and 0xFF
            val hex = "%02X".format(byte)

            var interpretation = ""

            // Interprétations possibles
            when {
                byte == TARGET_BATTERY -> interpretation += "[BATTERIE=${byte}%] "
                byte in 0..100 -> interpretation += "[Batterie possible=${byte}%] "
            }

            // 16-bit à cette position
            if (i + 1 < data.size) {
                val short16 = parseUInt16(data, i)
                when {
                    short16 == TARGET_VOLTAGE -> interpretation += "[VOLTAGE=${short16/100f}V] "
                    short16 in 3000..6000 -> interpretation += "[Voltage possible=${short16/100f}V] "
                    short16 in 0..5000 -> interpretation += "[Vitesse possible=${short16/100f}km/h] "
                }
            }

            // 32-bit à cette position
            if (i + 3 < data.size) {
                val int32 = parseUInt32(data, i)
                when {
                    int32 == TARGET_ODOMETER -> interpretation += "[ODOMÈTRE=${int32/100f}km] "
                    int32 in 10000..1000000 -> interpretation += "[Odomètre possible=${int32/100f}km] "
                }
            }

            if (interpretation.isNotEmpty()) {
                Log.d(TAG, "  [$i] = 0x$hex ($byte) $interpretation")
            }
        }
    }

    /**
     * Génère des recommandations basées sur les positions trouvées
     */
    private fun generateRecommendations(
        batteryPos: List<Int>,
        odometerPos: List<Int>,
        voltagePos: List<Int>,
        timePos: List<Int>
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (batteryPos.isNotEmpty()) {
            recommendations.add("✓ Batterie détectée à offset(s): ${batteryPos.joinToString(", ")}")
            recommendations.add("  → Mettre OFFSET_BATTERY = ${batteryPos.first()}")
        } else {
            recommendations.add("✗ Batterie NON trouvée - vérifier la valeur actuelle (84%)")
        }

        if (odometerPos.isNotEmpty()) {
            recommendations.add("✓ Odomètre détecté à offset(s): ${odometerPos.joinToString(", ")}")
            recommendations.add("  → Mettre OFFSET_ODOMETER = ${odometerPos.first()}")
        } else {
            recommendations.add("✗ Odomètre NON trouvé - vérifier la valeur (324.8 km)")
        }

        if (voltagePos.isNotEmpty()) {
            recommendations.add("✓ Voltage détecté à offset(s): ${voltagePos.joinToString(", ")}")
            recommendations.add("  → Mettre OFFSET_VOLTAGE = ${voltagePos.first()}")
        } else {
            recommendations.add("✗ Voltage NON trouvé - vérifier la valeur (50.8V)")
        }

        if (timePos.isNotEmpty()) {
            recommendations.add("✓ Temps total détecté à offset(s): ${timePos.joinToString(", ")}")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("⚠ AUCUNE valeur détectée - la trame ne contient peut-être pas ces données")
            recommendations.add("  → Essayer de capturer une trame différente")
            recommendations.add("  → Vérifier que les valeurs réelles correspondent bien")
        }

        return recommendations
    }

    // Utilitaires de parsing

    private fun parseUInt8(data: ByteArray, offset: Int): Int {
        return data[offset].toInt() and 0xFF
    }

    private fun parseUInt16(data: ByteArray, offset: Int): Int {
        return if (offset + 1 < data.size) {
            (data[offset].toInt() and 0xFF) or
                    ((data[offset + 1].toInt() and 0xFF) shl 8)
        } else 0
    }

    private fun parseUInt32(data: ByteArray, offset: Int): Int {
        return if (offset + 3 < data.size) {
            (data[offset].toInt() and 0xFF) or
                    ((data[offset + 1].toInt() and 0xFF) shl 8) or
                    ((data[offset + 2].toInt() and 0xFF) shl 16) or
                    ((data[offset + 3].toInt() and 0xFF) shl 24)
        } else 0
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(it) }
    }
}

/**
 * Extension pour faciliter l'utilisation dans BluetoothDataHandler
 */
fun ByteArray.analyze(): FrameAnalyzer.FrameAnalysis {
    return FrameAnalyzer.analyzeFrame(this)
}