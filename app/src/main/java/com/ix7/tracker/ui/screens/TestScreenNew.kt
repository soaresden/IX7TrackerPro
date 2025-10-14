package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.bluetooth.BluetoothRepository
import kotlinx.coroutines.launch

data class FrameMonitor(
    val type: String,
    val size: Int,
    val hex: String,
    val count: Int,
    val lastUpdate: Long,
    val decoded: Map<String, DecodedValue>
)

data class DecodedValue(
    val label: String,
    val value: String,
    val validated: Boolean,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreenNew(
    bluetoothManager: BluetoothRepository,
    isConnected: Boolean,
    onFrameReceived: (ByteArray) -> Unit  // Callback pour recevoir les trames
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }

    // État pour le monitoring des trames
    val framesState = remember { mutableStateMapOf<String, FrameMonitor>() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("🧪 Test & Monitor M0Robot") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        if (!isConnected) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text("❌ Non connecté - Connecte-toi pour utiliser les outils",
                    modifier = Modifier.padding(16.dp))
            }
            return
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("🔧 Builder") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("📡 Monitor") }
            )
        }

        when (selectedTab) {
            0 -> CommandBuilderTab(bluetoothManager, scope)
            1 -> FrameMonitorTab(framesState)
        }
    }

    // Callback pour mettre à jour le monitor quand une trame arrive
    LaunchedEffect(Unit) {
        // TODO: Connecter au flux de données Bluetooth
        // bluetoothManager.dataFlow.collect { frame ->
        //     updateFrameMonitor(framesState, frame)
        // }
    }
}

