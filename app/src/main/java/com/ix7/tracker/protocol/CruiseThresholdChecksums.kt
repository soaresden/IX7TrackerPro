// Table de checksums pour la commande 0xC7 (seuil régulateur)
// Basée sur l'analyse des logs btsnoop

object CruiseThresholdChecksums {

    // Structure: speed -> Pair(checksum1, checksum2)
    private val checksumTable = mapOf(
        // Valeurs confirmées dans les logs
        4 to Pair(0x04.toByte(), 0x41.toByte()),
        12 to Pair(0x0C.toByte(), 0x7E.toByte()),
        14 to Pair(0x0E.toByte(), 0x14.toByte()),
        20 to Pair(0x7A.toByte(), 0x43.toByte()),  // 0x14 = 20
        26 to Pair(0x1A.toByte(), 0xE3.toByte()),  // 0x1A = 26
        30 to Pair(0x1E.toByte(), 0xF7.toByte()),  // 0x1E = 30
        36 to Pair(0x13.toByte(), 0x9A.toByte()),  // 0x24 = 36
        60 to Pair(0x66.toByte(), 0xBF.toByte()),  // 0x3C = 60

        // Valeurs extrapolées (à tester)
        10 to Pair(0x74.toByte(), 0x8B.toByte()),
        15 to Pair(0x71.toByte(), 0x8E.toByte()),
        25 to Pair(0x77.toByte(), 0x88.toByte()),
        35 to Pair(0x4D.toByte(), 0xB2.toByte()),
        40 to Pair(0x46.toByte(), 0xB9.toByte()),
        45 to Pair(0x43.toByte(), 0xBC.toByte()),
        50 to Pair(0x5C.toByte(), 0xA3.toByte()),
        55 to Pair(0x59.toByte(), 0xA6.toByte())
    )

    /**
     * Construit la commande complète avec les bons checksums
     */
    fun buildCommand(speedKmh: Int): ByteArray {
        val cmd = byteArrayOf(
            0x61, 0x9E.toByte(),
            0x30, 0x14, 0x37,
            0xC7.toByte(),
            speedKmh.toByte(),
            0x00,  // checksum1
            0x00,  // checksum2
            0xCA.toByte()
        )

        // Chercher dans la table
        val checksums = checksumTable[speedKmh]

        if (checksums != null) {
            // Utiliser les checksums connus
            cmd[7] = checksums.first
            cmd[8] = checksums.second
        } else {
            // Calculer pour les valeurs non référencées
            // Pattern observé : checksum1 semble suivre une formule complexe
            // Pour l'instant, on fait un XOR simple
            var xor = 0
            for (i in 2..6) {
                xor = xor xor cmd[i].toInt()
            }
            cmd[7] = xor.toByte()

            // checksum2 semble être lié au checksum1
            // Pattern observé : souvent checksum2 = ~checksum1 ou une variante
            cmd[8] = (xor xor 0xFF).toByte()

            android.util.Log.w("CRUISE", "⚠️ Checksums calculés pour $speedKmh km/h (non testés)")
        }

        // Log pour debug
        val hex = cmd.joinToString(" ") { "%02X".format(it) }
        android.util.Log.i("CRUISE", "Commande 0xC7: $hex")

        return cmd
    }

    /**
     * Teste si une vitesse a des checksums connus
     */
    fun hasKnownChecksum(speedKmh: Int): Boolean {
        return checksumTable.containsKey(speedKmh)
    }
}

// Utilisation dans sendCruiseThreshold:
/*
fun sendCruiseThreshold(speed: Int) {
    scope.launch {
        val command = CruiseThresholdChecksums.buildCommand(speed)
        bluetoothManager.sendCommand(command)
        
        if (!CruiseThresholdChecksums.hasKnownChecksum(speed)) {
            Log.w("CRUISE", "⚠️ Vitesse $speed km/h utilise des checksums calculés")
        }
    }
}
*/