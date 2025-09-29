package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.bluetooth.BluetoothRepository
import com.ix7.tracker.core.BluetoothDeviceInfo
import com.ix7.tracker.core.ConnectionState
import com.ix7.tracker.ui.components.DeviceCard
import com.ix7.tracker.ui.components.StatusCard
import kotlinx.coroutines.launch

@Composable
fun ConnectionScreen(
    bluetoothManager: BluetoothRepository,
    discoveredDevices: List<BluetoothDeviceInfo>,
    connectionState: ConnectionState,
    isScanning: Boolean
) {
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status de connexion
        item {
            StatusCard(connectionState = connectionState)
        }

        // Boutons de contrôle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bouton de scan
                Button(
                    onClick = {
                        scope.launch {
                            if (isScanning) {
                                bluetoothManager.stopScan()
                            } else {
                                bluetoothManager.startScan()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = connectionState != ConnectionState.CONNECTING
                ) {
                    Text(
                        text = "•",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isScanning) "Arrêter" else "Scanner")
                }

                // Bouton de déconnexion (si connecté)
                if (connectionState == ConnectionState.CONNECTED) {
                    Button(
                        onClick = {
                            scope.launch {
                                bluetoothManager.disconnect()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = "•"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Déconnecter")
                    }
                }

                // Bouton Unlock (si connecté)
                if (connectionState == ConnectionState.CONNECTED) {
                    Button(
                        onClick = {
                            scope.launch {
                                bluetoothManager.unlockScooter()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("🔓 Déverrouiller")
                    }
                }


            }
        }

        // Instructions
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Instructions:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Assurez-vous que votre scooter est allumé\n2. Appuyez sur 'Scanner' pour rechercher\n3. Sélectionnez votre scooter dans la liste",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Liste des appareils découverts
        if (discoveredDevices.isNotEmpty()) {
            item {
                Text(
                    text = "Scooters trouvés (${discoveredDevices.size}):",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(discoveredDevices) { device ->
                DeviceCard(
                    device = device,
                    onConnect = {
                        scope.launch {
                            bluetoothManager.connectToDevice(device.address)
                        }
                    },
                    isConnectable = connectionState == ConnectionState.DISCONNECTED
                )
            }
        } else if (isScanning) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Recherche de scooters...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        } else if (connectionState == ConnectionState.DISCONNECTED) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Aucun scooter trouvé",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Appuyez sur 'Scanner' pour rechercher",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}