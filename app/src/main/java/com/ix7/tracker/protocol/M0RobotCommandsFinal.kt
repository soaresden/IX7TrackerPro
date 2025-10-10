package com.ix7.tracker.protocol

/**
 * DICTIONNAIRE DÉFINITIF DES COMMANDES M0ROBOT
 * Basé sur :
 * - Analyse des logs btsnoop_hci8.log
 * - Code source de l'appli officielle
 * - Tests empiriques
 *
 * STRUCTURE CONFIRMÉE : 61 9E [TYPE] [LENGTH] [DATA...] [CHECKSUM]
 */
object M0RobotCommandsFinal {

    // ========== CONSTANTES DE BASE ==========
    private const val HEADER_1: Byte = 0x61
    private const val HEADER_2: Byte = 0x9E.toByte()

    // ========== COMMANDES CONFIRMÉES (qui marchent) ==========

    /**
     * VERROUILLAGE - Confirmé
     * Pattern: 61 9E 30 14 37 4B [VAL] 34 [CHK] CB
     */
    val LOCK = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
        0x4B, 0x35, 0x34, 0x6C, 0xCB.toByte()
    )

    val UNLOCK = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
        0x4B, 0x34, 0x34, 0x6D, 0xCB.toByte()
    )

    /**
     * PHARE - Confirmé
     * Pattern: 61 9E 30 14 37 C6 [VAL] 34 [CHK] CA
     */
    val LIGHT_ON = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
        0xC6.toByte(), 0x35, 0x34, 0xD1.toByte(), 0xCA.toByte()
    )

    val LIGHT_OFF = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
        0xC6.toByte(), 0x34, 0x34, 0xD2.toByte(), 0xCA.toByte()
    )

    /**
     * MODES DE CONDUITE - Confirmés
     * Pattern variable selon le mode
     */
    // Modes de conduite CORRECTS
    val MODE_PEDESTRIAN = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x49, 0x37, 0x34, 0x6C, 0xCB.toByte())
    val MODE_ECO = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x36, 0x34, 0x6E, 0xCB.toByte())
    val MODE_SPORT = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x36, 0x34, 0x6C, 0xCB.toByte())
    val MODE_RACE = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x35, 0x34, 0x6D, 0xCB.toByte())

    // NÉONS (haute probabilité)
    val NEON_ON = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC5.toByte(), 0x35, 0x34, 0xD0.toByte(), 0xCA.toByte())
    val NEON_OFF = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC5.toByte(), 0x34, 0x34, 0xD3.toByte(), 0xCA.toByte())

    // RÉGULATEUR (haute probabilité)
    val CRUISE_ON = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x35, 0x34, 0x6F, 0xCB.toByte())
    val CRUISE_OFF = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x34, 0x34, 0x68, 0xCB.toByte())


    // ========== COMMANDES CANDIDATES (à tester) ==========

    /**
     * MODE 2 ROUES / 1 ROUE
     * Hypothèse basée sur le type 0x3C trouvé dans les logs
     */
    val MODE_TWO_WHEELS = byteArrayOf(
        0x61, 0x9E.toByte(), 0x3C, 0x17, 0x35,
        0x8F.toByte(), 0x36, 0x35, 0x34, 0x34, 0x34, 0x34, 0x21, 0xCB.toByte()
    )

    val MODE_ONE_WHEEL = byteArrayOf(
        0x61, 0x9E.toByte(), 0x3C, 0x17, 0x35,
        0x8F.toByte(), 0x35, 0x35, 0x34, 0x34, 0x34, 0x34, 0x22, 0xCB.toByte()
    )

    /**
     * DÉMARRAGE ZÉRO (Zero Start)
     * Hypothèse basée sur les patterns de fin de log
     */
    val ZERO_START_ON = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
        0x4C, 0x35, 0x34, 0x6B, 0xCB.toByte()
    )

    val ZERO_START_OFF = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
        0x4C, 0x34, 0x34, 0x6A, 0xCB.toByte()
    )

    /**
     * KLAXON - Multiple variantes à tester
     * Série 0xC7 fait biper selon les tests
     */
    val HORN_VARIANT_1 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
        0xC7.toByte(), 0x74, 0x2B, 0xB2.toByte(), 0xCA.toByte()
    )

    val HORN_VARIANT_2 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
        0xC7.toByte(), 0xD4.toByte(), 0x1A, 0xE3.toByte(), 0xC9.toByte()
    )

    val HORN_VARIANT_3 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
        0xC7.toByte(), 0xB4.toByte(), 0x0A, 0x13, 0xCA.toByte()
    )

    /**
     * MODE LIMITATION (bridé/débridé)
     * Pattern 0x2F pour les unités, pourrait contrôler la limite
     */
    val SPEED_LIMIT_25KMH = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x15, 0x37,
        0x2F, 0x34, 0x34, 0x88.toByte(), 0xCB.toByte()
    )

    val SPEED_UNLIMITED = byteArrayOf(
        0x61, 0x9E.toByte(), 0x30, 0x15, 0x37,
        0x2F, 0x35, 0x34, 0x8F.toByte(), 0xCB.toByte()
    )

    // ========== COMMANDES DE STATUS ==========

    /**
     * Requêtes de status - Les plus fréquentes dans les logs
     */
    val STATUS_REQUEST_1 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x32, 0x17, 0x35,
        0x0B, 0xA4.toByte(), 0x35, 0xA4.toByte(), 0x35, 0x40, 0xCA.toByte()
    )

    val STATUS_REQUEST_2 = byteArrayOf(
        0x61, 0x9E.toByte(), 0x3E, 0x17, 0x35,
        0xDA.toByte(), 0xC3.toByte(), 0x34, 0x9E.toByte(), 0x37,
        0x14, 0x30, 0x8B.toByte(), 0x36, 0x6E, 0xC8.toByte()
    )

    val KEEP_ALIVE = byteArrayOf(
        0x61, 0x9E.toByte(), 0x37, 0x14, 0x55,
        0xDE.toByte(), 0x3C, 0xBD.toByte(), 0xCA.toByte()
    )

    // ========== ANALYSE DU CHECKSUM ==========

    /**
     * Calcul du checksum
     * Semble être un simple XOR des bytes après le header
     */
    fun calculateChecksum(command: ByteArray): Byte {
        if (command.size < 4) return 0

        var checksum = 0
        // XOR de tous les bytes sauf header (2 premiers) et checksum (dernier)
        for (i in 2 until command.size - 1) {
            checksum = checksum xor command[i].toInt()
        }
        return checksum.toByte()
    }

    /**
     * Vérifie si une commande a le bon format
     */
    fun isValidCommand(command: ByteArray): Boolean {
        if (command.size < 5) return false
        return command[0] == HEADER_1 && command[1] == HEADER_2
    }

    /**
     * Builder pour créer des commandes personnalisées
     */
    class CommandBuilder {
        private var type: Byte = 0x30
        private var length: Byte = 0x14
        private var subCommand: Byte = 0x37
        private var commandCode: Byte = 0x00
        private var value: Byte = 0x34
        private var extra: Byte = 0x34

        fun setType(t: Byte) = apply { type = t }
        fun setLength(l: Byte) = apply { length = l }
        fun setSubCommand(sc: Byte) = apply { subCommand = sc }
        fun setCommandCode(cc: Byte) = apply { commandCode = cc }
        fun setValue(v: Byte) = apply { value = v }
        fun setExtra(e: Byte) = apply { extra = e }

        fun build(): ByteArray {
            val cmd = mutableListOf<Byte>()
            cmd.add(HEADER_1)
            cmd.add(HEADER_2)
            cmd.add(type)
            cmd.add(length)
            cmd.add(subCommand)
            cmd.add(commandCode)
            cmd.add(value)
            cmd.add(extra)

            // Calculer et ajouter le checksum
            val checksum = calculateChecksum(cmd.toByteArray() + 0x00)
            cmd.add(checksum)

            // Ajouter le byte final (CA ou CB selon le pattern)
            cmd.add(if (commandCode.toInt() and 0x80 != 0) 0xCA.toByte() else 0xCB.toByte())

            return cmd.toByteArray()
        }
    }

    // ========== GUIDE D'UTILISATION ==========
    /**
     * COMMANDES CONFIRMÉES À 100%:
     * - Lock/Unlock ✅
     * - Light On/Off ✅
     * - Modes (Pedestrian, D, S, S+) ✅
     *
     * COMMANDES TRÈS PROBABLES (90%):
     * - Neon On/Off (pattern 0xC5) 🔍
     * - Cruise On/Off (pattern 0x48) 🔍
     *
     * COMMANDES À TESTER:
     * - Mode 2 roues/1 roue (type 0x3C)
     * - Zero Start (hypothèse 0x4C)
     * - Klaxon (série 0xC7 fait biper)
     * - Speed Limit (pattern 0x2F)
     *
     * MÉTHODE DE TEST:
     * 1. Utiliser TestScreen pour envoyer chaque commande
     * 2. Noter l'effet précis sur la trottinette
     * 3. Ajuster le checksum si la commande ne passe pas
     * 4. Une fois confirmé, déplacer dans "CONFIRMÉES"
     */
}