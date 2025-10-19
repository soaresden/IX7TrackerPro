package com.ix7.tracker.protocol

/**
 * 🎯 PROTOCOLE M0ROBOT SIMPLIFIÉ - VERSION COMPLÈTE
 *
 * Toutes les constantes en un seul endroit !
 * Usage : ProtocolSimple.CMD_LUMIERE_ON, ProtocolSimple.OFFSET_BATTERIE, etc.
 *
 * Basé sur l'analyse complète des logs et de l'app officielle
 * Date: 14 octobre 2025
 */
object ProtocolSimple {

    // ═══════════════════════════════════════════════════════════
    // 📡 BLUETOOTH - UUIDs
    // ═══════════════════════════════════════════════════════════

    const val SERVICE_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
    const val TX_CHAR_UUID = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"  // Pour écrire
    const val RX_CHAR_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"  // Pour lire

    // ═══════════════════════════════════════════════════════════
    // 🎯 HEADER DES TRAMES - Toutes les trames commencent par ça
    // ═══════════════════════════════════════════════════════════

    const val HEADER_1: Byte = 0x61
    const val HEADER_2: Byte = 0x9E.toByte()

    // ═══════════════════════════════════════════════════════════
    // 📦 TYPES DE TRAMES REÇUES (byte 2 de la trame)
    // ═══════════════════════════════════════════════════════════

    const val TRAME_INIT: Byte = 0x02        // Initialisation (67 bytes)
    const val TRAME_ODO_COURT: Byte = 0x03   // Odomètre version courte
    const val TRAME_ETENDU: Byte = 0x04      // Données étendues (51 bytes)
    const val TRAME_COMBINE: Byte = 0x16     // Batterie + Odomètre (47 bytes)
    const val TRAME_DETAIL: Byte = 0x1A      // Odomètre détaillé (49 bytes)
    const val TRAME_BATTERIE: Byte = 0x20    // Batterie seule
    const val TRAME_INFO_EXT: Byte = 0x26    // Info étendue (24 bytes)
    const val TRAME_STATUS: Byte = 0x30      // État/Mode (10 bytes)
    const val TRAME_TEMP: Byte = 0x32        // Températures (12 bytes)
    const val TRAME_VITESSE: Byte = 0x37     // ⭐ Vitesse temps réel (9 bytes)
    const val TRAME_SPECIAL_1: Byte = 0x38   // Spécial 1 (19 bytes)
    const val TRAME_SPECIAL_2: Byte = 0x3A   // Spécial 2 (20 bytes)
    const val TRAME_SPECIAL_3: Byte = 0x3C   // Spécial 3 (18 bytes)
    const val TRAME_REALTIME: Byte = 0x3E    // Données temps réel (16 bytes)
    const val TRAME_SYSTEME: Byte = 0xD3.toByte() // Système

    // ═══════════════════════════════════════════════════════════
    // 🔢 OFFSETS VALIDÉS - Position des données dans les trames
    // ═══════════════════════════════════════════════════════════

    // BATTERIE (%) - Plusieurs sources possibles
    const val OFFSET_BATTERIE_0x20 = 45      // Dans trame 0x20
    const val OFFSET_BATTERIE_0x3E = -1      // Chercher dans trame 0x3E (position variable)
    const val OFFSET_BATTERIE_0xD3 = 43      // Dans trame 0xD3

    // VOLTAGE (V) - Big Endian, diviser par 1000
    const val OFFSET_VOLTAGE_START = 6       // Dans trame 0x3E, bytes 6-7
    const val OFFSET_VOLTAGE_END = 7

    // ODOMÈTRE (km)
    const val OFFSET_ODO_0x03_START = 2      // Dans 0x03, bytes 2-3, LE/100
    const val OFFSET_ODO_0x03_END = 3
    const val OFFSET_ODO_0x30_START = 35     // Dans 0x30, bytes 35-36, LE/10
    const val OFFSET_ODO_0x30_END = 36

    // TEMPÉRATURE (°C)
    const val OFFSET_TEMP_0x3E = 49          // Dans trame 0x3E
    const val OFFSET_TEMP_0xD3_1 = 17        // Dans trame 0xD3
    const val OFFSET_TEMP_0xD3_2 = 29        // Dans trame 0xD3 (alternative)

    // VITESSE (km/h)
    const val OFFSET_VITESSE = 5             // Dans trame 0x32

    // ═══════════════════════════════════════════════════════════
    // 🎮 COMMANDES - Pour contrôler le scooter
    // ═══════════════════════════════════════════════════════════

