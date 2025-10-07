package com.ix7.tracker.protocol

/**
 * Constantes du protocole 61 9E pour iX7 Pro
 * Basé sur l'analyse des logs btsnoop et de l'app officielle
 */
object ProtocolConstants {

    // ===== HEADERS DE TRAME =====
    const val HEADER_1: Byte = 0x61
    const val HEADER_2: Byte = 0x9E.toByte()

    // ===== TYPES DE TRAMES (byte 2) =====
    const val FRAME_STATUS: Byte = 0x30          // État/Mode (10 bytes)
    const val FRAME_TEMP: Byte = 0x32            // Températures (12 bytes)
    const val FRAME_SPEED: Byte = 0x37           // ⭐ VITESSE en temps réel (9 bytes)
    const val FRAME_REALTIME: Byte = 0x3E        // Données temps réel (16 bytes)
    const val FRAME_INIT: Byte = 0x02            // Initialisation (67 bytes)
    const val FRAME_EXTENDED: Byte = 0x04        // Données étendues (51 bytes)
    const val FRAME_COMBINED: Byte = 0x16        // Données combinées (47 bytes)
    const val FRAME_DETAILED: Byte = 0x1A        // Données détaillées (49 bytes)
    const val FRAME_INFO_EXT: Byte = 0x26        // Info étendue (24 bytes)
    const val FRAME_SPECIAL_1: Byte = 0x38       // Spécial 1 (19 bytes)
    const val FRAME_SPECIAL_2: Byte = 0x3A       // Spécial 2 (20 bytes)
    const val FRAME_SPECIAL_3: Byte = 0x3C       // Spécial 3 (18 bytes)
    const val FRAME_INFO: Byte = 0x00            // Info générale (40 bytes)

    // ===== COMMANDES TX (vers scooter) =====
    // Format : 61 9E 30 14 37 [CMD] [VAL] 34 [CRC] CB/CA

    // Modes de conduite (CMD = 0x4A)
    val CMD_MODE_PEDESTRIAN = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x37, 0x34, 0x63, 0xCB.toByte())
    val CMD_MODE_ECO = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x36, 0x34, 0x6C, 0xCB.toByte())
    val CMD_MODE_RACE = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x35, 0x34, 0x6D, 0xCB.toByte())
    val CMD_MODE_SPORT = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x34, 0x34, 0x6E, 0xCB.toByte())

    // Lumières (CMD = 0xC6)
    val CMD_LIGHTS_ON = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC6.toByte(), 0x35, 0x34, 0xD1.toByte(), 0xCA.toByte())
    val CMD_LIGHTS_OFF = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC6.toByte(), 0x34, 0x34, 0xD2.toByte(), 0xCA.toByte())

    // Verrou (CMD = 0x4B)
    val CMD_LOCK = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4B, 0x35, 0x34, 0x6C, 0xCB.toByte())
    val CMD_UNLOCK = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4B, 0x34, 0x34, 0x6D, 0xCB.toByte())

    // Unités (CMD = 0x2F)
    val CMD_UNIT_KMH = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x15, 0x37, 0x2F, 0x34, 0x34, 0x88.toByte(), 0xCB.toByte())
    val CMD_UNIT_MPH = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x15, 0x37, 0x2F, 0x35, 0x34, 0x8F.toByte(), 0xCB.toByte())

    // Régulateur / Cruise Control (CMD = 0x48)
    val CMD_CRUISE_ON = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x35, 0x34, 0x6F, 0xCB.toByte())
    val CMD_CRUISE_OFF = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x36, 0x34, 0x6E, 0xCB.toByte())

    // Néon (CMD = 0x49) - À TESTER
    val CMD_NEON_ON = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x49, 0x35, 0x34, 0x6E, 0xCB.toByte())
    val CMD_NEON_OFF = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x49, 0x34, 0x34, 0x6F, 0xCB.toByte())

    // Klaxon (CMD = 0xC7)
    val CMD_HORN = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x74, 0x2B, 0xB2.toByte(), 0xCA.toByte())

    // ===== COMMANDES DE REQUÊTE =====
    val CMD_KEEP_ALIVE = byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0x00, 0x00, 0x41, 0xCB.toByte())
    val CMD_REQUEST_1 = byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0x01, 0x00, 0x40, 0xCB.toByte())
    val CMD_REQUEST_2 = byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0x02, 0x00, 0x43, 0xCB.toByte())
    val CMD_REQUEST_3 = byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0x03, 0x00, 0x42, 0xCB.toByte())
    val CMD_REQUEST_4 = byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0x04, 0x00, 0x45, 0xCB.toByte())
    val CMD_REQUEST_5 = byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0x05, 0x00, 0x44, 0xCB.toByte())

    /**
     * Vérifie si les données commencent par le bon header
     */
    fun hasValidHeader(data: ByteArray): Boolean {
        return data.size >= 2 && data[0] == HEADER_1 && data[1] == HEADER_2
    }

    /**
     * Calcule le checksum d'une trame (XOR simple)
     */
    fun calculateChecksum(data: ByteArray): Byte {
        if (data.size < 3) return 0
        var xor: Byte = 0
        for (i in 2 until data.size - 2) {  // Exclut header et 2 derniers bytes
            xor = (xor.toInt() xor data[i].toInt()).toByte()
        }
        return xor
    }

    /**
     * Vérifie le checksum d'une trame
     */
    fun verifyChecksum(data: ByteArray): Boolean {
        if (data.size < 5) return false
        val calculated = calculateChecksum(data)
        val received = data[data.size - 2]
        return calculated == received
    }
}