package com.ix7.tracker.core

import android.util.Log

/**
 * Utilitaires pour le protocole 55 AA
 * CORRIGÉ pour fonctionner comme l'app officielle
 */
object ProtocolUtils {
    private const val TAG = "ProtocolUtils"

    /**
     * Calcule le checksum XOR pour une trame 55 AA
     * Le checksum est le XOR de tous les bytes sauf le header et le checksum lui-même
     */
    fun calculateChecksum(data: ByteArray): Byte {
        if (data.size < 3) return 0

        var xor: Byte = 0
        // XOR de tous les bytes sauf les 2 premiers (header) et le dernier (checksum)
        for (i in 2 until data.size - 1) {
            xor = (xor.toInt() xor data[i].toInt()).toByte()
        }
        return xor
    }

    /**
     * Vérifie si une trame a le bon header 55 AA
     */
    fun isValidFrame(data: ByteArray): Boolean {
        return data.size >= 5 &&
                data[0] == ProtocolConstants.FRAME_HEADER_1 &&
                data[1] == ProtocolConstants.FRAME_HEADER_2
    }

    /**
     * Construit une commande complète avec header et checksum
     * Format: 55 AA [length] [command] [data...] [checksum]
     */
    fun buildCommand(command: Byte, data: ByteArray = byteArrayOf()): ByteArray {
        val length = (1 + data.size).toByte() // command + data

        // Construire la trame sans checksum
        val frame = byteArrayOf(
            ProtocolConstants.FRAME_HEADER_1,
            ProtocolConstants.FRAME_HEADER_2,
            length,
            command
        ) + data + 0x00.toByte() // Placeholder pour checksum

        // Calculer et insérer le checksum
        val checksum = calculateChecksum(frame)
        frame[frame.size - 1] = checksum

        val hex = frame.joinToString(" ") { "%02X".format(it) }
        Log.d(TAG, "Commande construite: $hex")

        return frame
    }

    /**
     * Commande de demande de statut
     * Format: 55 AA 02 20 22
     */
    fun buildStatusRequest(): ByteArray {
        return buildCommand(ProtocolConstants.CMD_STATUS)
    }

    /**
     * Commande de demande de données
     * Format: 55 AA 03 22 01 00 26
     */
    fun buildDataRequest(): ByteArray {
        return buildCommand(
            ProtocolConstants.CMD_REQUEST_DATA,
            byteArrayOf(0x01, 0x00)
        )
    }

    /**
     * Commande de demande de version
     * Format: 55 AA 02 03 01
     */
    fun buildVersionRequest(): ByteArray {
        return buildCommand(ProtocolConstants.CMD_GET_VERSION)
    }

    /**
     * Commande keep-alive
     * Format: 55 AA 01 00 56
     */
    fun buildKeepAlive(): ByteArray {
        return buildCommand(ProtocolConstants.CMD_KEEP_ALIVE)
    }

    /**
     * Log une trame en format lisible
     */
    fun logFrame(prefix: String, data: ByteArray) {
        val hex = data.joinToString(" ") { "%02X".format(it) }
        Log.d(TAG, "$prefix: $hex (${data.size} bytes)")
    }
}