@Composable
fun CommandBuilderTab(
    bluetoothManager: BluetoothRepository,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var cmdByte by remember { mutableStateOf("4A") }
    var valByte by remember { mutableStateOf("35") }
    var extraByte by remember { mutableStateOf("34") }
    var lastCommand by remember { mutableStateOf("") }
    var lastResult by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🔧 BUILDER DE COMMANDES",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        "Structure: 61 9E 30 14 37 [CMD] [VAL] [EXTRA] [CHK] CB",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // CMD Byte
                    OutlinedTextField(
                        value = cmdByte,
                        onValueChange = { if (it.length <= 2) cmdByte = it.uppercase() },
                        label = { Text("CMD (hex)", color = Color.White) },
                        placeholder = { Text("4A") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // VAL Byte
                    OutlinedTextField(
                        value = valByte,
                        onValueChange = { if (it.length <= 2) valByte = it.uppercase() },
                        label = { Text("VAL (35=ON, 34=OFF)", color = Color.White) },
                        placeholder = { Text("35") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // EXTRA Byte
                    OutlinedTextField(
                        value = extraByte,
                        onValueChange = { if (it.length <= 2) extraByte = it.uppercase() },
                        label = { Text("EXTRA (généralement 34)", color = Color.White) },
                        placeholder = { Text("34") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Prévisualisation de la commande
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("📋 Prévisualisation:", color = Color.Gray, fontSize = 12.sp)

                            val cmd = cmdByte.toIntOrNull(16) ?: 0
                            val v = valByte.toIntOrNull(16) ?: 0
                            val e = extraByte.toIntOrNull(16) ?: 0x34
                            val checksum = (0x30 xor 0x14 xor 0x37 xor cmd xor v xor e)

                            val preview = "61 9E 30 14 37 ${cmdByte} ${valByte} ${extraByte} %02X CB".format(checksum)

                            Text(
                                preview,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = Color(0xFF00FF00),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bouton d'envoi
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val cmd = cmdByte.toIntOrNull(16) ?: 0
                                    val v = valByte.toIntOrNull(16) ?: 0
                                    val e = extraByte.toIntOrNull(16) ?: 0x34
                                    val checksum = (0x30 xor 0x14 xor 0x37 xor cmd xor v xor e)

                                    val command = byteArrayOf(
                                        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                        cmd.toByte(), v.toByte(), e.toByte(),
                                        checksum.toByte(), 0xCB.toByte()
                                    )

                                    bluetoothManager.sendCommand(command)
                                    lastCommand = command.joinToString(" ") { "%02X".format(it) }
                                    lastResult = "✅ Envoyé avec succès"
                                } catch (e: Exception) {
                                    lastResult = "❌ Erreur: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("📤 ENVOYER LA COMMANDE", fontSize = 16.sp)
                    }

                    if (lastCommand.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Dernière commande:",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            lastCommand,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFF00FF00)
                        )
                        Text(
                            lastResult,
                            fontSize = 12.sp,
                            color = if (lastResult.startsWith("✅")) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "⚡ PRESETS RAPIDES",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("🎮 Modes de conduite:", fontSize = 13.sp, color = Color.Gray)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetButton(
                            label = "🚶 Piéton",
                            cmd = "49", v = "37", e = "34",
                            onApply = { cmdByte = it.first; valByte = it.second; extraByte = it.third },
                            modifier = Modifier.weight(1f)
                        )
                        PresetButton(
                            label = "🌱 Eco",
                            cmd = "4A", v = "37", e = "34",
                            onApply = { cmdByte = it.first; valByte = it.second; extraByte = it.third },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetButton(
                            label = "⚡ Sport",
                            cmd = "48", v = "36", e = "34",
                            onApply = { cmdByte = it.first; valByte = it.second; extraByte = it.third },
                            modifier = Modifier.weight(1f)
                        )
                        PresetButton(
                            label = "🏎️ Race",
                            cmd = "4A", v = "35", e = "34",
                            onApply = { cmdByte = it.first; valByte = it.second; extraByte = it.third },
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF4CAF50)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("💡 Phares:", fontSize = 13.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetButton(
                            label = "ON",
                            cmd = "C6", v = "35", e = "34",
                            onApply = { cmdByte = it.first; valByte = it.second; extraByte = it.third },
                            modifier = Modifier.weight(1f)
                        )
                        PresetButton(
                            label = "OFF",
                            cmd = "C6", v = "34", e = "34",
                            onApply = { cmdByte = it.first; valByte = it.second; extraByte = it.third },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("🔒 Verrouillage:", fontSize = 13.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PresetButton(
                            label = "LOCK",
                            cmd = "4B", v = "35", e = "34",
                            onApply = { cmdByte = it.first; valByte = it.second; extraByte = it.third },
                            modifier = Modifier.weight(1f)
                        )
                        PresetButton(
                            label = "UNLOCK",
                            cmd = "4B", v = "34", e = "34",
                            onApply = { cmdByte = it.first; valByte = it.second; extraByte = it.third },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PresetButton(
    label: String,
    cmd: String,
    v: String,
    e: String,
    onApply: (Triple<String, String, String>) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF6200EE)
) {
    Button(
        onClick = { onApply(Triple(cmd, v, e)) },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 14.sp)
            Text("$cmd-$v", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun FrameMonitorTab(frames: MutableMap<String, FrameMonitor>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📡 MONITOR DES TRAMES",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        "Affichage en temps réel des trames reçues",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        if (frames.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "⏳ En attente de données...\nLes trames apparaîtront ici automatiquement",
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(frames.values.sortedByDescending { it.lastUpdate }) { frame ->
                FrameCard(frame)
            }
        }
    }
}

@Composable
fun FrameCard(frame: FrameMonitor) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // En-tête avec type et compteur
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Type ${frame.type}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF00)
                )
                Text(
                    "× ${frame.count}",
                    fontSize = 14.sp,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                "${frame.size} bytes",
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Données brutes
            Text(
                frame.hex,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color.White,
                lineHeight = 16.sp
            )

            // Valeurs décodées
            if (frame.decoded.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.Gray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                frame.decoded.forEach { (key, decoded) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                decoded.label,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            if (decoded.validated) {
                                Text(
                                    "✅ Validé",
                                    fontSize = 10.sp,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                        Text(
                            decoded.value,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = decoded.color
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

// Fonction utilitaire pour mettre à jour le monitor
fun updateFrameMonitor(frames: MutableMap<String, FrameMonitor>, frame: ByteArray) {
    if (frame.size < 3) return

    val type = "0x%02X".format(frame[2])
    val hex = frame.joinToString(" ") { "%02X".format(it) }

    val existing = frames[type]
    val count = (existing?.count ?: 0) + 1

    // Décoder les valeurs connues
    val decoded = mutableMapOf<String, DecodedValue>()

    when (frame[2].toInt() and 0xFF) {
        0x16 -> {  // Trame combinée - Batterie + Odomètre
            if (frame.size > 7) {
                val battery = frame[7].toInt() and 0xFF
                decoded["battery"] = DecodedValue(
                    "🔋 Batterie (offset 7)",
                    "$battery%",
                    true,
                    Color(0xFF4CAF50)
                )
            }
            if (frame.size >= 19) {
                val odometerRaw = ((frame[18].toInt() and 0xFF) shl 8) or (frame[17].toInt() and 0xFF)
                val odometer = odometerRaw / 100.0f
                decoded["odometer"] = DecodedValue(
                    "🛣️ Odomètre (offset 17-18)",
                    "%.2f km".format(odometer),
                    true,
                    Color(0xFF2196F3)
                )
            }
        }

        0x1A -> {  // Trame détaillée - Odomètre seul
            if (frame.size >= 11) {
                val odometerRaw = ((frame[10].toInt() and 0xFF) shl 8) or (frame[9].toInt() and 0xFF)
                val odometer = odometerRaw / 100.0f
                decoded["odometer"] = DecodedValue(
                    "🛣️ Odomètre (offset 9-10)",
                    "%.2f km".format(odometer),
                    true,
                    Color(0xFF2196F3)
                )
            }
        }

        0x37 -> {  // Trame temps réel
            if (frame.size >= 30) {
                // Vitesse
                val speedRaw = ((frame[6].toInt() and 0xFF) shl 8) or (frame[5].toInt() and 0xFF)
                val speed = speedRaw / 10.0f
                decoded["speed"] = DecodedValue(
                    "🏍️ Vitesse (offset 5-6)",
                    "%.1f km/h".format(speed),
                    true,
                    Color(0xFFFF9800)
                )

                // Batterie
                val battery = frame[7].toInt() and 0xFF
                decoded["battery"] = DecodedValue(
                    "🔋 Batterie (offset 7)",
                    "$battery%",
                    true,
                    Color(0xFF4CAF50)
                )

                // Tension
                val voltageRaw = ((frame[9].toInt() and 0xFF) shl 8) or (frame[8].toInt() and 0xFF)
                val voltage = voltageRaw / 10.0f
                decoded["voltage"] = DecodedValue(
                    "⚡ Tension (offset 8-9)",
                    "%.1f V".format(voltage),
                    true,
                    Color(0xFFFFEB3B)
                )

                // Courant
                val currentRaw = ((frame[11].toInt() and 0xFF) shl 8) or (frame[10].toInt() and 0xFF)
                val current = currentRaw / 10.0f
                decoded["current"] = DecodedValue(
                    "⚡ Courant (offset 10-11)",
                    "%.1f A".format(current),
                    true,
                    Color(0xFFFFEB3B)
                )

                // Température
                val temp = (frame[12].toInt() and 0xFF) - 40
                decoded["temp"] = DecodedValue(
                    "🌡️ Température (offset 12)",
                    "${temp}°C",
                    true,
                    Color(0xFFFF5722)
                )
            }
        }

        0x30 -> {  // Status/Mode
            if (frame.size >= 7) {
                val cmd = frame[5].toInt() and 0xFF
                val sub = frame[6].toInt() and 0xFF
                decoded["command"] = DecodedValue(
                    "📝 Commande (offset 5)",
                    "0x%02X".format(cmd),
                    false,
                    Color(0xFF9C27B0)
                )
                decoded["subcode"] = DecodedValue(
                    "📝 Sub-code (offset 6)",
                    "0x%02X".format(sub),
                    false,
                    Color(0xFF9C27B0)
                )
            }
        }

        0x3E -> {  // Batterie seule
            if (frame.size > 7) {
                val battery = frame[7].toInt() and 0xFF
                decoded["battery"] = DecodedValue(
                    "🔋 Batterie (offset 7)",
                    "$battery%",
                    true,
                    Color(0xFF4CAF50)
                )
            }
        }
    }

    frames[type] = FrameMonitor(
        type = type,
        size = frame.size,
        hex = hex,
        count = count,
        lastUpdate = System.currentTimeMillis(),
        decoded = decoded
    )
}