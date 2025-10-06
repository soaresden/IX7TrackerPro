package com.ix7.tracker.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ix7.tracker.bluetooth.BluetoothRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun TestScreen(
    bluetoothManager: BluetoothRepository,
    isConnected: Boolean
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🧪 TEST MANUEL DES COMMANDES", style = MaterialTheme.typography.headlineMedium)

        if (!isConnected) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    "❌ Non connecté - Connectez-vous d'abord",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
            return
        }

        // ========== MODES DE CONDUITE ==========
        SectionCard("🏍️ MODES DE CONDUITE") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, bluetoothManager, "PIÉTON",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x37, 0x34, 0x63, 0xCB.toByte()),
                    Modifier.weight(1f))
                TestButton(scope, bluetoothManager, "ECO",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x36, 0x34, 0x6C, 0xCB.toByte()),
                    Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, bluetoothManager, "SPORT",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x34, 0x34, 0x6E, 0xCB.toByte()),
                    Modifier.weight(1f))
                TestButton(scope, bluetoothManager, "RACE",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4A, 0x35, 0x34, 0x6D, 0xCB.toByte()),
                    Modifier.weight(1f))
            }
        }

        // ========== VERROUILLAGE ==========
        SectionCard("🔒 VERROUILLAGE") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, bluetoothManager, "VERROUILLER",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4B, 0x35, 0x34, 0x6C, 0xCB.toByte()),
                    Modifier.weight(1f))
                TestButton(scope, bluetoothManager, "DÉVERROUILLER",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x4B, 0x34, 0x34, 0x6D, 0xCB.toByte()),
                    Modifier.weight(1f))
            }
        }

        // ========== LUMIÈRES ==========
        SectionCard("💡 LUMIÈRES") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, bluetoothManager, "ALLUMER",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC6.toByte(), 0x35, 0x34, 0xD1.toByte(), 0xCA.toByte()),
                    Modifier.weight(1f))
                TestButton(scope, bluetoothManager, "ÉTEINDRE",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC6.toByte(), 0x34, 0x34, 0xD2.toByte(), 0xCA.toByte()),
                    Modifier.weight(1f))
            }
        }

        // ========== NÉON (49 Series) ==========
        SectionCard("🎨 NÉON (49 Series)") {
            Text("⚠️ Font bip - Effet à confirmer", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, bluetoothManager, "49-1",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x49, 0x35, 0x34, 0x6E, 0xCB.toByte()),
                    Modifier.weight(1f))
                TestButton(scope, bluetoothManager, "49-2",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x49, 0x34, 0x34, 0x6F, 0xCB.toByte()),
                    Modifier.weight(1f))
            }
        }

        // ========== RÉGULATEUR (48 Series) ==========
        SectionCard("⚙️ RÉGULATEUR (48 Series)") {
            Text("À confirmer", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, bluetoothManager, "48-ON?",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x35, 0x34, 0x6F, 0xCB.toByte()),
                    Modifier.weight(1f))
                TestButton(scope, bluetoothManager, "48-OFF?",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x34, 0x34, 0x68, 0xCB.toByte()),
                    Modifier.weight(1f))
            }
        }

        // ========== UNITÉS ==========
        SectionCard("📏 UNITÉS") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, bluetoothManager, "KM/H",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x15, 0x37, 0x2F, 0x34, 0x34, 0x88.toByte(), 0xCB.toByte()),
                    Modifier.weight(1f))
                TestButton(scope, bluetoothManager, "MPH",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x15, 0x37, 0x2F, 0x35, 0x34, 0x8F.toByte(), 0xCB.toByte()),
                    Modifier.weight(1f))
            }
        }

        // ========== KLAXON (C7 Series) ==========
        SectionCard("🔊 C7 Series (Klaxon/Bip)") {
            Text("Font tous bip - Probablement klaxon ou alarme", style = MaterialTheme.typography.bodySmall)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, bluetoothManager, "C7-1",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x74, 0x2B, 0xB2.toByte(), 0xCA.toByte()))
                TestButton(scope, bluetoothManager, "C7-2",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0xD4.toByte(), 0x1A, 0xE3.toByte(), 0xC9.toByte()))
                TestButton(scope, bluetoothManager, "C7-3",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0xB4.toByte(), 0x0A, 0x13, 0xCA.toByte()))
                TestButton(scope, bluetoothManager, "C7-4",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0xA4.toByte(), 0x51, 0xC4.toByte(), 0xC9.toByte()))
                TestButton(scope, bluetoothManager, "C7-5",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x04, 0x41, 0x74, 0xCA.toByte()))
            }
        }

        // ========== DB (inactif) ==========
        SectionCard("🔧 DB (Inactif)") {
            TestButton(scope, bluetoothManager, "Toggle DB",
                byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xDB.toByte(), 0x3E, 0xB6.toByte(), 0xCA.toByte()))
        }

        // ========== KEEP ALIVE & STATUS ==========
        SectionCard("🔄 STATUS & KEEP-ALIVE") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, bluetoothManager, "Keep-Alive",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xDE.toByte(), 0x3C, 0xBD.toByte(), 0xCA.toByte()))
                TestButton(scope, bluetoothManager, "Status 53",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x53, 0x2E, 0xE0.toByte(), 0x19, 0xCB.toByte()))
                TestButton(scope, bluetoothManager, "Status 9E",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x15, 0x9E.toByte(), 0xD3.toByte(), 0x5A, 0x8E.toByte(), 0xCB.toByte()))
            }
        }

        // ========== COMMANDES MYSTÈRE ==========
        SectionCard("❓ COMMANDES MYSTÈRE") {
            Text("Commandes non identifiées - À tester", style = MaterialTheme.typography.bodySmall)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TestButton(scope, bluetoothManager, "Mystery 1",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x17, 0x35, 0xC2.toByte(), 0x34, 0x34, 0x00, 0xCA.toByte()))
                TestButton(scope, bluetoothManager, "Mystery 2",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x17, 0x35, 0xC1.toByte(), 0x34, 0x34, 0x00, 0xCA.toByte()))
                TestButton(scope, bluetoothManager, "Mystery 3",
                    byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xDA.toByte(), 0x3E, 0xB7.toByte(), 0xCA.toByte()))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("📝 INSTRUCTIONS", style = MaterialTheme.typography.titleMedium)
                Text(
                    """
                    1. Appuie sur chaque bouton
                    2. Note l'effet sur la trottinette
                    3. Regarde les logs dans l'onglet Logs
                    4. Partage-moi tes découvertes
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun TestButton(
    scope: CoroutineScope,
    bluetoothManager: BluetoothRepository,
    label: String,
    command: ByteArray,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            scope.launch {
                val hex = command.joinToString(" ") { "%02X".format(it) }
                Log.e("TEST_BUTTON", "🔘 $label")
                Log.e("TEST_BUTTON", "📤 $hex")
                bluetoothManager.sendCommand(command)
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Text(label)
    }
}