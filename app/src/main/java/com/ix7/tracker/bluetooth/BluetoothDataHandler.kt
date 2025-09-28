package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.ScooterData
import java.util.*

class BluetoothDataHandler(
    private val onDataUpdate: (ScooterData) -> Unit,
    private val sendCommand: (ByteArray) -> Unit  // Callback pour envoyer des commandes
) {
    companion object {
        private const val TAG = "BluetoothDataHandler"

        // Commandes spécifiques M0Robot
        private val CMD_GET_INFO = byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x03, 0x22, 0x01, 0x01, 0x27) // Demande infos générales
        private val CMD_GET_BATTERY = byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x03, 0x22, 0x01, 0x31, 0x57.toByte()) // Demande batterie
        private val CMD_GET_ODOMETER = byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x03, 0x22, 0x01, 0x29, 0x4F) // Demande odométrie
        private val CMD_GET_REALTIME = byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x03, 0x22, 0x01, 0x20, 0x46) // Demande données temps réel
        private val CMD_GET_TEMPERATURE = byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x03, 0x22, 0x01, 0x1A, 0x40) // Demande température

        // Types de frames reconnus
        private const val FRAME_TYPE_KEEP_ALIVE = "KEEP_ALIVE"
        private const val FRAME_TYPE_M0ROBOT_MAIN_8BYTE = "M0ROBOT_MAIN_8BYTE"
        private const val FRAME_TYPE_M0ROBOT_EXTENDED_16BYTE = "M0ROBOT_EXTENDED_16BYTE"
        private const val FRAME_TYPE_DIAGNOSTIC = "DIAGNOSTIC"
        private const val FRAME_TYPE_EMPTY = "EMPTY"
        private const val FRAME_TYPE_RESPONSE = "RESPONSE" // Nouvelles réponses aux commandes
        private const val FRAME_TYPE_UNKNOWN = "UNKNOWN"
    }

    private var currentData = ScooterData()
    private val bluetoothDiag = BluetoothDiag
    private var frameCount = 0
    private var validFrameCount = 0
    private var isInitialized = false
    private var commandSequenceStep = 0

    // Statistiques pour le rapport
    private val frameTypeStats = mutableMapOf<String, Int>()
    private val rawDataBuffer = mutableMapOf<String, ByteArray>()

    fun handleData(data: ByteArray) {
        frameCount++

        try {
            Log.d(TAG, "[BLE] RX: [] ${data.size} bytes: ${bytesToHex(data)}")

            // Analyser avec BluetoothDiag
            val diagnosticResult = bluetoothDiag.analyzeFrame(data)

            val frameType = identifyFrameType(data)
            updateFrameStats(frameType)

            Log.d(TAG, "Type de frame détecté: $frameType")

            // Si pas encore initialisé, commencer la séquence d'init
            if (!isInitialized && frameType == FRAME_TYPE_KEEP_ALIVE) {
                startInitializationSequence()
                return
            }

            // Parser selon le type de frame
            when (frameType) {
                FRAME_TYPE_KEEP_ALIVE -> {
                    Log.d(TAG, "Keep-alive reçu")
                    if (isInitialized) {
                        continueCommandSequence()
                    }
                    generateDiagnosticReport()
                    return
                }

                FRAME_TYPE_RESPONSE -> {
                    validFrameCount++
                    parseCommandResponse(data)
                    return
                }

                FRAME_TYPE_M0ROBOT_MAIN_8BYTE -> {
                    validFrameCount++
                    val parsedData = parseM0RobotMainFrame(data)
                    updateCurrentData(parsedData)
                }

                FRAME_TYPE_M0ROBOT_EXTENDED_16BYTE -> {
                    validFrameCount++
                    val parsedData = parseM0RobotExtendedFrame(data)
                    updateCurrentData(parsedData)
                }

                FRAME_TYPE_DIAGNOSTIC -> {
                    Log.d(TAG, "Frame de diagnostic reçue: ${bytesToHex(data)}")
                    return
                }

                FRAME_TYPE_EMPTY -> {
                    Log.d(TAG, "Frame vide ignorée")
                    return
                }

                else -> {
                    Log.w(TAG, "Type de frame non reconnu: ${bytesToHex(data)}")
                    // Essayer de parser comme réponse de commande
                    parseCommandResponse(data)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du traitement des données", e)
        }
    }

    private fun startInitializationSequence() {
        Log.i(TAG, "🚀 DÉMARRAGE SÉQUENCE D'INITIALISATION M0ROBOT")
        isInitialized = true
        commandSequenceStep = 0
        continueCommandSequence()
    }

    private fun continueCommandSequence() {
        when (commandSequenceStep) {
            0 -> {
                Log.d(TAG, "📡 Envoi commande: Demande infos générales")
                sendCommand(CMD_GET_INFO)
            }
            1 -> {
                Log.d(TAG, "📡 Envoi commande: Demande batterie")
                sendCommand(CMD_GET_BATTERY)
            }
            2 -> {
                Log.d(TAG, "📡 Envoi commande: Demande odométrie")
                sendCommand(CMD_GET_ODOMETER)
            }
            3 -> {
                Log.d(TAG, "📡 Envoi commande: Demande données temps réel")
                sendCommand(CMD_GET_REALTIME)
            }
            4 -> {
                Log.d(TAG, "📡 Envoi commande: Demande température")
                sendCommand(CMD_GET_TEMPERATURE)
            }
            else -> {
                // Revenir au début pour maintenir le flux de données
                commandSequenceStep = -1
            }
        }
        commandSequenceStep++
    }

    private fun identifyFrameType(data: ByteArray): String {
        return when {
            data.isEmpty() -> FRAME_TYPE_EMPTY
            data.size == 2 && data.contentEquals(byteArrayOf(0x00, 0x01)) -> FRAME_TYPE_KEEP_ALIVE
            data.size == 4 && data[2] == 0xFF.toByte() && data[3] == 0xFF.toByte() -> FRAME_TYPE_DIAGNOSTIC
            data.size == 8 && data[0] == 0x08.toByte() -> FRAME_TYPE_M0ROBOT_MAIN_8BYTE
            data.size == 16 && data[0] == 0x5A.toByte() -> FRAME_TYPE_M0ROBOT_EXTENDED_16BYTE

            // Détecter les réponses aux commandes
            data.size >= 6 && data[0] == 0x55.toByte() && data[1] == 0xAA.toByte() -> FRAME_TYPE_RESPONSE
            data.size >= 3 && data[0] == 0xAA.toByte() -> FRAME_TYPE_RESPONSE

            else -> FRAME_TYPE_UNKNOWN
        }
    }

    private fun parseCommandResponse(data: ByteArray) {
        Log.d(TAG, "🔍 ANALYSE RÉPONSE COMMANDE: ${bytesToHex(data)}")

        when {
            // Réponse format 55 AA ...
            data.size >= 6 && data[0] == 0x55.toByte() && data[1] == 0xAA.toByte() -> {
                parseProtocolResponse(data)
            }

            // Réponse format AA ...
            data.size >= 3 && data[0] == 0xAA.toByte() -> {
                parseSimpleResponse(data)
            }

            // Autres formats possibles
            else -> {
                Log.d(TAG, "Format de réponse non reconnu, stockage raw data")
                rawDataBuffer["unknown_${System.currentTimeMillis()}"] = data
                analyzeUnknownResponse(data)
            }
        }
    }

    private fun parseProtocolResponse(data: ByteArray) {
        try {
            val length = data[2].toUByte().toInt()
            val command = data[3].toUByte().toInt()
            val subCommand = if (data.size > 4) data[4].toUByte().toInt() else 0

            Log.d(TAG, "Réponse protocole - Length: $length, Cmd: 0x${command.toString(16)}, SubCmd: 0x${subCommand.toString(16)}")

            when (command) {
                0x22 -> parseInfoResponse(data, subCommand)
                0x23 -> parseBatteryResponse(data, subCommand)
                0x21 -> parseRealtimeResponse(data, subCommand)
                else -> {
                    Log.d(TAG, "Commande inconnue: 0x${command.toString(16)}")
                    rawDataBuffer["cmd_${command}_${subCommand}"] = data
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing réponse protocole", e)
        }
    }

    private fun parseInfoResponse(data: ByteArray, subCommand: Int) {
        Log.d(TAG, "📊 PARSING RÉPONSE INFO (subcmd: 0x${subCommand.toString(16)})")

        when (subCommand) {
            0x01 -> {
                // Infos générales
                if (data.size >= 10) {
                    val battery = data[5].toUByte().toInt()
                    val voltage = if (data.size > 6) ((data[7].toUByte().toInt() shl 8) or data[6].toUByte().toInt()) / 100f else 0f

                    Log.d(TAG, "✅ BATTERIE TROUVÉE: ${battery}%")
                    Log.d(TAG, "✅ VOLTAGE TROUVÉ: ${voltage}V")

                    updateCurrentData(ScooterData(battery = battery.toFloat(), voltage = voltage))
                }
            }

            0x31 -> {
                // Réponse spécifique batterie
                if (data.size >= 6) {
                    val battery = data[5].toUByte().toInt()
                    Log.d(TAG, "🔋 BATTERIE SPÉCIFIQUE: ${battery}%")
                    updateCurrentData(ScooterData(battery = battery.toFloat()))
                }
            }

            0x29 -> {
                // Réponse odométrie
                if (data.size >= 8) {
                    val odometerRaw = ((data[7].toUByte().toInt() shl 8) or data[6].toUByte().toInt())
                    val odometer = odometerRaw / 10f
                    Log.d(TAG, "🛣️ ODOMÉTRIE TROUVÉE: ${odometer}km (raw: $odometerRaw)")
                    updateCurrentData(ScooterData(odometer = odometer))
                }
            }

            0x1A -> {
                // Réponse température
                if (data.size >= 6) {
                    val temperature = data[5].toUByte().toInt().toFloat()
                    Log.d(TAG, "🌡️ TEMPÉRATURE TROUVÉE: ${temperature}°C")
                    updateCurrentData(ScooterData(temperature = temperature))
                }
            }
        }
    }

    private fun parseBatteryResponse(data: ByteArray, subCommand: Int) {
        Log.d(TAG, "🔋 PARSING RÉPONSE BATTERIE")
        // Implémentation pour réponses spécifiques batterie
    }

    private fun parseRealtimeResponse(data: ByteArray, subCommand: Int) {
        Log.d(TAG, "⚡ PARSING RÉPONSE TEMPS RÉEL")

        if (data.size >= 8) {
            val speed = ((data[6].toUByte().toInt() shl 8) or data[5].toUByte().toInt()) / 100f
            val current = if (data.size > 8) ((data[8].toUByte().toInt() shl 8) or data[7].toUByte().toInt()) / 100f else 0f

            Log.d(TAG, "🏃 VITESSE TEMPS RÉEL: ${speed}km/h")
            if (current > 0) Log.d(TAG, "⚡ COURANT: ${current}A")

            updateCurrentData(ScooterData(speed = speed, current = current))
        }
    }

    private fun parseSimpleResponse(data: ByteArray) {
        Log.d(TAG, "Simple response: ${bytesToHex(data)}")
        rawDataBuffer["simple_${System.currentTimeMillis()}"] = data
    }

    private fun analyzeUnknownResponse(data: ByteArray) {
        Log.d(TAG, "🔍 ANALYSE FRAME INCONNUE")

        // Rechercher des patterns de données
        for (i in data.indices) {
            val value = data[i].toUByte().toInt()

            // Batterie potentielle (0-100)
            if (value in 20..100) {
                Log.d(TAG, "Candidat batterie à offset $i: ${value}%")
            }

            // Température potentielle (0-80°C)
            if (value in 10..80) {
                Log.d(TAG, "Candidat température à offset $i: ${value}°C")
            }
        }

        // Rechercher des valeurs 16-bit
        for (i in 0 until data.size - 1 step 2) {
            val value16LE = ((data[i + 1].toInt() and 0xFF) shl 8) or (data[i].toInt() and 0xFF)
            val value16BE = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)

            // Odométrie potentielle (100-9999 = 10-999.9km)
            if (value16LE in 100..9999) {
                Log.d(TAG, "Candidat odométrie à offset $i (LE): ${value16LE / 10f}km")
            }
            if (value16BE in 100..9999 && value16BE != value16LE) {
                Log.d(TAG, "Candidat odométrie à offset $i (BE): ${value16BE / 10f}km")
            }

            // Voltage potentiel (2000-7000 = 20-70V)
            if (value16LE in 2000..7000) {
                Log.d(TAG, "Candidat voltage à offset $i (LE): ${value16LE / 100f}V")
            }
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

        return ScooterData(speed = speed, voltage = voltage)
    }

    private fun parseM0RobotExtendedFrame(data: ByteArray): ScooterData {
        // Frame format: 5A 00 00 34 00 00 00 00 00 00 00 00 00 00 00 71
        // Cette frame semble être un placeholder - les vraies données viennent des commandes

        Log.d(TAG, "Frame étendue reçue - demande de vraies données via commandes")
        return ScooterData()
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

            Log.i(TAG, "📱 DONNÉES MISES À JOUR: Vitesse=${currentData.speed}km/h, Batterie=${currentData.battery}%, Voltage=${currentData.voltage}V, Odométrie=${currentData.odometer}km, Temp=${currentData.temperature}°C")
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
        Log.i(TAG, "Initialisé: $isInitialized, Étape commande: $commandSequenceStep")
        Log.i(TAG, "Données actuelles: Speed=${currentData.speed}km/h, Battery=${currentData.battery}%, Voltage=${currentData.voltage}V, Odometer=${currentData.odometer}km")

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
        rawDataBuffer.clear()
        isInitialized = false
        commandSequenceStep = 0
        Log.d(TAG, "Statistiques et état réinitialisés")
    }

    fun forceInitialization() {
        Log.i(TAG, "🔄 FORCE RÉINITIALISATION")
        resetStats()
        startInitializationSequence()
    }
}