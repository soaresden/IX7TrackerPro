// RideScreen.kt - Version compacte et complète
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.ScooterData
import com.ix7.tracker.ui.components.ride.*
import kotlin.math.*

enum class RideMode(val emoji: String, val label: String) {
    PEDESTRIAN("🚶", "Piéton"),
    ECO("🌱", "Eco"),
    RACE("🏎️", "Race"),
    POWER("🔥", "Power")
}

enum class WheelMode(val emoji: String, val label: String) {
    ONE_WHEEL("🛴", "1 roue"),
    TWO_WHEELS("🏍️", "2 roues")
}

enum class SpeedUnit {
    KMH, MPH
}

data class SpeedLimits(
    val pedestrian: Int,
    val eco: Int,
    val race: Int,
    val power: Int
)

@Composable
fun RideScreen(
    scooterData: ScooterData,
    isConnected: Boolean
) {
    var wheelMode by remember { mutableStateOf(WheelMode.ONE_WHEEL) }
    var speedUnit by remember { mutableStateOf(SpeedUnit.KMH) }
    var rideMode by remember { mutableStateOf(RideMode.ECO) }
    var isLocked by remember { mutableStateOf(false) }
    var isRiding by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var isDebridged by remember { mutableStateOf(true) }
    var showActionsPopup by remember { mutableStateOf(false) }
    var leftTurn by remember { mutableStateOf(false) }
    var rightTurn by remember { mutableStateOf(false) }
    var warningLights by remember { mutableStateOf(false) }

    // Définir les limites de vitesse selon le mode
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
        RideMode.POWER -> speedLimits.power
    }

    val displayMaxSpeed = if (speedUnit == SpeedUnit.MPH) (maxSpeed * 0.621371).toInt() else maxSpeed
    val currentSpeed = if (speedUnit == SpeedUnit.MPH) scooterData.speed * 0.621371f else scooterData.speed

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            // Compteur en arc
            CompactSpeedometer(
                speed = if (isConnected) currentSpeed else 0f,
                maxSpeed = displayMaxSpeed.toFloat(),
                modifier = Modifier.weight(1f)
            )

            // Lock à droite
            Button(
                onClick = { isLocked = !isLocked },
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
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Ligne 1: Type véhicule et contrôles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                wheelMode = if (wheelMode == WheelMode.ONE_WHEEL) WheelMode.TWO_WHEELS else WheelMode.ONE_WHEEL
                            },
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(wheelMode.emoji, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(wheelMode.label, fontSize = 12.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { isDebridged = !isDebridged },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDebridged) Color.Red else Color.Gray
                            ),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(if (isDebridged) "Débridé" else "Bridé", fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                speedUnit = if (speedUnit == SpeedUnit.KMH) SpeedUnit.MPH else SpeedUnit.KMH
                            },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(speedUnit.name, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ligne 2: Modes uniformes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RideMode.values().forEach { mode ->
                        val maxSpeedForMode = when (mode) {
                            RideMode.PEDESTRIAN -> speedLimits.pedestrian
                            RideMode.ECO -> speedLimits.eco
                            RideMode.RACE -> speedLimits.race
                            RideMode.POWER -> speedLimits.power
                        }

                        val displaySpeed = if (speedUnit == SpeedUnit.MPH) {
                            (maxSpeedForMode * 0.621371).toInt()
                        } else {
                            maxSpeedForMode
                        }

                        Button(
                            onClick = { rideMode = mode },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (rideMode == mode)
                                    MaterialTheme.colorScheme.primary else
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(mode.emoji, fontSize = 14.sp)
                                Text(mode.label, fontSize = 8.sp, maxLines = 1)
                                Text("$displaySpeed", fontSize = 8.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // 3. Graphique compact
        Card(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            RealTimeSpeedGraph(
                isRiding = isRiding,
                currentSpeed = if (isConnected) currentSpeed else 0f,
                currentBattery = if (isConnected) scooterData.battery else 0f,
                maxSpeed = displayMaxSpeed.toFloat()
            )
        }

        // 4. Clignotants
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    leftTurn = !leftTurn
                    if (leftTurn) { rightTurn = false; warningLights = false }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (leftTurn) Color(0xFFFFB300) else Color.Gray
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Text("◀", fontSize = 16.sp, color = Color.White)
            }

            Button(
                onClick = {
                    warningLights = !warningLights
                    if (warningLights) { leftTurn = false; rightTurn = false }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (warningLights) Color.Red else Color.Gray
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Text("⚠", fontSize = 16.sp)
            }

            Button(
                onClick = {
                    rightTurn = !rightTurn
                    if (rightTurn) { leftTurn = false; warningLights = false }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (rightTurn) Color(0xFFFFB300) else Color.Gray
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Text("▶", fontSize = 16.sp, color = Color.White)
            }
        }

        // 5. Contrôles de trajet
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { isRiding = true; isPaused = false },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                enabled = !isRiding || isPaused
            ) {
                Text("▶", fontSize = 16.sp)
            }

            Button(
                onClick = { isPaused = !isPaused },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                enabled = isRiding
            ) {
                Text(if (isPaused) "▶" else "⏸", fontSize = 16.sp)
            }

            Button(
                onClick = { isRiding = false; isPaused = false },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                enabled = isRiding
            ) {
                Text("⏹", fontSize = 16.sp)
            }
        }

        // 6. Données compactes
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DataItem("Batterie", "${scooterData.battery.toInt()}%")
                DataItem("Voltage", "${scooterData.voltage}V")
                DataItem("Odomètre", "${scooterData.odometer}km")
                DataItem("Temp", "${scooterData.temperature.toInt()}°C")
            }
        }

        // 7. Actions
        Button(
            onClick = { showActionsPopup = true },
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Actions", fontSize = 12.sp)
        }
    }

    // Popup des actions
    if (showActionsPopup) {
        ActionsPopup(onDismiss = { showActionsPopup = false })
    }
}

