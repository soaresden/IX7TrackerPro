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
                return parseGenericFrame(data, currentData) // Tenter parsing générique pour petites frames
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
     * Parse les données principales (vitesse, batterie, voltage, odometer, etc.)
     */
    private fun parseMainDataFrame(data: ByteArray, current: ScooterData): ScooterData {
        return try {
            val speed = parseSpeed(data, ProtocolConstants.OFFSET_SPEED)
            val battery = parseBattery(data, ProtocolConstants.OFFSET_BATTERY)
            val voltage = parseVoltage(data, ProtocolConstants.OFFSET_VOLTAGE)
            val currentVal = parseCurrent(data, ProtocolConstants.OFFSET_CURRENT)
            val power = calculatePower(voltage, currentVal)
            val odometer = parseOdometer(data, ProtocolConstants.OFFSET_ODOMETER)  // AJOUTÉ
            val tripDistance = parseOdometer(data, ProtocolConstants.OFFSET_TRIP)  // AJOUTÉ

            Log.d(TAG, "Main data - Speed: ${speed}km/h, Battery: ${battery}%, Voltage: ${voltage}V, Odometer: ${odometer}km")

            current.copy(
                speed = speed,
                battery = battery,
                voltage = voltage,
                current = currentVal,
                power = power,
                odometer = odometer,        // AJOUTÉ
                tripDistance = tripDistance, // AJOUTÉ
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
     * AMÉLIORATION : Recherche plus agressive de l'odomètre et de la batterie
     */
    private fun parseGenericFrame(data: ByteArray, current: ScooterData): ScooterData {
        return try {
            var updated = current

            // Chercher des valeurs plausibles à différentes positions
            for (i in 0 until minOf(data.size - 3, 15)) {
                val value16 = parseUInt16(data, i)
                val value32 = if (i + 3 < data.size) parseUInt32(data, i) else 0
                val value8 = parseUInt8(data, i)

                // Heuristiques pour identifier les données
                when {
                    // Vitesse probable (0-50 km/h)
                    value16 in 0..5000 && i in 2..6 -> {
                        val speed = value16 / 100f
                        if (speed != current.speed && speed > 0) {
                            updated = updated.copy(speed = speed)
                            Log.d(TAG, "Generic: Speed detected at offset $i: ${speed}km/h")
                        }
                    }

                    // Batterie probable (0-100%)
                    value8 in 0..100 && i in 2..10 -> {
                        if (value8.toFloat() != current.battery && value8 > 0) {
                            updated = updated.copy(battery = value8.toFloat())
                            Log.d(TAG, "Generic: Battery detected at offset $i: $value8%")
                        }
                    }

                    // Voltage probable (30-60V)
                    value16 in 3000..6000 && i in 6..12 -> {
                        val voltage = value16 / 100f
                        if (voltage != current.voltage && voltage > 0) {
                            updated = updated.copy(voltage = voltage)
                            Log.d(TAG, "Generic: Voltage detected at offset $i: ${voltage}V")
                        }
                    }

                    // AJOUTÉ : Odomètre probable (0-10000 km en décamètres)
                    value32 in 0..1000000 && i in 8..14 -> {
                        val odometer = value32 / 100f  // Convertir décamètres en km
                        if (odometer != current.odometer && odometer > 0) {
                            updated = updated.copy(odometer = odometer)
                            Log.d(TAG, "Generic: Odometer detected at offset $i: ${odometer}km")
                        }
                    }
                }
            }

            if (updated != current) {
                updated.copy(lastUpdate = Date(), isConnected = true)
            } else {
                current
            }
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
            val raw = parseUInt8(data, offset)
            raw.toFloat().coerceIn(0f, 100f) // Limiter entre 0 et 100%
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

    /**
     * AJOUTÉ : Parse l'odomètre (peut être en décamètres ou mètres selon protocole)
     */
    private fun parseOdometer(data: ByteArray, offset: Int): Float {
        return if (offset + 3 < data.size) {
            val raw = parseUInt32(data, offset)
            raw / 100f // Convertir en km (dépend du protocole, peut nécessiter ajustement)
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

    /**
     * AJOUTÉ : Parse un entier 32 bits non signé (pour odomètre)
     */
    private fun parseUInt32(data: ByteArray, offset: Int): Int {
        return if (offset + 3 < data.size) {
            (data[offset].toInt() and 0xFF) or
                    ((data[offset + 1].toInt() and 0xFF) shl 8) or
                    ((data[offset + 2].toInt() and 0xFF) shl 16) or
                    ((data[offset + 3].toInt() and 0xFF) shl 24)
        } else 0
    }

    private fun calculatePower(voltage: Float, current: Float): Float {
        return kotlin.math.abs(voltage * current)
    }
}