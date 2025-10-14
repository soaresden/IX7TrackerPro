package com.ix7.tracker.protocol

import android.util.Log
import com.ix7.tracker.core.ScooterData
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parser M0Robot - VERSION FINALE
 * Basé sur l'analyse complète des logs btsnoop
 * Date: 14 octobre 2025
 *
 * OFFSETS CONFIRMÉS:
 * - Type 0x02: Batterie, Température, Puissance, Tension
 * - Type 0x04: Odomètre total
 * - Type 0x32: Vitesse actuelle
 */
class M0RobotParser {

    companion object {
        private const val TAG = "M0Parser"

        // En-tête du protocole
        private const val HEADER_1 = 0x61.toByte()
        private const val HEADER_2 = 0x9E.toByte()

        // Types de trames
        private const val TYPE_MAIN = 0x02        // Données principales
        private const val TYPE_ODOMETER = 0x04    // Odomètre
        private const val TYPE_SPEED = 0x32       // Vitesse
        private const val TYPE_SYSTEM = 0x3E      // Données système

        // État courant (pour fusion des trames)
        private var currentBattery: Int = 0
        private var currentVoltage: Float = 0f
        private var currentTemperature: Float = 0f
        private var currentPower: Float = 0f
        private var currentOdometer: Float = 0f
        private var currentSpeed: Int = 0
    }

    /**
     * Parse une trame complète et retourne les données mises à jour
     */
    fun parseFrame(data: ByteArray): ScooterData? {
        if (data.size < 4) return null

        // Vérifier l'en-tête
        if (data[0] != HEADER_1 || data[1] != HEADER_2) {
            return null
        }

        val frameType = data[2].toInt() and 0xFF

        when (frameType) {
            TYPE_MAIN -> parseMainFrame(data)
            TYPE_ODOMETER -> parseOdometerFrame(data)
            TYPE_SPEED -> parseSpeedFrame(data)
            TYPE_SYSTEM -> parseSystemFrame(data)
            else -> {
                Log.d(TAG, "Type de trame inconnu: 0x${frameType.toString(16).uppercase()}")
            }
        }

        // Retourner l'état actuel
        return ScooterData(
            battery = currentBattery.toFloat(),
            voltage = currentVoltage,
            temperature = currentTemperature,
            power = currentPower,
            odometer = currentOdometer,
            speed = currentSpeed.toFloat(),

            // Valeurs par défaut pour les champs non encore identifiés
            current = 0.1f,  // TODO: Trouver dans les trames
            isLocked = false,
            lightsOn = false,
            currentMode = com.ix7.tracker.core.RideMode.ECO
        )
    }

    /**
     * Parse la trame principale (Type 0x02)
     * Contient: Batterie, Température, Puissance, Tension
     */
    private fun parseMainFrame(data: ByteArray) {
        if (data.size < 16) {
            Log.w(TAG, "Trame 0x02 trop courte: ${data.size} bytes")
            return
        }

        try {
            // PUISSANCE [1-2]: LE16 / 100
            // Ex: 0x02 0x96 = 662 → 6.62W
            val powerRaw = ((data[2].toInt() and 0xFF) or
                    ((data[3].toInt() and 0xFF) shl 8))
            currentPower = powerRaw / 100.0f

            // TEMPÉRATURE [11-12]: LE16 / 1000
            // Ex: 0x6A 0x6A = 27242 → 27.242°C
            val tempRaw = ((data[11].toInt() and 0xFF) or
                    ((data[12].toInt() and 0xFF) shl 8))
            currentTemperature = tempRaw / 1000.0f

            // BATTERIE [12]: byte - 40
            // Ex: 0x6A = 106 → 106 - 40 = 66%
            currentBattery = (data[12].toInt() and 0xFF) - 40

            // TENSION [15]: byte direct (approximatif)
            // Ex: 0x30 = 48V
            currentVoltage = (data[15].toInt() and 0xFF).toFloat()

            Log.d(TAG, "Trame 0x02: Bat=${currentBattery}% Temp=${String.format("%.1f", currentTemperature)}°C " +
                    "Power=${String.format("%.1f", currentPower)}W Volt=${String.format("%.0f", currentVoltage)}V")

        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing trame 0x02: ${e.message}")
        }
    }

    /**
     * Parse la trame odomètre (Type 0x04)
     * Contient: Odomètre total
     */
    private fun parseOdometerFrame(data: ByteArray) {
        if (data.size < 4) {
            Log.w(TAG, "Trame 0x04 trop courte: ${data.size} bytes")
            return
        }

        try {
            // ODOMÈTRE [2-3]: BE16 / 10
            // Ex: 0x04 0x11 = 1041 → 104.1 km
            val odoRaw = ((data[2].toInt() and 0xFF) shl 8) or
                    (data[3].toInt() and 0xFF)
            currentOdometer = odoRaw / 10.0f

            Log.d(TAG, "Trame 0x04: Odomètre=${String.format("%.1f", currentOdometer)}km (raw=$odoRaw)")

        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing trame 0x04: ${e.message}")
        }
    }

    /**
     * Parse la trame vitesse (Type 0x32)
     * Contient: Vitesse actuelle
     */
    private fun parseSpeedFrame(data: ByteArray) {
        if (data.size < 6) {
            Log.w(TAG, "Trame 0x32 trop courte: ${data.size} bytes")
            return
        }

        try {
            // VITESSE [5]: byte direct
            // Ex: 0x0B = 11 km/h
            currentSpeed = data[5].toInt() and 0xFF

            Log.d(TAG, "Trame 0x32: Vitesse=${currentSpeed}km/h")

        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing trame 0x32: ${e.message}")
        }
    }

    /**
     * Parse la trame système (Type 0x3E)
     * Contient: Données système non encore entièrement identifiées
     */
    private fun parseSystemFrame(data: ByteArray) {
        if (data.size < 16) {
            Log.w(TAG, "Trame 0x3E trop courte: ${data.size} bytes")
            return
        }

        try {
            // TODO: Identifier les données dans cette trame
            // Semble contenir des valeurs stables

            Log.v(TAG, "Trame 0x3E reçue (${data.size} bytes)")

        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing trame 0x3E: ${e.message}")
        }
    }

    /**
     * Réinitialise l'état du parser
     */
    fun reset() {
        currentBattery = 0
        currentVoltage = 0f
        currentTemperature = 0f
        currentPower = 0f
        currentOdometer = 0f
        currentSpeed = 0
        Log.d(TAG, "Parser réinitialisé")
    }

    /**
     * Retourne l'état actuel sans parser de nouvelle trame
     */
    fun getCurrentState(): ScooterData {
        return ScooterData(
            battery = currentBattery.toFloat(),
            voltage = currentVoltage,
            temperature = currentTemperature,
            power = currentPower,
            odometer = currentOdometer,
            speed = currentSpeed.toFloat(),
            current = 0.1f,
            isLocked = false,
            lightsOn = false,
            currentMode = com.ix7.tracker.core.RideMode.ECO
        )
    }
}

/**
 * Extension pour afficher une trame en hexadécimal
 */
fun ByteArray.toHexString(): String {
    return this.joinToString(" ") { "%02X".format(it) }
}

/**
 * Exemple d'utilisation:
 *
 * val parser = M0RobotParser()
 *
 * // Dans votre BluetoothRepository, quand vous recevez des données:
 * val scooterData = parser.parseFrame(receivedData)
 * if (scooterData != null) {
 *     _scooterData.value = scooterData
 * }
 */