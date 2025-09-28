package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.ScooterData
import java.util.*

class BluetoothDataHandler(
    private val onDataUpdate: (ScooterData) -> Unit
) {
    companion object {
        private const val TAG = "BluetoothDataHandler"

        // Types de frames reconnus
        private const val FRAME_TYPE_NULL_RESPONSE = "NULL_RESPONSE"
        private const val FRAME_TYPE_KEEP_ALIVE = "KEEP_ALIVE"
        private const val FRAME_TYPE_M0ROBOT_MAIN_8BYTE = "M0ROBOT_MAIN_8BYTE"
        private const val FRAME_TYPE_M0ROBOT_EXTENDED_16BYTE = "M0ROBOT_EXTENDED_16BYTE"
        private const val FRAME_TYPE_DIAGNOSTIC = "DIAGNOSTIC"
        private const val FRAME_TYPE_EMPTY = "EMPTY"
        private const val FRAME_TYPE_UNKNOWN = "UNKNOWN"
    }

    private var currentData = ScooterData()
    private val bluetoothDiag = BluetoothDiag
    private val frameHistory = mutableListOf<ByteArray>()
    private var frameCount = 0
    private var validFrameCount = 0

    // Statistiques pour le rapport
    private val frameTypeStats = mutableMapOf<String, Int>()

    fun handleData(data: ByteArray) {
        frameCount++
        frameHistory.add(data)

        try {
            Log.d(TAG, "[BLE] RX: [] ${data.size} bytes: ${bytesToHex(data)}")
            Log.d(TAG, "Frame reçue [${data.size} bytes]: ${bytesToHex(data)}")

            // Analyser avec BluetoothDiag
            val diagnosticResult = bluetoothDiag.analyzeFrame(data)

            val frameType = identifyFrameType(data)
            updateFrameStats(frameType)

            Log.d(TAG, "Type de frame détecté: $frameType")

            // Parser selon le type de frame
            val parsedData = when (frameType) {
                FRAME_TYPE_NULL_RESPONSE -> {
                    Log.d(TAG, "Frame NULL response - ignorer")
                    return
                }
                FRAME_TYPE_KEEP_ALIVE -> {
                    Log.d(TAG, "Keep-alive reçu")
                    generateDiagnosticReport()
                    return
                }
                FRAME_TYPE_M0ROBOT_MAIN_8BYTE -> {
                    validFrameCount++
                    parseM0RobotMainFrame(data)
                }
                FRAME_TYPE_M0ROBOT_EXTENDED_16BYTE -> {
                    validFrameCount++
                    parseM0RobotExtendedFrame(data)
                }
                FRAME_TYPE_DIAGNOSTIC -> {
                    Log.d(TAG, "Frame de diagnostic reçue: ${bytesToHex(data)}")
                    return
                }
                FRAME_TYPE_EMPTY -> {
                    Log.d(TAG, "Tentative parsing générique pour frame inconnue: ${bytesToHex(data)}")
                    return
                }
                else -> {
                    Log.w(TAG, "Type de frame non reconnu: ${bytesToHex(data)}")
                    tryGenericParsing(data)
                }
            }

            // Mettre à jour les données si nécessaire
            updateCurrentData(parsedData)

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du traitement des données", e)
        }
    }

    private fun identifyFrameType(data: ByteArray): String {
        return when {
            data.isEmpty() -> FRAME_TYPE_EMPTY
            data.size == 2 && data.contentEquals(byteArrayOf(0x00, 0x00)) -> FRAME_TYPE_NULL_RESPONSE
            data.size == 2 && data.contentEquals(byteArrayOf(0x00, 0x01)) -> FRAME_TYPE_KEEP_ALIVE
            data.size == 4 && data[2] == 0xFF.toByte() && data[3] == 0xFF.toByte() -> FRAME_TYPE_DIAGNOSTIC
            data.size == 8 && data[0] == 0x08.toByte() -> FRAME_TYPE_M0ROBOT_MAIN_8BYTE
            data.size == 16 && data[0] == 0x5A.toByte() -> FRAME_TYPE_M0ROBOT_EXTENDED_16BYTE
            else -> FRAME_TYPE_UNKNOWN
        }
    }

    private fun parseM0RobotMainFrame(data: ByteArray): ScooterData {
        // Frame format: 08 00 0A 00 00 00 90 01
        var speed = 0f
        var voltage = 0f

        try {
            // Vitesse: bytes 2-3 (Little Endian)
            val speedRaw = ((data[3].toInt() and 0xFF) shl 8) or (data[2].toInt() and 0xFF)
            if (speedRaw in 1..8000) {
                speed = speedRaw / 100f * 2.56f  // Facteur observé dans les logs
                Log.d(TAG, "Vitesse mise à jour: ${speed}km/h (raw: $speedRaw)")
            }

            // Voltage: bytes 6-7 (Little Endian)
            val voltageRaw = ((data[7].toInt() and 0xFF) shl 8) or (data[6].toInt() and 0xFF)
            if (voltageRaw in 200..700) {  // Plage réaliste pour voltage * 10
                voltage = voltageRaw / 10f
                Log.d(TAG, "Voltage mis à jour: ${voltage}V (raw: $voltageRaw)")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing frame principale", e)
        }

        return ScooterData(
            speed = speed,
            voltage = voltage
        )
    }

    private fun parseM0RobotExtendedFrame(data: ByteArray): ScooterData {
        // Frame format: 5A 00 00 34 00 00 00 00 00 00 00 00 00 00 00 71
        var battery = 0f
        var odometer = 0f
        var temperature = 0f

        try {
            Log.d(TAG, "=== DEBUG FRAME 16 BYTES ===")
            Log.d(TAG, "Frame complète: ${bytesToHex(data)}")

            // Debug chaque byte
            for (i in data.indices) {
                Log.d(TAG, "Byte[$i] = 0x${"%02X".format(data[i])} = ${data[i].toUByte().toInt()}")
            }

            // Batterie: byte 3 (confirmé dans les logs)
            battery = data[3].toUByte().toFloat()
            if (battery in 1f..100f) {
                Log.d(TAG, "✓ Batterie trouvée: ${battery}% (byte[3] = ${data[3].toUByte().toInt()})")
            }

            // Recherche simplifiée de l'odométrie
            Log.d(TAG, "--- RECHERCHE ODOMÉTRE SIMPLIFIÉE ---")

            // Analyse exhaustive de tous les bytes pour trouver 324.8km (3248 en raw)
            val target3248 = 3248 // 324.8km * 10
            val target32480 = 32480 // 324.8km * 100

            // Test toutes les combinaisons possibles
            for (offset in 1..14) {
                if (offset + 1 < data.size) {
                    val value16LE = ((data[offset + 1].toInt() and 0xFF) shl 8) or (data[offset].toInt() and 0xFF)
                    val value16BE = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

                    Log.d(TAG, "Offset $offset: LE=$value16LE, BE=$value16BE")

                    // Recherche spécifique de 324.8km
                    if (value16LE == target3248 || value16LE == target32480) {
                        odometer = if (value16LE == target3248) value16LE / 10f else value16LE / 100f
                        Log.d(TAG, "🎯 ODOMÉTRIE TROUVÉE! Offset $offset (LE): ${odometer}km (raw: $value16LE)")
                    }
                    if (value16BE == target3248 || value16BE == target32480) {
                        odometer = if (value16BE == target3248) value16BE / 10f else value16BE / 100f
                        Log.d(TAG, "🎯 ODOMÉTRIE TROUVÉE! Offset $offset (BE): ${odometer}km (raw: $value16BE)")
                    }
                }

                // Test aussi 32-bit
                if (offset + 3 < data.size) {
                    val value32LE = ((data[offset + 3].toInt() and 0xFF) shl 24) or
                            ((data[offset + 2].toInt() and 0xFF) shl 16) or
                            ((data[offset + 1].toInt() and 0xFF) shl 8) or
                            (data[offset].toInt() and 0xFF)

                    if (value32LE == target3248 || value32LE == target32480) {
                        odometer = if (value32LE == target3248) value32LE / 10f else value32LE / 100f
                        Log.d(TAG, "🎯 ODOMÉTRIE 32-BIT TROUVÉE! Offset $offset: ${odometer}km (raw: $value32LE)")
                    }
                }
            }

            // Si pas trouvé, chercher des valeurs dans une plage raisonnable
            if (odometer == 0f) {
                Log.d(TAG, "❌ Odométrie 324.8km non trouvée, recherche valeurs plausibles...")
                for (offset in 1..14 step 2) {
                    if (offset + 1 < data.size) {
                        val value16LE = ((data[offset + 1].toInt() and 0xFF) shl 8) or (data[offset].toInt() and 0xFF)
                        if (value16LE in 100..9999) {
                            Log.d(TAG, "Candidat plausible offset $offset: ${value16LE/10f}km")
                        }
                    }
                }
            }

            // Recherche de température (généralement 10-80°C)
            Log.d(TAG, "--- RECHERCHE TEMPÉRATURE ---")
            for (i in data.indices) {
                val tempValue = data[i].toUByte().toInt()
                if (tempValue in 10..80 && tempValue != battery.toInt()) {
                    temperature = tempValue.toFloat()
                    Log.d(TAG, "Température trouvée: ${temperature}°C (byte[$i])")
                    break
                }
            }

            Log.d(TAG, "✅ Données mises à jour - Odomètre: ${odometer}km, Temp: ${temperature}°C")
            Log.d(TAG, "=== FIN DEBUG FRAME 16 BYTES ===")

        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing frame étendue", e)
        }

        return ScooterData(
            battery = battery,
            odometer = odometer,
            temperature = temperature
        )
    }

    private fun tryGenericParsing(data: ByteArray): ScooterData {
        // Tentative de parsing générique pour frames inconnues
        var speed = 0f
        var battery = 0f
        var voltage = 0f

        try {
            when (data.size) {
                2 -> {
                    // Données simples
                    val val1 = data[0].toUByte().toInt()
                    val val2 = data[1].toUByte().toInt()

                    if (val1 in 1..100) battery = val1.toFloat()
                    if (val2 in 1..100 && val2 != val1) speed = val2.toFloat()
                }

                4 -> {
                    // Données moyennes
                    for (i in 0..1) {
                        val value = data[i].toUByte().toInt()
                        if (value in 1..100) {
                            if (battery == 0f) battery = value.toFloat()
                            else if (speed == 0f) speed = value.toFloat()
                        }
                    }
                }

                else -> {
                    // Recherche de patterns dans frames plus longues
                    for (i in data.indices) {
                        val value = data[i].toUByte().toInt()
                        if (value in 1..100) {
                            if (battery == 0f) battery = value.toFloat()
                            else if (speed == 0f && value != battery.toInt()) speed = value.toFloat()
                        }
                    }
                }
            }

            if (speed > 0f || battery > 0f || voltage > 0f) {
                Log.d(TAG, "Parsing générique réussi: speed=$speed, battery=$battery, voltage=$voltage")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing générique", e)
        }

        return ScooterData(speed = speed, battery = battery, voltage = voltage)
    }

    private fun updateCurrentData(newData: ScooterData) {
        var hasUpdates = false
        var updatedData = currentData

        // Mettre à jour seulement si les nouvelles valeurs sont valides et différentes
        if (newData.speed > 0f && newData.speed != currentData.speed) {
            updatedData = updatedData.copy(speed = newData.speed)
            hasUpdates = true
        }

        if (newData.battery > 0f && newData.battery != currentData.battery) {
            updatedData = updatedData.copy(battery = newData.battery)
            hasUpdates = true
        }

        if (newData.voltage > 0f && newData.voltage != currentData.voltage) {
            updatedData = updatedData.copy(voltage = newData.voltage)
            hasUpdates = true
        }

        if (newData.current != 0f && newData.current != currentData.current) {
            updatedData = updatedData.copy(current = newData.current)
            hasUpdates = true
        }

        if (newData.power > 0f && newData.power != currentData.power) {
            updatedData = updatedData.copy(power = newData.power)
            hasUpdates = true
        }

        if (newData.temperature > 0f && newData.temperature != currentData.temperature) {
            updatedData = updatedData.copy(temperature = newData.temperature)
            hasUpdates = true
        }

        if (newData.odometer > 0f && newData.odometer != currentData.odometer) {
            updatedData = updatedData.copy(odometer = newData.odometer)
            hasUpdates = true
        }

        if (hasUpdates) {
            currentData = updatedData.copy(
                lastUpdate = Date(),
                isConnected = true
            )

            Log.d(TAG, "[SCOOTER] Données reçues - Vitesse: ${currentData.speed}km/h, Batterie: ${currentData.battery}%, Tension: ${currentData.voltage}V")
            onDataUpdate(currentData)
        }
    }

    private fun updateFrameStats(frameType: String) {
        frameTypeStats[frameType] = frameTypeStats.getOrDefault(frameType, 0) + 1
    }

    private fun generateDiagnosticReport() {
        val validityRate = if (frameCount > 0) (validFrameCount.toFloat() / frameCount * 100).toInt() else 0

        Log.i(TAG, "=== RAPPORT DIAGNOSTIC ===")
        Log.i(TAG, "Frames récentes: $frameCount")
        Log.i(TAG, "Frames valides: $validFrameCount ($validityRate%)")
        Log.i(TAG, "Types de frames: $frameTypeStats")
        Log.i(TAG, "Dernières données: Speed=${currentData.speed}km/h, Battery=${currentData.battery}%, Voltage=${currentData.voltage}V")

        if (validityRate < 50) {
            Log.w(TAG, "ATTENTION: Faible taux de frames valides - connexion instable")
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(it) }
    }

    fun getCurrentData(): ScooterData = currentData

    fun resetStats() {
        frameCount = 0
        validFrameCount = 0
        frameTypeStats.clear()
        frameHistory.clear()
        Log.d(TAG, "Statistiques réinitialisées")
    }
}