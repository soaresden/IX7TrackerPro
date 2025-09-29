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
 * Constantes pour le protocole des scooters
 */
object ProtocolConstants {

    // En-têtes de trames
    const val FRAME_HEADER_MAIN = 0x5A.toByte()
    const val FRAME_HEADER_EXTENDED = 0xA5.toByte()
    const val FRAME_HEADER_RESPONSE = 0x3A.toByte()

    // Tailles de trames
    const val MIN_FRAME_SIZE = 10
    const val MAX_FRAME_SIZE = 20

    // Commandes
    const val CMD_REQUEST_DATA = 0x01.toByte()
    const val CMD_SET_PARAMETER = 0x02.toByte()
    const val CMD_GET_VERSION = 0x03.toByte()

    // Offsets dans les trames (à ajuster selon le protocole réel)
    const val OFFSET_SPEED = 4
    const val OFFSET_BATTERY = 6
    const val OFFSET_VOLTAGE = 8
    const val OFFSET_CURRENT = 10
    const val OFFSET_TEMPERATURE = 12
    const val OFFSET_ODOMETER = 14          // AJOUTÉ : Position de l'odomètre
    const val OFFSET_TRIP = 16              // AJOUTÉ : Position du trajet
    const val OFFSET_ERROR_CODES = 18
    const val OFFSET_WARNING_CODES = 19
}

/**
 * Préfixes des noms de scooters supportés
 */
object ScooterPrefixes {
    val SUPPORTED_PREFIXES = listOf(
        "M0", "H1", "M1", "Mini", "Plus", "X1", "X3", "M6",
        "GoKart", "A6", "MI", "N3MTenbot", "miniPLUS_",
        "MiniPro", "SFSO", "V5Robot", "EO STREET", "XRIDER",
        "TECAR", "MAX", "i10", "NEXRIDE", "E-WHEELS", "E12",
        "E9PRO", "T10", "MQRobot", "KING", "oP9G", "Mentor", "E9G"
    )
}