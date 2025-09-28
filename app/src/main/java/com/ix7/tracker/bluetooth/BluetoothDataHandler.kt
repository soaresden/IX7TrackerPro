// BluetoothDataHandler.kt - Debug complet pour M0Robot
package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.ScooterData
import com.ix7.tracker.utils.LogManager
import java.util.*

/**
 * Gestionnaire de données Bluetooth optimisé pour M0Robot avec debug avancé
 */
class BluetoothDataHandler(
    private val onDataUpdate: (ScooterData) -> Unit
) {
    private val TAG = "BluetoothDataHandler"
    private val logManager = LogManager.getInstance()

    private var currentData = ScooterData()
    private var lastUpdateTime = 0L
    private val frameHistory = mutableListOf<FrameAnalysis>()

    data class FrameAnalysis(
        val timestamp: Long,
        val rawData: ByteArray,
        val hexString: String,
        val frameType: String,
        val isValid: Boolean,
        val extractedData: Map<String, Any>
    )

    /**
     * Traite les données reçues du scooter M0Robot
     */
    fun handleData(data: ByteArray, deviceName: String = "") {
        try {
            val now = System.currentTimeMillis()
            val hexString = bytesToHex(data)

            // Log détaillé de la frame reçue
            logManager.logBluetoothData("RX", data, deviceName)
            Log.d(TAG, "Frame reçue [${data.size} bytes]: $hexString")

            // Analyser la frame
            val analysis = analyzeFrame(data)
            frameHistory.add(analysis)

            // Garder seulement les 50 dernières frames
            if (frameHistory.size > 50) {
                frameHistory.removeAt(0)
            }

            // Parser selon le type de frame détecté
            when (analysis.frameType) {
                "M0ROBOT_MAIN_8BYTE" -> parseMainFrame8Byte(data)
                "M0ROBOT_EXTENDED_16BYTE" -> parseExtendedFrame16ByteWithDebug(data)
                "M0ROBOT_SPEED_DATA" -> parseSpeedOnlyFrame(data)
                "KEEP_ALIVE" -> handleKeepAlive(data)
                "DIAGNOSTIC" -> parseDiagnosticFrame(data)
                else -> attemptGenericParsing(data)
            }

            // Générer rapport de diagnostic périodique
            if (now - lastUpdateTime > 5000) { // Toutes les 5 secondes
                generateDiagnosticReport()
                lastUpdateTime = now
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur traitement données", e)
            logManager.error("Erreur parsing: ${e.message}", TAG)
        }
    }

    /**
     * Parse frame principale de 8 bytes (format: 08 XX XX XX XX XX XX XX)
     */
    private fun parseMainFrame8Byte(data: ByteArray) {
        if (data.size != 8) return

        try {
            // Format observé: 08 00 0A 00 00 00 90 01
            val speedRaw = getShortBE(data, 1)  // Big endian pour vitesse
            val voltageRaw = getShortLE(data, 6)

            var hasUpdate = false
            var newData = currentData

            // Vitesse - convertir de 0x000A (10 decimal) à 2.56 km/h
            if (speedRaw in 0..8000) {
                val speed = speedRaw * 0.256f  // Facteur de conversion observé
                if (speed != currentData.speed) {
                    newData = newData.copy(speed = speed)
                    hasUpdate = true
                    Log.d(TAG, "Vitesse mise à jour: ${speed}km/h (raw: $speedRaw)")
                }
            }

            // Voltage - 0x0190 = 400, divisé par 10 = 40V
            if (voltageRaw in 200..700) {
                val voltage = voltageRaw / 10f
                if (voltage != currentData.voltage) {
                    newData = newData.copy(voltage = voltage)
                    hasUpdate = true
                    Log.d(TAG, "Voltage mis à jour: ${voltage}V (raw: $voltageRaw)")
                }
            }

            if (hasUpdate) {
                currentData = newData.copy(
                    lastUpdate = Date(),
                    isConnected = true
                )
                onDataUpdate(currentData)
                logManager.logScooterData(currentData.speed, currentData.battery, currentData.voltage)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing frame 8 bytes", e)
        }
    }

    /**
     * Parse frame étendue avec debug complet pour trouver odomètre et température
     */
    private fun parseExtendedFrame16ByteWithDebug(data: ByteArray) {
        if (data.size != 16 || data[0] != 0x5A.toByte()) return

        try {
            Log.d(TAG, "=== DEBUG FRAME 16 BYTES ===")
            Log.d(TAG, "Frame complète: ${bytesToHex(data)}")

            // Afficher chaque byte individuellement
            for (i in data.indices) {
                val byteVal = data[i].toUByte().toInt()
                Log.d(TAG, "Byte[$i] = 0x${"%02X".format(data[i])} = $byteVal")
            }

            val batteryRaw = data[3].toUByte().toInt() // 0x34 = 52% confirmé
            var hasUpdate = false
            var newData = currentData

            // Batterie confirmée
            if (batteryRaw in 0..100) {
                val battery = batteryRaw.toFloat()
                if (battery != currentData.battery) {
                    newData = newData.copy(battery = battery)
                    hasUpdate = true
                    Log.d(TAG, "✓ Batterie trouvée: ${battery}% (byte[3] = $batteryRaw)")
                }
            }

            Log.d(TAG, "--- RECHERCHE ODOMÉTRE (cible: 324.8km) ---")
            // Tester tous les formats possibles pour l'odomètre
            for (offset in 1..12) {
                if (offset + 3 < data.size) {
                    // Tests 32-bit
                    val le32 = getIntLE(data, offset)
                    val be32 = getIntBE(data, offset)

                    // Tests 16-bit
                    val le16 = getShortLE(data, offset)
                    val be16 = getShortBE(data, offset)

                    Log.d(TAG, "Offset $offset: LE32=$le32, BE32=$be32, LE16=$le16, BE16=$be16")

                    // Tests pour 324.8km encodé de différentes façons
                    val possibleValues = listOf(
                        le32 to "LE32",
                        be32 to "BE32",
                        le16 to "LE16",
                        be16 to "BE16"
                    )

                    for ((value, format) in possibleValues) {
                        when {
                            value == 3248 -> {
                                val km = value / 10f
                                Log.d(TAG, "🎯 ODOMÈTRE TROUVÉ! Offset $offset ($format): ${km}km (raw=$value, /10)")
                                newData = newData.copy(odometer = km)
                                hasUpdate = true
                            }
                            value == 32480 -> {
                                val km = value / 100f
                                Log.d(TAG, "🎯 ODOMÈTRE TROUVÉ! Offset $offset ($format): ${km}km (raw=$value, /100)")
                                newData = newData.copy(odometer = km)
                                hasUpdate = true
                            }
                            value == 324800 -> {
                                val km = value / 1000f
                                Log.d(TAG, "🎯 ODOMÈTRE TROUVÉ! Offset $offset ($format): ${km}km (raw=$value, /1000)")
                                newData = newData.copy(odometer = km)
                                hasUpdate = true
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "--- RECHERCHE TEMPÉRATURE (cible: 26.4°C) ---")
            // Tester tous les formats pour la température
            for (offset in 1..14) {
                val byteVal = data[offset].toUByte().toInt()

                if (offset + 1 < data.size) {
                    val le16 = getShortLE(data, offset)
                    val be16 = getShortBE(data, offset)

                    Log.d(TAG, "Temp offset $offset: byte=$byteVal, LE16=$le16, BE16=$be16")

                    // Tests pour 26.4°C
                    when {
                        byteVal == 26 -> {
                            Log.d(TAG, "🌡️ TEMPÉRATURE TROUVÉE! Offset $offset (byte): 26°C")
                            newData = newData.copy(temperature = 26f)
                            hasUpdate = true
                        }
                        byteVal == 264 -> {
                            val temp = byteVal / 10f
                            Log.d(TAG, "🌡️ TEMPÉRATURE TROUVÉE! Offset $offset (byte/10): ${temp}°C")
                            newData = newData.copy(temperature = temp)
                            hasUpdate = true
                        }
                        le16 == 264 -> {
                            val temp = le16 / 10f
                            Log.d(TAG, "🌡️ TEMPÉRATURE TROUVÉE! Offset $offset (LE16/10): ${temp}°C")
                            newData = newData.copy(temperature = temp)
                            hasUpdate = true
                        }
                        be16 == 264 -> {
                            val temp = be16 / 10f
                            Log.d(TAG, "🌡️ TEMPÉRATURE TROUVÉE! Offset $offset (BE16/10): ${temp}°C")
                            newData = newData.copy(temperature = temp)
                            hasUpdate = true
                        }
                    }
                }
            }

            if (hasUpdate) {
                currentData = newData.copy(
                    lastUpdate = Date(),
                    isConnected = true
                )
                onDataUpdate(currentData)
                Log.d(TAG, "✅ Données mises à jour - Odomètre: ${currentData.odometer}km, Temp: ${currentData.temperature}°C")
            }

            Log.d(TAG, "=== FIN DEBUG FRAME 16 BYTES ===")

        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing frame 16 bytes", e)
        }
    }

    /**
     * Parse frame simple de vitesse (2-4 bytes)
     */
    private fun parseSpeedOnlyFrame(data: ByteArray) {
        if (data.size !in 2..4) return

        try {
            when (data.size) {
                2 -> {
                    val speedRaw = getShortLE(data, 0)
                    if (speedRaw in 0..8000) {
                        val speed = speedRaw * 0.256f
                        updateSingleValue("speed", speed)
                    }
                }
                4 -> {
                    val value1 = data[0].toUByte().toInt()
                    val value2 = data[1].toUByte().toInt()

                    if (value1 in 0..100) updateSingleValue("battery", value1.toFloat())
                    if (value2 in 0..100) updateSingleValue("speed", value2 * 0.256f)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing frame vitesse", e)
        }
    }

    /**
     * Parse frame de diagnostic
     */
    private fun parseDiagnosticFrame(data: ByteArray) {
        Log.d(TAG, "Frame de diagnostic reçue: ${bytesToHex(data)}")
    }

    /**
     * Gère les frames keep-alive
     */
    private fun handleKeepAlive(data: ByteArray) {
        Log.d(TAG, "Keep-alive reçu")
        currentData = currentData.copy(
            lastUpdate = Date(),
            isConnected = true
        )
    }

    /**
     * Tentative de parsing générique pour frames inconnues
     */
    private fun attemptGenericParsing(data: ByteArray) {
        Log.d(TAG, "Tentative parsing générique pour frame inconnue: ${bytesToHex(data)}")
    }

    /**
     * Met à jour une seule valeur si elle a changé
     */
    private fun updateSingleValue(field: String, value: Float) {
        val currentValue = when (field) {
            "speed" -> currentData.speed
            "battery" -> currentData.battery
            "voltage" -> currentData.voltage
            "temperature" -> currentData.temperature
            else -> return
        }

        if (value != currentValue) {
            currentData = when (field) {
                "speed" -> currentData.copy(speed = value)
                "battery" -> currentData.copy(battery = value)
                "voltage" -> currentData.copy(voltage = value)
                "temperature" -> currentData.copy(temperature = value)
                else -> currentData
            }.copy(
                lastUpdate = Date(),
                isConnected = true
            )

            onDataUpdate(currentData)
            Log.d(TAG, "Valeur $field mise à jour: $value")
        }
    }

    /**
     * Analyse une frame pour déterminer son type et sa validité
     */
    private fun analyzeFrame(data: ByteArray): FrameAnalysis {
        val hexString = bytesToHex(data)
        val extractedData = mutableMapOf<String, Any>()

        val frameType = when {
            data.isEmpty() -> "EMPTY"
            data.size == 2 && data.contentEquals(byteArrayOf(0x00, 0x01)) -> "KEEP_ALIVE"
            data.size == 2 && data.contentEquals(byteArrayOf(0x00, 0x00)) -> "NULL_RESPONSE"
            data.size == 4 && data[2] == 0xFF.toByte() && data[3] == 0xFF.toByte() -> "DIAGNOSTIC"
            data.size == 8 && data[0] == 0x08.toByte() -> "M0ROBOT_MAIN_8BYTE"
            data.size == 16 && data[0] == 0x5A.toByte() -> "M0ROBOT_EXTENDED_16BYTE"
            data.size in 2..4 && hasSpeedPattern(data) -> "M0ROBOT_SPEED_DATA"
            data.all { it == 0.toByte() } -> "ALL_ZEROS"
            else -> "UNKNOWN"
        }

        val isValid = validateFrame(data, frameType)

        return FrameAnalysis(
            timestamp = System.currentTimeMillis(),
            rawData = data,
            hexString = hexString,
            frameType = frameType,
            isValid = isValid,
            extractedData = extractedData
        )
    }

    /**
     * Détecte si la frame contient un pattern de vitesse
     */
    private fun hasSpeedPattern(data: ByteArray): Boolean {
        for (i in 0 until data.size - 1) {
            val value = getShortLE(data, i)
            if (value in 0..8000) {
                val speed = value * 0.256f
                if (speed > 0 && speed <= 80) return true
            }
        }
        return false
    }

    /**
     * Valide une frame selon son type
     */
    private fun validateFrame(data: ByteArray, frameType: String): Boolean {
        return when (frameType) {
            "M0ROBOT_MAIN_8BYTE" -> data.size == 8 && data[0] == 0x08.toByte()
            "M0ROBOT_EXTENDED_16BYTE" -> data.size == 16 && data[0] == 0x5A.toByte()
            "KEEP_ALIVE" -> data.contentEquals(byteArrayOf(0x00, 0x01))
            "DIAGNOSTIC" -> data.size == 4 && data[2] == 0xFF.toByte()
            "EMPTY" -> false
            "ALL_ZEROS" -> false
            else -> data.any { it != 0.toByte() }
        }
    }

    /**
     * Génère un rapport de diagnostic
     */
    private fun generateDiagnosticReport() {
        val recentFrames = frameHistory.takeLast(20)
        val validFrames = recentFrames.count { it.isValid }
        val frameTypes = recentFrames.groupBy { it.frameType }.mapValues { it.value.size }

        Log.i(TAG, "=== RAPPORT DIAGNOSTIC ===")
        Log.i(TAG, "Frames récentes: ${recentFrames.size}")
        Log.i(TAG, "Frames valides: $validFrames (${(validFrames * 100 / recentFrames.size.coerceAtLeast(1))}%)")
        Log.i(TAG, "Types de frames: $frameTypes")
        Log.i(TAG, "Dernières données: Speed=${currentData.speed}km/h, Battery=${currentData.battery}%, Voltage=${currentData.voltage}V")

        if (validFrames < recentFrames.size * 0.5) {
            Log.w(TAG, "ATTENTION: Faible taux de frames valides - connexion instable")
        }

        if (frameTypes.getOrDefault("UNKNOWN", 0) > 5) {
            Log.w(TAG, "ATTENTION: Beaucoup de frames inconnues - protocole non reconnu")
        }
    }

    // Utilitaires pour lecture des données
    private fun getShortLE(data: ByteArray, offset: Int): Int {
        return if (offset + 1 < data.size) {
            (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
        } else 0
    }

    private fun getShortBE(data: ByteArray, offset: Int): Int {
        return if (offset + 1 < data.size) {
            ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
        } else 0
    }

    private fun getIntLE(data: ByteArray, offset: Int): Int {
        return if (offset + 3 < data.size) {
            (data[offset].toInt() and 0xFF) or
                    ((data[offset + 1].toInt() and 0xFF) shl 8) or
                    ((data[offset + 2].toInt() and 0xFF) shl 16) or
                    ((data[offset + 3].toInt() and 0xFF) shl 24)
        } else 0
    }

    private fun getIntBE(data: ByteArray, offset: Int): Int {
        return if (offset + 3 < data.size) {
            ((data[offset].toInt() and 0xFF) shl 24) or
                    ((data[offset + 1].toInt() and 0xFF) shl 16) or
                    ((data[offset + 2].toInt() and 0xFF) shl 8) or
                    (data[offset + 3].toInt() and 0xFF)
        } else 0
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(it) }
    }

    /**
     * Reset des données
     */
    fun reset() {
        currentData = ScooterData()
        frameHistory.clear()
        Log.i(TAG, "Handler reset")
    }
}