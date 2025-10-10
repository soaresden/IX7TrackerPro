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

// ✅ NOUVELLES COMMANDES NÉON (corrigées)
    val CMD_NEON_ON = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
        0xC5.toByte(), 0x35, 0x34, 0xD0.toByte(), 0xCA.toByte()
    )

    val CMD_NEON_OFF = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
        0xC5.toByte(), 0x34, 0x34, 0xD3.toByte(), 0xCA.toByte()
    )
    // Klaxon (CMD = 0xC7)
// Tentatives pour le klaxon
    val CMD_HORN_TRY_1 = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x35, 0x34, 0xD0.toByte(), 0xCA.toByte())
    val CMD_HORN_TRY_2 = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC8.toByte(), 0x35, 0x34, 0xD0.toByte(), 0xCA.toByte())
    val CMD_HORN_TRY_3 = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4F, 0x35, 0x34, 0x6E, 0xCB.toByte())

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

    // ===== COMMANDE SEUIL RÉGULATEUR (0xC7) =====
// Cette commande définit le seuil de déclenchement du régulateur
// Format : 61 9E 30 14 37 C7 [SPEED] [CHK1] [CHK2] CA
// Exemples trouvés dans les logs :
//   0x14 (20 km/h) : 61 9e 30 14 37 c7 14 7a 43 ca
//   0x3C (60 km/h) : 61 9e 30 14 37 c7 3c 66 bf ca

    /**
     * Construit une commande pour définir le seuil du régulateur
     * @param speed Vitesse en km/h (10-60)
     * @return ByteArray de la commande complète
     */
    fun buildCruiseThresholdCommand(speed: Int): ByteArray {
        val cmd = byteArrayOf(
            0x61, 0x9E.toByte(),
            0x30, 0x14, 0x37,
            0xC7.toByte(),      // Commande cruise threshold
            speed.toByte(),     // Vitesse en km/h
            0x00,              // Checksum 1 (à calculer)
            0x00,              // Checksum 2 (à calculer)
            0xCA.toByte()      // Fin
        )

        // Calcul du checksum (basé sur l'analyse des patterns)
        // Le checksum semble être un XOR des bytes 2 à 6
        var checksum = 0
        for (i in 2..6) {
            checksum = checksum xor cmd[i].toInt()
        }
        cmd[7] = checksum.toByte()

        // Le second checksum semble être l'inverse du premier
        cmd[8] = (0xFF - checksum).toByte()

        return cmd
    }

    // Exemples de commandes précalculées pour les vitesses courantes
    val CMD_CRUISE_THRESHOLD_10 = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x0A, 0x74, 0x8B.toByte(), 0xCA.toByte())
    val CMD_CRUISE_THRESHOLD_15 = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x0F, 0x71, 0x8E.toByte(), 0xCA.toByte())
    val CMD_CRUISE_THRESHOLD_20 = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x14, 0x7A, 0x43, 0xCA.toByte())
    val CMD_CRUISE_THRESHOLD_25 = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x19, 0x77, 0x88.toByte(), 0xCA.toByte())
    val CMD_CRUISE_THRESHOLD_30 = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x1E, 0x70, 0x8F.toByte(), 0xCA.toByte())
    val CMD_CRUISE_THRESHOLD_35 = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x23, 0x4D, 0xB2.toByte(), 0xCA.toByte())
    val CMD_CRUISE_THRESHOLD_40 = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x28, 0x46, 0xB9.toByte(), 0xCA.toByte())
    val CMD_CRUISE_THRESHOLD_45 = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x2D, 0x43, 0xBC.toByte(), 0xCA.toByte())
    val CMD_CRUISE_THRESHOLD_50 = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x32, 0x5C, 0xA3.toByte(), 0xCA.toByte())
    val CMD_CRUISE_THRESHOLD_55 = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x37, 0x59, 0xA6.toByte(), 0xCA.toByte())
    val CMD_CRUISE_THRESHOLD_60 = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x3C, 0x66, 0xBF.toByte(), 0xCA.toByte())







}