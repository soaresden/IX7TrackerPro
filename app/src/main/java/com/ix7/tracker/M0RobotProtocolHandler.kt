package com.ix7.tracker

import kotlin.experimental.and

class M0RobotProtocolHandler {

    companion object {
        // Commandes pour demander des informations
        const val CMD_GET_BATTERY = 0x31.toByte()
        const val CMD_GET_SPEED = 0x32.toByte()
        const val CMD_GET_DISTANCE = 0x33.toByte()
        const val CMD_GET_INFO = 0x34.toByte()
        const val CMD_GET_STATUS = 0x35.toByte()

        // Headers de réponse typiques
        const val HEADER_BATTERY = 0xA1.toByte()
        const val HEADER_SPEED = 0xA2.toByte()
        const val HEADER_DISTANCE = 0xA3.toByte()
        const val HEADER_INFO = 0xA4.toByte()
        const val HEADER_STATUS = 0xA5.toByte()
    }

    /**
     * Parse les données reçues de la trottinette
     */
    fun parseIncomingData(data: ByteArray): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        if (data.isEmpty()) return result

        try {
            when (data[0]) {
                HEADER_BATTERY -> parseBatteryData(data, result)
                HEADER_SPEED -> parseSpeedData(data, result)
                HEADER_DISTANCE -> parseDistanceData(data, result)
                HEADER_INFO -> parseInfoData(data, result)
                HEADER_STATUS -> parseStatusData(data, result)
                else -> parseGenericData(data, result)
            }
        } catch (e: Exception) {
            LogManager.logError("Erreur lors du parsing des données", e)
        }

