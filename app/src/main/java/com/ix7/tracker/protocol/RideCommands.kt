package com.ix7.tracker.protocol

import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.WheelMode
import android.util.Log

/**
 * 🎮 CENTRALISATION DE TOUTES LES COMMANDES
 *
 * Adapté pour ton projet:
 * - Utilise M0RobotCommands (commandes de base)
 * - Utilise ProtocolSimple (constantes)
 * - Support RideMode.PIETON (pas PEDESTRIAN)
 * - Élimine les doublons
 */
object RideCommands {

    private const val TAG = "RideCommands"

    // ═══════════════════════════════════════════════════════════════
    // 🔐 COMMANDES LOCK/UNLOCK
    // ═══════════════════════════════════════════════════════════════

    fun lock(): ByteArray {
        Log.i(TAG, "🔒 Envoi CMD_LOCK")
        return M0RobotCommands.LOCK
    }

    fun unlock(): ByteArray {
        Log.i(TAG, "🔓 Envoi CMD_UNLOCK")
        return M0RobotCommands.UNLOCK
    }

    // ═══════════════════════════════════════════════════════════════
    // 💡 COMMANDES PHARES & NÉON
    // ═══════════════════════════════════════════════════════════════

    fun lightsOn(): ByteArray {
        Log.i(TAG, "💡 Phares ON")
        return M0RobotCommands.LIGHT_ON
    }

    fun lightsOff(): ByteArray {
        Log.i(TAG, "💡 Phares OFF")
        return M0RobotCommands.LIGHT_OFF
    }

    fun neonOn(): ByteArray {
        Log.i(TAG, "🌈 Néon ON")
        return M0RobotCommands.NEON_ON
    }

    fun neonOff(): ByteArray {
        Log.i(TAG, "🌈 Néon OFF")
        return M0RobotCommands.NEON_OFF
    }

    fun hornTrigger(): ByteArray {
        Log.i(TAG, "📯 Klaxon activé")
        return M0RobotCommands.KEEP_ALIVE  // Placeholder - à confirmer
    }

    // ═══════════════════════════════════════════════════════════════
    // 🎯 COMMANDES MODES DE CONDUITE
    // ═══════════════════════════════════════════════════════════════

    fun setMode(mode: RideMode): ByteArray {
        return when (mode) {
            RideMode.PIETON -> {
                Log.i(TAG, "🚶 Mode PIÉTON")
                M0RobotCommands.MODE_PIETON
            }
            RideMode.ECO -> {
                Log.i(TAG, "🌱 Mode ECO")
                M0RobotCommands.MODE_ECO
            }
            RideMode.SPORT -> {
                Log.i(TAG, "⚡ Mode SPORT")
                M0RobotCommands.MODE_SPORT
            }
            RideMode.RACE -> {
                Log.i(TAG, "🏎️ Mode RACE")
                M0RobotCommands.MODE_RACE
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🛞 COMMANDES ROUES (1 ROUE vs 2 ROUES)
    // ═══════════════════════════════════════════════════════════════

    fun setWheelMode(mode: WheelMode): ByteArray {
        return when (mode) {
            WheelMode.ONE_WHEEL -> {
                Log.i(TAG, "🛞 Mode 1 ROUE")
                byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x54, 0x02, 0x34, 0x7C, 0xCB.toByte())
            }
            WheelMode.TWO_WHEELS -> {
                Log.i(TAG, "🛞 Mode 2 ROUES")
                byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x54, 0x01, 0x34, 0x7D, 0xCB.toByte())
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ⏱️ COMMANDES RÉGULATEUR DE VITESSE (CRUISE CONTROL)
    // ═══════════════════════════════════════════════════════════════

    fun enableCruiseControl(): ByteArray {
        Log.i(TAG, "✅ Régulateur ACTIVÉ")
        return M0RobotCommands.CRUISE_ON
    }

    fun disableCruiseControl(): ByteArray {
        Log.i(TAG, "❌ Régulateur DÉSACTIVÉ")
        return M0RobotCommands.CRUISE_OFF
    }

    /**
     * 🎚️ Envoyer le seuil du régulateur
     *
     * @param speedKmh Vitesse en km/h (10-60)
     * @return Commande à envoyer
     */
    fun setCruiseThreshold(speedKmh: Int): ByteArray {
        return CruiseThresholdChecksums.buildCommand(speedKmh)
    }

    // ═══════════════════════════════════════════════════════════════
    // 📊 COMMANDES BRIDAGE (LIMITED vs UNLIMITED)
    // ═══════════════════════════════════════════════════════════════

    fun setBridageMode(isUnlimited: Boolean): ByteArray {
        return if (isUnlimited) {
            Log.i(TAG, "🔓 Mode ILLIMITÉ")
            byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x50, 0x01, 0x34, 0x7F, 0xCB.toByte())
        } else {
            Log.i(TAG, "🔒 Mode BRIDÉ")
            byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x50, 0x00, 0x34, 0x80.toByte(), 0xCB.toByte())
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 📱 COMMANDES UNITÉS
    // ═══════════════════════════════════════════════════════════════

    fun setUnitKMH(): ByteArray {
        Log.i(TAG, "📏 Unité: KM/H")
        return ProtocolSimple.CMD_UNIT_KMH
    }

    fun setUnitMPH(): ByteArray {
        Log.i(TAG, "📏 Unité: MPH")
        return ProtocolSimple.CMD_UNIT_MPH
    }
}