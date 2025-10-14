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
    framesState: MutableMap<String, FrameMonitor>
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }

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
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("📊 Variables") }
            )
        }

        when (selectedTab) {
            0 -> CommandBuilderTab(bluetoothManager, scope)
            1 -> FrameMonitorTab(framesState)
            2 -> VariablesMonitorTab(framesState)
        }
    }
}

// ✅ Tab de monitoring des variables importantes
@Composable
fun VariablesMonitorTab(frames: MutableMap<String, FrameMonitor>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📊 VARIABLES TROUVÉES ✅",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        "Offsets validés avec le log btsnoop",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Section Alimentation
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "⚡ ALIMENTATION",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFEB3B)
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray)

                    VariableRow(
                        icon = "🔋",
                        name = "Batterie",
                        target = "66%",
                        current = findBattery(frames),
                        frameType = "0x20[45] ou 0x3E ou 0xD3[43]"
                    )

                    VariableRow(
                        icon = "⚡",
                        name = "Voltage",
                        target = "49.0V",
                        current = findVoltage(frames),
                        frameType = "0x3E[6-7] BE/1000"
                    )

                    VariableRow(
                        icon = "💪",
                        name = "Puissance",
                        target = "6.4W",
                        current = findPower(frames),
                        frameType = "❓ À trouver"
                    )
                }
            }
        }

        // Section Mouvement
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🏃 MOUVEMENT",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FF00)
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray)

                    VariableRow(
                        icon = "🏃",
                        name = "Vitesse",
                        target = "0-30 km/h",
                        current = findSpeed(frames),
                        frameType = "0x32[5]"
                    )

                    VariableRow(
                        icon = "🛣️",
                        name = "Odomètre",
                        target = "102.9 km",
                        current = findOdometer(frames),
                        frameType = "0x03[2-3] LE/100 ou 0x30[35-36] LE/10"
                    )

                    VariableRow(
                        icon = "⏱️",
                        name = "Temps conduite",
                        target = "44h29m",
                        current = findRideTime(frames),
                        frameType = "❓ À trouver"
                    )
                }
            }
        }

        // Section Température
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🌡️ TEMPÉRATURES",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray)

                    VariableRow(
                        icon = "🌡️",
                        name = "Température",
                        target = "26-27°C",
                        current = findTemp1(frames),
                        frameType = "0x3E[49] ou 0xD3[17,29]"
                    )
                }
            }
        }

        // Trames disponibles
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📦 TRAMES DISPONIBLES",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    frames.keys.sorted().forEach { type ->
                        val frame = frames[type]
                        if (frame != null) {
                            Text(
                                "$type (${frame.size}B) × ${frame.count}",
                                fontSize = 12.sp,
                                color = Color(0xFF64B5F6),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VariableRow(
    icon: String,
    name: String,
    target: String,
    current: String,
    frameType: String
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        frameType,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Cible: $target",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Text(
                    current,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (current.contains("❓") || current.contains("❌"))
                        Color(0xFFF44336)
                    else
                        Color(0xFF4CAF50)
                )
            }
        }
    }
}

// ✅ Fonctions d'extraction des valeurs (mises à jour)
fun findBattery(frames: MutableMap<String, FrameMonitor>): String {
    // Chercher dans 0x20, 0x3E, 0xD3
    listOf("0x20", "0x3E", "0xD3").forEach { offset ->
        val frame = frames[offset]
        val battery = frame?.decoded?.get("battery")?.value
        if (battery != null) return battery
    }
    return "❓ Non trouvé"
}

fun findVoltage(frames: MutableMap<String, FrameMonitor>): String {
    // Chercher dans 0x3E en priorité, puis 0x30
    listOf("0x3E", "0x30").forEach { offset ->
        val frame = frames[offset]
        val voltage = frame?.decoded?.get("voltage")?.value
        if (voltage != null) return voltage
    }
    return "❓ Non trouvé"
}

fun findSpeed(frames: MutableMap<String, FrameMonitor>): String {
    val frame = frames["0x32"] ?: return "❓ Non trouvé"
    return frame.decoded["speed"]?.value ?: "❓ Non décodé"
}