@Composable
fun CompactSpeedometer(
    speed: Float,
    maxSpeed: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val center = center
            val radius = size.minDimension / 2 * 0.8f

            // Arc de fond
            drawArc(
                color = Color.Gray.copy(alpha = 0.3f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )

            // Arc de vitesse
            val speedAngle = (speed / maxSpeed) * 180f
            drawArc(
                color = when {
                    speed < maxSpeed * 0.6f -> Color.Green
                    speed < maxSpeed * 0.8f -> Color.Yellow
                    else -> Color.Red
                },
                startAngle = 180f,
                sweepAngle = speedAngle,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )

            // Graduations
            for (i in 0..4) {
                val angle = 180f + (i * 45f)
                val angleRad = Math.toRadians(angle.toDouble())
                val startRadius = radius * 0.9f
                val endRadius = radius

                val startX = center.x + cos(angleRad).toFloat() * startRadius
                val startY = center.y + sin(angleRad).toFloat() * startRadius
                val endX = center.x + cos(angleRad).toFloat() * endRadius
                val endY = center.y + sin(angleRad).toFloat() * endRadius

                drawLine(
                    color = Color.Gray,
                    start = androidx.compose.ui.geometry.Offset(startX, startY),
                    end = androidx.compose.ui.geometry.Offset(endX, endY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Affichage numérique
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = 20.dp)
        ) {
            Text(
                text = "${speed.toInt()}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    speed < maxSpeed * 0.6f -> Color.Green
                    speed < maxSpeed * 0.8f -> Color.Yellow
                    else -> Color.Red
                }
            )
            Text(
                text = "Max: $maxSpeed",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun DataItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun RealTimeSpeedGraph(
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

    Box(
        modifier = Modifier.fillMaxSize().padding(8.dp)
    ) {
        if (speedHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("▶ pour démarrer", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val pointSpacing = width / speedHistory.size.coerceAtLeast(1)

                // Courbe de vitesse (bleue)
                val                 speedPath = Path()
                speedHistory.forEachIndexed { index, speed ->
                    val x = index * pointSpacing
                    val y = height - (speed / maxSpeed * height)
                    if (index == 0) {
                        speedPath.moveTo(x, y)
                    } else {
                        speedPath.lineTo(x, y)
                    }
                }
                drawPath(
                    path = speedPath,
                    color = Color.Blue,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Courbe de batterie (verte)
                val batteryPath = Path()
                batteryHistory.forEachIndexed { index, battery ->
                    val x = index * pointSpacing
                    val y = height - (battery / 100f * height)
                    if (index == 0) {
                        batteryPath.moveTo(x, y)
                    } else {
                        batteryPath.lineTo(x, y)
                    }
                }
                drawPath(
                    path = batteryPath,
                    color = Color.Green,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun ActionsPopup(
    onDismiss: () -> Unit
) {
    var headlights by remember { mutableStateOf(false) }
    var neonLights by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { /* Ne se ferme pas automatiquement */ },
        title = { Text("Actions", fontSize = 16.sp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Phares avec illustration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (headlights) "💡" else "🔦", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Phares", fontSize = 14.sp)
                    }
                    Switch(
                        checked = headlights,
                        onCheckedChange = { headlights = it }
                    )
                }

                // Néons avec illustration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (neonLights) "🌈" else "💫", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Néons", fontSize = 14.sp)
                    }
                    Switch(
                        checked = neonLights,
                        onCheckedChange = { neonLights = it }
                    )
                }

                Divider()

                // Klaxon
                Button(
                    onClick = { /* TODO: Klaxon */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔊 Klaxon", fontSize = 14.sp)
                }

                // Enregistrement de trajet
                Button(
                    onClick = { /* TODO: Save ride */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                ) {
                    Text("💾 Enregistrer trajet", fontSize = 14.sp)
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