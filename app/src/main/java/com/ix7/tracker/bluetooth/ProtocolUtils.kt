package com.ix7.tracker.core

import android.util.Log

/**
 * Utilitaires pour le protocole M0Robot 55 AA
 */
object ProtocolUtils {
    private const val TAG = "ProtocolUtils"

    /**
     * Calcule le checksum d'une trame (XOR simple)
     * Format: XOR de tous les bytes sauf header et checksum
     */
    fun calculateChecksum(frame: ByteArray): Byte {
        if (frame.size < 4) return 0

        var checksum = 0
        // Calculer XOR à partir du byte 2 (length) jusqu'à l'avant-dernier byte
        for (i in 2 until frame.size - 1) {
            checksum = checksum xor (frame[i].toInt() and 0xFF)
        }

        return checksum.toByte()
    }

    /**
     * Vérifie si une trame a un header valide
     */
    fun hasValidHeader(data: ByteArray): Boolean {
        return data.size >= 2 &&
                data[0] == ProtocolConstants.FRAME_HEADER_1 &&
                data[1] == ProtocolConstants.FRAME_HEADER_2
    }

    /**
     * Extrait la longueur d'une trame
     */
    fun getFrameLength(data: ByteArray): Int? {
        if (data.size < 3) return null
        return data[2].toInt() and 0xFF
    }

    /**
     * Parse un entier 16-bit en little-endian
     */
    fun parseUInt16LE(data: ByteArray, offset: Int): Int {
        if (offset + 1 >= data.size) return 0
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    /**
     * Parse un entier 32-bit en little-endian
     */
    fun parseUInt32LE(data: ByteArray, offset: Int): Int {
        if (offset + 3 >= data.size) return 0
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    /**
     * Parse un entier 16-bit signé
     */
    fun parseInt16LE(data: ByteArray, offset: Int): Int {
        val unsigned = parseUInt16LE(data, offset)
        return if (unsigned > 32767) unsigned - 65536 else unsigned
    }

    /**
     * Crée une commande complète avec header et checksum
     */
    fun buildCommand(command: Byte, subCommand: Byte = 0x00, data: ByteArray = byteArrayOf()): ByteArray {
        val length = 2 + data.size // command + subCommand + data
        val frame = mutableListOf<Byte>()

        // Header
        frame.add(ProtocolConstants.FRAME_HEADER_1)
        frame.add(ProtocolConstants.FRAME_HEADER_2)

        // Length
        frame.add(length.toByte())

        // Command
        frame.add(command)

        // SubCommand
        frame.add(subCommand)

        // Data
        frame.addAll(data.toList())

        // Checksum
        val checksumData = frame.drop(2).toByteArray() // Depuis length jusqu'à la fin
        var checksum = 0
        for (byte in checksumData) {
            checksum = checksum xor (byte.toInt() and 0xFF)
        }
        frame.add(checksum.toByte())

        return frame.toByteArray()
    }

    /**
     * Formate un ByteArray en chaîne hexadécimale lisible
     */
    fun toHexString(data: ByteArray): String {
        return data.joinToString(" ") { "%02X".format(it) }
    }

    /**
     * Log détaillé d'une trame
     */
    fun logFrame(tag: String, label: String, data: ByteArray) {
        if (data.isEmpty()) {
            Log.d(tag, "$label: [VIDE]")
            return
        }

        val hex = toHexString(data)
        val info = StringBuilder()

        if (hasValidHeader(data)) {
            info.append("\n  Header: OK (55 AA)")

            if (data.size >= 3) {
                val length = data[2].toInt() and 0xFF
                info.append("\n  Length: $length")
            }

            if (data.size >= 4) {
                val command = data[3]
                info.append("\n  Command: ${"%02X".format(command)}")
            }

            if (data.size >= 5) {
                val subCommand = data[4]
                info.append("\n  SubCommand: ${"%02X".format(subCommand)}")
            }
        } else {
            info.append("\n  ⚠ Header invalide")
        }

        Log.d(tag, "$label (${data.size} bytes): $hex$info")
    }

    /**
     * Valide une trame complète (header + checksum)
     */
    fun validateFrame(data: ByteArray): Boolean {
        if (data.size < 5) return false
        if (!hasValidHeader(data)) return false

        val calculatedChecksum = calculateChecksum(data)
        val receivedChecksum = data[data.size - 1]

        return calculatedChecksum == receivedChecksum
    }

    /**
     * Extrait les données utiles d'une trame (sans header, length, command, checksum)
     */
    fun extractPayload(data: ByteArray): ByteArray {
        if (data.size < 6) return byteArrayOf()
        // Payload commence à l'offset 5 et va jusqu'à avant-dernier byte
        return data.copyOfRange(5, data.size - 1)
    }

    /**
     * Recherche une valeur 32-bit dans une trame (pour trouver l'odomètre)
     */
    fun searchUInt32(data: ByteArray, targetValue: Int, startOffset: Int = 0, endOffset: Int = data.size - 3): List<Int> {
        val positions = mutableListOf<Int>()

        for (i in startOffset..minOf(endOffset, data.size - 4)) {
            val value = parseUInt32LE(data, i)
            if (value == targetValue) {
                positions.add(i)
            }
        }

        return positions
    }

    /**
     * Recherche une valeur dans une plage (utile pour odomètre approximatif)
     */
    fun searchUInt32InRange(
        data: ByteArray,
        minValue: Int,
        maxValue: Int,
        startOffset: Int = 0,
        endOffset: Int = data.size - 3
    ): Map<Int, Int> {
        val results = mutableMapOf<Int, Int>()

        for (i in startOffset..minOf(endOffset, data.size - 4)) {
            val value = parseUInt32LE(data, i)
            if (value in minValue..maxValue) {
                results[i] = value
            }
        }

        return results
    }

    /**
     * Analyse automatique d'une trame pour détecter les données
     * Retourne les positions probables de batterie, odomètre, voltage, etc.
     */
    fun analyzeFrame(data: ByteArray): FrameAnalysis {
        val batteryPositions = mutableListOf<Int>()
        val temperaturePositions = mutableListOf<Int>()
        val voltagePositions = mutableListOf<Int>()
        val odometerPositions = mutableListOf<Int>()

        // Recherche batterie (0-100%)
        for (i in data.indices) {
            val value = data[i].toInt() and 0xFF
            if (value in 0..100) {
                batteryPositions.add(i)
            }
        }

        // Recherche température (0-100°C)
        for (i in data.indices) {
            val value = data[i].toInt() and 0xFF
            if (value in 15..80) {
                temperaturePositions.add(i)
            }
        }

        // Recherche voltage (30-60V en dixièmes = 300-600)
        for (i in 0 until data.size - 1) {
            val value = parseUInt16LE(data, i)
            if (value in 300..600) {
                voltagePositions.add(i)
            }
        }

        // Recherche odomètre (100-100000 km = 10000-10000000 décamètres)
        for (i in 0 until data.size - 3) {
            val value = parseUInt32LE(data, i)
            if (value in 10000..10000000) {
                odometerPositions.add(i)
            }
        }

        return FrameAnalysis(
            batteryPositions = batteryPositions,
            temperaturePositions = temperaturePositions,
            voltagePositions = voltagePositions,
            odometerPositions = odometerPositions
        )
    }

    data class FrameAnalysis(
        val batteryPositions: List<Int>,
        val temperaturePositions: List<Int>,
        val voltagePositions: List<Int>,
        val odometerPositions: List<Int>
    )
}