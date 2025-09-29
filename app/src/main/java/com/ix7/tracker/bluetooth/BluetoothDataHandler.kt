package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.ScooterData
import java.util.*

class BluetoothDataHandler(
    private val onDataUpdate: (ScooterData) -> Unit
) {
    companion object {
        private const val TAG = "BluetoothDataHandler"
    }

    private var currentData = ScooterData()

    fun handleData(data: ByteArray) {
        try {
            Log.d(TAG, "Données reçues: ${bytesToHex(data)} (${data.size} bytes)")

            if (data.isEmpty()) {
                Log.d(TAG, "Données vides reçues")
                return
            }

            val parsedData = parseData(data)

            // Mettre à jour SEULEMENT si de vraies données sont parsées
            var hasUpdates = false
            var updatedData = currentData

            if (parsedData.speed > 0 && parsedData.speed != currentData.speed) {
                updatedData = updatedData.copy(speed = parsedData.speed)
                hasUpdates = true
            }

            if (parsedData.battery > 0 && parsedData.battery != currentData.battery) {
                updatedData = updatedData.copy(battery = parsedData.battery)
                hasUpdates = true
            }

            if (parsedData.voltage > 0 && parsedData.voltage != currentData.voltage) {
                updatedData = updatedData.copy(voltage = parsedData.voltage)
                hasUpdates = true
            }

            if (parsedData.current != 0f && parsedData.current != currentData.current) {
                updatedData = updatedData.copy(current = parsedData.current)
                hasUpdates = true
            }

            if (parsedData.power > 0 && parsedData.power != currentData.power) {
                updatedData = updatedData.copy(power = parsedData.power)
                hasUpdates = true
            }

            if (parsedData.temperature > 0 && parsedData.temperature != currentData.temperature) {
                updatedData = updatedData.copy(temperature = parsedData.temperature)
                hasUpdates = true
            }

            // AJOUTÉ : Mise à jour de l'odomètre
            if (parsedData.odometer > 0 && parsedData.odometer != currentData.odometer) {
                updatedData = updatedData.copy(odometer = parsedData.odometer)
                hasUpdates = true
                Log.d(TAG, "Odomètre mis à jour: ${parsedData.odometer}km")
            }

            // AJOUTÉ : Mise à jour de la distance du trajet
            if (parsedData.tripDistance > 0 && parsedData.tripDistance != currentData.tripDistance) {
                updatedData = updatedData.copy(tripDistance = parsedData.tripDistance)
                hasUpdates = true
            }

            if (hasUpdates) {
                currentData = updatedData.copy(
                    lastUpdate = Date(),
                    isConnected = true
                )
                Log.d(TAG, "Données mises à jour: Speed=${currentData.speed}, Battery=${currentData.battery}, Voltage=${currentData.voltage}, Odometer=${currentData.odometer}")
                onDataUpdate(currentData)
            } else {
                Log.d(TAG, "Aucune donnée valide trouvée dans la trame")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erreur parsing data", e)
        }
    }

    private fun parseData(data: ByteArray): ScooterData {
        var speed = 0f
        var battery = 0f
        var voltage = 0f
        var current = 0f
        var power = 0f
        var temperature = 0f
        var odometer = 0f
        var tripDistance = 0f

        // Analyse des patterns connus dans les logs
        when (data.size) {
            2 -> {
                // Pattern "00 01" ou similaire - possiblement vitesse ou état
                val value = getShort(data, 0)
                if (value in 1..100) {
                    speed = value / 100f
                }
            }

            4 -> {
                // Pattern "01 00 FF FF" - données complexes
                val value1 = data[0].toUByte().toInt()
                val value2 = data[1].toUByte().toInt()

                if (value1 in 1..100) battery = value1.toFloat()
                if (value2 in 1..100) speed = value2.toFloat()
            }

            8 -> {
                // Pattern "08 00 0A 00 00 00 90 01" - frame principale
                val speedRaw = getShort(data, 0)
                val batteryRaw = data[2].toUByte().toInt()
                val voltageRaw = getShort(data, 6)

                if (speedRaw in 1..8000) speed = speedRaw / 100f
                if (batteryRaw in 1..100) battery = batteryRaw.toFloat()
                if (voltageRaw in 2000..7000) voltage = voltageRaw / 100f
            }

            16 -> {
                // Pattern "5A 00 00 34..." - frame étendue M0Robot
                val header = data[0].toUByte().toInt()
                if (header == 0x5A) {
                    val speedRaw = getShort(data, 1)
                    val batteryRaw = data[3].toUByte().toInt()
                    val voltageRaw = getShort(data, 4)
                    val tempRaw = data[6].toUByte().toInt()

                    // AJOUTÉ : Tentative de lecture de l'odomètre
                    val odometerRaw = getLong(data, 8)  // Position estimée

                    if (speedRaw in 1..8000) speed = speedRaw / 100f
                    if (batteryRaw in 1..100) battery = batteryRaw.toFloat()
                    if (voltageRaw in 2000..7000) voltage = voltageRaw / 100f
                    if (tempRaw in 10..80) temperature = tempRaw.toFloat()
                    if (odometerRaw in 0..1000000) odometer = odometerRaw / 100f
                }
            }

            else -> {
                // AMÉLIORATION : Pour toute autre taille, scanner les positions possibles
                // Recherche de la batterie (byte entre 0 et 100)
                for (i in 0 until minOf(data.size, 10)) {
                    val value = data[i].toUByte().toInt()
                    if (value in 1..100 && battery == 0f) {
                        battery = value.toFloat()
                        Log.d(TAG, "Batterie détectée à l'offset $i: $value%")
                        break
                    }
                }

                // Recherche de l'odomètre (valeur 32 bits importante)
                for (i in 0 until minOf(data.size - 3, 12)) {
                    val value = getLong(data, i)
                    if (value in 100..1000000 && odometer == 0f) {
                        odometer = value / 100f
                        Log.d(TAG, "Odomètre détecté à l'offset $i: ${odometer}km")
                        break
                    }
                }

                // Recherche du voltage (valeur 16 bits entre 30V et 60V)
                for (i in 0 until minOf(data.size - 1, 12)) {
                    val value = getShort(data, i)
                    if (value in 3000..6000 && voltage == 0f) {
                        voltage = value / 100f
                        Log.d(TAG, "Voltage détecté à l'offset $i: ${voltage}V")
                        break
                    }
                }
            }
        }

        // Calculer la puissance si voltage et courant disponibles
        if (voltage > 0 && current != 0f) {
            power = voltage * current
        }

        Log.d(TAG, "Données parsées: speed=$speed, battery=$battery, voltage=$voltage, current=$current, power=$power, temp=$temperature, odometer=$odometer")

        return ScooterData(
            speed = speed,
            battery = battery,
            voltage = voltage,
            current = current,
            power = power,
            temperature = temperature,
            odometer = odometer,
            tripDistance = tripDistance
        )
    }

    private fun getShort(data: ByteArray, offset: Int): Int {
        return if (offset + 1 < data.size) {
            ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
        } else 0
    }

    /**
     * AJOUTÉ : Lecture d'un entier 32 bits (pour odomètre)
     */
    private fun getLong(data: ByteArray, offset: Int): Int {
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
     * Diagnostics pour analyser les patterns de données reçues
     */
    fun analyzeDataPattern(data: ByteArray): String {
        val hex = bytesToHex(data)
        return when {
            data.isEmpty() -> "Données vides"
            data.size == 2 && data.contentEquals(byteArrayOf(0x00, 0x01)) -> "Keep-alive ou état basique"
            data.size == 4 && data[2] == 0xFF.toByte() && data[3] == 0xFF.toByte() -> "Frame de diagnostic"
            data.size == 8 && data[0] == 0x08.toByte() -> "Frame de données principales M0Robot"
            data.size == 16 && data[0] == 0x5A.toByte() -> "Frame étendue M0Robot"
            data.all { it == 0.toByte() } -> "Données nulles"
            else -> "Pattern inconnu: $hex"
        }
    }
}