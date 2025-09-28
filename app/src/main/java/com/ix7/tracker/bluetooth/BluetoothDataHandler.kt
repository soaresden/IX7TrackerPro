// BluetoothDataHandler.kt - Amélioré avec diagnostics M0Robot
package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.ScooterData
import com.ix7.tracker.utils.LogManager
import java.util.*
import kotlin.experimental.and

/**
 * Gestionnaire de données Bluetooth optimisé pour M0Robot
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
                "M0ROBOT_EXTENDED_16BYTE" -> parseExtendedFrame16Byte(data)
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
     * Parse frame principale de 8 bytes (format: 08 XX XX XX XX XX XX XX)
     */
    private fun parseMainFrame8Byte(data: ByteArray) {
        if (data.size != 8) return

        try {
            // Format observé: 08 00 0A 00 00 00 90 01
            // Byte 0: Header (0x08)
            // Bytes 1-2: Vitesse (big endian, divisé par 1000)
            // Byte 3: Réservé
            // Bytes 4-5: Réservé
            // Bytes 6-7: Voltage (little endian, divisé par 100)

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
     * Parse frame étendue de 16 bytes (format: 5A XX XX XX...)
     */
    private fun parseExtendedFrame16Byte(data: ByteArray) {
        if (data.size != 16 || data[0] != 0x5A.toByte()) return

        try {
            // Format étendu M0Robot: 5A 00 00 34 00 00 00 00 00 00 00 00 00 00 00 71
            // Byte 0: Header (0x5A)
            // Bytes 1-2: Vitesse (big endian)
            // Byte 3: Batterie (0x34 = 52%)
            // Bytes 4-5: Voltage
            // Byte 6: Température
            // Bytes 7-10: Odomètre (4 bytes little endian)
            // Bytes 11-14: Réservé
            // Byte 15: Checksum

            val speedRaw = getShortBE(data, 1)
            val batteryRaw = data[3].toUByte().toInt()
            val voltageRaw = getShortLE(data, 4)
            val tempRaw = data[6].toUByte().toInt()
            val odometerRaw = getIntLE(data, 7)  // 4 bytes pour l'odomètre

            var hasUpdate = false
            var newData = currentData

            // Vitesse
            if (speedRaw in 0..8000) {
                val speed = speedRaw / 100f
                if (speed != currentData.speed) {
                    newData = newData.copy(speed = speed)
                    hasUpdate = true
                }
            }

            // Batterie (en %) - c'est la donnée fiable dans la frame étendue
            if (batteryRaw in 0..100) {
                val battery = batteryRaw.toFloat()
                if (battery != currentData.battery) {
                    newData = newData.copy(battery = battery)
                    hasUpdate = true
                    Log.d(TAG, "Batterie mise à jour: ${battery}% (raw: $batteryRaw)")
                }
            }

            // Odomètre - données importantes pour les statistiques
            if (odometerRaw > 0) {
                val odometer = odometerRaw / 1000f  // Convertir en km
                if (odometer != currentData.odometer) {
                    newData = newData.copy(odometer = odometer)
                    hasUpdate = true
                    Log.d(TAG, "Odomètre mis à jour: ${odometer}km (raw: $odometerRaw)")
                }
            }

            if (hasUpdate) {
                // Calculer la puissance
                val power = if (newData.voltage > 0 && newData.current != 0f) {
                    newData.voltage * newData.current
                } else 0f

                currentData = newData.copy(
                    power = power,
                    lastUpdate = Date(),
                    isConnected = true
                )
                onDataUpdate(currentData)
            }

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
                        val speed = speedRaw / 100f
                        updateSingleValue("speed", speed)
                    }
                }
                4 -> {
                    val value1 = data[0].toUByte().toInt()
                    val value2 = data[1].toUByte().toInt()

                    if (value1 in 0..100) updateSingleValue("battery", value1.toFloat())
                    if (value2 in 0..100) updateSingleValue("speed", value2.toFloat())
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
        // Traiter les informations de diagnostic si nécessaires
    }

    /**
     * Gère les frames keep-alive
     */
    private fun handleKeepAlive(data: ByteArray) {
        Log.d(TAG, "Keep-alive reçu")
        // Maintenir la connexion active
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

        // Rechercher des patterns de vitesse/batterie dans la frame
        for (i in 0 until data.size - 1) {
            val value = getShortLE(data, i)

            // Pattern possible de vitesse (0-80 km/h)
            if (value in 0..8000) {
                val speed = value / 100f
                if (speed > 0 && speed <= 80) {
                    Log.d(TAG, "Vitesse possible détectée à l'offset $i: ${speed}km/h")
                    updateSingleValue("speed", speed)
                    break
                }
            }
        }

        // Rechercher des patterns de batterie
        for (i in data.indices) {
            val value = data[i].toUByte().toInt()
            if (value in 1..100) {
                Log.d(TAG, "Batterie possible détectée à l'offset $i: ${value}%")
                updateSingleValue("battery", value.toFloat())
                break
            }
        }
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
     * Détecte si la frame contient un pattern de vitesse
     */
    private fun hasSpeedPattern(data: ByteArray): Boolean {
        for (i in 0 until data.size - 1) {
            val value = getShortLE(data, i)
            if (value in 0..8000) {
                val speed = value / 100f
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