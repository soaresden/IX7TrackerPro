package com.ix7.tracker.protocol

import android.util.Log
import com.ix7.tracker.bluetooth.BluetoothRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager de communication M0Robot
 * Centralise l'envoi de commandes et le décodage des réponses
 */
class M0RobotManager(
    private val bluetoothRepository: BluetoothRepository
) {

    private val TAG = "M0RobotManager"

    // ========== ÉTATS OBSERVABLES ==========
    private val _lastDecodedData = MutableStateFlow<M0RobotDecoder.DecodedData?>(null)
    val lastDecodedData: StateFlow<M0RobotDecoder.DecodedData?> = _lastDecodedData.asStateFlow()

    private val _isPolling = MutableStateFlow(false)
    val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()

    private val _commandHistory = MutableStateFlow<List<CommandHistoryEntry>>(emptyList())
    val commandHistory: StateFlow<List<CommandHistoryEntry>> = _commandHistory.asStateFlow()

    data class CommandHistoryEntry(
        val timestamp: Long,
        val commandName: String,
        val commandHex: String,
        val success: Boolean,
        val response: String? = null
    )

    // ========== ENUM MODES (UNE SEULE DÉCLARATION) ==========
    enum class ScooterMode {
        PEDESTRIAN,
        ECO,
        SPORT,
        RACE
    }

    // ========== ENVOI DE COMMANDES ==========

    suspend fun sendCommand(command: ByteArray, commandName: String = "CUSTOM"): Result<Unit> {
        val hex = command.joinToString(" ") { "%02X".format(it) }
        Log.d(TAG, "📤 Envoi: $commandName -> $hex")

        val result = bluetoothRepository.sendCommand(command)

        // Ajouter à l'historique
        addToHistory(
            CommandHistoryEntry(
                timestamp = System.currentTimeMillis(),
                commandName = commandName,
                commandHex = hex,
                success = result.isSuccess
            )
        )

        return result
    }

    // ========== COMMANDES PRINCIPALES ==========

    suspend fun lock(): Result<Unit> {
        return sendCommand(M0RobotCommands.LOCK, "LOCK")
    }

    suspend fun unlock(): Result<Unit> {
        return sendCommand(M0RobotCommands.UNLOCK, "UNLOCK")
    }

    suspend fun setLight(on: Boolean): Result<Unit> {
        val command = if (on) M0RobotCommands.LIGHT_ON else M0RobotCommands.LIGHT_OFF
        val name = if (on) "LIGHT_ON" else "LIGHT_OFF"
        return sendCommand(command, name)
    }

    suspend fun setMode(mode: ScooterMode): Result<Unit> {
        val (command, name) = when (mode) {
            ScooterMode.PEDESTRIAN -> M0RobotCommands.MODE_PEDESTRIAN to "MODE_PEDESTRIAN"
            ScooterMode.ECO -> M0RobotCommands.MODE_ECO to "MODE_ECO"
            ScooterMode.SPORT -> M0RobotCommands.MODE_SPORT to "MODE_SPORT"
            ScooterMode.RACE -> M0RobotCommands.MODE_RACE to "MODE_RACE"
        }
        return sendCommand(command, name)
    }

    // ========== COMMANDES EXPÉRIMENTALES ==========

    suspend fun testNeon(on: Boolean): Result<Unit> {
        val command = if (on) M0RobotCommands.NEON_ON else M0RobotCommands.NEON_OFF
        val name = if (on) "NEON_ON" else "NEON_OFF"
        return sendCommand(command, name)
    }

    suspend fun testRegulator(on: Boolean): Result<Unit> {
        val command = if (on) M0RobotCommands.CRUISE_ON else M0RobotCommands.CRUISE_OFF
        val name = if (on) "CRUISE_ON" else "CRUISE_OFF"
        return sendCommand(command, name)
    }

    suspend fun sendKeepAlive(): Result<Unit> {
        return sendCommand(M0RobotCommands.KEEP_ALIVE, "KEEP_ALIVE")
    }

    suspend fun requestStatus(): Result<Unit> {
        return sendCommand(M0RobotCommands.STATUS_REQUEST_1, "STATUS_REQUEST")
    }

    // ========== RÉCEPTION ET DÉCODAGE ==========

    fun processIncomingData(data: ByteArray) {
        val decoded = M0RobotDecoder.decode(data)
        _lastDecodedData.value = decoded

        Log.d(TAG, "📥 Reçu: ${decoded.type} - ${decoded.hexString}")

        // Log des données importantes
        decoded.speed?.let { Log.d(TAG, "  Vitesse: $it km/h") }
        decoded.battery?.let { Log.d(TAG, "  Batterie: $it%") }
        decoded.voltage?.let { Log.d(TAG, "  Tension: $it V") }
        decoded.temperature?.let { Log.d(TAG, "  Température: $it°C") }
        decoded.mode?.let { Log.d(TAG, "  Mode: $it") }

        // Mettre à jour la dernière commande si c'est une réponse
        if (decoded.type == M0RobotDecoder.ResponseType.ACKNOWLEDGE) {
            updateLastCommandResponse(decoded.hexString)
        }
    }

    // ========== POLLING AUTOMATIQUE ==========

    suspend fun startPolling(intervalMs: Long = 1000) {
        _isPolling.value = true

        while (_isPolling.value) {
            requestStatus()
            kotlinx.coroutines.delay(intervalMs)
        }
    }

    fun stopPolling() {
        _isPolling.value = false
    }

    // ========== BUILDER DE COMMANDES PERSONNALISÉES ==========

    fun buildCustomCommand(
        type: Byte = 0x30,
        length: Byte = 0x14,
        subCommand: Byte = 0x37,
        commandCode: Byte,
        value: Byte = 0x34,
        extra: Byte = 0x34
    ): ByteArray {
        val cmd = mutableListOf<Byte>()
        cmd.add(0x61)
        cmd.add(0x9E.toByte())
        cmd.add(type)
        cmd.add(length)
        cmd.add(subCommand)
        cmd.add(commandCode)
        cmd.add(value)
        cmd.add(extra)

        // Calculer le checksum (XOR simple)
        var checksum: Byte = 0
        for (i in 2 until cmd.size) {
            checksum = (checksum.toInt() xor cmd[i].toInt()).toByte()
        }
        cmd.add(checksum)

        // Ajouter le byte final (CA ou CB selon le pattern)
        cmd.add(if (commandCode.toInt() and 0x80 != 0) 0xCA.toByte() else 0xCB.toByte())

        return cmd.toByteArray()
    }

    // ========== UTILITAIRES ==========

    private fun addToHistory(entry: CommandHistoryEntry) {
        val current = _commandHistory.value.toMutableList()
        current.add(0, entry) // Ajouter au début

        // Garder seulement les 50 dernières commandes
        if (current.size > 50) {
            current.removeAt(current.size - 1)
        }

        _commandHistory.value = current
    }

    private fun updateLastCommandResponse(response: String) {
        val current = _commandHistory.value.toMutableList()
        if (current.isNotEmpty()) {
            current[0] = current[0].copy(response = response)
            _commandHistory.value = current
        }
    }

    fun clearHistory() {
        _commandHistory.value = emptyList()
    }

    fun analyzeLastResponse(): String {
        val data = _lastDecodedData.value ?: return "Aucune donnée"

        return buildString {
            appendLine("=== ANALYSE DE LA DERNIÈRE RÉPONSE ===")
            appendLine("Type: ${data.type}")
            appendLine("Hex: ${data.hexString}")
            appendLine("Pattern: ${M0RobotDecoder.analyzePattern(data.rawData)}")
            appendLine()

            data.speed?.let { appendLine("Vitesse: $it km/h") }
            data.battery?.let { appendLine("Batterie: $it%") }
            data.voltage?.let { appendLine("Tension: $it V") }
            data.current?.let { appendLine("Courant: $it A") }
            data.totalDistance?.let { appendLine("Distance totale: $it km") }
            data.temperature?.let { appendLine("Température: $it°C") }
            data.mode?.let { appendLine("Mode: $it") }
            data.isLocked?.let { appendLine("Verrouillé: $it") }
            data.lightOn?.let { appendLine("Phare: $it") }
            data.errorCode?.let { appendLine("Code erreur: $it") }

            if (data.parseErrors.isNotEmpty()) {
                appendLine()
                appendLine("⚠️ Erreurs de parsing:")
                data.parseErrors.forEach { appendLine("  - $it") }
            }
        }
    }
}