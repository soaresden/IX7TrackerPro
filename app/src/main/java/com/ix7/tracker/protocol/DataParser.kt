package com.ix7.tracker.protocol

import com.ix7.tracker.core.ScooterData
import com.ix7.tracker.core.ProtocolConstants
import android.util.Log
import java.util.*

/**
 * Parser pour décoder les trames des scooters M0Robot
 */
object DataParser {

    private const val TAG = "DataParser"

    /**
     * Parse une trame reçue du scooter
     */
    fun parseScooterFrame(data: ByteArray, currentData: ScooterData): ScooterData {
        try {
            if (data.isEmpty()) return currentData

            val hexString = data.joinToString(" ") { "%02X".format(it) }
            Log.d(TAG, "Parsing frame [${data.size} bytes]: $hexString")

            // Vérifier la taille minimale
            if (data.size < ProtocolConstants.MIN_FRAME_SIZE) {
                Log.w(TAG, "Frame too short: ${data.size} bytes")
                return currentData
            }

            return when (data[0]) {
                ProtocolConstants.FRAME_HEADER_MAIN -> parseMainDataFrame(data, currentData)
                ProtocolConstants.FRAME_HEADER_EXTENDED -> parseExtendedDataFrame(data, currentData)
                ProtocolConstants.FRAME_HEADER_RESPONSE -> parseResponseFrame(data, currentData)
                else -> {
                    Log.w(TAG, "Unknown frame header: 0x${"%02X".format(data[0])}")
                    parseGenericFrame(data, currentData) // Tentative de parsing générique
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing frame", e)
            return currentData
        }
    }

    /**
     * Parse les données principales (vitesse, batterie, etc.)
     */
    private fun parseMainDataFrame(data: ByteArray, current: ScooterData): ScooterData {
        return try {
            val speed = parseSpeed(data, ProtocolConstants.OFFSET_SPEED)
            val battery = parseBattery(data, ProtocolConstants.OFFSET_BATTERY)
            val voltage = parseVoltage(data, ProtocolConstants.OFFSET_VOLTAGE)
            val currentVal = parseCurrent(data, ProtocolConstants.OFFSET_CURRENT)
            val power = calculatePower(voltage, currentVal)

            Log.d(TAG, "Main data - Speed: ${speed}km/h, Battery: ${battery}%, Voltage: ${voltage}V")

            current.copy(
                speed = speed,
                battery = battery,
                voltage = voltage,
                current = currentVal,
                power = power,
                lastUpdate = Date(),
                isConnected = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing main frame", e)
            current
        }
    }

    /**
     * Parse les données étendues (températures, codes d'erreur)
     */
    private fun parseExtendedDataFrame(data: ByteArray, current: ScooterData): ScooterData {
        return try {
            val temperature = parseTemperature(data, ProtocolConstants.OFFSET_TEMPERATURE)
            val errorCodes = parseErrorCodes(data, ProtocolConstants.OFFSET_ERROR_CODES)
            val warningCodes = parseWarningCodes(data, ProtocolConstants.OFFSET_WARNING_CODES)

            Log.d(TAG, "Extended data - Temp: ${temperature}°C, Errors: $errorCodes, Warnings: $warningCodes")

            current.copy(
                temperature = temperature,
                errorCodes = errorCodes,
                warningCodes = warningCodes,
                lastUpdate = Date()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing extended frame", e)
            current
        }
    }

    /**
     * Parse les réponses aux commandes
     */
    private fun parseResponseFrame(data: ByteArray, current: ScooterData): ScooterData {
        return try {
            // Parser les versions, états, etc.
            Log.d(TAG, "Response frame received")
            current.copy(lastUpdate = Date())
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response frame", e)
            current
        }
    }

    /**
     * Tentative de parsing générique pour les trames inconnues
     */
    private fun parseGenericFrame(data: ByteArray, current: ScooterData): ScooterData {
        return try {
            // Essayer de détecter des patterns connus dans la trame
            var updated = current

            // Chercher des valeurs plausibles à différentes positions
            for (i in 0 until minOf(data.size - 1, 15)) {
                val value16 = parseUInt16(data, i)
                val value8 = parseUInt8(data, i)

                // Heuristiques pour identifier les données
                when {
                    // Vitesse probable (0-50 km/h)
                    value16 in 0..5000 && i in 2..6 -> {
                        val speed = value16 / 100f
                        if (speed != current.speed) {
                            updated = updated.copy(speed = speed)
                            Log.d(TAG, "Generic: Speed detected at offset $i: ${speed}km/h")
                        }
                    }

                    // Batterie probable (0-100%)
                    value8 in 0..100 && i in 4..8 -> {
                        if (value8.toFloat() != current.battery) {
                            updated = updated.copy(battery = value8.toFloat())
                            Log.d(TAG, "Generic: Battery detected at offset $i: $value8%")
                        }
                    }

                    // Voltage probable (30-60V)
                    value16 in 3000..6000 && i in 6..10 -> {
                        val voltage = value16 / 100f
                        if (voltage != current.voltage) {
                            updated = updated.copy(voltage = voltage)
                            Log.d(TAG, "Generic: Voltage detected at offset $i: ${voltage}V")
                        }
                    }
                }
            }

            updated.copy(lastUpdate = Date(), isConnected = true)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing generic frame", e)
            current
        }
    }

    // Fonctions de parsing individuelles

    private fun parseSpeed(data: ByteArray, offset: Int): Float {
        return if (offset + 1 < data.size) {
            val raw = parseUInt16(data, offset)
            raw / 100f // Vitesse en km/h avec 2 décimales
        } else 0f
    }

    private fun parseBattery(data: ByteArray, offset: Int): Float {
        return if (offset < data.size) {
            parseUInt8(data, offset).toFloat()
        } else 0f
    }

    private fun parseVoltage(data: ByteArray, offset: Int): Float {
        return if (offset + 1 < data.size) {
            val raw = parseUInt16(data, offset)
            raw / 100f // Voltage en volts avec 2 décimales
        } else 0f
    }

    private fun parseCurrent(data: ByteArray, offset: Int): Float {
        return if (offset + 1 < data.size) {
            val raw = parseUInt16(data, offset)
            (raw - 32768) / 100f // Courant signé en ampères
        } else 0f
    }

    private fun parseTemperature(data: ByteArray, offset: Int): Float {
        return if (offset < data.size) {
            parseUInt8(data, offset) - 40f // Température avec offset de 40°C
        } else 0f
    }

    private fun parseErrorCodes(data: ByteArray, offset: Int): Int {
        return if (offset < data.size) {
            parseUInt8(data, offset)
        } else 0
    }

    private fun parseWarningCodes(data: ByteArray, offset: Int): Int {
        return if (offset < data.size) {
            parseUInt8(data, offset)
        } else 0
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

    private fun calculatePower(voltage: Float, current: Float): Float {
        return kotlin.math.abs(voltage * current)
    }
}