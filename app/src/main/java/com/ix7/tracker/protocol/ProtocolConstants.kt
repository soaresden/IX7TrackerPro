package com.ix7.tracker.protocol

/**
 * Constantes du protocole Bluetooth iX7 Pro
 * Protocole identifié : 61 9E (pas 55 AA !)
 *
 * Basé sur l'analyse de 840 commandes et 1183 réponses capturées
 */
object ProtocolConstants {

    // ========== HEADERS ==========
    const val HEADER_1: Byte = 0x61
    const val HEADER_2: Byte = 0x9E.toByte()

    // ========== TYPES DE COMMANDES (envoyées) ==========
    const val CMD_POLLING: Byte = 0x37        // Keep-alive
    const val CMD_MODE: Byte = 0x30           // Changement de mode

    // ========== TYPES DE TRAMES (reçues) ==========
    const val FRAME_INIT: Byte = 0x02         // Initialisation (~60 bytes)
    const val FRAME_EXTENDED: Byte = 0x04     // Données étendues avec kilométrage (~40 bytes)
    const val FRAME_DETAILED: Byte = 0x1A     // Données détaillées (~40 bytes)
    const val FRAME_REALTIME: Byte = 0x3E     // Temps réel - vitesse, batterie (16 bytes)
    const val FRAME_STATUS: Byte = 0x30       // État/mode actuel (10 bytes)
    const val FRAME_TEMP: Byte = 0x32         // Températures (12 bytes)
    const val FRAME_COMBINED: Byte = 0x16     // Combiné
    const val FRAME_SPECIAL_1: Byte = 0x38    // Type spécial
    const val FRAME_SPECIAL_2: Byte = 0x3A    // Type spécial
    const val FRAME_SPECIAL_3: Byte = 0x3C    // Type spécial
    const val FRAME_INFO: Byte = 0x00         // Informations diverses
    const val FRAME_INFO_EXT: Byte = 0x26     // Informations étendues

    // ========== COMMANDES COMPLÈTES ==========

    /**
     * Commande de KEEP-ALIVE / POLLING (211 fois dans les logs)
     * À envoyer régulièrement (toutes les 1-2 secondes)
     */
    val CMD_KEEP_ALIVE = byteArrayOf(
        0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0x2E.toByte(), 0x00, 0x19, 0xCB.toByte()
    )

