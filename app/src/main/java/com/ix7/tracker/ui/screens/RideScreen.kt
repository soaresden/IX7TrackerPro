// RideScreen.kt - Remanié selon spécifications
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
    ONE_WHEEL("🛴", "Trottinette"),
    TWO_WHEELS("🏍️", "Moto")
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
    var isDebridged by remember { mutableStateOf(true) } // Mode débridé par défaut
    var showActionsPopup by remember { mutableStateOf(false) }

    // Visibilité des blocs
    var showSpeedometer by remember { mutableStateOf(true) }
    var showRideModes by remember { mutableStateOf(true) }
    var showGraph by remember { mutableStateOf(true) }
    var showTurnSignals by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var showDataTable by remember { mutableStateOf(true) }

    // Définir les limites de vitesse selon le mode
    val speedLimits = when {
        isDebridged && wheelMode == WheelMode.ONE_WHEEL -> SpeedLimits(20, 30, 40, 50)
        isDebridged && wheelMode == WheelMode.TWO_WHEELS -> SpeedLimits(15, 30, 45, 60)
        !isDebridged && wheelMode == WheelMode.ONE_WHEEL -> SpeedLimits(5, 10, 15, 25)
        else -> SpeedLimits(5, 10, 15, 25) // Bridé 2 roues
    }

    val maxSpeed = when (rideMode) {
        RideMode.PEDESTRIAN -> speedLimits.pedestrian
        RideMode.ECO -> speedLimits.eco
        RideMode.RACE -> speedLimits.race
        RideMode.POWER -> speedLimits.power
    }

    // Convertir en mph si nécessaire
    val displayMaxSpeed = if (speedUnit == SpeedUnit.MPH) (maxSpeed * 0.621371).toInt() else maxSpeed
    val currentSpeed = if (speedUnit == SpeedUnit.MPH) scooterData.speed * 0.621371f else scooterData.speed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Bloc Compteur de vitesse avec Lock
        if (showSpeedometer) {
            CollapsibleBlock(
                title = "Compteur",
                isVisible = showSpeedometer,
                onToggleVisibility = { showSpeedometer = !showSpeedometer }
            ) {
                SpeedCounterWithLock(
                    speed = if (isConnected) currentSpeed else 0f,
                    speedUnit = speedUnit,
                    maxSpeed = displayMaxSpeed.toFloat(),
                    isLocked = isLocked,
                    onLockToggle = { isLocked = !isLocked }
                )
            }
        }

        // Bloc Modes de conduite avec bouton Trottinette/Moto
        if (showRideModes) {
            CollapsibleBlock(
                title = "Mode de conduite",
                isVisible = showRideModes,
                onToggleVisibility = { showRideModes = !showRideModes }
            ) {
                RideModesWithVehicle(
                    currentMode = rideMode,
                    wheelMode = wheelMode,
                    speedUnit = speedUnit,
                    speedLimits = speedLimits,
                    isDebridged = isDebridged,
                    onModeChange = { rideMode = it },
                    onWheelModeChange = { wheelMode = it },
                    onSpeedUnitChange = { speedUnit = it },
                    onDebridgedToggle = { isDebridged = !isDebridged }
                )
            }
        }

        // Bloc Graphique de vitesse
        if (showGraph) {
            CollapsibleBlock(
                title = "Graphique temps réel",
                isVisible = showGraph,
                onToggleVisibility = { showGraph = !showGraph }
            ) {
                RealTimeSpeedGraph(
                    isRiding = isRiding,
                    currentSpeed = if (isConnected) currentSpeed else 0f,
                    currentBattery = if (isConnected) scooterData.battery else 0f,
                    maxSpeed = displayMaxSpeed.toFloat()
                )
            }
        }

        // Bloc Clignotants
        if (showTurnSignals) {
            CollapsibleBlock(
                title = "Clignotants",
                isVisible = showTurnSignals,
                onToggleVisibility = { showTurnSignals = !showTurnSignals }
            ) {
                TurnSignalsDesign()
            }
        }

        // Bloc Contrôles de trajet
        if (showControls) {
            CollapsibleBlock(
                title = "Contrôles",
                isVisible = showControls,
                onToggleVisibility = { showControls = !showControls }
            ) {
                RideControlButtons(
                    isRiding = isRiding,
                    isPaused = isPaused,
                    onStartRide = { isRiding = true; isPaused = false },
                    onPauseRide = { isPaused = !isPaused },
                    onStopRide = { isRiding = false; isPaused = false }
                )
            }
        }

        // Bloc Tableau de données
        if (showDataTable) {
            CollapsibleBlock(
                title = "Données",
                isVisible = showDataTable,
                onToggleVisibility = { showDataTable = !showDataTable }
            ) {
                RideDataTable(
                    scooterData = scooterData,
                    isConnected = isConnected,
                    isRiding = isRiding
                )
            }
        }

        // Bouton Actions popup
        Button(
            onClick = { showActionsPopup = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Actions")
        }
    }

    // Popup des actions
    if (showActionsPopup) {
        ActionsPopup(
            onDismiss = { showActionsPopup = false }
        )
    }
}

@Composable
fun CollapsibleBlock(
    title: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleVisibility() }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = if (isVisible) "▲" else "▼",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isVisible) {
                content()
            }
        }
    }
}

