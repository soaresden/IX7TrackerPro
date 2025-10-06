package com.ix7.tracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.bluetooth.BluetoothRepository
import com.ix7.tracker.core.TemperatureThresholds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.*
import com.ix7.tracker.core.ScooterData
import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.WheelMode
import com.ix7.tracker.core.SpeedUnit
import com.ix7.tracker.core.SpeedLimits
@Composable
fun RideScreen(
    scooterData: ScooterData,
    isConnected: Boolean,
    bluetoothManager: BluetoothRepository
) {
    val scope = rememberCoroutineScope()

    // ⚠️ TOUS LES ÉTATS LOCAUX SUPPRIMÉS - On utilise directement scooterData
    var wheelMode by remember { mutableStateOf(WheelMode.ONE_WHEEL) }
    var speedUnit by remember { mutableStateOf(SpeedUnit.KMH) }
    var isRiding by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var showActionsPopup by remember { mutableStateOf(false) }

    // États clignotants (pas encore implémentés dans le protocole)
    var leftTurn by remember { mutableStateOf(false) }
    var rightTurn by remember { mutableStateOf(false) }
    var warningLights by remember { mutableStateOf(false) }

    // Utilise scooterData.isLocked au lieu d'un état local
    val isLocked = scooterData.isLocked
    val isDebridged = !scooterData.isLocked // Débridé = non verrouillé
    val rideMode = scooterData.currentMode

    val speedLimits = when {
        isDebridged && wheelMode == WheelMode.ONE_WHEEL -> SpeedLimits(20, 30, 40, 50)
        isDebridged && wheelMode == WheelMode.TWO_WHEELS -> SpeedLimits(15, 30, 45, 60)
        !isDebridged && wheelMode == WheelMode.ONE_WHEEL -> SpeedLimits(5, 10, 15, 25)
        else -> SpeedLimits(5, 10, 15, 25)
    }

    val maxSpeed = when (rideMode) {
        RideMode.PEDESTRIAN -> speedLimits.pedestrian
        RideMode.ECO -> speedLimits.eco
        RideMode.RACE -> speedLimits.race
        RideMode.SPORT -> speedLimits.sport
    }

    val displayMaxSpeed = if (speedUnit == SpeedUnit.MPH) (maxSpeed * 0.621371).toInt() else maxSpeed
    val currentSpeed = if (speedUnit == SpeedUnit.MPH) scooterData.speed * 0.621371f else scooterData.speed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Compteur compact avec lock
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactSpeedometer(
                speed = if (isConnected) currentSpeed else 0f,
                maxSpeed = displayMaxSpeed.toFloat(),
                modifier = Modifier.weight(1f)
            )

            // Bouton Lock/Unlock - Envoie commande et attend confirmation
            Button(
                onClick = {
                    scope.launch {
                        // Envoie la commande opposée à l'état actuel
                        bluetoothManager.connector?.setUnlocked(!isLocked)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLocked) Color.Red else Color.Green
                ),
                modifier = Modifier.size(60.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(if (isLocked) "🔒" else "🔓", fontSize = 24.sp)
            }
        }

        // 2. Mode de conduite compact
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                wheelMode = if (wheelMode == WheelMode.ONE_WHEEL)
                                    WheelMode.TWO_WHEELS else WheelMode.ONE_WHEEL
                            },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(wheelMode.emoji, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(wheelMode.label, fontSize = 12.sp, color = Color.White)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Bouton débridage - Reflète l'état de la trottinette
                        Button(
                            onClick = {
                                scope.launch {
                                    bluetoothManager.connector?.setUnlocked(!isDebridged)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDebridged) Color.Red else Color.Gray
                            ),
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("⚡", fontSize = 16.sp)
                        }

                        Button(
                            onClick = { showActionsPopup = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("⚙️", fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Boutons modes - Reflètent scooterData.currentMode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RideMode.values().forEach { mode ->
                        Button(
                            onClick = {
                                scope.launch {
                                    bluetoothManager.connector?.setRideMode(mode)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (rideMode == mode) Color.Blue else Color.DarkGray
                            ),
                            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(mode.emoji, fontSize = 14.sp)
                                Text(
                                    when (mode) {
                                        RideMode.PEDESTRIAN -> "${speedLimits.pedestrian}"
                                        RideMode.ECO -> "${speedLimits.eco}"
                                        RideMode.RACE -> "${speedLimits.race}"
                                        RideMode.SPORT -> "${speedLimits.sport}"
                                    },
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Clignotants (non fonctionnels)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                enabled = false,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("⬅️", fontSize = 20.sp)
            }

            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                enabled = false,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("⚠️", fontSize = 20.sp)
            }

            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                enabled = false,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("➡️", fontSize = 20.sp)
            }
        }

        // 4. Graphique temps réel
        Card(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            RealTimeGraph(
                isRiding = isRiding,
                currentSpeed = currentSpeed,
                currentBattery = scooterData.battery,
                maxSpeed = displayMaxSpeed.toFloat()
            )
        }

        // 5. Contrôles de trajet
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { isRiding = true; isPaused = false },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                enabled = !isRiding || isPaused,
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
            ) {
                Text("▶", fontSize = 20.sp)
            }

            Button(
                onClick = { isPaused = !isPaused },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                enabled = isRiding,
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
            ) {
                Text(if (isPaused) "▶" else "⏸", fontSize = 20.sp)
            }

            Button(
                onClick = { isRiding = false; isPaused = false },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                enabled = isRiding,
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
            ) {
                Text("⏹", fontSize = 20.sp)
            }
        }

        // 6. Données compactes
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DataItem("Batterie", "${scooterData.battery.toInt()}%")
                DataItem("Voltage", "%.1fV".format(scooterData.voltage))
                DataItem("Odomètre", "%.1fkm".format(scooterData.odometer))
                TemperatureIndicator(temperature = scooterData.temperature)
            }
        }

        // 7. Trip stats
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DataItem("Trajet", "%.1fkm".format(scooterData.tripDistance))
                DataItem("Temps", scooterData.totalRideTime)
                DataItem("Puissance", "%.0fW".format(scooterData.power))
            }
        }
    }

    if (showActionsPopup) {
        ActionsPopup(
            scooterData = scooterData, // Passe scooterData au popup
            onDismiss = { showActionsPopup = false },
            bluetoothManager = bluetoothManager,
            scope = scope
        )
    }
}

