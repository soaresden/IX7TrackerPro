package com.ix7.tracker.core

import java.util.*

/**
 * Constantes pour la communication Bluetooth
 */
object BluetoothConstants {

    // UUIDs standard
    const val CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb"

    // Timeouts
    const val SCAN_TIMEOUT_MS = 30000L
    const val CONNECTION_TIMEOUT_MS = 10000L
    const val RECONNECTION_DELAY_MS = 2000L

    // Actions Intent
    const val ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED"
    const val ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED"
    const val ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED"
    const val ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE"
    const val EXTRA_DATA = "EXTRA_DATA"

    // États de connexion
    const val STATE_DISCONNECTED = 0
    const val STATE_CONNECTING = 1
    const val STATE_CONNECTED = 2
}

/**
 * Constantes pour le protocole des hoverboards
 * CORRIGÉ basé sur l'analyse de l'application officielle (protocole 55 AA)
 */
object ProtocolConstants {

    // ===== EN-TÊTES DE TRAMES CORRIGÉS =====
    // L'application officielle utilise 55 AA (pas 5A A5 !)
    const val FRAME_HEADER_1 = 0x55.toByte()  // Premier byte: 0x55
    const val FRAME_HEADER_2 = 0xAA.toByte()  // Second byte: 0xAA

    // Ancien headers (INCORRECTS pour ce modèle)
    // const val FRAME_HEADER_MAIN = 0x5A.toByte() // ← FAUX
    // const val FRAME_HEADER_EXTENDED = 0xA5.toByte() // ← FAUX

    // Tailles de trames observées dans les logs
    const val MIN_FRAME_SIZE = 5        // Minimum observé
    const val MAX_FRAME_SIZE = 60       // Maximum observé (jusqu'à 60 bytes)
    const val SHORT_FRAME_SIZE = 10     // Trame courte typique
    const val LONG_FRAME_SIZE = 60      // Trame longue avec padding

    // Types de commandes observés
    const val CMD_REQUEST_DATA = 0x22.toByte()      // Demande données
    const val CMD_STATUS = 0x20.toByte()            // Demande statut
    const val CMD_SET_PARAMETER = 0x02.toByte()     // Modifier paramètre
    const val CMD_KEEP_ALIVE = 0x00.toByte()        // Keep-alive
    const val CMD_GET_VERSION = 0x03.toByte()       // Version firmware

    // ===== STRUCTURE DES TRAMES =====
    // Basé sur l'analyse des logs:
    // Offset 0-1: Header (55 AA)
    // Offset 2: Length
    // Offset 3: Command
    // Offset 4: SubCommand
    // Offset 5+: Data
    // Offset N-1: Checksum

    const val OFFSET_HEADER_1 = 0
    const val OFFSET_HEADER_2 = 1
    const val OFFSET_LENGTH = 2
    const val OFFSET_COMMAND = 3
    const val OFFSET_SUBCOMMAND = 4
    const val OFFSET_DATA_START = 5

    // Offsets dans les trames de données (à ajuster selon tests réels)
    const val OFFSET_SPEED = 6
    const val OFFSET_BATTERY = 8
    const val OFFSET_VOLTAGE = 10
    const val OFFSET_CURRENT = 12
    const val OFFSET_TEMPERATURE = 14
    const val OFFSET_ERROR_CODES = 16
    const val OFFSET_WARNING_CODES = 18

    // ===== VALEURS OBSERVÉES =====
    // Ces valeurs ont été observées dans les logs de l'app officielle
    const val RESPONSE_ID1 = 0x7E.toByte()  // Identifiant SET4_ID1
    const val RESPONSE_ID2 = 0x03.toByte()  // Identifiant SET4_ID2

    // Mode types observés
    const val MODE_TYPE_2 = 2  // Type:2 typeLow:2 typeHide:0
}

/**
 * Préfixes des noms de scooters supportés
 * Note: Le hoverboard testé semble utiliser "Loby Balance Car" dans l'app officielle
 */
object ScooterPrefixes {
    val SUPPORTED_PREFIXES = listOf(
        // Hoverboards/Balance cars
        "Loby", "Balance", "Car",

        // Scooters M0Robot
        "M0", "H1", "M1", "Mini", "Plus", "X1", "X3", "M6",
        "GoKart", "A6", "MI", "N3MTenbot", "miniPLUS_",
        "MiniPro", "SFSO", "V5Robot", "EO STREET", "XRIDER",
        "TECAR", "MAX", "i10", "NEXRIDE", "E-WHEELS", "E12",
        "E9PRO", "T10", "MQRobot", "KING", "oP9G", "Mentor", "E9G"
    )