@Composable
fun SpeedCounterWithLock(
    speed: Float,
    speedUnit: SpeedUnit,
    maxSpeed: Float,
    isLocked: Boolean,
    onLockToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compteur de vitesse
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val center = center
                val radius = size.minDimension / 2 * 0.8f

                // Dessiner le cadran
                drawCircle(
                    color = Color.Gray,
                    radius = radius,
                    style = Stroke(width = 4.dp.toPx())
                )

                // Dessiner les graduations
                for (i in 0..maxSpeed.toInt() step (maxSpeed / 5).toInt().coerceAtLeast(1)) {
                    val angle = (i / maxSpeed) * 240f - 120f
                    val angleRad = Math.toRadians(angle.toDouble())
                    val startRadius = radius * 0.85f
                    val endRadius = radius * 0.95f

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

                // Aiguille
                val normalizedSpeed = (speed / maxSpeed).coerceIn(0f, 1f)
                val needleAngle = normalizedSpeed * 240f - 120f
                val needleAngleRad = Math.toRadians(needleAngle.toDouble())
                val needleLength = radius * 0.7f

                val needleEndX = center.x + cos(needleAngleRad).toFloat() * needleLength
                val needleEndY = center.y + sin(needleAngleRad).toFloat() * needleLength

                drawLine(
                    color = if (speed > 0) Color.Red else Color.Gray,
                    start = center,
                    end = androidx.compose.ui.geometry.Offset(needleEndX, needleEndY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = Color.Black,
                    radius = 8.dp.toPx(),
                    center = center
                )
            }

            // Affichage numérique
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = 50.dp)
            ) {
                Text(
                    text = "${speed.toInt()}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (speed > 0) Color.Red else Color.Gray
                )
                Text(
                    text = "Max: $maxSpeed",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // Bouton de verrouillage
        Button(
            onClick = onLockToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLocked) Color.Red else Color.Green
            ),
            modifier = Modifier.size(80.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (isLocked) "🔒" else "🔓",
                fontSize = 32.sp
            )
        }
    }
}

@Composable
fun RideModesWithVehicle(
    currentMode: RideMode,
    wheelMode: WheelMode,
    speedUnit: SpeedUnit,
    speedLimits: SpeedLimits,
    isDebridged: Boolean,
    onModeChange: (RideMode) -> Unit,
    onWheelModeChange: (WheelMode) -> Unit,
    onSpeedUnitChange: (SpeedUnit) -> Unit,
    onDebridgedToggle: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Ligne du haut: Type véhicule et unité
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val newMode = if (wheelMode == WheelMode.ONE_WHEEL) WheelMode.TWO_WHEELS else WheelMode.ONE_WHEEL
                        onWheelModeChange(newMode)
                    },
                    modifier = Modifier.size(60.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(wheelMode.emoji, fontSize = 24.sp)
                }
                Text(
                    text = wheelMode.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDebridgedToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDebridged) Color.Red else Color.Gray
                    )
                ) {
                    Text(if (isDebridged) "Débridé" else "Bridé", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val newUnit = if (speedUnit == SpeedUnit.KMH) SpeedUnit.MPH else SpeedUnit.KMH
                        onSpeedUnitChange(newUnit)
                    }
                ) {
                    Text(speedUnit.name, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Modes de conduite avec emojis
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    onClick = { onModeChange(mode) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentMode == mode)
                            MaterialTheme.colorScheme.primary else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(mode.emoji, fontSize = 20.sp)
                        Text(mode.label, fontSize = 10.sp)
                        Text("$displaySpeed", fontSize = 8.sp, color = Color.Gray)
                    }
                }
            }
        }
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
            speedHistory = speedHistory.takeLast(99) + currentSpeed
            batteryHistory = batteryHistory.takeLast(99) + currentBattery
        } else {
            speedHistory = emptyList()
            batteryHistory = emptyList()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (speedHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Appuyez sur Play pour voir le graphique", color = Color.Gray)
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val pointSpacing = width / speedHistory.size.coerceAtLeast(1)

                    // Dessiner la grille
                    for (i in 0..5) {
                        val y = height * i / 5
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Courbe de vitesse (bleue)
                    val speedPath = Path()
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
                        style = Stroke(width = 3.dp.toPx())
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
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun TurnSignalsDesign() {
    var leftTurn by remember { mutableStateOf(false) }
    var rightTurn by remember { mutableStateOf(false) }
    var warningLights by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clignotant gauche
        Button(
            onClick = {
                leftTurn = !leftTurn
                if (leftTurn) { rightTurn = false; warningLights = false }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (leftTurn) Color(0xFFFFB300) else Color.Gray
            ),
            modifier = Modifier.size(60.dp)
        ) {
            Text("←", fontSize = 24.sp, color = Color.White)
        }

        // Warning
        Button(
            onClick = {
                warningLights = !warningLights
                if (warningLights) { leftTurn = false; rightTurn = false }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (warningLights) Color.Red else Color.Gray
            ),
            modifier = Modifier.size(60.dp)
        ) {
            Text("⚠️", fontSize = 24.sp)
        }

        // Clignotant droit
        Button(
            onClick = {
                rightTurn = !rightTurn
                if (rightTurn) { leftTurn = false; warningLights = false }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (rightTurn) Color(0xFFFFB300) else Color.Gray
            ),
            modifier = Modifier.size(60.dp)
        ) {
            Text("→", fontSize = 24.sp, color = Color.White)
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
        title = { Text("Actions") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Phares")
                    Switch(
                        checked = headlights,
                        onCheckedChange = { headlights = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Néons")
                    Switch(
                        checked = neonLights,
                        onCheckedChange = { neonLights = it }
                    )
                }

                Button(
                    onClick = { /* TODO: Klaxon */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔊 Klaxon")
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