@Composable
private fun ActionsPopup(
    scooterData: ScooterData, // Reçoit scooterData
    onDismiss: () -> Unit,
    bluetoothManager: BluetoothRepository,
    scope: CoroutineScope
) {
    // ⚠️ Utilise directement scooterData au lieu d'états locaux
    val headlights = scooterData.headlightsOn
    val neonLights = scooterData.neonOn

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Actions", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (headlights) "💡" else "⚫", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Phares", fontSize = 16.sp)
                    }
                    Switch(
                        checked = headlights,
                        onCheckedChange = { newValue ->
                            android.util.Log.e("RideScreen", "🔍 Switch cliqué! Nouvelle valeur: $newValue")
                            android.util.Log.e("RideScreen", "🔍 bluetoothManager: $bluetoothManager")
                            android.util.Log.e("RideScreen", "🔍 connector: ${bluetoothManager.connector}")

                            scope.launch {
                                android.util.Log.e("RideScreen", "🔍 Dans la coroutine!")
                                bluetoothManager.connector?.setLights(newValue)
                                android.util.Log.e("RideScreen", "🔍 Après setLights")
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (neonLights) "🟣" else "⚫", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Néons", fontSize = 16.sp)
                    }
                    Switch(
                        checked = neonLights, // Reflète l'état de la trottinette
                        onCheckedChange = {
                            scope.launch {
                                bluetoothManager.connector?.setNeon(it)
                            }
                        }
                    )
                }

                HorizontalDivider()

                Button(
                    onClick = { },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("🔊 Klaxon", fontSize = 16.sp)
                }

                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("💾 Enregistrer trajet", fontSize = 16.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
private fun TemperatureIndicator(temperature: Float) {
    val (emoji, color, warning) = when {
        temperature > TemperatureThresholds.MOTOR_CRITICAL -> Triple("🔥", Color(0xFFF44336), true)
        temperature > TemperatureThresholds.MOTOR_WARNING -> Triple("🌡️", Color(0xFFFF9800), true)
        else -> Triple("🌡️", Color(0xFF4CAF50), false)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { }
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Text(
            text = "${temperature.toInt()}°C",
            fontSize = 14.sp,
            fontWeight = if (warning) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
        Text(text = "Temp", fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
private fun DataItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
private fun CompactSpeedometer(speed: Float, maxSpeed: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${speed.toInt()}", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "km/h", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "max ${maxSpeed.toInt()}", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun RealTimeGraph(
    isRiding: Boolean,
    currentSpeed: Float,
    currentBattery: Float,
    maxSpeed: Float
) {
    var speedHistory by remember { mutableStateOf(listOf<Float>()) }
    var batteryHistory by remember { mutableStateOf(listOf<Float>()) }

    LaunchedEffect(isRiding, currentSpeed, currentBattery) {
        if (isRiding) {
            speedHistory = speedHistory.takeLast(49) + currentSpeed
            batteryHistory = batteryHistory.takeLast(49) + currentBattery
        } else {
            speedHistory = emptyList()
            batteryHistory = emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        if (speedHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("▶ pour démarrer", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val pointSpacing = width / speedHistory.size.coerceAtLeast(1)

                val speedPath = Path()
                speedHistory.forEachIndexed { index, speed ->
                    val x = index * pointSpacing
                    val y = height - (speed / maxSpeed * height).coerceIn(0f, height)
                    if (index == 0) speedPath.moveTo(x, y) else speedPath.lineTo(x, y)
                }
                drawPath(path = speedPath, color = Color(0xFF2196F3), style = Stroke(width = 3f))

                val batteryPath = Path()
                batteryHistory.forEachIndexed { index, battery ->
                    val x = index * pointSpacing
                    val y = height - (battery / 100f * height)
                    if (index == 0) batteryPath.moveTo(x, y) else batteryPath.lineTo(x, y)
                }
                drawPath(path = batteryPath, color = Color(0xFF4CAF50), style = Stroke(width = 2f))
            }
        }
    }
}