    // INITIALISATION
    val CMD_INIT_1 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x02, 0x14, 0x55, 0x01, 0xCC.toByte(), 0xCB.toByte()
    )

    val CMD_KEEP_ALIVE = byteArrayOf(
        0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xDE.toByte(), 0x3C, 0xBD.toByte(), 0xCA.toByte()
    )

    // LUMIÈRES
    val CMD_LIGHTS_ON = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC6.toByte(), 0x35, 0x34, 0xD1.toByte(), 0xCA.toByte()
    )

    val CMD_LIGHTS_OFF = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC6.toByte(), 0x34, 0x34, 0xD2.toByte(), 0xCA.toByte()
    )

    // NÉONS
    val CMD_NEON_ON = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC5.toByte(), 0x35, 0x34, 0xD0.toByte(), 0xCA.toByte()
    )

    val CMD_NEON_OFF = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC5.toByte(), 0x34, 0x34, 0xD3.toByte(), 0xCA.toByte()
    )

    // VERROUILLAGE
    val CMD_LOCK = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4B, 0x35, 0x34, 0x6C, 0xCB.toByte()
    )

    val CMD_UNLOCK = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4B, 0x34, 0x34, 0x6D, 0xCB.toByte()
    )

    // MODES DE CONDUITE
    val CMD_MODE_PIETON = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x37, 0x34, 0x63, 0xCB.toByte()
    )

    val CMD_MODE_ECO = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x36, 0x34, 0x6C, 0xCB.toByte()
    )

    val CMD_MODE_SPORT = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x34, 0x34, 0x6E, 0xCB.toByte()
    )

    val CMD_MODE_RACE = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x35, 0x34, 0x6D, 0xCB.toByte()
    )

    // ✅ UNITÉS DE VITESSE (AJOUT)
    val CMD_UNIT_KMH = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4D, 0x34, 0x34, 0x69, 0xCB.toByte()
    )

    val CMD_UNIT_MPH = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4D, 0x35, 0x34, 0x68, 0xCB.toByte()
    )

    // ✅ KLAXON (AJOUT) - Tentatives basées sur le pattern
    val CMD_HORN_TRY_1 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC8.toByte(), 0x35, 0x34, 0xD2.toByte(), 0xCA.toByte()
    )

    // ═══════════════════════════════════════════════════════════
    // 🛠️ FONCTIONS UTILITAIRES
    // ═══════════════════════════════════════════════════════════

    /**
     * Vérifie si une trame a le bon header
     */
    fun estTrameValide(data: ByteArray): Boolean {
        return data.size >= 2 && data[0] == HEADER_1 && data[1] == HEADER_2
    }

    /**
     * Retourne le type de la trame (byte 2)
     */
    fun getTypeTrame(data: ByteArray): Byte? {
        return if (data.size >= 3) data[2] else null
    }

    /**
     * Convertit une trame en string hexadécimal pour debug
     */
    fun trameToHex(data: ByteArray): String {
        return data.joinToString(" ") { "%02X".format(it) }
    }

    /**
     * Lit un entier 16-bit Little Endian
     */
    fun lireInt16LE(data: ByteArray, offset: Int): Int {
        if (offset + 1 >= data.size) return 0
        return ((data[offset + 1].toInt() and 0xFF) shl 8) or (data[offset].toInt() and 0xFF)
    }

    /**
     * Lit un entier 16-bit Big Endian
     */
    fun lireInt16BE(data: ByteArray, offset: Int): Int {
        if (offset + 1 >= data.size) return 0
        return ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
    }

    /**
     * Extrait la batterie d'une trame
     */
    fun extraireBatterie(data: ByteArray): Int? {
        val type = getTypeTrame(data) ?: return null

        return when (type) {
            TRAME_BATTERIE -> {
                if (data.size > OFFSET_BATTERIE_0x20) {
                    val bat = data[OFFSET_BATTERIE_0x20].toInt() and 0xFF
                    if (bat in 0..100) bat else null
                } else null
            }
            TRAME_SYSTEME -> {
                if (data.size > OFFSET_BATTERIE_0xD3) {
                    val bat = data[OFFSET_BATTERIE_0xD3].toInt() and 0xFF
                    if (bat in 0..100) bat else null
                } else null
            }
            else -> null
        }
    }

    /**
     * Extrait le voltage d'une trame (en volts)
     */
    fun extraireVoltage(data: ByteArray): Float? {
        val type = getTypeTrame(data) ?: return null

        if (type == TRAME_REALTIME && data.size > OFFSET_VOLTAGE_END) {
            val volts = lireInt16BE(data, OFFSET_VOLTAGE_START)
            val voltage = volts / 1000.0f
            return if (voltage > 30.0f && voltage < 70.0f) voltage else null
        }
        return null
    }

    /**
     * Extrait l'odomètre d'une trame (en km)
     */
    fun extraireOdometer(data: ByteArray): Float? {
        val type = getTypeTrame(data) ?: return null

        return when (type) {
            TRAME_ODO_COURT -> {
                if (data.size > OFFSET_ODO_0x03_END) {
                    val odo = lireInt16LE(data, OFFSET_ODO_0x03_START)
                    odo / 100.0f
                } else null
            }
            TRAME_STATUS -> {
                if (data.size > OFFSET_ODO_0x30_END) {
                    val odo = lireInt16LE(data, OFFSET_ODO_0x30_START)
                    odo / 10.0f
                } else null
            }
            else -> null
        }
    }

    /**
     * Extrait la température d'une trame (en °C)
     */
    fun extraireTemperature(data: ByteArray): Int? {
        val type = getTypeTrame(data) ?: return null

        return when (type) {
            TRAME_REALTIME -> {
                if (data.size > OFFSET_TEMP_0x3E) {
                    val temp = data[OFFSET_TEMP_0x3E].toInt() and 0xFF
                    if (temp in 0..80) temp else null
                } else null
            }
            TRAME_SYSTEME -> {
                if (data.size > OFFSET_TEMP_0xD3_1) {
                    val temp = data[OFFSET_TEMP_0xD3_1].toInt() and 0xFF
                    if (temp in 0..80) temp else null
                } else null
            }
            else -> null
        }
    }

    /**
     * Extrait la vitesse d'une trame (en km/h)
     */
    fun extraireVitesse(data: ByteArray): Int? {
        val type = getTypeTrame(data) ?: return null

        if (type == TRAME_TEMP && data.size > OFFSET_VITESSE) {
            return data[OFFSET_VITESSE].toInt() and 0xFF
        }
        return null
    }
}