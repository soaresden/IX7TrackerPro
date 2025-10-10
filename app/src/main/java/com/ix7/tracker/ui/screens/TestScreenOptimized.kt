package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.bluetooth.BluetoothRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreenOptimized(
    bluetoothManager: BluetoothRepository,
    isConnected: Boolean
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("🧪 Test Optimisé M0Robot") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        if (!isConnected) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text("❌ Non connecté", modifier = Modifier.padding(16.dp))
            }
            return
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Builder") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Modes") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Néons") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Régulateur") }
            )
        }

        when (selectedTab) {
            0 -> CommandBuilderTab(bluetoothManager, scope)
            1 -> ModesTestTab(bluetoothManager, scope)
            2 -> NeonsTestTab(bluetoothManager, scope)
            3 -> RegulateurTab(bluetoothManager, scope)
        }
    }
}

@Composable
fun CommandBuilderTab(
    bluetoothManager: BluetoothRepository,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var cmdByte by remember { mutableStateOf("4C") }
    var valByte by remember { mutableStateOf("35") }
    var extraByte by remember { mutableStateOf("34") }
    var lastCommand by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🔧 BUILDER DE COMMANDES", style = MaterialTheme.typography.titleLarge)
                Text("Structure: 61 9E 30 14 37 [CMD] [VAL] [EXTRA] [CHK] CB",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp)

                Spacer(modifier = Modifier.height(16.dp))

                // Champs éditables
                OutlinedTextField(
                    value = cmdByte,
                    onValueChange = { if (it.length <= 2) cmdByte = it.uppercase() },
                    label = { Text("CMD (hex)") },
                    placeholder = { Text("4C") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )

                OutlinedTextField(
                    value = valByte,
                    onValueChange = { if (it.length <= 2) valByte = it.uppercase() },
                    label = { Text("VAL (35=ON, 34=OFF)") },
                    placeholder = { Text("35") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )

                OutlinedTextField(
                    value = extraByte,
                    onValueChange = { if (it.length <= 2) extraByte = it.uppercase() },
                    label = { Text("EXTRA (généralement 34)") },
                    placeholder = { Text("34") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bouton d'envoi
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                // Calculer le checksum
                                val cmd = cmdByte.toIntOrNull(16) ?: 0
                                val v = valByte.toIntOrNull(16) ?: 0
                                val e = extraByte.toIntOrNull(16) ?: 0x34

                                // XOR simple pour checksum
                                val checksum = (0x30 xor 0x14 xor 0x37 xor cmd xor v xor e)

                                val command = byteArrayOf(
                                    0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                    cmd.toByte(), v.toByte(), e.toByte(),
                                    checksum.toByte(), 0xCB.toByte()
                                )

                                bluetoothManager.sendCommand(command)
                                lastCommand = command.joinToString("") { "%02X".format(it) }
                            } catch (e: Exception) {
                                lastCommand = "Erreur: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("📤 ENVOYER LA COMMANDE")
                }

                if (lastCommand.isNotEmpty()) {
                    Text("Dernière: $lastCommand",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets rapides
                Text("⚡ PRESETS RAPIDES", style = MaterialTheme.typography.titleMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { cmdByte = "49"; valByte = "35"; extraByte = "34" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("49-35")
                    }
                    Button(
                        onClick = { cmdByte = "49"; valByte = "34"; extraByte = "34" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("49-34")
                    }
                    Button(
                        onClick = { cmdByte = "4C"; valByte = "35"; extraByte = "34" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("4C-35")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { cmdByte = "4C"; valByte = "34"; extraByte = "34" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("4C-34")
                    }
                    Button(
                        onClick = { cmdByte = "C7"; valByte = "35"; extraByte = "34" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("C7-35")
                    }
                    Button(
                        onClick = { cmdByte = "C7"; valByte = "34"; extraByte = "34" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("C7-34")
                    }
                }
            }
        }
    }
}

@Composable
fun ModesTestTab(
    bluetoothManager: BluetoothRepository,
    scope: kotlinx.coroutines.CoroutineScope
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🏍️ TEST DES MODES CORRIGÉS", style = MaterialTheme.typography.titleLarge)
                    Text("Teste ces commandes dans l'ordre")

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mode PIÉTON
                    CommandButton(
                        label = "🚶 PIÉTON (devrait être 6 km/h)",
                        hex = "619E30143749363468CB",
                        bluetoothManager = bluetoothManager,
                        scope = scope
                    )

                    // Mode ECO
                    CommandButton(
                        label = "🍃 ECO (devrait être 10 km/h)",
                        hex = "619E3014374A37346CCB",
                        bluetoothManager = bluetoothManager,
                        scope = scope
                    )

                    // Mode SPORT
                    CommandButton(
                        label = "⚡ SPORT (devrait être 20 km/h)",
                        hex = "619E30143748363468CB",
                        bluetoothManager = bluetoothManager,
                        scope = scope
                    )

                    // Mode RACE (celui qui marche déjà)
                    CommandButton(
                        label = "🏁 RACE (25 km/h) - FONCTIONNE ✅",
                        hex = "619E3014374A35346DCB",
                        bluetoothManager = bluetoothManager,
                        scope = scope,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
fun NeonsTestTab(
    bluetoothManager: BluetoothRepository,
    scope: kotlinx.coroutines.CoroutineScope
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✨ RECHERCHE DES NÉONS", style = MaterialTheme.typography.titleLarge)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Série C7 (fait biper)", style = MaterialTheme.typography.titleMedium)
                    CommandButton("C7-35", "619E301437C735346FCB", bluetoothManager, scope)
                    CommandButton("C7-34", "619E301437C734346ECB", bluetoothManager, scope)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Série 49", style = MaterialTheme.typography.titleMedium)
                    CommandButton("49-35", "619E30143749353469CB", bluetoothManager, scope)
                    CommandButton("49-34", "619E30143749343468CB", bluetoothManager, scope)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Série 4C", style = MaterialTheme.typography.titleMedium)
                    CommandButton("4C-35", "619E3014374C35346BCB", bluetoothManager, scope)
                    CommandButton("4C-34", "619E3014374C34346ACB", bluetoothManager, scope)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Série 4D", style = MaterialTheme.typography.titleMedium)
                    CommandButton("4D-35", "619E3014374D35346ACB", bluetoothManager, scope)
                    CommandButton("4D-34", "619E3014374D34346BCB", bluetoothManager, scope)
                }
            }
        }
    }
}

@Composable
fun RegulateurTab(
    bluetoothManager: BluetoothRepository,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var targetSpeed by remember { mutableStateOf(15f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("⚙️ RÉGULATEUR DE VITESSE", style = MaterialTheme.typography.titleLarge)
                Text("✅ FONCTIONNE !", color = Color(0xFF4CAF50))

                Spacer(modifier = Modifier.height(16.dp))

                // Commandes de base qui marchent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val command = byteArrayOf(
                                    0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                    0x48, 0x35, 0x34, 0x6F, 0xCB.toByte()
                                )
                                bluetoothManager.sendCommand(command)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("🎯 ACTIVER")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val command = byteArrayOf(
                                    0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                    0x48, 0x34, 0x34, 0x68, 0xCB.toByte()
                                )
                                bluetoothManager.sendCommand(command)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                    ) {
                        Text("❌ DÉSACTIVER")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("🔬 TEST VITESSE RÉGULATEUR", style = MaterialTheme.typography.titleMedium)
                Text("Essayons de trouver comment définir la vitesse cible")

                Text("Vitesse cible: ${targetSpeed.toInt()} km/h",
                    style = MaterialTheme.typography.bodyLarge)

                Slider(
                    value = targetSpeed,
                    onValueChange = { targetSpeed = it },
                    valueRange = 5f..25f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        scope.launch {
                            // Tentative 1: Vitesse dans VAL byte
                            val speedByte = targetSpeed.toInt().toByte()
                            val checksum = (0x30 xor 0x14 xor 0x37 xor 0x48 xor speedByte.toInt() xor 0x34)

                            val command = byteArrayOf(
                                0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                0x48, speedByte, 0x34,
                                checksum.toByte(), 0xCB.toByte()
                            )
                            bluetoothManager.sendCommand(command)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tester avec vitesse dans VAL")
                }

                Button(
                    onClick = {
                        scope.launch {
                            // Tentative 2: Vitesse dans EXTRA byte
                            val speedByte = targetSpeed.toInt().toByte()
                            val checksum = (0x30 xor 0x14 xor 0x37 xor 0x48 xor 0x35 xor speedByte.toInt())

                            val command = byteArrayOf(
                                0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                0x48, 0x35, speedByte,
                                checksum.toByte(), 0xCB.toByte()
                            )
                            bluetoothManager.sendCommand(command)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tester avec vitesse dans EXTRA")
                }
            }
        }
    }
}

@Composable
fun CommandButton(
    label: String,
    hex: String,
    bluetoothManager: BluetoothRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = {
            scope.launch {
                try {
                    val bytes = hex.chunked(2)
                        .map { it.toInt(16).toByte() }
                        .toByteArray()
                    bluetoothManager.sendCommand(bytes)
                } catch (e: Exception) {
                    // Log error
                }
            }
        },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Column {
            Text(label)
            Text(hex, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}