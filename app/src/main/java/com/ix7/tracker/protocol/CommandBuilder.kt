package com.ix7.tracker.protocol

import com.ix7.tracker.core.ProtocolUtils

/**
 * Constructeur de commandes pour les scooters/hoverboards
 * REMPLACÉ : Utilise maintenant ProtocolUtils avec le bon protocole 55 AA
 */
object CommandBuilder {

    /**
     * Demande de données temps réel
     */
    fun buildDataRequestCommand(): ByteArray {
        return ProtocolUtils.buildDataRequest()
    }

    /**
     * Demande de statut
     */
    fun buildStatusRequestCommand(): ByteArray {
        return ProtocolUtils.buildStatusRequest()
    }

    /**
     * Demande de version firmware
     */
    fun buildVersionRequestCommand(): ByteArray {
        return ProtocolUtils.buildVersionRequest()
    }

    /**
     * Keep-alive
     */
    fun buildKeepAliveCommand(): ByteArray {
        return ProtocolUtils.buildKeepAlive()
    }

    /**
     * Commande de paramétrage (réservée pour usage avancé)
     */
    fun buildSetParameterCommand(parameter: Int, value: Int): ByteArray {
        // Format: 55 AA [length] 02 [param] [value] [checksum]
        return ProtocolUtils.buildCommand(
            0x02.toByte(),
            byteArrayOf(parameter.toByte(), value.toByte())
        )
    }

    /**
     * Commande unlock (déverrouillage scooter)
     */
    fun buildUnlockCommand(): ByteArray {
        // Format spécifique selon le modèle
        // Pour le moment, on envoie un keep-alive
        return buildKeepAliveCommand()
    }
}