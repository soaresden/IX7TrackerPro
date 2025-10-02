package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ix7.tracker.bluetooth.BluetoothRepository
import com.ix7.tracker.bluetooth.HoverboardCommands
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tests des commandes",
            style = MaterialTheme.typography.headlineMedium
        )

        if (!isConnected) {
            Text(
                text = "❌ Connectez-vous d'abord au hoverboard",
                color = MaterialTheme.colorScheme.error
            )
            return
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Commande Toggle A", style = MaterialTheme.typography.titleMedium)
                Text("Testez pour identifier : verrouillage/lampe/régulateur",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { scope.launch { bluetoothManager.sendCommand(HoverboardCommands.TOGGLE_A) } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Envoyer TOGGLE A")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Commande Toggle B", style = MaterialTheme.typography.titleMedium)
                Text("Probablement l'opposé de A (OFF si A=ON)",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { scope.launch { bluetoothManager.sendCommand(HoverboardCommands.TOGGLE_B) } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Envoyer TOGGLE B")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Commande C", style = MaterialTheme.typography.titleMedium)
                Text("Commande unique - testez l'effet",
                    style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { scope.launch { bluetoothManager.sendCommand(HoverboardCommands.COMMAND_C) } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Envoyer COMMANDE C")
                }
            }
        }

        Text(
            text = "📝 Note ce qui se passe après chaque appui",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}