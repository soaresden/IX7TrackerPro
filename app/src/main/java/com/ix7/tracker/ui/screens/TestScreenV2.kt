package com.ix7.tracker.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ix7.tracker.bluetooth.BluetoothRepository
import com.ix7.tracker.protocol.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreenV2(
    bluetoothManager: BluetoothRepository,
    isConnected: Boolean
) {
    val scope = rememberCoroutineScope()
    val m0Manager = remember { M0RobotManager(bluetoothManager) }

    // États
    val lastData by m0Manager.lastDecodedData.collectAsState()
    val commandHistory by m0Manager.commandHistory.collectAsState()
    val isPolling by m0Manager.isPolling.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        TopAppBar(
            title = { Text("🧪 M0Robot Control Center") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        if (!isConnected) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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

        // Tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Contrôles") },
                icon = { Icon(Icons.Default.Settings, null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Expérimental") },
                icon = { Icon(Icons.Default.Build, null) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Données") },
                icon = { Icon(Icons.Default.Info, null) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Historique") },
                icon = { Icon(Icons.Default.List, null) }
            )
        }

        // Contenu des tabs
        when (selectedTab) {
            0 -> ControlsTab(m0Manager, scope)
            1 -> ExperimentalTab(m0Manager, scope)
            2 -> DataTab(lastData, isPolling, m0Manager, scope)
            3 -> HistoryTab(commandHistory, m0Manager)
        }
    }
}

@Composable
fun ControlsTab(
    manager: M0RobotManager,
    scope: kotlinx.coroutines.CoroutineScope
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("🎮 CONTRÔLES CONFIRMÉS", style = MaterialTheme.typography.headlineSmall)
        }

        // Verrouillage
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🔒 Verrouillage", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { scope.launch { manager.lock() } },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Lock, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Verrouiller")
                        }
                        Button(
                            onClick = { scope.launch { manager.unlock() } },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                        ) {
                            Icon(Icons.Default.Lock, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Déverrouiller")
                        }
                    }
                }
            }
        }

        // Phare
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("💡 Éclairage", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { scope.launch { manager.setLight(true) } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Star, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Allumer")
                        }
                        Button(
                            onClick = { scope.launch { manager.setLight(false) } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Star, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Éteindre")
                        }
                    }
                }
            }
        }

        // Modes
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🏍️ Modes de conduite", style = MaterialTheme.typography.titleMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { scope.launch { manager.setMode(M0RobotManager.ScooterMode.PEDESTRIAN) } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("🚶 Mode PIÉTON (6 km/h)")
                        }
                        Button(
                            onClick = { scope.launch { manager.setMode(M0RobotManager.ScooterMode.ECO) } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Text("🍃 Mode ECO (10 km/h)")
                        }
                        Button(
                            onClick = { scope.launch { manager.setMode(M0RobotManager.ScooterMode.SPORT) } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                        ) {
                            Text("⚡ Mode SPORT (20 km/h)")
                        }
                        Button(
                            onClick = { scope.launch { manager.setMode(M0RobotManager.ScooterMode.RACE) } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                        ) {
                            Text("🏁 Mode RACE (25 km/h)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExperimentalTab(
    manager: M0RobotManager,
    scope: kotlinx.coroutines.CoroutineScope
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("🔬 COMMANDES EXPÉRIMENTALES", style = MaterialTheme.typography.headlineSmall)
            Text(
                "⚠️ Ces commandes sont en test, notez les effets!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        // Néons
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("✨ Tests Néons", style = MaterialTheme.typography.titleMedium)
                    Text("Basé sur le type 0xC5 (95% de probabilité)", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { scope.launch { manager.testNeon(true) } },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                        ) {
                            Text("✨ Néon ON")
                        }
                        Button(
                            onClick = { scope.launch { manager.testNeon(false) } },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("⚫ Néon OFF")
                        }
                    }
                }
            }
        }

        // Régulateur
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("⚙️ Test Régulateur", style = MaterialTheme.typography.titleMedium)
                    Text("Type 0x48 (90% de probabilité)", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { scope.launch { manager.testRegulator(true) } },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))
                        ) {
                            Text("🎯 Régulateur ON")
                        }
                        Button(
                            onClick = { scope.launch { manager.testRegulator(false) } },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("❌ Régulateur OFF")
                        }
                    }
                }
            }
        }

        // Status & Keep-Alive
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("📊 Communication", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { scope.launch { manager.requestStatus() } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📊 Status")
                        }
                        Button(
                            onClick = { scope.launch { manager.sendKeepAlive() } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("❤️ Keep-Alive")
                        }
                    }
                }
            }
        }

        // Commandes personnalisées
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🛠️ Commande personnalisée", style = MaterialTheme.typography.titleMedium)
                    var hexInput by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { hexInput = it },
                        label = { Text("Hex (ex: 619E301437...)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val bytes = hexInput.chunked(2)
                                        .map { it.toInt(16).toByte() }
                                        .toByteArray()
                                    manager.sendCommand(bytes, "CUSTOM")
                                } catch (e: Exception) {
                                    Log.e("TestScreen", "Erreur parsing hex: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hexInput.isNotEmpty()
                    ) {
                        Text("📤 Envoyer")
                    }
                }
            }
        }
    }
}

@Composable
fun DataTab(
    lastData: M0RobotDecoder.DecodedData?,
    isPolling: Boolean,
    manager: M0RobotManager,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Contrôle du polling
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-refresh", modifier = Modifier.weight(1f))
                Switch(
                    checked = isPolling,
                    onCheckedChange = {
                        scope.launch {
                            if (it) manager.startPolling() else manager.stopPolling()
                        }
                    }
                )
            }
        }

        // Données actuelles
        if (lastData != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("📡 DERNIÈRES DONNÉES", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    DataRow("Type", lastData.type.toString())
                    DataRow("Hex", lastData.hexString, fontFamily = FontFamily.Monospace)

                    lastData.speed?.let { DataRow("Vitesse", "$it km/h") }
                    lastData.battery?.let { DataRow("Batterie", "$it%") }
                    lastData.voltage?.let { DataRow("Tension", "$it V") }
                    lastData.current?.let { DataRow("Courant", "$it A") }
                    lastData.totalDistance?.let { DataRow("Distance totale", "$it km") }
                    lastData.temperature?.let { DataRow("Température", "$it°C") }
                    lastData.mode?.let { DataRow("Mode", it) }
                    lastData.isLocked?.let { DataRow("Verrouillé", if (it) "OUI" else "NON") }
                    lastData.lightOn?.let { DataRow("Phare", if (it) "ON" else "OFF") }
                }
            }

            // Analyse
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("🔍 ANALYSE", style = MaterialTheme.typography.titleMedium)
                    Text(
                        manager.analyzeLastResponse(),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Aucune donnée reçue",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun HistoryTab(
    history: List<M0RobotManager.CommandHistoryEntry>,
    manager: M0RobotManager
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📜 HISTORIQUE", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = { manager.clearHistory() }) {
                    Text("Effacer")
                }
            }
        }

        items(history) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (entry.success)
                        Color(0xFF1B5E20) else Color(0xFFB71C1C)
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            entry.commandName,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
                                .format(entry.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        entry.commandHex,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        fontFamily = FontFamily.Monospace
                    )
                    entry.response?.let {
                        Text(
                            "↩️ $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DataRow(label: String, value: String, fontFamily: FontFamily? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold
        )
    }
}