package com.ix7.tracker.bluetooth

import android.util.Log
import kotlinx.coroutines.*

/**
 * Scanner automatique de commandes Bluetooth
 * Teste toutes les combinaisons possibles pour trouver celles qui fonctionnent
 */
class CommandScanner(
    private val sendCommand: suspend (ByteArray) -> Unit,
    private val onResponseReceived: (command: ByteArray, response: ByteArray) -> Unit
) {
    private val TAG = "CommandScanner"
    private var scanJob: Job? = null
    private val responses = mutableMapOf<String, ByteArray>()
    private var lastResponseTime = 0L
    private var currentTestLabel: String? = null

    /**
     * Lance un scan exhaustif de toutes les commandes possibles
     */
    fun startFullScan() {
        scanJob?.cancel()
        responses.clear()
        scanJob = CoroutineScope(Dispatchers.IO).launch {
            Log.i(TAG, "════════════════════════════════════════════════")
            Log.i(TAG, "🔍 DÉMARRAGE SCAN EXHAUSTIF DES COMMANDES")
            Log.i(TAG, "════════════════════════════════════════════════")

            // Phase 1: Commandes simples (1 byte de commande)
            Log.i(TAG, "\n📋 PHASE 1: Commandes simples")
            scanSimpleCommands(this)
            delay(2000)

            // Phase 2: Commandes avec sous-commande
            Log.i(TAG, "\n📋 PHASE 2: Commandes avec sous-commande")
            scanCommandsWithSubcommand(this)
            delay(2000)

            // Phase 3: Commandes avec data
            Log.i(TAG, "\n📋 PHASE 3: Commandes avec données")
            scanCommandsWithData(this)
            delay(2000)

            // Phase 4: Commandes spéciales observées
            Log.i(TAG, "\n📋 PHASE 4: Commandes observées dans les logs")
            scanKnownCommands(this)

            Log.i(TAG, "\n════════════════════════════════════════════════")
            Log.i(TAG, "✓ SCAN TERMINÉ - ${responses.size} commandes ont obtenu une réponse")
            Log.i(TAG, "════════════════════════════════════════════════")
            printResults()
        }
    }

    /**
     * Phase 1: Teste toutes les commandes simples (55 AA length command checksum)
     */
    private suspend fun scanSimpleCommands(scope: CoroutineScope) {
        val importantCommands = listOf(
            0x20, 0x21, 0x22, 0x23, 0x24, 0x25,  // Commandes de base
            0x01, 0x02, 0x03, 0x04, 0x05,        // Commandes système
            0x10, 0x11, 0x12, 0x13,              // Commandes lecture
            0x30, 0x31, 0x32, 0x33,              // Commandes écriture
            0xA0, 0xA1, 0xA2, 0xA3               // Commandes avancées
        )
        for (cmd in importantCommands) {
            if (!scope.isActive) break
            val command = byteArrayOf(
                0x55.toByte(), 0xAA.toByte(),
                0x01.toByte(),
                cmd.toByte(),
                (0x01 xor cmd).toByte()  // Checksum
            )
            testCommand(command, "Simple_${"%02X".format(cmd)}", scope)
            delay(300)
        }
    }

    /**
     * Phase 2: Teste commandes avec sous-commande
     */
    private suspend fun scanCommandsWithSubcommand(scope: CoroutineScope) {
        val commands = listOf(0x20, 0x21, 0x22, 0x23)
        val subcommands = listOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x10, 0x20)
        for (cmd in commands) {
            if (!scope.isActive) break
            for (sub in subcommands) {
                if (!scope.isActive) break
                val command = byteArrayOf(
                    0x55.toByte(), 0xAA.toByte(),
                    0x02.toByte(),
                    cmd.toByte(),
                    sub.toByte(),
                    (0x02 xor cmd xor sub).toByte()
                )
                testCommand(command, "WithSub_${"%02X".format(cmd)}_${"%02X".format(sub)}", scope)
                delay(300)
            }
        }
    }

    /**
     * Phase 3: Teste commandes avec 1 byte de données
     */
    private suspend fun scanCommandsWithData(scope: CoroutineScope) {
        val commands = listOf(0x22, 0x23)
        val subcommands = listOf(0x01, 0x02)
        val dataValues = listOf(0x00, 0x01, 0x02, 0xFF)
        for (cmd in commands) {
            if (!scope.isActive) break
            for (sub in subcommands) {
                if (!scope.isActive) break
                for (data in dataValues) {
                    if (!scope.isActive) break
                    val command = byteArrayOf(
                        0x55.toByte(), 0xAA.toByte(),
                        0x03.toByte(),
                        cmd.toByte(),
                        sub.toByte(),
                        data.toByte(),
                        (0x03 xor cmd xor sub xor data).toByte()
                    )
                    testCommand(command, "WithData_${"%02X".format(cmd)}_${"%02X".format(sub)}_${"%02X".format(data)}", scope)
                    delay(300)
                }
            }
        }
    }

    /**
     * Phase 4: Teste les commandes connues de l'app officielle
     */
    private suspend fun scanKnownCommands(scope: CoroutineScope) {
        val knownCommands = listOf(
            // Commandes observées dans les logs
            byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x03.toByte(), 0x22.toByte(), 0x01.toByte(), 0x00.toByte(), 0x20.toByte()),
            byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x02.toByte(), 0x20.toByte(), 0x22.toByte()),
            byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x02.toByte(), 0x03.toByte(), 0x01.toByte()),
            // Variantes possibles
            byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x01.toByte(), 0x20.toByte(), 0x21.toByte()),
            byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x01.toByte(), 0x21.toByte(), 0x20.toByte()),
            byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x01.toByte(), 0x22.toByte(), 0x23.toByte()),
            byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x01.toByte(), 0x23.toByte(), 0x22.toByte()),
            // Commandes wake-up possibles
            byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x01.toByte(), 0x01.toByte(), 0x00.toByte()),
            byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x02.toByte(), 0x01.toByte(), 0x01.toByte(), 0x02.toByte()),
            // Commandes init possibles
            byteArrayOf(0xF0.toByte(), 0x55.toByte(), 0xAA.toByte(), 0xA5.toByte()),
            byteArrayOf(0xAA.toByte(), 0x55.toByte()),
            byteArrayOf(0x5A.toByte(), 0xA5.toByte(), 0x00.toByte()),
            // Commandes sans header (protocole alternatif)
            byteArrayOf(0x20.toByte()),
            byteArrayOf(0x01.toByte()),
            byteArrayOf(0xFF.toByte(), 0xFF.toByte())
        )
        knownCommands.forEachIndexed { index, cmd ->
            if (!scope.isActive) return@forEachIndexed
            testCommand(cmd, "Known_${index+1}", scope)
            delay(500)
        }
    }

    /**
     * Teste une commande et attend une réponse
     */
    private suspend fun testCommand(command: ByteArray, label: String, scope: CoroutineScope) {
        try {
            currentTestLabel = label
            val hex = command.joinToString(" ") { "%02X".format(it) }
            Log.d(TAG, "→ Test [$label]: $hex")
            lastResponseTime = System.currentTimeMillis()
            sendCommand(command)
            delay(500) // Attendre une réponse (500ms max)
            currentTestLabel = null
        } catch (e: Exception) {
            Log.e(TAG, "Erreur test commande $label", e)
            currentTestLabel = null
        }
    }

    /**
     * Enregistre une réponse reçue
     */
    fun onResponseReceived(response: ByteArray) {
        lastResponseTime = System.currentTimeMillis()
        val hex = response.joinToString(" ") { "%02X".format(it) }
        Log.i(TAG, "📨 RÉPONSE REÇUE: $hex")
        currentTestLabel?.let { responses[it] = response }
    }

    /**
     * Affiche les résultats du scan
     */
    private fun printResults() {
        Log.i(TAG, "\n")
        Log.i(TAG, "═══════════════════════════════════════════════════════")
        Log.i(TAG, "📊 RÉSULTATS DU SCAN")
        Log.i(TAG, "═══════════════════════════════════════════════════════")
        if (responses.isEmpty()) {
            Log.w(TAG, "⚠ AUCUNE commande n'a obtenu de réponse")
            Log.w(TAG, "")
            Log.w(TAG, "Possibilités:")
            Log.w(TAG, "  1. Le hoverboard est en veille profonde")
            Log.w(TAG, "  2. Le protocole est différent (pas 55 AA)")
            Log.w(TAG, "  3. Les UUIDs utilisés ne sont pas corrects")
            Log.w(TAG, "  4. Le hoverboard nécessite une séquence d'init spéciale")
        } else {
            Log.i(TAG, "✓ ${responses.size} commande(s) fonctionnelle(s):")
            Log.i(TAG, "")
            responses.forEach { (label, response) ->
                val hex = response.joinToString(" ") { "%02X".format(it) }
                Log.i(TAG, "  [$label]")
                Log.i(TAG, "    Réponse: $hex")
                Log.i(TAG, "    Taille: ${response.size} bytes")
                Log.i(TAG, "")
            }
        }
        Log.i(TAG, "═══════════════════════════════════════════════════════")
    }

    /**
     * Arrête le scan
     */
    fun stop() {
        scanJob?.cancel()
        Log.i(TAG, "⏹ Scan arrêté")
    }

    /**
     * Lance un scan rapide des commandes les plus probables
     */
    fun startQuickScan() {
        scanJob?.cancel()
        responses.clear()
        scanJob = CoroutineScope(Dispatchers.IO).launch {
            Log.i(TAG, "════════════════════════════════════════════════")
            Log.i(TAG, "⚡ SCAN RAPIDE (commandes les plus probables)")
            Log.i(TAG, "════════════════════════════════════════════════")
            val quickCommands = listOf(
                // Wake-up
                byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x01.toByte(), 0x01.toByte(), 0x00.toByte()),
                // Demandes basiques
                byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x01.toByte(), 0x20.toByte(), 0x21.toByte()),
                byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x01.toByte(), 0x21.toByte(), 0x20.toByte()),
                byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x01.toByte(), 0x22.toByte(), 0x23.toByte()),
                byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x01.toByte(), 0x23.toByte(), 0x22.toByte()),
                // Avec sous-commandes
                byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x02.toByte(), 0x20.toByte(), 0x01.toByte(), 0x23.toByte()),
                byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x02.toByte(), 0x22.toByte(), 0x01.toByte(), 0x21.toByte()),
                byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x02.toByte(), 0x23.toByte(), 0x01.toByte(), 0x20.toByte()),
                // Protocoles alternatifs
                byteArrayOf(0xAA.toByte(), 0x55.toByte()),
                byteArrayOf(0x20.toByte()),
                byteArrayOf(0x01.toByte())
            )
            quickCommands.forEachIndexed { index, cmd ->
                if (!isActive) return@forEachIndexed
                testCommand(cmd, "Quick_${index+1}", this)
                delay(400)
            }
            Log.i(TAG, "════════════════════════════════════════════════")
            printResults()
        }
    }
}