    /**
     * Vérifie si un nom de device correspond aux préfixes supportés
     */
    fun isSupported(deviceName: String?): Boolean {
        if (deviceName.isNullOrBlank()) return false
        return SUPPORTED_PREFIXES.any { prefix ->
            deviceName.contains(prefix, ignoreCase = true)
        }
    }
}

/**
 * Types de scooters supportés avec leurs UUIDs
 * CORRIGÉ: Le type FFE0/FFE1 est le bon pour le hoverboard testé
 */
enum class ScooterType(
    val uuid_service: String,
    val uuid_notify: String,
    val uuid_write: String,
    val description: String
) {
    // ===== TYPE PRINCIPAL (HOVERBOARD TESTÉ) =====
    TYPE_FFE0(
        "0000ffe0-0000-1000-8000-00805f9b34fb",
        "0000ffe1-0000-1000-8000-00805f9b34fb",
        "0000ffe1-0000-1000-8000-00805f9b34fb",
        "Hoverboard standard (FFE0/FFE1) - Protocole 55 AA"
    ),

    // Autres types (pour référence, non testés)
    TYPE_NORDIC(
        "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
        "6e400002-b5a3-f393-e0a9-e50e24dcca9e",
        "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
        "Type Nordic UART"
    ),

    TYPE_AE00(
        "0000ae00-0000-1000-8000-00805f9b34fb",
        "0000ae01-0000-1000-8000-00805f9b34fb",
        "0000ae02-0000-1000-8000-00805f9b34fb",
        "Type AE00"
    ),

    TYPE_FFF0(
        "0000fff0-0000-1000-8000-00805f9b34fb",
        "0000fff3-0000-1000-8000-00805f9b34fb",
        "0000fff7-0000-1000-8000-00805f9b34fb",
        "Type FFF0"
    ),

    UNKNOWN("", "", "", "Inconnu")
}

/**
 * Exemples de commandes pour tests
 */
object TestCommands {
    // Commandes observées dans les logs
    val REQUEST_DATA = byteArrayOf(
        0x55.toByte(), 0xAA.toByte(),  // Header
        0x03.toByte(),                  // Length
        0x22.toByte(),                  // Command
        0x01.toByte(),                  // SubCommand
        0x00.toByte(),                  // Data
        0x26.toByte()                   // Checksum
    )

    val REQUEST_STATUS = byteArrayOf(
        0x55.toByte(), 0xAA.toByte(),  // Header
        0x02.toByte(),                  // Length
        0x20.toByte(),                  // Command
        0x22.toByte()                   // Checksum
    )

    val KEEP_ALIVE = byteArrayOf(
        0x55.toByte(), 0xAA.toByte(),  // Header
        0x01.toByte(),                  // Length
        0x00.toByte(),                  // Command
        0x56.toByte()                   // Checksum
    )
}

/**
 * Utilitaires pour le protocole
 */
object ProtocolUtils {
    /**
     * Calcule le checksum XOR (basé sur les logs: "XOR=0")
     */
    fun calculateChecksum(data: ByteArray, startIndex: Int = 2, endIndex: Int = data.size - 1): Byte {
        var xor: Byte = 0
        for (i in startIndex until endIndex) {
            xor = (xor.toInt() xor data[i].toInt()).toByte()
        }
        return xor
    }

    /**
     * Vérifie si une trame a le bon header 55 AA
     */
    fun isValidFrame(data: ByteArray): Boolean {
        return data.size >= 3 &&
                data[0] == ProtocolConstants.FRAME_HEADER_1 &&
                data[1] == ProtocolConstants.FRAME_HEADER_2
    }

    /**
     * Construit une commande avec header et checksum
     */
    fun buildCommand(command: Byte, subCommand: Byte, payload: ByteArray = byteArrayOf()): ByteArray {
        val length = (1 + payload.size).toByte() // command + payload
        val frame = byteArrayOf(
            ProtocolConstants.FRAME_HEADER_1,
            ProtocolConstants.FRAME_HEADER_2,
            length,
            command
        ) + payload

        val checksum = calculateChecksum(frame)
        return frame + checksum
    }
}