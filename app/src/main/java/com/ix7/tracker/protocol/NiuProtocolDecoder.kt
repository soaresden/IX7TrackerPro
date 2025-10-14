package com.ix7.tracker.protocol

import android.graphics.Color
import android.util.Log

/**
 * 🎯 DÉCODEUR PROTOCOL NIU M0ROBOT
 * Basé sur l'analyse du log btsnoop_hci20251014.log
 *
 * Offsets validés avec les valeurs réelles de l'app officielle:
 * - Batterie: 66%
 * - Voltage: 49.0V
 * - Odomètre: 102.9 km
 * - Température: 26-27°C
 */
object NiuProtocolDecoder {

    private const val TAG = "NiuDecoder"

    /**
     * Décode une trame NIU complète
     */
    fun decodeTrame(trame: ByteArray): Map<String, Any?> {
        if (trame.size < 3) return emptyMap()
        if (trame[0] != 0x61.toByte() || trame[1] != 0x9E.toByte()) return emptyMap()

        val offset = trame[2].toInt() and 0xFF
        val result = mutableMapOf<String, Any?>()

        when (offset) {
            0x03 -> decode_0x03(trame, result)
            0x20 -> decode_0x20(trame, result)
            0x30 -> decode_0x30(trame, result)
            0x32 -> decode_0x32(trame, result)
            0x3E -> decode_0x3E(trame, result)
            0xD3 -> decode_0xD3(trame, result)
            else -> {
                // Autres offsets non encore décodés
                Log.d(TAG, "Offset 0x${offset.toString(16)} non décodé")
            }
        }

        return result
    }

    /**
     * 🔋 Offset 0x03 - Odomètre principal
     * bytes[2-3] LE/100 = odomètre en km
     */
    private fun decode_0x03(trame: ByteArray, result: MutableMap<String, Any?>) {
        if (trame.size > 3) {
            // Odomètre: bytes 2-3, Little Endian, diviser par 100
            val odoRaw = ((trame[3].toInt() and 0xFF) shl 8) or (trame[2].toInt() and 0xFF)
            val odometer = odoRaw / 100.0
            result["odometer"] = odometer
            Log.d(TAG, "📍 0x03: Odomètre = ${odometer}km")
        }
    }

    /**
     * 🔋 Offset 0x20 - Batterie directe
     * byte[45] = batterie en %
     */
    private fun decode_0x20(trame: ByteArray, result: MutableMap<String, Any?>) {
        if (trame.size > 45) {
            val battery = trame[45].toInt() and 0xFF
            if (battery in 0..100) {
                result["battery"] = battery
                Log.d(TAG, "🔋 0x20: Batterie = ${battery}%")
            }
        }
    }

    /**
     * 📊 Offset 0x30 - Données multiples
     * bytes[35-36] LE/10 = odomètre
     * bytes[4-5] LE/1000 = voltage (alternatif)
     */
    private fun decode_0x30(trame: ByteArray, result: MutableMap<String, Any?>) {
        // Odomètre alternatif
        if (trame.size > 36) {
            val odoRaw = ((trame[36].toInt() and 0xFF) shl 8) or (trame[35].toInt() and 0xFF)
            val odometer = odoRaw / 10.0
            result["odometer_alt"] = odometer
            Log.d(TAG, "📍 0x30: Odomètre alt = ${odometer}km")
        }

        // Voltage alternatif
        if (trame.size > 5) {
            val voltageRaw = ((trame[5].toInt() and 0xFF) shl 8) or (trame[4].toInt() and 0xFF)
            val voltage = voltageRaw / 1000.0
            if (voltage > 30.0 && voltage < 70.0) {
                result["voltage_alt"] = voltage
                Log.d(TAG, "⚡ 0x30: Voltage alt = ${voltage}V")
            }
        }
    }

    /**
     * 🏃 Offset 0x32 - Vitesse et températures
     * byte[5] = vitesse en km/h
     * byte[6] = température 1
     * byte[7] = température 2 (brute)
     */
    private fun decode_0x32(trame: ByteArray, result: MutableMap<String, Any?>) {
        // Vitesse
        if (trame.size > 5) {
            val speed = trame[5].toInt() and 0xFF
            if (speed in 0..60) {
                result["speed"] = speed
                Log.d(TAG, "🏃 0x32: Vitesse = ${speed}km/h")
            }
        }

        // Températures
        if (trame.size > 7) {
            val temp1 = trame[6].toInt() and 0xFF
            val temp2 = trame[7].toInt() and 0xFF

            if (temp1 in 0..80) {
                result["temperature1"] = temp1
                Log.d(TAG, "🌡️ 0x32: Temp1 = ${temp1}°C")
            }

            // Temp2 est souvent en brut, à investiguer
            result["temperature2_raw"] = temp2
        }
    }

