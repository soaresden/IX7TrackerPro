package com.ix7.tracker.protocol

import android.util.Log
import com.ix7.tracker.core.RideMode

/**
 * Constructeur de commandes pour le protocole 61 9E
 * Permet de contrôler néon, lumières, débridage, modes
 */
object CommandBuilder {
    private const val TAG = "CommandBuilder"

    // Headers protocole 61 9E
    private const val HEADER_1: Byte = 0x61
    private const val HEADER_2: Byte = 0x9E.toByte()
    private const val CMD_SET_MODE: Byte = 0x30

    /**
     * Calcule le checksum (XOR de tous les bytes sauf le dernier)
     */
    private fun calculateChecksum(frame: ByteArray): Byte {
        var xor: Byte = 0
        for (i in 0 until frame.size - 1) {
            xor = (xor.toInt() xor frame[i].toInt()).toByte()
        }
        return xor
    }

    /**
     * Construit une commande MODE complète
     * Format: 61 9E 30 17 35 [FLAGS] [MODE] 34 [?] [CHK]
     */
    fun buildModeCommand(
        unlocked: Boolean = true,
        dualWheel: Boolean = false,
        neonOn: Boolean = true,
        lightsOn: Boolean = true,
        modeType: Byte = 0x34.toByte()
    ): ByteArray {
        // Byte 5 FLAGS (binaire)
        var flags: Int = 0x40 // Bit 6 toujours à 1

        if (unlocked) flags = flags or 0x80    // Bit 7: débridage
        if (dualWheel) flags = flags or 0x08   // Bit 3: 2 roues
        if (neonOn) flags = flags or 0x02      // Bit 1: néon
        if (lightsOn) flags = flags or 0x01    // Bit 0: lumières

        val frame = byteArrayOf(
            HEADER_1,           // 0: 61
            HEADER_2,           // 1: 9E
            CMD_SET_MODE,       // 2: 30
            0x17,               // 3: Constante
            0x35,               // 4: Constante
            flags.toByte(),     // 5: FLAGS
            modeType,           // 6: Mode de conduite
            0x34,               // 7: Constante
            0x00,               // 8: Padding
            0x00                // 9: Checksum (calculé après)
        )

        frame[9] = calculateChecksum(frame)

        val hex = frame.joinToString(" ") { "%02X".format(it) }
        Log.d(TAG, "CMD MODE: $hex (Débridé:$unlocked Néon:$neonOn Lumières:$lightsOn)")

        return frame
    }

    /**
     * Toggle néon uniquement
     */
    fun buildToggleNeonCommand(
        neonOn: Boolean,
        currentData: com.ix7.tracker.core.ScooterData
    ): ByteArray {
        return buildModeCommand(
            unlocked = !currentData.isLocked,
            dualWheel = false,
            neonOn = neonOn,
            lightsOn = currentData.headlightsOn,
            modeType = getModeTypeByte(currentData.currentMode)
        )
    }

    /**
     * Toggle débridage uniquement
     */
    fun buildToggleUnlockCommand(
        unlocked: Boolean,
        currentData: com.ix7.tracker.core.ScooterData
    ): ByteArray {
        return buildModeCommand(
            unlocked = unlocked,
            dualWheel = false,
            neonOn = currentData.neonOn,
            lightsOn = currentData.headlightsOn,
            modeType = getModeTypeByte(currentData.currentMode)
        )
    }

    /**
     * Toggle lumières uniquement
     */
    fun buildToggleLightsCommand(
        lightsOn: Boolean,
        currentData: com.ix7.tracker.core.ScooterData
    ): ByteArray {
        return buildModeCommand(
            unlocked = !currentData.isLocked,
            dualWheel = false,
            neonOn = currentData.neonOn,
            lightsOn = lightsOn,
            modeType = getModeTypeByte(currentData.currentMode)
        )
    }

    /**
     * Change mode de conduite
     */
    fun buildChangeModeCommand(
        mode: RideMode,
        currentData: com.ix7.tracker.core.ScooterData
    ): ByteArray {
        return buildModeCommand(
            unlocked = !currentData.isLocked,
            dualWheel = false,
            neonOn = currentData.neonOn,
            lightsOn = currentData.headlightsOn,
            modeType = getModeTypeByte(mode)
        )
    }

    /**
     * Convertit RideMode en byte protocole
     */
    private fun getModeTypeByte(mode: RideMode): Byte {
        return when (mode) {
            RideMode.PEDESTRIAN -> 0xE1.toByte()
            RideMode.ECO -> 0x34.toByte()
            RideMode.RACE -> 0x35.toByte()
            RideMode.SPORT -> 0xB8.toByte()
        }
    }

    /**
     * Commande de débridage rapide
     */
    fun buildUnlockCommand(): ByteArray {
        return buildModeCommand(
            unlocked = true,
            dualWheel = false,
            neonOn = false,
            lightsOn = false,
            modeType = 0x34.toByte()
        )
    }
}