    /**
     * Autres commandes fréquentes identifiées (101 fois chacune)
     */
    val CMD_REQUEST_1 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xDE.toByte(), 0x14, 0x45, 0xCA.toByte()
    )

    val CMD_REQUEST_2 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x37, 0x15, 0x35, 0x20, 0x1A, 0xAC.toByte(), 0xCB.toByte()
    )

    val CMD_REQUEST_3 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x37, 0x1D, 0x35, 0x34, 0x2C, 0x8E.toByte(), 0xCB.toByte()
    )

    val CMD_REQUEST_4 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xF6.toByte(), 0x18, 0xB9.toByte(), 0xCA.toByte()
    )

    val CMD_REQUEST_5 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0x58, 0x00, 0xEF.toByte(), 0xCA.toByte()
    )

    // ========== COMMANDES NÉON ET LUMIÈRES (IDENTIFIÉES !) ==========

    /**
     * Commande NÉON (envoyée 8 fois = testée ON/OFF)
     * Toggle le néon de la trottinette
     */
    val CMD_TOGGLE_NEON = byteArrayOf(
        0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0x6A, 0x06, 0xDF.toByte(), 0xCA.toByte()
    )

    /**
     * Commande LUMIÈRES (envoyée 8 fois = testée ON/OFF)
     * Toggle les lumières avant/arrière
     */
    val CMD_TOGGLE_LIGHTS = byteArrayOf(
        0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0x72, 0x06, 0x37, 0xCB.toByte()
    )

    /**
     * Commande SPÉCIALE (envoyée 8 fois)
     * Pourrait être un mode spécial d'éclairage ou autre fonction
     */
    val CMD_TOGGLE_SPECIAL = byteArrayOf(
        0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0x8F.toByte(), 0x32, 0x8E.toByte(), 0xCA.toByte()
    )

    /**
     * Autre commande identifiée (8 fois)
     */
    val CMD_SPECIAL_REQUEST = byteArrayOf(
        0x61, 0x9E.toByte(), 0x37, 0x15, 0x35, 0x20, 0x24, 0x82.toByte(), 0xCB.toByte()
    )

    // ========== MODES DE CONDUITE ==========

    // Identifiants de modes (byte 5 dans les commandes 0x30)
    const val MODE_PEDESTRIAN: Byte = 0x49
    const val MODE_ECO: Byte = 0x4A
    const val MODE_SPORT: Byte = 0x48
    const val MODE_RACE: Byte = 0x4B

    // États ON/OFF (byte 6 dans les commandes 0x30)
    const val STATE_34: Byte = 0x34
    const val STATE_35: Byte = 0x35
    const val STATE_36: Byte = 0x36
    const val STATE_37: Byte = 0x37

    // ========== COMMANDES DE MODES IDENTIFIÉES ==========

    /**
     * Mode PIÉTON
     */
    val CMD_MODE_PEDESTRIAN_1 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x49, 0x35, 0x34, 0x6E, 0xCB.toByte()
    )

    val CMD_MODE_PEDESTRIAN_2 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x49, 0x34, 0x34, 0x6F, 0xCB.toByte()
    )

    /**
     * Mode ECO
     */
    val CMD_MODE_ECO_1 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x36, 0x34, 0x6C, 0xCB.toByte()
    )

    val CMD_MODE_ECO_2 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x35, 0x34, 0x6D, 0xCB.toByte()
    )

    /**
     * Mode SPORT
     */
    val CMD_MODE_SPORT_1 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x35, 0x34, 0x6F, 0xCB.toByte()
    )

    val CMD_MODE_SPORT_2 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x34, 0x34, 0x68, 0xCB.toByte()
    )

    /**
     * Mode RACE
     */
    val CMD_MODE_RACE_1 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4B, 0x35, 0x34, 0x6C, 0xCB.toByte()
    )

    val CMD_MODE_RACE_2 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4B, 0x34, 0x34, 0x6D, 0xCB.toByte()
    )

    // ========== RÉGULATEUR DE VITESSE (CRUISE CONTROL) ==========

    /**
     * Régulateur ON (identifié : byte 48 avec state 35)
     */
    val CMD_CRUISE_CONTROL_ON = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x35, 0x34, 0x6F, 0xCB.toByte()
    )

    /**
     * Régulateur OFF (identifié : byte 48 avec state 34)
     */
    val CMD_CRUISE_CONTROL_OFF = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x34, 0x34, 0x68, 0xCB.toByte()
    )

    // ========== COMMANDES SPÉCIALES ==========

    /**
     * Lock/Unlock/Débridage (commandes avec C6/C7)
     * Envoyées 1 fois = test unique
     */
    val CMD_LOCK_UNLOCK_1 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC6.toByte(), 0x35, 0x34, 0xD1.toByte(), 0xCA.toByte()
    )

    val CMD_LOCK_UNLOCK_2 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC6.toByte(), 0x34, 0x34, 0xD2.toByte(), 0xCA.toByte()
    )

    val CMD_SPECIAL_C7 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0xBC.toByte(), 0x27, 0x7E, 0xCA.toByte()
    )

    // ========== HELPERS ==========

    /**
     * Vérifie si une trame commence par le bon header
     */
    fun isValidFrame(data: ByteArray): Boolean {
        return data.size >= 2 && data[0] == HEADER_1 && data[1] == HEADER_2
    }

    /**
     * Récupère le type de trame (3ème byte)
     */
    fun getFrameType(data: ByteArray): Byte? {
        return if (data.size >= 3 && isValidFrame(data)) data[2] else null
    }

    /**
     * Calcule un checksum simple (XOR de tous les bytes sauf le dernier)
     */
    fun calculateChecksum(data: ByteArray): Byte {
        var checksum: Byte = 0
        for (i in 0 until data.size - 1) {
            checksum = (checksum.toInt() xor data[i].toInt()).toByte()
        }
        return checksum
    }

    /**
     * Vérifie si le checksum est correct
     */
    fun verifyChecksum(data: ByteArray): Boolean {
        if (data.isEmpty()) return false
        val expected = data.last()
        val calculated = calculateChecksum(data)
        return expected == calculated
    }
}