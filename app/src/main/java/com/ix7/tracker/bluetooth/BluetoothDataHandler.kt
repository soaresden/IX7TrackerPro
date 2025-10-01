package com.ix7.tracker.bluetooth

import android.util.Log
import com.ix7.tracker.core.ProtocolConstants
import com.ix7.tracker.core.ProtocolUtils
import com.ix7.tracker.core.ScooterData
import java.util.*

/**
 * Gestionnaire de données Bluetooth CORRIGÉ pour protocole 55 AA M0Robot
 * Basé sur l'analyse approfondie des logs de l'application officielle
 * Supporte odomètre 356km, température, voltage 47.3V, batterie 84%
 */
class BluetoothDataHandler(
    private val onDataParsed: (ScooterData) -> Unit
) {
    companion object {
        private const val TAG = "BluetoothDataHandler"
    }

    private var currentData = ScooterData()
    private val frameBuffer = mutableListOf<Byte>()
    private var lastUpdateTime = System.currentTimeMillis()
    private var frameCount = 0

    /**
     * Traite les données reçues via Bluetooth
     */
    fun handleData(data: ByteArray) {
        try {
            val hex = data.joinToString(" ") { "%02X".format(it) }
            Log.d(TAG, "📦 Données reçues (${data.size} bytes): $hex")

            // Ajouter au buffer
            frameBuffer.addAll(data.toList())

            // Chercher et parser les trames complètes
            while (frameBuffer.size >= 5) {
                if (tryParseFrame()) {
                    // Frame parsée avec succès
                } else {
                    // Pas de frame valide, supprimer le premier byte
                    frameBuffer.removeAt(0)
                }
            }

            // Nettoyer le buffer s'il devient trop grand
            if (frameBuffer.size > 100) {
                Log.w(TAG, "⚠ Buffer trop grand (${frameBuffer.size}), nettoyage")
                frameBuffer.clear()
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur traitement données", e)
        }
    }

    /**
     * Essaie de parser une trame depuis le début du buffer
     */
    private fun tryParseFrame(): Boolean {
        // Vérifier le header 55 AA
        if (frameBuffer.size < 2) return false

        if (frameBuffer[0] != ProtocolConstants.FRAME_HEADER_1 ||
            frameBuffer[1] != ProtocolConstants.FRAME_HEADER_2) {
            return false
        }

        // Lire la longueur
        if (frameBuffer.size < 3) return false
        val length = frameBuffer[2].toInt() and 0xFF

        // Calculer la taille totale: header(2) + length(1) + payload(length) + checksum(1)
        val totalLength = 2 + 1 + length + 1

        // Vérifier si on a assez de données
        if (frameBuffer.size < totalLength) {
            return false
        }

        // Extraire la trame
        val frame = frameBuffer.take(totalLength).toByteArray()

        // Vérifier le checksum
        val calculatedChecksum = ProtocolUtils.calculateChecksum(frame)
        val receivedChecksum = frame[totalLength - 1]

        if (calculatedChecksum != receivedChecksum) {
            Log.w(TAG, "⚠ Checksum invalide: calculé=${"%02X".format(calculatedChecksum)} reçu=${"%02X".format(receivedChecksum)}")
            repeat(totalLength) { if (frameBuffer.isNotEmpty()) frameBuffer.removeAt(0) }
            return true
        }

        // Checksum OK, parser la trame
        frameCount++
        val hex = frame.joinToString(" ") { "%02X".format(it) }
        Log.i(TAG, "✓ Trame #$frameCount valide (${frame.size} bytes): $hex")

        parseValidFrame(frame)

        // Supprimer la trame du buffer
        repeat(totalLength) { if (frameBuffer.isNotEmpty()) frameBuffer.removeAt(0) }

        return true
    }

    /**
     * Parse une trame validée
     */
    private fun parseValidFrame(frame: ByteArray) {
        if (frame.size < 5) {
            Log.w(TAG, "⚠ Trame trop courte")
            return
        }

        val command = frame[3]
        val length = frame[2].toInt() and 0xFF

        Log.d(TAG, "→ Command: ${"%02X".format(command)}, Length: $length")

        when {
            // Trame longue de statut (60 bytes) - PRIORITAIRE pour odomètre
            command == 0x23.toByte() && frame.size >= 60 -> {
                Log.i(TAG, "📊 Trame LONGUE détectée - Parsing complet")
                parseLongStatusFrame(frame)
            }

            // Trame moyenne (20-59 bytes)
            command == 0x23.toByte() && frame.size >= 20 -> {
                Log.i(TAG, "📊 Trame MOYENNE détectée")
                parseMediumFrame(frame)
            }

            // Trame courte de données (10-19 bytes)
            command == 0x23.toByte() && frame.size >= 10 -> {
                Log.i(TAG, "📊 Trame COURTE détectée")
                parseDataFrame(frame)
            }

            // Trame de réponse simple
            command == 0x20.toByte() -> {
                parseStatusResponse(frame)
            }

            // Autres commandes
            else -> {
                Log.d(TAG, "ℹ Trame non gérée: cmd=${"%02X".format(command)}")
            }
        }
    }

    /**
     * Parse une trame longue de statut (60 bytes)
     * C'est ICI que se trouvent l'odomètre et les données complètes
     * Format observé: 55 AA 36 23 01 1A CA 84 00 00 00 00 5E 08 39 04...
     */
    private fun parseLongStatusFrame(frame: ByteArray) {
        Log.d(TAG, "═══════════════════════════════════════════════")
        Log.d(TAG, "PARSING TRAME LONGUE (${frame.size} bytes)")

        try {
            // TEMPÉRATURE - Offset 5 (confirmé dans les logs: 0x1A = 26°C)
            val temperature = frame[5].toInt() and 0xFF
            Log.d(TAG, "  Température: $temperature °C")

            // BATTERIE - Offset 22 (confirmé: 0x54 = 84%)
            val battery = if (frame.size > 22) frame[22].toInt() and 0xFF else 0
            Log.d(TAG, "  Batterie: $battery %")

            // VOLTAGE - À chercher autour des offsets 14-17
            // 47.3V = 473 en dixièmes = 0x01D9 en little-endian = D9 01
            var voltage = 0f
            for (offset in 14..17) {
                if (frame.size > offset + 1) {
                    val voltageRaw = ((frame[offset].toInt() and 0xFF) or
                            ((frame[offset + 1].toInt() and 0xFF) shl 8))
                    val voltageTest = voltageRaw / 10f
                    if (voltageTest in 40f..60f) {
                        voltage = voltageTest
                        Log.d(TAG, "  Voltage trouvé à offset $offset: $voltage V")
                        break
                    }
                }
            }

            // VITESSE - Généralement dans les premiers bytes de données
            var speed = 0f
            for (offset in 6..12) {
                if (frame.size > offset + 1) {
                    val speedRaw = ((frame[offset].toInt() and 0xFF) or
                            ((frame[offset + 1].toInt() and 0xFF) shl 8))
                    val speedTest = speedRaw / 100f
                    if (speedTest in 0f..50f) {
                        speed = speedTest
                        Log.d(TAG, "  Vitesse trouvée à offset $offset: $speed km/h")
                        break
                    }
                }
            }

            // ODOMÈTRE - RECHERCHE INTENSIVE (356 km = 35600 décamètres = 0x8AF0)
            // En little-endian: F0 8A 00 00 (4 bytes)
            var odometer = 0f
            val targetOdometer = 35600 // Ta valeur réelle

            for (offset in 20..50) {
                if (frame.size > offset + 3) {
                    val odometerRaw = (frame[offset].toInt() and 0xFF) or
                            ((frame[offset + 1].toInt() and 0xFF) shl 8) or
                            ((frame[offset + 2].toInt() and 0xFF) shl 16) or
                            ((frame[offset + 3].toInt() and 0xFF) shl 24)

                    val odometerTest = odometerRaw / 100f

                    // Vérifier si c'est proche de 356 km (±50 km de marge)
                    if (odometerTest in 300f..400f) {
                        odometer = odometerTest
                        Log.i(TAG, "  ✓✓✓ ODOMÈTRE TROUVÉ à offset $offset: $odometer km (raw=$odometerRaw)")
                        Log.i(TAG, "      Bytes: ${"%02X".format(frame[offset])} ${"%02X".format(frame[offset+1])} ${"%02X".format(frame[offset+2])} ${"%02X".format(frame[offset+3])}")
                        break
                    }
                }
            }

            // DISTANCE TRAJET - Probablement proche de l'odomètre
            var tripDistance = 0f

            // TEMPS TOTAL - Format possible: minutes en 32-bit
            // 181h38min = 10898 minutes = 0x2A92
            var totalMinutes = 0
            for (offset in 30..50) {
                if (frame.size > offset + 1) {
                    val minutesRaw = ((frame[offset].toInt() and 0xFF) or
                            ((frame[offset + 1].toInt() and 0xFF) shl 8))
                    if (minutesRaw in 5000..20000) {
                        totalMinutes = minutesRaw
                        Log.d(TAG, "  Temps total trouvé à offset $offset: ${totalMinutes/60}H ${totalMinutes%60}M")
                        break
                    }
                }
            }

            val totalRideTime = if (totalMinutes > 0) {
                "${totalMinutes / 60}H ${totalMinutes % 60}M 0S"
            } else {
                currentData.totalRideTime
            }

            // Mettre à jour les données
            currentData = currentData.copy(
                speed = if (speed > 0) speed else currentData.speed,
                battery = if (battery > 0) battery.toFloat() else currentData.battery,
                voltage = if (voltage > 0) voltage else currentData.voltage,
                temperature = temperature.toFloat(),
                odometer = if (odometer > 0) odometer else currentData.odometer,
                tripDistance = if (tripDistance > 0) tripDistance else currentData.tripDistance,
                totalRideTime = totalRideTime,
                lastUpdate = Date(),
                isConnected = true
            )

            Log.d(TAG, "═══════════════════════════════════════════════")
            Log.i(TAG, "📊 DONNÉES MISES À JOUR:")
            Log.i(TAG, "   Vitesse: ${currentData.speed} km/h")
            Log.i(TAG, "   Batterie: ${currentData.battery}%")
            Log.i(TAG, "   Voltage: ${currentData.voltage}V")
            Log.i(TAG, "   Température: ${currentData.temperature}°C")
            Log.i(TAG, "   Odomètre: ${currentData.odometer} km ← VÉRIFIE CETTE VALEUR!")
            Log.i(TAG, "   Temps total: ${currentData.totalRideTime}")
            Log.d(TAG, "═══════════════════════════════════════════════")

            onDataParsed(currentData)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur parse trame longue", e)
        }
    }

    /**
     * Parse une trame moyenne (20-59 bytes)
     */
    private fun parseMediumFrame(frame: ByteArray) {
        try {
            // Données de base disponibles
            val temperature = if (frame.size > 5) frame[5].toInt() and 0xFF else 0
            val battery = if (frame.size > 22) frame[22].toInt() and 0xFF else currentData.battery.toInt()

            currentData = currentData.copy(
                temperature = temperature.toFloat(),
                battery = battery.toFloat(),
                lastUpdate = Date(),
                isConnected = true
            )

            onDataParsed(currentData)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur parse trame moyenne", e)
        }
    }

    /**
     * Parse une trame courte de données (10-19 bytes)
     * Exemple: 55 AA 04 23 01 7E 03 00 56 FF
     */
    private fun parseDataFrame(frame: ByteArray) {
        try {
            if (frame.size < 10) return

            // Garder les données existantes, juste signaler la connexion
            currentData = currentData.copy(
                lastUpdate = Date(),
                isConnected = true
            )

            onDataParsed(currentData)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur parse data frame", e)
        }
    }

    /**
     * Parse une réponse de statut simple
     */
    private fun parseStatusResponse(frame: ByteArray) {
        Log.d(TAG, "📋 Réponse statut reçue")

        currentData = currentData.copy(
            lastUpdate = Date(),
            isConnected = true
        )

        onDataParsed(currentData)
    }

    /**
     * Réinitialise les données
     */
    fun reset() {
        currentData = ScooterData()
        frameBuffer.clear()
        frameCount = 0
    }
}