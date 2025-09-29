package com.ix7.tracker.bluetooth

import android.util.Log

/**
 * Outil pour collecter toutes les trames reçues et les analyser après coup
 */
object FrameCollector {
    private const val TAG = "FrameCollector"

    // Valeurs cibles à chercher
    private const val TARGET_BATTERY = 84        // 84%
    private const val TARGET_ODOMETER = 32480    // 324.8 km en décamètres
    private const val TARGET_VOLTAGE = 5080      // 50.8 V en centièmes

    private val collectedFrames = mutableListOf<ByteArray>()

    /**
     * Ajoute une trame à la collection
     */
    fun addFrame(data: ByteArray) {
        collectedFrames.add(data.copyOf())
    }

    /**
     * Analyse TOUTES les trames collectées pour trouver les valeurs cibles
     */
    fun analyzeAllFrames() {
        if (collectedFrames.isEmpty()) {
            Log.d(TAG, "Aucune trame à analyser")
            return
        }

        Log.d(TAG, "")
        Log.d(TAG, "╔═══════════════════════════════════════════════════════════════╗")
        Log.d(TAG, "║         ANALYSE DE ${collectedFrames.size} TRAMES COLLECTÉES         ║")
        Log.d(TAG, "╚═══════════════════════════════════════════════════════════════╝")
        Log.d(TAG, "")
        Log.d(TAG, "Recherche de :")
        Log.d(TAG, "  • Batterie = $TARGET_BATTERY% (0x${"%02X".format(TARGET_BATTERY)})")
        Log.d(TAG, "  • Odomètre = ${TARGET_ODOMETER/100f}km (0x${"%04X".format(TARGET_ODOMETER)})")
        Log.d(TAG, "  • Voltage = ${TARGET_VOLTAGE/100f}V (0x${"%04X".format(TARGET_VOLTAGE)})")
        Log.d(TAG, "")

        val batteryFindings = mutableListOf<String>()
        val odometerFindings = mutableListOf<String>()
        val voltageFindings = mutableListOf<String>()

        collectedFrames.forEachIndexed { frameIdx, frame ->
            val hex = frame.joinToString(" ") { "%02X".format(it) }

            // Chercher dans chaque frame
            for (i in frame.indices) {
                // Batterie (1 byte)
                if (i < frame.size) {
                    val byte8 = frame[i].toInt() and 0xFF
                    if (byte8 == TARGET_BATTERY) {
                        val finding = "Frame #$frameIdx, Offset $i: BATTERIE=${byte8}%"
                        batteryFindings.add(finding)
                        Log.d(TAG, "✓✓✓ $finding")
                        Log.d(TAG, "    Trame: $hex")
                    }
                }

                // Voltage (2 bytes, little-endian)
                if (i + 1 < frame.size) {
                    val short16 = ((frame[i].toInt() and 0xFF) or
                            ((frame[i + 1].toInt() and 0xFF) shl 8))
                    if (short16 == TARGET_VOLTAGE) {
                        val finding = "Frame #$frameIdx, Offset $i: VOLTAGE=${short16/100f}V"
                        voltageFindings.add(finding)
                        Log.d(TAG, "✓✓✓ $finding")
                        Log.d(TAG, "    Trame: $hex")
                    }
                }

                // Odomètre (4 bytes, little-endian)
                if (i + 3 < frame.size) {
                    val int32 = (frame[i].toInt() and 0xFF) or
                            ((frame[i + 1].toInt() and 0xFF) shl 8) or
                            ((frame[i + 2].toInt() and 0xFF) shl 16) or
                            ((frame[i + 3].toInt() and 0xFF) shl 24)
                    if (int32 == TARGET_ODOMETER) {
                        val finding = "Frame #$frameIdx, Offset $i: ODOMETRE=${int32/100f}km"
                        odometerFindings.add(finding)
                        Log.d(TAG, "✓✓✓ $finding")
                        Log.d(TAG, "    Trame: $hex")
                    }
                }
            }
        }

        Log.d(TAG, "")
        Log.d(TAG, "╔═══════════════════════════════════════════════════════════════╗")
        Log.d(TAG, "║                    RÉSULTATS DE L'ANALYSE                      ║")
        Log.d(TAG, "╚═══════════════════════════════════════════════════════════════╝")
        Log.d(TAG, "")

        if (batteryFindings.isNotEmpty()) {
            Log.d(TAG, "✅ BATTERIE TROUVÉE (${batteryFindings.size} occurrence(s)) :")
            batteryFindings.forEach { Log.d(TAG, "   → $it") }
            Log.d(TAG, "")
            Log.d(TAG, "👉 RECOMMANDATION : OFFSET_BATTERY = ${extractOffset(batteryFindings.first())}")
        } else {
            Log.d(TAG, "❌ BATTERIE NON TROUVÉE")
            Log.d(TAG, "   Vérifiez que la batterie est bien à 84%")
        }
        Log.d(TAG, "")

        if (voltageFindings.isNotEmpty()) {
            Log.d(TAG, "✅ VOLTAGE TROUVÉ (${voltageFindings.size} occurrence(s)) :")
            voltageFindings.forEach { Log.d(TAG, "   → $it") }
            Log.d(TAG, "")
            Log.d(TAG, "👉 RECOMMANDATION : OFFSET_VOLTAGE = ${extractOffset(voltageFindings.first())}")
        } else {
            Log.d(TAG, "❌ VOLTAGE NON TROUVÉ")
            Log.d(TAG, "   Vérifiez que le voltage est bien à 50.8V")
        }
        Log.d(TAG, "")

        if (odometerFindings.isNotEmpty()) {
            Log.d(TAG, "✅ ODOMÈTRE TROUVÉ (${odometerFindings.size} occurrence(s)) :")
            odometerFindings.forEach { Log.d(TAG, "   → $it") }
            Log.d(TAG, "")
            Log.d(TAG, "👉 RECOMMANDATION : OFFSET_ODOMETER = ${extractOffset(odometerFindings.first())}")
        } else {
            Log.d(TAG, "❌ ODOMÈTRE NON TROUVÉ")
            Log.d(TAG, "   Vérifiez que l'odomètre est bien à 324.8 km")
        }

        Log.d(TAG, "")
        Log.d(TAG, "═══════════════════════════════════════════════════════════════")
    }

    private fun extractOffset(finding: String): Int {
        val regex = "Offset (\\d+)".toRegex()
        return regex.find(finding)?.groupValues?.get(1)?.toInt() ?: -1
    }

    /**
     * Efface toutes les trames collectées
     */
    fun clear() {
        collectedFrames.clear()
        Log.d(TAG, "Collection de trames effacée")
    }

    /**
     * Retourne le nombre de trames collectées
     */
    fun getFrameCount(): Int = collectedFrames.size
}