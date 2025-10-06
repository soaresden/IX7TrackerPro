package com.ix7.tracker.protocol

import android.util.Log
import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.ScooterData

/**
 * Constructeur de commandes pour le protocole 61 9E (iX7 Pro)
 *
 * IMPORTANT : Ce protocole est DIFFÉRENT du protocole 55 AA (Xiaomi/Ninebot)
 */
object CommandBuilder {
    private const val TAG = "CommandBuilder"

    // ========== COMMANDES PRINCIPALES ==========

    /**
     * Commande de KEEP-ALIVE / POLLING
     * À envoyer toutes les 1-2 secondes pour maintenir la connexion
     */
    fun buildKeepAliveCommand(): ByteArray {
        return ProtocolConstants.CMD_KEEP_ALIVE
    }

    /**
     * Séquence d'initialisation complète
     * Envoie toutes les commandes nécessaires au démarrage
     */
    fun getInitSequence(): List<ByteArray> {
        return listOf(
            ProtocolConstants.CMD_KEEP_ALIVE,
            ProtocolConstants.CMD_REQUEST_1,
            ProtocolConstants.CMD_REQUEST_2,
            ProtocolConstants.CMD_REQUEST_3,
            ProtocolConstants.CMD_REQUEST_4,
            ProtocolConstants.CMD_REQUEST_5
        )
    }

    // ========== COMMANDES DE MODES ==========

    /**
     * Change le mode de conduite
     *
     * @param mode Le mode souhaité
     * @param currentData Les données actuelles (pour préserver les autres états)
     */
    fun buildChangeModeCommand(
        mode: RideMode,
        currentData: ScooterData = ScooterData()
    ): ByteArray {
        return when (mode) {
            RideMode.PEDESTRIAN -> ProtocolConstants.CMD_MODE_PEDESTRIAN_1
            RideMode.ECO -> ProtocolConstants.CMD_MODE_ECO_1
            RideMode.SPORT -> ProtocolConstants.CMD_MODE_SPORT_1
            RideMode.RACE -> ProtocolConstants.CMD_MODE_RACE_1
        }
    }

    /**
     * Construit une commande de mode personnalisée
     * Format : 61 9E 30 14 37 [MODE] [STATE] [DATA] [CHECKSUM]
     */
    fun buildCustomModeCommand(
        modeId: Byte,
        state: Byte = ProtocolConstants.STATE_35
    ): ByteArray {
        val command = byteArrayOf(
            ProtocolConstants.HEADER_1,
            ProtocolConstants.HEADER_2,
            ProtocolConstants.CMD_MODE,
            0x14,
            0x37,
            modeId,
            state,
            0x34,
            0x00, // Placeholder
            0x00  // Checksum (calculé après)
        )

        // Calculer et mettre à jour le checksum
        command[command.size - 1] = ProtocolConstants.calculateChecksum(command)

        val hex = command.joinToString(" ") { "%02X".format(it) }
        Log.d(TAG, "🔧 Commande MODE construite: $hex")

        return command
    }

    // ========== COMMANDES LOCK/UNLOCK ==========

    /**
     * Active/désactive le verrouillage
     */
    fun buildToggleLockCommand(locked: Boolean): ByteArray {
        return if (locked) {
            ProtocolConstants.CMD_LOCK_UNLOCK_1
        } else {
            ProtocolConstants.CMD_LOCK_UNLOCK_2
        }
    }

    // ========== COMMANDES NÉON ET LUMIÈRES (IDENTIFIÉES !) ==========

    /**
     * Active/désactive le NÉON
     *
     * Commandes identifiées dans les logs (envoyées 8 fois = ON/OFF testés)
     */
    fun buildToggleNeonCommand(
        neonOn: Boolean,
        currentData: ScooterData = ScooterData()
    ): ByteArray {
        Log.i(TAG, "🎨 Construction commande NÉON: ${if (neonOn) "ON" else "OFF"}")

        // Commande identifiée : 61 9E 37 14 55 6A 06 DF CA
        // Cette commande a été envoyée 8 fois pendant les tests
        return ProtocolConstants.CMD_TOGGLE_NEON
    }

    /**
     * Active/désactive les LUMIÈRES
     *
     * Commandes identifiées dans les logs (envoyées 8 fois = ON/OFF testés)
     */
    fun buildToggleLightsCommand(
        lightsOn: Boolean,
        currentData: ScooterData = ScooterData()
    ): ByteArray {
        Log.i(TAG, "💡 Construction commande LUMIÈRES: ${if (lightsOn) "ON" else "OFF"}")

        // Commande identifiée : 61 9E 37 14 55 72 06 37 CB
        // Cette commande a été envoyée 8 fois pendant les tests
        return ProtocolConstants.CMD_TOGGLE_LIGHTS
    }

    /**
     * Commande alternative pour néon/lumières
     * Peut être utilisée selon le contexte
     */
    fun buildToggleLightsModeCommand(enabled: Boolean): ByteArray {
        // Autre commande identifiée : 61 9E 37 14 55 8F 32 8E CA
        // Envoyée 8 fois aussi - pourrait être un mode spécial
        return ProtocolConstants.CMD_TOGGLE_SPECIAL
    }

    /**
     * Active/désactive le débridage
     */
    fun buildToggleUnlockCommand(
        unlocked: Boolean,
        currentData: ScooterData = ScooterData()
    ): ByteArray {
        // Les commandes C6 semblent liées au lock/unlock/débridage
        return buildToggleLockCommand(!unlocked)
    }

    /**
     * Active/désactive le régulateur de vitesse (cruise control)
     *
     * Identifié dans les logs : commandes avec bytes 48
     */
    fun buildToggleCruiseControlCommand(enabled: Boolean): ByteArray {
        // Commandes identifiées pour le régulateur :
        // ON  : 61 9E 30 14 37 48 35 34 6F CB
        // OFF : 61 9E 30 14 37 48 34 34 68 CB
        return if (enabled) {
            ProtocolConstants.CMD_CRUISE_CONTROL_ON
        } else {
            ProtocolConstants.CMD_CRUISE_CONTROL_OFF
        }
    }

    // ========== HELPERS ==========

    /**
     * Construit une commande personnalisée complète
     *
     * @param cmdType Type de commande (byte 3)
     * @param flags Flags (byte 4)
     * @param data Données supplémentaires
     */
    fun buildCustomCommand(
        cmdType: Byte,
        flags: Byte,
        data: ByteArray
    ): ByteArray {
        val command = ByteArray(3 + data.size + 1) // Header + cmd + flags + data + checksum
        command[0] = ProtocolConstants.HEADER_1
        command[1] = ProtocolConstants.HEADER_2
        command[2] = cmdType
        command[3] = flags

        // Copier les données
        data.copyInto(command, destinationOffset = 4)

        // Calculer le checksum
        command[command.size - 1] = ProtocolConstants.calculateChecksum(command)

        return command
    }

    /**
     * Convertit RideMode en byte d'identifiant de mode
     */
    private fun getModeIdByte(mode: RideMode): Byte {
        return when (mode) {
            RideMode.PEDESTRIAN -> ProtocolConstants.MODE_PEDESTRIAN
            RideMode.ECO -> ProtocolConstants.MODE_ECO
            RideMode.SPORT -> ProtocolConstants.MODE_SPORT
            RideMode.RACE -> ProtocolConstants.MODE_RACE
        }
    }

    /**
     * Log une commande en format lisible
     */
    fun logCommand(command: ByteArray, label: String) {
        val hex = command.joinToString(" ") { "%02X".format(it) }
        Log.d(TAG, "📤 $label: $hex")
    }
}