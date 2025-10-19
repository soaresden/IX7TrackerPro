package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.bluetooth.BluetoothRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegulatorTestScreen(
    bluetoothManager: BluetoothRepository,
    isConnected: Boolean
) {
    val scope = rememberCoroutineScope()
    var targetSpeed by remember { mutableStateOf(20f) }
    var lastCommand by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TopAppBar(
            title = { Text("⚙️ Test Régulateur avec Vitesse") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        if (!isConnected) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text("❌ Non connecté", modifier = Modifier.padding(16.dp))
            }
            return
        }

        // Section régulateur de base
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("✅ COMMANDES DE BASE (qui marchent)", style = MaterialTheme.typography.titleLarge)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                // 48-35 = ON
                                val command = byteArrayOf(
                                    0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                    0x48, 0x35, 0x34, 0x6F, 0xCB.toByte()
                                )
                                bluetoothManager.sendCommand(command)
                                lastCommand = "48-35-34 (ON)"
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
                                // 48-34 = OFF
                                val command = byteArrayOf(
                                    0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                    0x48, 0x34, 0x34, 0x68, 0xCB.toByte()
                                )
                                bluetoothManager.sendCommand(command)
                                lastCommand = "48-34-34 (OFF)"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                    ) {
                        Text("❌ DÉSACTIVER")
                    }
                }
            }
        }

        // Section vitesse
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🎯 RÉGLAGE DE LA VITESSE", style = MaterialTheme.typography.titleLarge)

                Text(
                    "Vitesse cible: ${targetSpeed.toInt()} km/h",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Slider(
                    value = targetSpeed,
                    onValueChange = { targetSpeed = it },
                    valueRange = 0f..60f,
                    steps = 59,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Méthode 1: Vitesse en hex direct dans VAL
                Button(
                    onClick = {
                        scope.launch {
                            val speedHex = targetSpeed.toInt()
                            val checksum = (0x30 xor 0x14 xor 0x37 xor 0x48 xor speedHex xor 0x34)

                            val command = byteArrayOf(
                                0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                0x48, speedHex.toByte(), 0x34,
                                checksum.toByte(), 0xCB.toByte()
                            )
                            bluetoothManager.sendCommand(command)
                            lastCommand = "48-${"%02X".format(speedHex)}-34"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("Méthode 1: Vitesse dans VAL (48-${"%02X".format(targetSpeed.toInt())}-34)")
                }

                // Méthode 2: Vitesse en BCD (décimal codé binaire)
                Button(
                    onClick = {
                        scope.launch {
                            val speedBCD = ((targetSpeed.toInt() / 10) shl 4) or (targetSpeed.toInt() % 10)
                            val checksum = (0x30 xor 0x14 xor 0x37 xor 0x48 xor speedBCD xor 0x34)

                            val command = byteArrayOf(
                                0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                0x48, speedBCD.toByte(), 0x34,
                                checksum.toByte(), 0xCB.toByte()
                            )
                            bluetoothManager.sendCommand(command)
                            lastCommand = "48-${"%02X".format(speedBCD)}-34 (BCD)"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                ) {
                    Text("Méthode 2: Vitesse en BCD")
                }

                // Méthode 3: Vitesse dans EXTRA
                Button(
                    onClick = {
                        scope.launch {
                            val speedHex = targetSpeed.toInt()
                            val checksum = (0x30 xor 0x14 xor 0x37 xor 0x48 xor 0x35 xor speedHex)

                            val command = byteArrayOf(
                                0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                0x48, 0x35, speedHex.toByte(),
                                checksum.toByte(), 0xCB.toByte()
                            )
                            bluetoothManager.sendCommand(command)
                            lastCommand = "48-35-${"%02X".format(speedHex)}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("Méthode 3: Vitesse dans EXTRA (48-35-${"%02X".format(targetSpeed.toInt())})")
                }

                // Méthode 4: Vitesse x10 (pour la précision)
                Button(
                    onClick = {
                        scope.launch {
                            val speedX10 = (targetSpeed * 10).toInt()
                            val speedHigh = (speedX10 shr 8) and 0xFF
                            val speedLow = speedX10 and 0xFF
                            val checksum = (0x30 xor 0x14 xor 0x37 xor 0x48 xor speedHigh xor speedLow)

                            val command = byteArrayOf(
                                0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                0x48, speedHigh.toByte(), speedLow.toByte(),
                                checksum.toByte(), 0xCB.toByte()
                            )
                            bluetoothManager.sendCommand(command)
                            lastCommand = "48-${"%02X".format(speedHigh)}-${"%02X".format(speedLow)} (x10)"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Méthode 4: Vitesse x10 (${(targetSpeed * 10).toInt()})")
                }

                // Méthode 5: Commande étendue
                Button(
                    onClick = {
                        scope.launch {
                            val speedHex = targetSpeed.toInt()
                            // Essayer un format étendu avec plus de bytes
                            val checksum = (0x30 xor 0x15 xor 0x37 xor 0x48 xor speedHex xor 0x00 xor 0x34)

                            val command = byteArrayOf(
                                0x61, 0x9E.toByte(), 0x30, 0x15, 0x37,
                                0x48, speedHex.toByte(), 0x00, 0x34,
                                checksum.toByte(), 0xCB.toByte()
                            )
                            bluetoothManager.sendCommand(command)
                            lastCommand = "Format étendu: 48-${"%02X".format(speedHex)}-00-34"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795548))
                ) {
                    Text("Méthode 5: Format étendu")
                }

                if (lastCommand.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Dernière commande: $lastCommand",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Presets de vitesse rapides
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("⚡ PRESETS RAPIDES", style = MaterialTheme.typography.titleMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { targetSpeed = 10f },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("10")
                    }
                    Button(
                        onClick = { targetSpeed = 15f },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("15")
                    }
                    Button(
                        onClick = { targetSpeed = 20f },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("20")
                    }
                    Button(
                        onClick = { targetSpeed = 25f },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("25")
                    }
                }
            }
        }

        // Section test verrouillage
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🔒 TEST VERROUILLAGE", style = MaterialTheme.typography.titleMedium)
                Text("Les commandes 4B ne semblent pas correctes. Essayons:", fontSize = 12.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                // Format trouvé dans le log
                                val command = byteArrayOf(
                                    0x61, 0x9E.toByte(), 0x2C, 0x50, 0x57,
                                    0x4B, 0x01, 0x61, 0xA9.toByte(), 0xCC.toByte()
                                )
                                bluetoothManager.sendCommand(command)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Format log")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                // Essayer 4D à la place
                                val command = byteArrayOf(
                                    0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                    0x4D, 0x35, 0x34, 0x6A, 0xCB.toByte()
                                )
                                bluetoothManager.sendCommand(command)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("4D-35")
                    }
                }
            }
        }
    }
}