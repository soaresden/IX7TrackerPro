package com.ix7.tracker.protocol

import com.ix7.tracker.core.ProtocolConstants

/**
 * Constructeur de commandes pour les scooters M0Robot
 */
object CommandBuilder {

    /**
     * Construit une commande de demande de données
     */
    fun buildDataRequestCommand(): ByteArray {
        return byteArrayOf(
            ProtocolConstants.CMD_REQUEST_DATA,
            0x00, // Paramètre
            0x00, // Checksum (à calculer)
        ).apply {
            this[2] = calculateChecksum(this, 2).toByte()
        }
    }

    /**
     * Construit une commande de demande de version
     */
    fun buildVersionRequestCommand(): ByteArray {
        return byteArrayOf(
            ProtocolConstants.CMD_GET_VERSION,
            0x00,
            0x00
        ).apply {
            this[2] = calculateChecksum(this, 2).toByte()
        }
    }

    /**
     * Construit une commande de configuration de paramètre
     */
    fun buildSetParameterCommand(parameter: Int, value: Int): ByteArray {
        return byteArrayOf(
            ProtocolConstants.CMD_SET_PARAMETER,
            parameter.toByte(),
            value.toByte(),
            0x00 // Checksum
        ).apply {
            this[3] = calculateChecksum(this, 3).toByte()
        }
    }

    /**
     * Calcule le checksum simple
     */
    private fun calculateChecksum(data: ByteArray, length: Int): Int {
        var sum = 0
        for (i in 0 until length) {
            sum += data[i].toInt() and 0xFF
        }
        return sum and 0xFF
    }
}