        return result
    }

    private fun parseBatteryData(data: ByteArray, result: MutableMap<String, Any>) {
        if (data.size >= 6) {
            // Format typique: [Header][Level][Voltage_High][Voltage_Low][Temp][Checksum]
            val batteryLevel = (data[1] and 0xFF.toByte()).toInt()
            val voltageHigh = (data[2] and 0xFF.toByte()).toInt()
            val voltageLow = (data[3] and 0xFF.toByte()).toInt()
            val temperature = (data[4] and 0xFF.toByte()).toInt()

            val voltage = (voltageHigh * 256 + voltageLow) / 100.0

            result["batteryLevel"] = batteryLevel
            result["voltage"] = voltage
            result["batteryTemp"] = temperature
        }
    }

    private fun parseSpeedData(data: ByteArray, result: MutableMap<String, Any>) {
        if (data.size >= 4) {
            // Format: [Header][Speed_High][Speed_Low][Checksum]
            val speedHigh = (data[1] and 0xFF.toByte()).toInt()
            val speedLow = (data[2] and 0xFF.toByte()).toInt()
            val speed = (speedHigh * 256 + speedLow) / 100.0

            result["speed"] = speed
        }
    }

    private fun parseDistanceData(data: ByteArray, result: MutableMap<String, Any>) {
        if (data.size >= 6) {
            // Format: [Header][Dist_B3][Dist_B2][Dist_B1][Dist_B0][Checksum]
            val distance = ((data[1] and 0xFF.toByte()).toInt() shl 24) +
                    ((data[2] and 0xFF.toByte()).toInt() shl 16) +
                    ((data[3] and 0xFF.toByte()).toInt() shl 8) +
                    (data[4] and 0xFF.toByte()).toInt()

            result["totalDistance"] = distance / 1000.0 // Convertir en km
        }
    }

    private fun parseInfoData(data: ByteArray, result: MutableMap<String, Any>) {
        if (data.size >= 10) {
            // Format: [Header][Model][FW_Major][FW_Minor][Serial...][Checksum]
            val model = "IX7-${(data[1] and 0xFF.toByte()).toInt()}"
            val firmwareMajor = (data[2] and 0xFF.toByte()).toInt()
            val firmwareMinor = (data[3] and 0xFF.toByte()).toInt()
            val firmware = "$firmwareMajor.$firmwareMinor"

            result["model"] = model
            result["firmware"] = firmware
        }
    }

    private fun parseStatusData(data: ByteArray, result: MutableMap<String, Any>) {
        if (data.size >= 4) {
            // Format: [Header][Status_Flags][Error_Code][Checksum]
            val statusFlags = (data[1] and 0xFF.toByte()).toInt()
            val errorCode = (data[2] and 0xFF.toByte()).toInt()

            // Analyse des flags de statut
            result["brakeStatus"] = (statusFlags and 0x01) != 0
            result["lightStatus"] = (statusFlags and 0x02) != 0
            result["tiltSensor"] = (statusFlags and 0x04) != 0
            result["motorTemp"] = (statusFlags and 0xF0) shr 4 // Température moteur encodée

            if (errorCode != 0) {
                result["errorCode"] = "E${errorCode.toString().padStart(2, '0')}"
            }
        }
    }

    private fun parseGenericData(data: ByteArray, result: MutableMap<String, Any>) {
        // Tentative de parsing générique pour données non reconnues
        if (data.size >= 2) {
            val hex = data.joinToString("") { "%02X".format(it) }
            LogManager.logInfo("Données non reconnues: $hex")

            // Essayer de détecter des patterns communs
            if (data.size >= 3) {
                val value = ((data[1] and 0xFF.toByte()).toInt() shl 8) +
                        (data[2] and 0xFF.toByte()).toInt()

                // Heuristiques basées sur les valeurs
                when {
                    value in 0..100 -> result["possibleBatteryLevel"] = value
                    value in 0..5000 -> result["possibleSpeed"] = value / 100.0
                    value > 10000 -> result["possibleDistance"] = value / 1000.0
                }
            }
        }
    }

    /**
     * Crée une commande pour demander des informations spécifiques
     */
    fun createInfoRequest(command: Byte): ByteArray {
        // Format simple: [Header][Command][Checksum]
        val packet = byteArrayOf(0x55.toByte(), command, 0x00)
        packet[2] = calculateChecksum(packet.sliceArray(0..1))
        return packet
    }

    /**
     * Crée une requête pour obtenir toutes les informations
     */
    fun createFullInfoRequest(): List<ByteArray> {
        return listOf(
            createInfoRequest(CMD_GET_BATTERY),
            createInfoRequest(CMD_GET_SPEED),
            createInfoRequest(CMD_GET_DISTANCE),
            createInfoRequest(CMD_GET_INFO),
            createInfoRequest(CMD_GET_STATUS)
        )
    }

    /**
     * Calcule le checksum pour un packet
     */
    private fun calculateChecksum(data: ByteArray): Byte {
        var checksum = 0
        for (byte in data) {
            checksum += (byte and 0xFF.toByte()).toInt()
        }
        return (checksum and 0xFF).toByte()
    }

    /**
     * Valide le checksum d'un packet reçu
     */
    fun validateChecksum(data: ByteArray): Boolean {
        if (data.size < 2) return false

        val receivedChecksum = data.last()
        val calculatedChecksum = calculateChecksum(data.sliceArray(0 until data.size - 1))

        return receivedChecksum == calculatedChecksum
    }

    /**
     * Convertit les données brutes en format hexadécimal pour debug
     */
    fun dataToHex(data: ByteArray): String {
        return data.joinToString(" ") { "%02X".format(it) }
    }

    /**
     * Parse les données selon le protocole M365/IX7 standard
     * Adapté pour les trottinettes IX7 qui utilisent souvent le même protocole
     */
    fun parseM365Protocol(data: ByteArray): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        if (data.size < 3) return result

        // Le protocole M365 utilise généralement le format:
        // [0x55][0xAA][Longueur][Commande][Données...][Checksum1][Checksum2]

        if (data[0] == 0x55.toByte() && data[1] == 0xAA.toByte()) {
            val length = (data[2] and 0xFF.toByte()).toInt()
            val command = data[3]

            if (data.size >= length + 6) { // Header(2) + Length(1) + Command(1) + Data + Checksum(2)
                when (command) {
                    0x21.toByte() -> parseM365BatteryInfo(data.sliceArray(4 until 4 + length), result)
                    0x26.toByte() -> parseM365SpeedInfo(data.sliceArray(4 until 4 + length), result)
                    0x25.toByte() -> parseM365Status(data.sliceArray(4 until 4 + length), result)
                    else -> LogManager.logInfo("Commande M365 inconnue: 0x${"%02X".format(command)}")
                }
            }
        }

        return result
    }

    private fun parseM365BatteryInfo(data: ByteArray, result: MutableMap<String, Any>) {
        if (data.size >= 12) {
            // Format M365 pour les infos batterie
            val batteryLevel = (data[0] and 0xFF.toByte()).toInt()
            val voltage = ((data[2] and 0xFF.toByte()).toInt() shl 8) + (data[1] and 0xFF.toByte()).toInt()
            val current = ((data[4] and 0xFF.toByte()).toInt() shl 8) + (data[3] and 0xFF.toByte()).toInt()
            val temperature = ((data[6] and 0xFF.toByte()).toInt() shl 8) + (data[5] and 0xFF.toByte()).toInt()

            result["batteryLevel"] = batteryLevel
            result["voltage"] = voltage / 100.0
            result["current"] = if (current > 32767) (current - 65536) / 100.0 else current / 100.0
            result["batteryTemp"] = (temperature - 20) / 10
            result["power"] = (voltage / 100.0) * (if (current > 32767) (current - 65536) / 100.0 else current / 100.0)

            // État de la batterie (dérivé)
            result["batteryStatus"] = if (batteryLevel > 20) 1 else 0
        }
    }

    private fun parseM365SpeedInfo(data: ByteArray, result: MutableMap<String, Any>) {
        if (data.size >= 16) {
            val speed = ((data[1] and 0xFF.toByte()).toInt() shl 8) + (data[0] and 0xFF.toByte()).toInt()
            val totalDistance = ((data[5] and 0xFF.toByte()).toInt() shl 24) +
                    ((data[4] and 0xFF.toByte()).toInt() shl 16) +
                    ((data[3] and 0xFF.toByte()).toInt() shl 8) +
                    (data[2] and 0xFF.toByte()).toInt()

            result["speed"] = speed / 1000.0
            result["totalDistance"] = totalDistance / 1000.0

            // Température du scooter si disponible
            if (data.size >= 18) {
                val temp = ((data[11] and 0xFF.toByte()).toInt() shl 8) + (data[10] and 0xFF.toByte()).toInt()
                result["scooterTemp"] = (temp - 20) / 10.0
            }

            // Temps de conduite si disponible
            if (data.size >= 20) {
                val ridingTimeSeconds = ((data[9] and 0xFF.toByte()).toInt() shl 8) + (data[8] and 0xFF.toByte()).toInt()
                val hours = ridingTimeSeconds / 3600
                val minutes = (ridingTimeSeconds % 3600) / 60
                val seconds = ridingTimeSeconds % 60
                result["ridingTime"] = "${hours}H ${minutes}M ${seconds}S"
            }
        }
    }

    private fun parseM365Status(data: ByteArray, result: MutableMap<String, Any>) {
        if (data.size >= 8) {
            val errorCode = (data[0] and 0xFF.toByte()).toInt()
            val warningCode = (data[1] and 0xFF.toByte()).toInt()

            result["errorCode"] = errorCode
            result["warningCode"] = warningCode
        }

        // Versions si disponibles
        if (data.size >= 16) {
            try {
                val electricVersion = "${data[8]}.${data[9]}.${data[10]} (${String.format("%02X%02X%02X%02X", data[12], data[13], data[14], data[15])})"
                result["electricVersion"] = electricVersion

                val bluetoothVersion = "${data[4]}.${data[5]}.${data[6]} (${String.format("%02X%02X", data[7], data[8])})"
                result["bluetoothVersion"] = bluetoothVersion
            } catch (e: Exception) {
                LogManager.logError("Erreur parsing versions", e)
            }
        }
    }
}