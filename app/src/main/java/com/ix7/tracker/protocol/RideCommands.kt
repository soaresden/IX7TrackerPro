package com.ix7.tracker.protocol

import android.util.Log
import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.WheelMode

/**
 * 🎯 CENTRALISATION COMPLÈTE DES COMMANDES BLUETOOTH
 *
 * Format M0Robot: [0x61, 0x9E, HeaderBytes..., CMD, VAL, CHK, ChecksumXOR, Footer]
 *
 * Toutes les VRAIES commandes + les fonctions API au même endroit
 */
object RideCommands {
    private const val TAG = "RideCommands"

    // ═══════════════════════════════════════════════════════════════
    // 🔐 COMMANDES VERROUILLAGE
    // ═══════════════════════════════════════════════════════════════
    private val LOCK_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4B, 0x35, 0x34, 0x6C, 0xCB.toByte())
    private val UNLOCK_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4B, 0x34, 0x34, 0x6D, 0xCB.toByte())

    fun lock(): ByteArray {
        Log.i(TAG, "🔒 Envoi: LOCK")
        return LOCK_CMD
    }

    fun unlock(): ByteArray {
        Log.i(TAG, "🔓 Envoi: UNLOCK")
        return UNLOCK_CMD
    }

    // ═══════════════════════════════════════════════════════════════
    // 💡 COMMANDES PHARES
    // ═══════════════════════════════════════════════════════════════
    private val LIGHT_ON_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC6.toByte(), 0x35, 0x34, 0xD1.toByte(), 0xCA.toByte())
    private val LIGHT_OFF_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC6.toByte(), 0x34, 0x34, 0xD2.toByte(), 0xCA.toByte())

    fun headlightsOn(): ByteArray {
        Log.i(TAG, "💡 Envoi: HEADLIGHTS_ON")
        return LIGHT_ON_CMD
    }

    fun headlightsOff(): ByteArray {
        Log.i(TAG, "💡 Envoi: HEADLIGHTS_OFF")
        return LIGHT_OFF_CMD
    }

    fun lightsOn(): ByteArray {
        Log.i(TAG, "💡 Envoi: LIGHTS_ON")
        return LIGHT_ON_CMD
    }

    fun lightsOff(): ByteArray {
        Log.i(TAG, "💡 Envoi: LIGHTS_OFF")
        return LIGHT_OFF_CMD
    }

    // ═══════════════════════════════════════════════════════════════
    // ✨ COMMANDES NÉONS
    // ═══════════════════════════════════════════════════════════════
    private val NEON_ON_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC5.toByte(), 0x35, 0x34, 0xD0.toByte(), 0xCA.toByte())
    private val NEON_OFF_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC5.toByte(), 0x34, 0x34, 0xD3.toByte(), 0xCA.toByte())

    fun neonOn(): ByteArray {
        Log.i(TAG, "✨ Envoi: NEON_ON")
        return NEON_ON_CMD
    }

    fun neonOff(): ByteArray {
        Log.i(TAG, "✨ Envoi: NEON_OFF")
        return NEON_OFF_CMD
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎯 COMMANDES MODES DE CONDUITE (CMD=0x4A)
    // ═══════════════════════════════════════════════════════════════
    private val MODE_PIETON_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x37, 0x34, 0x63, 0xCB.toByte())
    private val MODE_ECO_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x36, 0x34, 0x6C, 0xCB.toByte())
    private val MODE_SPORT_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x34, 0x34, 0x6E, 0xCB.toByte())
    private val MODE_RACE_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x35, 0x34, 0x6D, 0xCB.toByte())

    fun setMode(mode: RideMode): ByteArray {
        return when (mode) {
            RideMode.PIETON -> {
                Log.i(TAG, "🚶 Envoi: MODE_PIETON")
                MODE_PIETON_CMD
            }
            RideMode.ECO -> {
                Log.i(TAG, "⚡ Envoi: MODE_ECO")
                MODE_ECO_CMD
            }
            RideMode.SPORT -> {
                Log.i(TAG, "🌱 Envoi: MODE_SPORT")
                MODE_SPORT_CMD
            }
            RideMode.RACE -> {
                Log.i(TAG, "🏎️ Envoi: MODE_RACE")
                MODE_RACE_CMD
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ⏱️ COMMANDES RÉGULATEUR DE VITESSE
    // ═══════════════════════════════════════════════════════════════
    private val CRUISE_ON_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x35, 0x34, 0x6F, 0xCB.toByte())
    private val CRUISE_OFF_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x34, 0x34, 0x68, 0xCB.toByte())

    fun enableCruiseControl(): ByteArray {
        Log.i(TAG, "⏱️ Envoi: CRUISE_ENABLE")
        return CRUISE_ON_CMD
    }

    fun disableCruiseControl(): ByteArray {
        Log.i(TAG, "⏱️ Envoi: CRUISE_DISABLE")
        return CRUISE_OFF_CMD
    }

    fun setCruiseThreshold(speedKmh: Int): ByteArray {
        Log.i(TAG, "⏱️ Envoi: CRUISE_THRESHOLD = $speedKmh km/h")
        // Utiliser la table de checksums pour les vitesses connues
        return CruiseThresholdChecksums.buildCommand(speedKmh)
    }

    // ═══════════════════════════════════════════════════════════════
    // 📢 COMMANDES SONORES (KLAXON)
    // ═══════════════════════════════════════════════════════════════
    private val KEEP_ALIVE_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xDE.toByte(), 0x3C, 0xBD.toByte(), 0xCA.toByte())

    fun hornTrigger(): ByteArray {
        Log.i(TAG, "📢 Envoi: HORN_TRIGGER")
        // TODO: Trouver la vraie commande klaxon
        return KEEP_ALIVE_CMD  // Placeholder
    }

    fun hornRelease(): ByteArray {
        Log.i(TAG, "📢 Envoi: HORN_RELEASE")
        return byteArrayOf()  // Pas de commande release
    }

    // ═══════════════════════════════════════════════════════════════
    // 🛞 COMMANDES MODE ROUES
    // ═══════════════════════════════════════════════════════════════
    private val WHEEL_ONE_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x54, 0x02, 0x34, 0x7C, 0xCB.toByte())
    private val WHEEL_TWO_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x54, 0x01, 0x34, 0x7D, 0xCB.toByte())

    fun setWheelMode(mode: WheelMode): ByteArray {
        return when (mode) {
            WheelMode.ONE_WHEEL -> {
                Log.i(TAG, "🛞 Envoi: ONE_WHEEL")
                WHEEL_ONE_CMD
            }
            WheelMode.TWO_WHEELS -> {
                Log.i(TAG, "🛞 Envoi: TWO_WHEELS")
                WHEEL_TWO_CMD
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🏎️ COMMANDES BRIDAGE / LIMITATION VITESSE
    // ═══════════════════════════════════════════════════════════════
    private val BRIDAGE_LIMITED_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x50, 0x00, 0x34, 0x80.toByte(), 0xCB.toByte())
    private val BRIDAGE_UNLIMITED_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x50, 0x01, 0x34, 0x7F, 0xCB.toByte())

    fun setBridageMode(isUnlimited: Boolean): ByteArray {
        return if (isUnlimited) {
            Log.i(TAG, "🔓 Envoi: BRIDAGE_UNLIMITED")
            BRIDAGE_UNLIMITED_CMD
        } else {
            Log.i(TAG, "🔒 Envoi: BRIDAGE_LIMITED")
            BRIDAGE_LIMITED_CMD
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 📏 COMMANDES UNITÉS
    // ═══════════════════════════════════════════════════════════════
    private val UNIT_KMH_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4D, 0x34, 0x34, 0x69, 0xCB.toByte())
    private val UNIT_MPH_CMD = byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4D, 0x35, 0x34, 0x68, 0xCB.toByte())

    fun setUnitKMH(): ByteArray {
        Log.i(TAG, "📏 Envoi: UNIT_KMH")
        return UNIT_KMH_CMD
    }

    fun setUnitMPH(): ByteArray {
        Log.i(TAG, "📏 Envoi: UNIT_MPH")
        return UNIT_MPH_CMD
    }

    // ═══════════════════════════════════════════════════════════════
    // 🆘 UTILITAIRES
    // ═══════════════════════════════════════════════════════════════

    fun commandToString(command: ByteArray): String {
        return command.joinToString(" ") { byte ->
            "%02X".format(byte.toInt() and 0xFF)
        }
    }

    init {
        Log.d(TAG, "✅ RideCommands chargées")
        Log.d(TAG, "  MODES: CMD=0x4A (PIETON=0x37, ECO=0x36, SPORT=0x34, RACE=0x35)")
    }
}