fun findOdometer(frames: MutableMap<String, FrameMonitor>): String {
    // Chercher dans 0x03, 0x30, 0x3E
    listOf("0x03", "0x30", "0x3E").forEach { offset ->
        val frame = frames[offset]
        val odo = frame?.decoded?.get("odometer")?.value
        if (odo != null) return odo
    }
    return "❓ Non trouvé"
}

fun findPower(frames: MutableMap<String, FrameMonitor>): String {
    return "❓ À chercher"
}

fun findRideTime(frames: MutableMap<String, FrameMonitor>): String {
    return "❓ À chercher"
}

fun findTemp1(frames: MutableMap<String, FrameMonitor>): String {
    // Chercher dans 0x3E, 0x32, 0xD3
    listOf("0x3E", "0xD3", "0x32").forEach { offset ->
        val frame = frames[offset]
        val temp = frame?.decoded?.get("temperature")?.value ?: frame?.decoded?.get("temp1")?.value
        if (temp != null) return temp
    }
    return "❓ Non trouvé"
}

fun findTemp2(frames: MutableMap<String, FrameMonitor>): String {
    val frame = frames["0x32"] ?: frames["0xD3"] ?: return "❓ Non trouvé"
    return frame.decoded["temp2"]?.value ?: "❓ Non décodé"
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
                        "Affichage en temps réel - ${frames.size} types détectés",
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
            items(
                items = frames.values.sortedBy { it.type },
                key = { it.type }
            ) { frame ->
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

            Text(
                frame.hex,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color.White,
                lineHeight = 16.sp
            )

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

// ✅ Fonction de mise à jour avec les VRAIS offsets du log btsnoop
fun updateFrameMonitor(frames: MutableMap<String, FrameMonitor>, frame: ByteArray) {
    if (frame.size < 3) return
    if (frame[0] != 0x61.toByte() || frame[1] != 0x9E.toByte()) return

    val type = "0x%02X".format(frame[2].toInt() and 0xFF)
    val hex = frame.joinToString(" ") { "%02X".format(it) }
    val existing = frames[type]
    val count = (existing?.count ?: 0) + 1
    val decoded = mutableMapOf<String, DecodedValue>()
    val frameType = frame[2].toInt() and 0xFF

    when (frameType) {
        // 🔋 OFFSET 0x03 - Odomètre principal
        0x03 -> {
            if (frame.size > 3) {
                val odoRaw = ((frame[3].toInt() and 0xFF) shl 8) or (frame[2].toInt() and 0xFF)
                val odometer = odoRaw / 100.0
                decoded["odometer"] = DecodedValue(
                    "🛣️ Odomètre",
                    "${String.format("%.1f", odometer)} km",
                    true,
                    Color(0xFF2196F3)
                )
            }
        }

        // 🔋 OFFSET 0x20 - Batterie directe
        0x20 -> {
            if (frame.size > 45) {
                val battery = frame[45].toInt() and 0xFF
                if (battery in 0..100) {
                    decoded["battery"] = DecodedValue(
                        "🔋 Batterie",
                        "$battery%",
                        true,
                        if (battery > 50) Color(0xFF4CAF50) else if (battery > 20) Color(0xFFFFC107) else Color(0xFFF44336)
                    )
                }
            }
        }

        // 📊 OFFSET 0x30 - Données multiples
        0x30 -> {
            // Odomètre
            if (frame.size > 36) {
                val odoRaw = ((frame[36].toInt() and 0xFF) shl 8) or (frame[35].toInt() and 0xFF)
                val odometer = odoRaw / 10.0
                decoded["odometer"] = DecodedValue(
                    "🛣️ Odomètre",
                    "${String.format("%.1f", odometer)} km",
                    true,
                    Color(0xFF2196F3)
                )
            }

            // Voltage alternatif
            if (frame.size > 5) {
                val voltageRaw = ((frame[5].toInt() and 0xFF) shl 8) or (frame[4].toInt() and 0xFF)
                val voltage = voltageRaw / 1000.0
                if (voltage > 30.0 && voltage < 70.0) {
                    decoded["voltage"] = DecodedValue(
                        "⚡ Voltage",
                        "${String.format("%.2f", voltage)}V",
                        true,
                        if (voltage > 40.0) Color(0xFF4CAF50) else Color(0xFFFFC107)
                    )
                }
            }
        }

        // 🏃 OFFSET 0x32 - Vitesse et températures
        0x32 -> {
            // Vitesse
            if (frame.size > 5) {
                val speed = frame[5].toInt() and 0xFF
                decoded["speed"] = DecodedValue(
                    "🏃 Vitesse",
                    "$speed km/h",
                    speed in 0..60,
                    if (speed > 0) Color(0xFF00FF00) else Color(0xFF9E9E9E)
                )
            }

            // Températures
            if (frame.size > 7) {
                val temp1 = frame[6].toInt() and 0xFF
                val temp2 = frame[7].toInt() and 0xFF

                decoded["temp1"] = DecodedValue(
                    "🌡️ Temp1",
                    "${temp1}°C",
                    temp1 in 0..80,
                    Color(0xFFFF9800)
                )

                decoded["temp2"] = DecodedValue(
                    "🌡️ Temp2",
                    "${temp2}°C (brut)",
                    false,
                    Color(0xFF9E9E9E)
                )
            }
        }

        // ⚡ OFFSET 0x3E - TRAME PRINCIPALE (voltage + odomètre + température)
        0x3E -> {
            // Voltage: bytes 6-7, Big Endian, diviser par 1000
            if (frame.size > 7) {
                val voltageRaw = ((frame[6].toInt() and 0xFF) shl 8) or (frame[7].toInt() and 0xFF)
                val voltage = voltageRaw / 1000.0
                if (voltage > 30.0 && voltage < 70.0) {
                    decoded["voltage"] = DecodedValue(
                        "⚡ Voltage",
                        "${String.format("%.2f", voltage)}V",
                        true,
                        if (voltage > 40.0) Color(0xFF4CAF50) else Color(0xFFFFC107)
                    )
                }
            }

            // Odomètre: bytes 46-47, Little Endian, diviser par 10
            if (frame.size > 47) {
                val odoRaw = ((frame[47].toInt() and 0xFF) shl 8) or (frame[46].toInt() and 0xFF)
                val odometer = odoRaw / 10.0
                decoded["odometer"] = DecodedValue(
                    "🛣️ Odomètre",
                    "${String.format("%.1f", odometer)} km",
                    true,
                    Color(0xFF2196F3)
                )
            }

            // Température
            if (frame.size > 49) {
                val temp = frame[49].toInt() and 0xFF
                if (temp in 0..80) {
                    decoded["temperature"] = DecodedValue(
                        "🌡️ Température",
                        "${temp}°C",
                        true,
                        Color(0xFFFF9800)
                    )
                }
            }

            // Chercher la batterie (peut être à plusieurs positions)
            for (i in 5 until minOf(frame.size, 50)) {
                val value = frame[i].toInt() and 0xFF
                if (value == 66 || value == 106) {
                    val battery = if (value == 106) value - 40 else value
                    decoded["battery"] = DecodedValue(
                        "🔋 Batterie",
                        "$battery%",
                        true,
                        if (battery > 50) Color(0xFF4CAF50) else if (battery > 20) Color(0xFFFFC107) else Color(0xFFF44336)
                    )
                    break
                }
            }
        }

        // 🌡️ OFFSET 0xD3 - Températures et batterie
        0xD3 -> {
            // Batterie
            if (frame.size > 43) {
                val battery = frame[43].toInt() and 0xFF
                if (battery in 0..100) {
                    decoded["battery"] = DecodedValue(
                        "🔋 Batterie",
                        "$battery%",
                        true,
                        if (battery > 50) Color(0xFF4CAF50) else if (battery > 20) Color(0xFFFFC107) else Color(0xFFF44336)
                    )
                }
            }

            // Températures
            if (frame.size > 17) {
                val temp1 = frame[17].toInt() and 0xFF
                if (temp1 in 0..80) {
                    decoded["temp1"] = DecodedValue(
                        "🌡️ Temp1",
                        "${temp1}°C",
                        true,
                        Color(0xFFFF9800)
                    )
                }
            }

            if (frame.size > 29) {
                val temp2 = frame[29].toInt() and 0xFF
                if (temp2 in 0..80) {
                    decoded["temp2"] = DecodedValue(
                        "🌡️ Temp2",
                        "${temp2}°C",
                        true,
                        Color(0xFFFF9800)
                    )
                }
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