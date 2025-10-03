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
        Text("Commandes non identifiées", style = MaterialTheme.typography.headlineMedium)

        if (!isConnected) {
            Text("Connectez-vous d'abord", color = MaterialTheme.colorScheme.error)
            return
        }

        // C7 (font bip)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("C7 Series (font bip)", style = MaterialTheme.typography.titleMedium)
                Text("Probablement: Klaxon, Alarme ou autre", style = MaterialTheme.typography.bodySmall)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TestButton(scope, bluetoothManager, "C7-1", byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x74, 0x2B, 0xB2.toByte(), 0xCA.toByte()))
                    TestButton(scope, bluetoothManager, "C7-2", byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0xD4.toByte(), 0x1A, 0xE3.toByte(), 0xC9.toByte()))
                    TestButton(scope, bluetoothManager, "C7-3", byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0xB4.toByte(), 0x0A, 0x13, 0xCA.toByte()))
                    TestButton(scope, bluetoothManager, "C7-4", byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0xA4.toByte(), 0x51, 0xC4.toByte(), 0xC9.toByte()))
                    TestButton(scope, bluetoothManager, "C7-5", byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0xC7.toByte(), 0x04, 0x41, 0x74, 0xCA.toByte()))
                }
            }
        }

        // 49 (supposé néon mais fait bip)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("49 Series (supposé néon)", style = MaterialTheme.typography.titleMedium)
                Text("Font bip mais effet inconnu", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TestButton(scope, bluetoothManager, "49-1", byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x49, 0x35, 0x34, 0x6E, 0xCB.toByte()), Modifier.weight(1f))
                    TestButton(scope, bluetoothManager, "49-2", byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x49, 0x34, 0x34, 0x6F, 0xCB.toByte()), Modifier.weight(1f))
                }
            }
        }

        // DB (ne fait rien)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Toggle DB (inactif)", style = MaterialTheme.typography.titleMedium)
                TestButton(scope, bluetoothManager, "Toggle DB", byteArrayOf(0x61, 0x9E.toByte(), 0x37, 0x14, 0x55, 0xDB.toByte(), 0x3E, 0xB6.toByte(), 0xCA.toByte()))
            }
        }

        // Régulateur (pas confirmé)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("48 Series (régulateur ?)", style = MaterialTheme.typography.titleMedium)
                Text("À confirmer", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TestButton(scope, bluetoothManager, "48-1 (ON?)", byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x35, 0x34, 0x6F, 0xCB.toByte()), Modifier.weight(1f))
                    TestButton(scope, bluetoothManager, "48-2 (OFF?)", byteArrayOf(0x61, 0x9E.toByte(), 0x30, 0x14, 0x37, 0x48, 0x34, 0x34, 0x68, 0xCB.toByte()), Modifier.weight(1f))
                }
            }
        }

        Text(
            "Appuie sur chaque bouton et note l'effet sur la trottinette",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
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
                Log.e("BOUTON_TEST", "🔘 $label - $hex")
                bluetoothManager.sendCommand(command)
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Text(label)
    }
}