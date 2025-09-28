package com.ix7.tracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.ScooterData
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

data class RideSession(
    val id: Int, // ID numérique au lieu de UUID
    val startTime: Long,
    val endTime: Long? = null,
    val startOdometer: Float,
    val endOdometer: Float? = null,
    val startBattery: Float,
    val endBattery: Float? = null,
    val maxSpeed: Float = 0f,
    val avgSpeed: Float = 0f,
    val isPaused: Boolean = false
)

data class SpeedRecord(
    val timestamp: Long,
    val speed: Float,
    val battery: Float
)

enum class RideMode {
    PEDESTRIAN, ECO, RACE, POWER
}

enum class WheelMode {
    ONE_WHEEL, TWO_WHEELS
}

enum class SpeedUnit {
    KMH, MPH
}

var sessionCounter = 1 // Compteur global pour les IDs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideScreen(
    scooterData: ScooterData,
    isConnected: Boolean
) {
    var currentSession by remember { mutableStateOf<RideSession?>(null) }
    var speedRecords by remember { mutableStateOf<List<SpeedRecord>>(emptyList()) }
    var wheelMode by remember { mutableStateOf(WheelMode.ONE_WHEEL) }
    var speedUnit by remember { mutableStateOf(SpeedUnit.KMH) }
    var rideMode by remember { mutableStateOf(RideMode.ECO) }
    var leftTurn by remember { mutableStateOf(false) }
    var rightTurn by remember { mutableStateOf(false) }
    var warningLights by remember { mutableStateOf(false) }
    var headlights by remember { mutableStateOf(false) }
    var neonLights by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var savedSessionId by remember { mutableStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header avec contrôles principaux
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode de roues (Scooter/Moto)
                Button(
                    onClick = {
                        wheelMode = if (wheelMode == WheelMode.ONE_WHEEL) WheelMode.TWO_WHEELS else WheelMode.ONE_WHEEL
                    },
                    modifier = Modifier.size(60.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (wheelMode == WheelMode.ONE_WHEEL) "🛴" else "🏍️", fontSize = 24.sp)
                }

                // Unité de vitesse
                Button(
                    onClick = { speedUnit = if (speedUnit == SpeedUnit.KMH) SpeedUnit.MPH else SpeedUnit.KMH },
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(speedUnit.name, fontWeight = FontWeight.Bold)
                }

                // Bouton Lock/Unlock plus gros
                Button(
                    onClick = { isLocked = !isLocked },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLocked) Color.Red else Color.Green
                    ),
                    modifier = Modifier.size(80.dp), // Plus gros
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isLocked) "🔒" else "🔓",
                        fontSize = 28.sp // Plus gros emoji
                    )
                }
            }
        }

        // Compteur de vitesse redesigné
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Aiguille en haut à gauche
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawSpeedometerNeedle(scooterData.speed, 50f, this)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Vitesse digitale à droite
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${scooterData.speed.toInt()}",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Text(
                            text = speedUnit.name.lowercase(),
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Graphique temps réel (toujours visible)
        item {
            GraphSection(speedRecords)
        }

        // Contrôles sous le graphique
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Clignotants à gauche
                Column {
                    Button(
                        onClick = { leftTurn = !leftTurn; if (leftTurn) { rightTurn = false; warningLights = false } },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (leftTurn) Color(0xFFFF8C00) else Color.Gray
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text("◀", fontSize = 20.sp) // Flèche identique mais dans l'autre sens
                    }

                    Button(
                        onClick = { rightTurn = !rightTurn; if (rightTurn) { leftTurn = false; warningLights = false } },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (rightTurn) Color(0xFFFF8C00) else Color.Gray
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text("▶", fontSize = 20.sp)
                    }

                    Button(
                        onClick = {
                            warningLights = !warningLights
                            if (warningLights) { leftTurn = false; rightTurn = false }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (warningLights) Color.Red else Color.Gray
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text("⚠", fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Modes de conduite
                ModeSelectorSection(rideMode) { newMode -> rideMode = newMode }
            }
        }

        // Boutons de contrôle de trajet
        item {
            RideControlButtons(
                currentSession = currentSession,
                onStartRide = {
                    currentSession = RideSession(
                        id = sessionCounter++,
                        startTime = System.currentTimeMillis(),
                        startOdometer = scooterData.odometer,
                        startBattery = scooterData.battery
                    )
                    speedRecords = emptyList()
                },
                onPauseRide = {
                    currentSession?.let { session ->
                        currentSession = session.copy(isPaused = !session.isPaused)
                    }
                },
                onStopRide = {
                    currentSession?.let { session ->
                        val finalSession = session.copy(
                            endTime = System.currentTimeMillis(),
                            endOdometer = scooterData.odometer,
                            endBattery = scooterData.battery
                        )
                        savedSessionId = finalSession.id
                        showSaveDialog = true
                        currentSession = null
                        speedRecords = emptyList()
                    }
                }
            )
        }

        // Tableau des trajets (toujours visible)
        item {
            RideTable(
                session = currentSession ?: RideSession(
                    id = 0,
                    startTime = 0L,
                    startOdometer = 0f,
                    startBattery = 0f
                ),
                currentData = scooterData,
                speedRecords = speedRecords,
                isEmpty = currentSession == null
            )
        }

        // Section Actions organisée
        item {
            ActionsSection(
                headlights = headlights,
                neonLights = neonLights,
                onHeadlightsToggle = { headlights = !headlights },
                onNeonToggle = { neonLights = !neonLights },
                onHornPress = { /* TODO: Klaxon */ }
            )
        }
    }

    // Dialog de sauvegarde
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("✅ Trajet Enregistré") },
            text = { Text("Trajet enregistré sous l'ID : $savedSessionId") },
            confirmButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun GraphSection(speedRecords: List<SpeedRecord>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (speedRecords.size >= 2) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Dessiner le graphique
                    val width = size.width
                    val height = size.height
                    val maxSpeed = speedRecords.maxOfOrNull { it.speed } ?: 50f

                    val speedPath = Path()
                    speedRecords.forEachIndexed { index, record ->
                        val x = width * index / (speedRecords.size - 1)
                        val y = height - (height * record.speed / maxSpeed)

                        if (index == 0) speedPath.moveTo(x, y)
                        else speedPath.lineTo(x, y)
                    }

                    drawPath(
                        path = speedPath,
                        color = Color.Blue,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📈 Graphique temps réel\n(démarrez un trajet)",
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun ModeSelectorSection(currentMode: RideMode, onModeChange: (RideMode) -> Unit) {
    Card {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Mode de conduite",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RideMode.values().forEach { mode ->
                    Button(
                        onClick = { onModeChange(mode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentMode == mode) Color.Yellow else Color.Gray
                        ),
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = mode.name.take(3),
                            fontSize = 10.sp,
                            fontWeight = if (currentMode == mode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RideControlButtons(
    currentSession: RideSession?,
    onStartRide: () -> Unit,
    onPauseRide: () -> Unit,
    onStopRide: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = onStartRide,
            enabled = currentSession == null,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
        ) {
            Text("▶")
        }

        Button(
            onClick = onPauseRide,
            enabled = currentSession != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (currentSession?.isPaused == true) Color.Blue else Color(0xFFFF9800)
            )
        ) {
            Text(if (currentSession?.isPaused == true) "▶" else "⏸")
        }

        Button(
            onClick = onStopRide,
            enabled = currentSession != null,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("⏹")
        }
    }
}

@Composable
fun RideTable(
    session: RideSession,
    currentData: ScooterData,
    speedRecords: List<SpeedRecord>,
    isEmpty: Boolean
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy\nHH:mm:ss", Locale.getDefault())

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            // En-têtes plus larges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("ID", "🚀 Départ", "📍 Actuel", "🏁 Arrivée").forEach { header ->
                    Text(
                        text = header,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Données
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = if (isEmpty) "-" else "${session.id}",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = Color.Yellow,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isEmpty) "-" else dateFormat.format(Date(session.startTime)),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = Color.Yellow
                )
                Text(
                    text = if (isEmpty) "-" else dateFormat.format(Date()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = Color.Yellow
                )
                Text(
                    text = if (isEmpty || session.endTime == null) "-" else dateFormat.format(Date(session.endTime)),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = Color.Yellow
                )
            }

            // Ligne batterie avec emojis
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("🔋 ${if (isEmpty) "-" else "${session.startBattery.toInt()}%"}", fontSize = 11.sp, color = Color.Yellow, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("🔋 ${currentData.battery.toInt()}%", fontSize = 11.sp, color = Color.Yellow, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("🔋 ${if (isEmpty || session.endBattery == null) "-" else "${session.endBattery.toInt()}%"}", fontSize = 11.sp, color = Color.Yellow, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }

            // Ligne kilomètres avec emojis
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("🛣️ ${if (isEmpty) "-" else "${session.startOdometer}km"}", fontSize = 11.sp, color = Color.Yellow, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("🛣️ ${currentData.odometer}km", fontSize = 11.sp, color = Color.Yellow, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("🛣️ ${if (isEmpty || session.endOdometer == null) "-" else "${session.endOdometer}km"}", fontSize = 11.sp, color = Color.Yellow, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }

            // Ligne vitesse moyenne
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("", modifier = Modifier.weight(1f))
                Text("🏃 Vmoy: ${if (speedRecords.isEmpty()) 0 else speedRecords.map { it.speed }.average().toInt()}km/h",
                    fontSize = 11.sp, color = Color.Yellow, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ActionsSection(
    headlights: Boolean,
    neonLights: Boolean,
    onHeadlightsToggle: () -> Unit,
    onNeonToggle: () -> Unit,
    onHornPress: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF006064))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "🎛️ ACTIONS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Section Lumières
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💡 Lumières", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    IconButton(
                        onClick = onHeadlightsToggle,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (headlights) Color.Yellow else Color.Black,
                                CircleShape
                            )
                    ) {
                        Text("💡", fontSize = 20.sp)
                    }

                    IconButton(
                        onClick = onNeonToggle,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (neonLights) Color.Magenta else Color.Black,
                                CircleShape
                            )
                    ) {
                        Text("✨", fontSize = 20.sp)
                    }
                }

                // Klaxon
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔊 Son", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = onHornPress,
                        modifier = Modifier.size(60.dp) // Plus gros
                    ) {
                        Text("📯", fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

// Fonction pour dessiner l'aiguille du compteur
fun drawSpeedometerNeedle(currentSpeed: Float, maxSpeed: Float, drawScope: DrawScope) {
    with(drawScope) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 * 0.8f

        // Arc de fond
        drawArc(
            color = Color.Gray,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 8.dp.toPx()),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )

        // Aiguille
        val speedAngle = 135f + (270f * (currentSpeed / maxSpeed).coerceIn(0f, 1f))
        val needleLength = radius * 0.7f
        val needleX = center.x + needleLength * cos(Math.toRadians(speedAngle.toDouble())).toFloat()
        val needleY = center.y + needleLength * sin(Math.toRadians(speedAngle.toDouble())).toFloat()

        drawLine(
            color = Color.Red,
            start = center,
            end = Offset(needleX, needleY),
            strokeWidth = 4.dp.toPx()
        )

        // Centre
        drawCircle(
            color = Color.Red,
            radius = 8.dp.toPx(),
            center = center
        )
    }
}