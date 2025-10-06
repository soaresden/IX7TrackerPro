package com.ix7.tracker.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.bluetooth.BluetoothRepository
import com.ix7.tracker.protocol.ProtocolConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun TestScreen(
    bluetoothManager: BluetoothRepository,
    isConnected: Boolean
) {
    val scope = rememberCoroutineScope()
    val connector = bluetoothManager.connector

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "🧪 INTERFACE TEST COMPLÈTE",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        if (!isConnected) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    "⚠️ Connecte-toi d'abord à la trottinette",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
            return
        }

        // ═══════════════════════════════════════════════════════════════════
        // ✅ SECTION 1: COMMANDES CONFIRMÉES (TU CONNAIS DÉJÀ)
        // ═══════════════════════════════════════════════════════════════════

        TestSection(
            title = "✅ LUMIÈRES (CONFIRMÉES)",
            subtitle = "C6-35 = ON | C6-34 = OFF"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(
                    scope, connector, "💡 LUMIÈRES ON",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC6.toByte(), 0x35, 0x34, 0xD1.toByte(), 0xCA.toByte()),
                    Modifier.weight(1f)
                )
                TestButton(
                    scope, connector, "💡 LUMIÈRES OFF",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC6.toByte(), 0x34, 0x34, 0xD2.toByte(), 0xCA.toByte()),
                    Modifier.weight(1f)
                )
            }
        }

        TestSection(
            title = "✅ MODES CONFIRMÉS",
            subtitle = "4A-36=SPORT | 4A-35=RACE (verrouille!) | 48=fait bruit"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(
                    scope, connector, "🏃 MODE SPORT (4A-36)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x36, 0x34, 0x6C, 0xCB.toByte())
                )
                TestButton(
                    scope, connector, "🔊 48-35 (fait bruit)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x35, 0x34, 0x6F, 0xCB.toByte())
                )
                Text(
                    "⚠️ NE PAS TESTER 4A-35 (RACE) - IL VERROUILLE LA TROTTINETTE !",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        TestSection(
            title = "✅ SONS/BIPS CONFIRMÉS",
            subtitle = "49 et toutes les C7 font bip"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, connector, "🔊 49-35 (bip)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x49, 0x35, 0x34, 0x6E, 0xCB.toByte()))
                TestButton(scope, connector, "🔊 49-34 (bip)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x49, 0x34, 0x34, 0x6F, 0xCB.toByte()))
                TestButton(scope, connector, "🔊 C7-74-2B",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x74, 0x2B, 0xB2.toByte(), 0xCA.toByte()))
                TestButton(scope, connector, "🔊 C7-D4-1A",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0xD4.toByte(), 0x1A, 0xE3.toByte(), 0xC9.toByte()))
                TestButton(scope, connector, "🔊 C7-B4-0A",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0xB4.toByte(), 0x0A, 0x13, 0xCA.toByte()))
                TestButton(scope, connector, "🔊 C7-A4-51",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0xA4.toByte(), 0x51, 0xC4.toByte(), 0xC9.toByte()))
                TestButton(scope, connector, "🔊 C7-04-41",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x04, 0x41, 0x74, 0xCA.toByte()))
                TestButton(scope, connector, "🔊 C7-BC-27",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0xBC.toByte(), 0x27, 0x7E, 0xCA.toByte()))
            }
        }

        TestSection(
            title = "✅ RÉGULATEUR (48 avec variations)",
            subtitle = "48-35 et 48-36 font bip"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, connector, "🎯 48-35 (bip)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x35, 0x34, 0x6F, 0xCB.toByte()),
                    Modifier.weight(1f))
                TestButton(scope, connector, "🎯 48-36 (bip)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x36, 0x34, 0x6E, 0xCB.toByte()),
                    Modifier.weight(1f))
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // ❌ SECTION 2: COMMANDES INACTIVES (TU SAIS QU'ELLES NE MARCHENT PAS)
        // ═══════════════════════════════════════════════════════════════════

        TestSection(
            title = "❌ COMMANDES INACTIVES",
            subtitle = "Tu as déjà testé - ne font rien"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, connector, "❌ 4A seul (inactif)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x34, 0x34, 0x6E, 0xCB.toByte()))
                TestButton(scope, connector, "❌ 6A (inactif)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0x6A, 0x06, 0xDF.toByte(), 0xCA.toByte()))
                TestButton(scope, connector, "❌ 8F (inactif)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0x8F.toByte(), 0x32, 0x8E.toByte(), 0xCA.toByte()))
                TestButton(scope, connector, "❌ 72 (inactif)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0x72, 0x06, 0x37, 0xCB.toByte()))
                TestButton(scope, connector, "❌ DB (inactif)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xDB.toByte(), 0x3E, 0xB6.toByte(), 0xCA.toByte()))
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // 🔮 SECTION 3: NOUVEAUX BOUTONS À TESTER (IMAGINABLES)
        // ═══════════════════════════════════════════════════════════════════

        TestSection(
            title = "🔮 NÉON (À DÉCOUVRIR)",
            subtitle = "Variations possibles du pattern C6"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, connector, "🟣 C5-35 (néon ON?)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC5.toByte(), 0x35, 0x37, 0x79, 0xCA.toByte()))
                TestButton(scope, connector, "🟣 C5-34 (néon OFF?)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC5.toByte(), 0x34, 0x37, 0x78, 0xCA.toByte()))
                TestButton(scope, connector, "🟣 C8-35",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC8.toByte(), 0x35, 0x37, 0x7F, 0xCA.toByte()))
                TestButton(scope, connector, "🟣 C8-34",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC8.toByte(), 0x34, 0x37, 0x7E, 0xCA.toByte()))
            }
        }

        TestSection(
            title = "🔮 MODES SUPPLÉMENTAIRES",
            subtitle = "À trouver: MODE ECO, MODE PIÉTON"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "⚠️ NE PAS TESTER 4B - IL VERROUILLE !",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                TestButton(scope, connector, "🚶 49-35 (piéton?)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x49, 0x35, 0x34, 0x6E, 0xCB.toByte()))
                TestButton(scope, connector, "🚶 49-36",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x49, 0x36, 0x34, 0x6F, 0xCB.toByte()))
                TestButton(scope, connector, "🏎️ 4C-36 (mode power?)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4C, 0x36, 0x34, 0x6A, 0xCB.toByte()))
                TestButton(scope, connector, "🏎️ 4C-35",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4C, 0x35, 0x34, 0x6B, 0xCB.toByte()))
                TestButton(scope, connector, "🌱 4D-36 (eco?)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4D, 0x36, 0x34, 0x69, 0xCB.toByte()))
                TestButton(scope, connector, "🌱 4D-35 (eco?)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4D, 0x35, 0x34, 0x68, 0xCB.toByte()))
            }
        }

        TestSection(
            title = "🔮 VERROUILLAGE/SÉCURITÉ",
            subtitle = "Lock/Unlock patterns possibles"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, connector, "🔒 C0-35 (lock?)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC0.toByte(), 0x35, 0x37, 0x73, 0xCA.toByte()))
                TestButton(scope, connector, "🔓 C0-34 (unlock?)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC0.toByte(), 0x34, 0x37, 0x72, 0xCA.toByte()))
                TestButton(scope, connector, "🔐 C1-35",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC1.toByte(), 0x35, 0x37, 0x74, 0xCA.toByte()))
                TestButton(scope, connector, "🔐 C1-34",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC1.toByte(), 0x34, 0x37, 0x75, 0xCA.toByte()))
            }
        }

        TestSection(
            title = "🔮 ROUES (1 vs 2 roues)",
            subtitle = "Basé sur le pattern 0x30"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, connector, "🛴 1 roue (50-35)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x50, 0x35, 0x34, 0x63, 0xCB.toByte()),
                    Modifier.weight(1f))
                TestButton(scope, connector, "🏍️ 2 roues (50-36)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x50, 0x36, 0x34, 0x62, 0xCB.toByte()),
                    Modifier.weight(1f))
            }
        }

        TestSection(
            title = "🔮 VARIATIONS C7 (Différents sons)",
            subtitle = "Plus de combinaisons C7 possibles"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, connector, "🔊 C7-35-35 (klaxon?)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x35, 0x35, 0xDD.toByte(), 0xCB.toByte()))
                TestButton(scope, connector, "🔊 C7-34-34 (alarme?)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x34, 0x34, 0xDE.toByte(), 0xCA.toByte()))
                TestButton(scope, connector, "🔊 C7-FF-FF",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x0F, 0xCA.toByte()))
                TestButton(scope, connector, "🔊 C7-00-00",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x00, 0x00, 0xF0.toByte(), 0xCB.toByte()))
            }
        }

        TestSection(
            title = "🔮 DÉBRIDAGE/LIMITES",
            subtitle = "Unlock speed limiter patterns"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, connector, "⚡ D0-35 (unlock?)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xD0.toByte(), 0x35, 0x37, 0x87.toByte(), 0xCA.toByte()))
                TestButton(scope, connector, "⚡ D0-34 (lock?)",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xD0.toByte(), 0x34, 0x37, 0x86.toByte(), 0xCA.toByte()))
                TestButton(scope, connector, "⚡ DD-35",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xDD.toByte(), 0x35, 0x37, 0x9A.toByte(), 0xCA.toByte()))
                TestButton(scope, connector, "⚡ DD-34",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xDD.toByte(), 0x34, 0x37, 0x9B.toByte(), 0xCA.toByte()))
            }
        }

        TestSection(
            title = "🔮 AUTRES PATTERNS (Type 0x37)",
            subtitle = "Commandes de type différent"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, connector, "🎲 A0",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xA0.toByte(), 0x00, 0xF5.toByte(), 0xCB.toByte()))
                TestButton(scope, connector, "🎲 B0",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xB0.toByte(), 0x00, 0xE5.toByte(), 0xCB.toByte()))
                TestButton(scope, connector, "🎲 E0",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xE0.toByte(), 0x00, 0xB5.toByte(), 0xCB.toByte()))
                TestButton(scope, connector, "🎲 F0",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xF0.toByte(), 0x00, 0xA5.toByte(), 0xCB.toByte()))
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // 📡 SECTION 4: REQUÊTES (POLLING/INFO)
        // ═══════════════════════════════════════════════════════════════════

        TestSection(
            title = "📡 REQUÊTES D'INFORMATIONS",
            subtitle = "Pour récupérer des données"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, connector, "🔄 Keep-Alive",
                    ProtocolConstants.CMD_KEEP_ALIVE)
                TestButton(scope, connector, "📊 Request 1",
                    ProtocolConstants.CMD_REQUEST_1)
                TestButton(scope, connector, "📊 Request 2",
                    ProtocolConstants.CMD_REQUEST_2)
                TestButton(scope, connector, "📊 Request 3",
                    ProtocolConstants.CMD_REQUEST_3)
                TestButton(scope, connector, "📊 Request 4",
                    ProtocolConstants.CMD_REQUEST_4)
                TestButton(scope, connector, "📊 Request 5",
                    ProtocolConstants.CMD_REQUEST_5)
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // 📝 INSTRUCTIONS
        // ═══════════════════════════════════════════════════════════════════

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "📋 INSTRUCTIONS DE TEST",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    """
                    ✅ DÉCOUVERTES CONFIRMÉES:
                    • C6-35/34 = Lumières ON/OFF ✓
                    • 4A-36 = Mode SPORT ✓
                    • 4A-35 = Mode RACE (verrouille!) ⚠️
                    • 48-35 = Fait du bruit (régulateur?) ✓
                    • 49 = Fait bip ✓
                    • 48-35/36 = Font bip ✓
                    • Toutes les C7 = Sons/Bips ✓
                    
                    ❌ INACTIFS CONFIRMÉS:
                    • 4A seul, 6A, 8F, 72, DB
                    
                    🔮 À TESTER:
                    • Sections "À DÉCOUVRIR" ci-dessus
                    • Mode ECO (peut-être 49 ou 4D?)
                    
                    📝 MÉTHODE:
                    1. Teste UN bouton à la fois
                    2. Note l'effet sur la trottinette
                    3. Vérifie Logcat (filtre: BOUTON_TEST)
                    4. Fais-moi un retour avec ce qui marche!
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TestSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            content()
        }
    }
}

@Composable
private fun TestButton(
    scope: CoroutineScope,
    connector: com.ix7.tracker.bluetooth.BluetoothConnector?,
    label: String,
    command: ByteArray,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            scope.launch {
                val hex = command.joinToString(" ") { "%02X".format(it) }
                Log.e("BOUTON_TEST", "$label")
                Log.e("BOUTON_TEST", "   HEX: $hex")
                connector?.sendCommand(command)
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Text(label)
    }
}