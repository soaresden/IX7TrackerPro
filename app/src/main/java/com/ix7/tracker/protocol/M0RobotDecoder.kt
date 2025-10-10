package com.ix7.tracker.protocol

import kotlin.math.roundToInt

/**
 * Décodeur des réponses M0Robot
 * Analyse et interprète les données reçues de la trottinette
 */
object M0RobotDecoder {

    // ========== TYPES DE RÉPONSES ==========
    enum class ResponseType {
        STATUS_BASIC,           // Données de base (vitesse, batterie)
        STATUS_EXTENDED,        // Données étendues
        TELEMETRY,             // Télémétrie complète
        ACKNOWLEDGE,           // Accusé de réception
        ERROR,                 // Erreur
        KEEP_ALIVE,           // Keep-alive
        UNKNOWN               // Non identifié
    }

    // ========== STRUCTURE DE DONNÉES DÉCODÉES ==========
    data class DecodedData(
        val type: ResponseType,
        val rawData: ByteArray,
        val hexString: String,

        // Données principales
        val speed: Float? = null,           // km/h
        val battery: Int? = null,           // %
        val voltage: Float? = null,         // V
        val current: Float? = null,         // A
        val totalDistance: Float? = null,   // km
        val tripDistance: Float? = null,    // km
        val temperature: Float? = null,     // °C

        // États
        val isLocked: Boolean? = null,
        val lightOn: Boolean? = null,
        val mode: String? = null,
        val errorCode: Int? = null,

        // Métadonnées
        val timestamp: Long = System.currentTimeMillis(),
        val isValid: Boolean = true,
        val parseErrors: List<String> = emptyList()
    )

    // ========== DÉCODAGE PRINCIPAL ==========
    fun decode(data: ByteArray): DecodedData {
        val hexString = data.joinToString(" ") { "%02X".format(it) }

        // Identifier le type de réponse
        val responseType = identifyResponseType(data)

        return when (responseType) {
            ResponseType.STATUS_BASIC -> decodeBasicStatus(data, hexString)
            ResponseType.STATUS_EXTENDED -> decodeExtendedStatus(data, hexString)
            ResponseType.TELEMETRY -> decodeTelemetry(data, hexString)
            ResponseType.ACKNOWLEDGE -> decodeAcknowledge(data, hexString)
            ResponseType.KEEP_ALIVE -> decodeKeepAlive(data, hexString)
            ResponseType.ERROR -> decodeError(data, hexString)
            ResponseType.UNKNOWN -> decodeUnknown(data, hexString)
        }
    }

    // ========== IDENTIFICATION DU TYPE ==========
    private fun identifyResponseType(data: ByteArray): ResponseType {
        if (data.isEmpty()) return ResponseType.UNKNOWN

        return when {
            // Pattern pour status basique (8 octets)
            data.size == 8 && data[0] in 0x00..0x7F -> ResponseType.STATUS_BASIC

            // Pattern pour status étendu (16+ octets)
            data.size >= 16 && data[0] == 0x5A.toByte() -> ResponseType.STATUS_EXTENDED

            // Pattern pour télémétrie complète (60 octets)
            data.size == 60 && data[0] == 0x55.toByte() && data[1] == 0xAA.toByte() -> ResponseType.TELEMETRY

            // Keep-alive (2 octets)
            data.size == 2 && data.contentEquals(byteArrayOf(0x00, 0x01)) -> ResponseType.KEEP_ALIVE

            // Acknowledge simple
            data.size <= 4 && data[0] == 0xAC.toByte() -> ResponseType.ACKNOWLEDGE

            // Erreur
            data.size >= 4 && data[0] == 0xEE.toByte() -> ResponseType.ERROR

            else -> ResponseType.UNKNOWN
        }
    }

    // ========== DÉCODEURS SPÉCIFIQUES ==========

    private fun decodeBasicStatus(data: ByteArray, hexString: String): DecodedData {
        try {
            // Format typique: [speed_H, speed_L, battery, voltage, flags, reserved...]
            val speed = if (data.size >= 2) {
                ((data[0].toInt() and 0xFF) shl 8 or (data[1].toInt() and 0xFF)) / 100.0f
            } else null

            val battery = if (data.size >= 3) {
                data[2].toInt() and 0xFF
            } else null

            val voltage = if (data.size >= 4) {
                (data[3].toInt() and 0xFF) / 2.0f
            } else null

            return DecodedData(
                type = ResponseType.STATUS_BASIC,
                rawData = data,
                hexString = hexString,
                speed = speed,
                battery = battery?.coerceIn(0, 100),
                voltage = voltage
            )
        } catch (e: Exception) {
            return DecodedData(
                type = ResponseType.STATUS_BASIC,
                rawData = data,
                hexString = hexString,
                isValid = false,
                parseErrors = listOf("Erreur de décodage: ${e.message}")
            )
        }
    }