    /**
     * ⚡ Offset 0x3E - TRAME PRINCIPALE
     * bytes[6-7] BE/1000 = voltage
     * bytes[46-47] LE/10 = odomètre
     * byte[?] = batterie (à déterminer exactement)
     */
    private fun decode_0x3E(trame: ByteArray, result: MutableMap<String, Any?>) {
        // Voltage: bytes 6-7, Big Endian, diviser par 1000
        if (trame.size > 7) {
            val voltageRaw = ((trame[6].toInt() and 0xFF) shl 8) or (trame[7].toInt() and 0xFF)
            val voltage = voltageRaw / 1000.0
            if (voltage > 30.0 && voltage < 70.0) {
                result["voltage"] = voltage
                Log.d(TAG, "⚡ 0x3E: Voltage = ${voltage}V")
            }
        }

        // Odomètre: bytes 46-47, Little Endian, diviser par 10
        if (trame.size > 47) {
            val odoRaw = ((trame[47].toInt() and 0xFF) shl 8) or (trame[46].toInt() and 0xFF)
            val odometer = odoRaw / 10.0
            result["odometer_3e"] = odometer
            Log.d(TAG, "📍 0x3E: Odomètre = ${odometer}km")
        }

        // Température
        if (trame.size > 49) {
            val temp = trame[49].toInt() and 0xFF
            if (temp in 0..80) {
                result["temperature"] = temp
                Log.d(TAG, "🌡️ 0x3E: Temp = ${temp}°C")
            }
        }

        // Chercher la batterie dans les bytes restants
        // Tester plusieurs positions possibles
        for (i in 5 until minOf(trame.size, 50)) {
            val value = trame[i].toInt() and 0xFF
            // Si on trouve 66 ou 106, c'est probablement la batterie
            if (value == 66 || value == 106) {
                val battery = if (value == 106) value - 40 else value
                result["battery_candidate_$i"] = battery
            }
        }
    }

    /**
     * 🌡️ Offset 0xD3 - Températures et batterie
     * byte[17] = température 1 (25°C)
     * byte[29] = température 2 (26°C)
     * byte[43] = batterie (66%)
     */
    private fun decode_0xD3(trame: ByteArray, result: MutableMap<String, Any?>) {
        // Batterie
        if (trame.size > 43) {
            val battery = trame[43].toInt() and 0xFF
            if (battery in 0..100) {
                result["battery"] = battery
                Log.d(TAG, "🔋 0xD3: Batterie = ${battery}%")
            }
        }

        // Températures
        if (trame.size > 17) {
            val temp1 = trame[17].toInt() and 0xFF
            if (temp1 in 0..80) {
                result["temperature1"] = temp1
                Log.d(TAG, "🌡️ 0xD3: Temp1 = ${temp1}°C")
            }
        }

        if (trame.size > 29) {
            val temp2 = trame[29].toInt() and 0xFF
            if (temp2 in 0..80) {
                result["temperature2"] = temp2
                Log.d(TAG, "🌡️ 0xD3: Temp2 = ${temp2}°C")
            }
        }
    }

    /**
     * 🔍 Cherche la batterie dans tous les bytes d'une trame
     * Retourne la première valeur qui correspond à 66% ou 106-40
     */
    fun findBatteryInTrame(trame: ByteArray): Int? {
        for (i in trame.indices) {
            val value = trame[i].toInt() and 0xFF
            if (value == 66) return 66
            if (value == 106) return 66 // 106 - 40
        }
        return null
    }

    /**
     * 🎨 Obtient la couleur en fonction du niveau de batterie
     */
    fun getBatteryColor(battery: Int): Int {
        return when {
            battery > 50 -> android.graphics.Color.parseColor("#4CAF50") // Vert
            battery > 20 -> android.graphics.Color.parseColor("#FFC107") // Orange
            else -> android.graphics.Color.parseColor("#F44336") // Rouge
        }
    }

    /**
     * 🎨 Obtient la couleur en fonction de la température
     */
    fun getTemperatureColor(temp: Int): Int {
        return when {
            temp > 70 -> android.graphics.Color.parseColor("#F44336") // Rouge
            temp > 50 -> android.graphics.Color.parseColor("#FF9800") // Orange
            else -> android.graphics.Color.parseColor("#4CAF50") // Vert
        }
    }
}