    private fun decodeExtendedStatus(data: ByteArray, hexString: String): DecodedData {
        try {
            // Skip header (0x5A)
            var offset = 1

            val speed = if (data.size > offset + 1) {
                ((data[offset].toInt() and 0xFF) shl 8 or (data[offset + 1].toInt() and 0xFF)) / 100.0f
            } else null
            offset += 2

            val battery = if (data.size > offset) {
                data[offset].toInt() and 0xFF
            } else null
            offset += 1

            val voltage = if (data.size > offset + 1) {
                ((data[offset].toInt() and 0xFF) shl 8 or (data[offset + 1].toInt() and 0xFF)) / 100.0f
            } else null
            offset += 2

            val current = if (data.size > offset + 1) {
                ((data[offset].toInt() and 0xFF) shl 8 or (data[offset + 1].toInt() and 0xFF)) / 100.0f
            } else null
            offset += 2

            val temperature = if (data.size > offset) {
                (data[offset].toInt() and 0xFF) - 40.0f // Offset typique pour température
            } else null

            return DecodedData(
                type = ResponseType.STATUS_EXTENDED,
                rawData = data,
                hexString = hexString,
                speed = speed,
                battery = battery?.coerceIn(0, 100),
                voltage = voltage,
                current = current,
                temperature = temperature
            )
        } catch (e: Exception) {
            return DecodedData(
                type = ResponseType.STATUS_EXTENDED,
                rawData = data,
                hexString = hexString,
                isValid = false,
                parseErrors = listOf("Erreur de décodage étendu: ${e.message}")
            )
        }
    }

    private fun decodeTelemetry(data: ByteArray, hexString: String): DecodedData {
        try {
            // Format complet avec header 55 AA
            if (data.size < 60) {
                return DecodedData(
                    type = ResponseType.TELEMETRY,
                    rawData = data,
                    hexString = hexString,
                    isValid = false,
                    parseErrors = listOf("Trame télémétrie incomplète")
                )
            }

            // Offsets basés sur l'analyse des logs
            val speed = ((data[5].toInt() and 0xFF) shl 8 or (data[6].toInt() and 0xFF)) / 100.0f
            val battery = data[7].toInt() and 0xFF
            val voltage = ((data[8].toInt() and 0xFF) shl 8 or (data[9].toInt() and 0xFF)) / 100.0f
            val current = ((data[10].toInt() and 0xFF) shl 8 or (data[11].toInt() and 0xFF)) / 100.0f

            // Odomètre total (offsets 26-29)
            val totalDistance = if (data.size > 29) {
                ((data[26].toInt() and 0xFF) shl 24 or
                        (data[27].toInt() and 0xFF) shl 16 or
                        (data[28].toInt() and 0xFF) shl 8 or
                        (data[29].toInt() and 0xFF)) / 1000.0f
            } else null

            // Température (offset 15)
            val temperature = if (data.size > 15) {
                (data[15].toInt() and 0xFF) - 40.0f
            } else null

            // Mode (offset 20)
            val mode = if (data.size > 20) {
                when (data[20].toInt() and 0x0F) {
                    1 -> "PIÉTON"
                    2 -> "D"
                    3 -> "S"
                    4 -> "S+"
                    else -> "INCONNU"
                }
            } else null

            return DecodedData(
                type = ResponseType.TELEMETRY,
                rawData = data,
                hexString = hexString,
                speed = speed,
                battery = battery?.coerceIn(0, 100),
                voltage = voltage,
                current = current,
                totalDistance = totalDistance,
                temperature = temperature,
                mode = mode,
                lightOn = (data[21].toInt() and 0x01) == 1,
                isLocked = (data[21].toInt() and 0x02) == 2
            )
        } catch (e: Exception) {
            return DecodedData(
                type = ResponseType.TELEMETRY,
                rawData = data,
                hexString = hexString,
                isValid = false,
                parseErrors = listOf("Erreur de décodage télémétrie: ${e.message}")
            )
        }
    }

    private fun decodeAcknowledge(data: ByteArray, hexString: String): DecodedData {
        return DecodedData(
            type = ResponseType.ACKNOWLEDGE,
            rawData = data,
            hexString = hexString
        )
    }

    private fun decodeKeepAlive(data: ByteArray, hexString: String): DecodedData {
        return DecodedData(
            type = ResponseType.KEEP_ALIVE,
            rawData = data,
            hexString = hexString
        )
    }

    private fun decodeError(data: ByteArray, hexString: String): DecodedData {
        val errorCode = if (data.size > 1) {
            data[1].toInt() and 0xFF
        } else null

        return DecodedData(
            type = ResponseType.ERROR,
            rawData = data,
            hexString = hexString,
            errorCode = errorCode,
            isValid = false,
            parseErrors = listOf("Code erreur: ${errorCode ?: "inconnu"}")
        )
    }

    private fun decodeUnknown(data: ByteArray, hexString: String): DecodedData {
        return DecodedData(
            type = ResponseType.UNKNOWN,
            rawData = data,
            hexString = hexString,
            isValid = false,
            parseErrors = listOf("Type de réponse non reconnu")
        )
    }

    // ========== UTILITAIRES ==========

    fun analyzePattern(data: ByteArray): String {
        return when {
            data.all { it == 0x34.toByte() } -> "PADDING (0x34 répété)"
            data.contains(0x61) && data.contains(0x9E.toByte()) -> "COMMANDE M0ROBOT"
            data.size == 2 -> "RÉPONSE COURTE"
            data.size == 8 -> "STATUS BASIQUE"
            data.size == 16 -> "STATUS ÉTENDU"
            data.size == 60 -> "TÉLÉMÉTRIE COMPLÈTE"
            else -> "PATTERN INCONNU (${data.size} octets)"
        }
    }
}