package com.ix7.tracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.ScooterData
import com.ix7.tracker.utils.FormatUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

data class RideSession(
    val id: String,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideScreen(
    scooterData: ScooterData,
    isConnected: Boolean
) {
    var currentSession by remember { mutableStateOf<RideSession?>(null) }
    var speedRecords by remember { mutableStateOf<List<SpeedRecord>>(emptyList()) }
    var isDebrided by remember { mutableStateOf(false) }
    var wheelMode by remember { mutableStateOf(WheelMode.ONE_WHEEL) }
    var speedUnit by remember { mutableStateOf(SpeedUnit.KMH) }
    var rideMode by remember { mutableStateOf(RideMode.ECO) }
    var leftTurn by remember { mutableStateOf(false) }
    var rightTurn by remember { mutableStateOf(false) }
    var warningLights by remember { mutableStateOf(false) }
    var headlights by remember { mutableStateOf(false) }
    var neonLights by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }

    // Calcul de la vitesse maximale selon le mode
    val maxSpeed = remember(isDebrided, wheelMode, rideMode) {
        when {
            isDebrided && wheelMode == WheelMode.ONE_WHEEL -> when (rideMode) {
                RideMode.PEDESTRIAN -> 20f
                RideMode.ECO -> 30f
                RideMode.RACE -> 40f
                RideMode.POWER -> 50f
            }
            isDebrided && wheelMode == WheelMode.TWO_WHEELS -> when (rideMode) {
                RideMode.PEDESTRIAN -> 15f
                RideMode.ECO -> 30f
                RideMode.RACE -> 45f
                RideMode.POWER -> 60f
            }
            else -> when (rideMode) { // Mode bridé
                RideMode.PEDESTRIAN -> 5f
                RideMode.ECO -> 10f
                RideMode.RACE -> 15f
                RideMode.POWER -> 25f
            }
        }
    }

    // Collecte des données toutes les 5 secondes pendant un trajet actif
    LaunchedEffect(currentSession, isConnected) {
        if (currentSession != null && !currentSession!!.isPaused && isConnected) {
            kotlinx.coroutines.delay(5000)
            val newRecord = SpeedRecord(
                timestamp = System.currentTimeMillis(),
                speed = scooterData.speed,
                battery = scooterData.battery
            )
            speedRecords = speedRecords + newRecord
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compteur de vitesse avec flèches et vitesse
        SpeedometerSection(
            currentSpeed = scooterData.speed,
            maxSpeed = maxSpeed,
            speedUnit = speedUnit,
            leftTurn = leftTurn,
            rightTurn = rightTurn,
            onLeftTurnClick = { leftTurn = !leftTurn; warningLights = false },
            onRightTurnClick = { rightTurn = !rightTurn; warningLights = false }
        )

        // Graphique en temps réel
        if (speedRecords.isNotEmpty()) {
            RealTimeChart(
                speedRecords = speedRecords,
                maxSpeed = maxSpeed,
                modifier = Modifier.height(120.dp)
            )
        }

        // Section mode et warning
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Bouton warning
            Button(
                onClick = {
                    warningLights = !warningLights
                    if (warningLights) {
                        leftTurn = false
                        rightTurn = false
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (warningLights) Color.Red else Color.Gray
                ),
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("⚠", fontSize = 20.sp)
            }

            // Mode et roues
            Card(
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("${rideMode.name.take(3)}. / ${if (wheelMode == WheelMode.ONE_WHEEL) "Eco" else "Rac"}.")
                    Text(if (wheelMode == WheelMode.ONE_WHEEL) "🛴" else "🏍", fontSize = 20.sp)
                }
            }
        }

        // Boutons de contrôle de trajet
        RideControlButtons(
            currentSession = currentSession,
            onStartRide = {
                currentSession = RideSession(
                    id = UUID.randomUUID().toString(),
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
                currentSession = currentSession?.copy(
                    endTime = System.currentTimeMillis(),
                    endOdometer = scooterData.odometer,
                    endBattery = scooterData.battery
                )
                // Ici on sauvegarderait la session
                currentSession = null
                speedRecords = emptyList()
            }
        )

        // APRÈS (solution)
        currentSession?.let { session ->
            RideTable(
                session = session,
                currentData = scooterData,
                speedRecords = speedRecords
            )
        }

        // Section Actions
        ActionsSection(
            headlights = headlights,
            neonLights = neonLights,
            wheelMode = wheelMode,
            speedUnit = speedUnit,
            isDebrided = isDebrided,
            isLocked = isLocked,
            onHeadlightsToggle = { headlights = !headlights },
            onNeonToggle = { neonLights = !neonLights },
            onWheelModeToggle = {
                wheelMode = if (wheelMode == WheelMode.ONE_WHEEL) WheelMode.TWO_WHEELS else WheelMode.ONE_WHEEL
            },
            onSpeedUnitToggle = {
                speedUnit = if (speedUnit == SpeedUnit.KMH) SpeedUnit.MPH else SpeedUnit.KMH
            },
            onDebridToggle = { isDebrided = !isDebrided },
            onLockToggle = { isLocked = !isLocked },
            onHornPress = { /* TODO: Déclencher klaxon */ }
        )
    }
}

@Composable
fun SpeedometerSection(
    currentSpeed: Float,
    maxSpeed: Float,
    speedUnit: SpeedUnit,
    leftTurn: Boolean,
    rightTurn: Boolean,
    onLeftTurnClick: () -> Unit,
    onRightTurnClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Flèche gauche
        Button(
            onClick = onLeftTurnClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (leftTurn) Color(0xFFFF8C00) else Color.Gray
            ),
            modifier = Modifier.size(48.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("⬅", fontSize = 20.sp)
        }

        // Compteur principal
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawSpeedometer(currentSpeed, maxSpeed, this)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${currentSpeed.toInt()}",
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

        // Flèche droite
        Button(
            onClick = onRightTurnClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (rightTurn) Color(0xFFFF8C00) else Color.Gray
            ),
            modifier = Modifier.size(48.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("➡", fontSize = 20.sp)
        }
    }
}

fun drawSpeedometer(currentSpeed: Float, maxSpeed: Float, drawScope: DrawScope) {
    with(drawScope) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2 * 0.8f

        // Arc principal
        drawArc(
            color = Color.Gray,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 8.dp.toPx()),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )

        // Graduations
        for (i in 0..10) {
            val angle = 135f + (270f * i / 10f)
            val startRadius = radius * 0.85f
            val endRadius = radius * 0.95f

            val startX = center.x + startRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val startY = center.y + startRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
            val endX = center.x + endRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val endY = center.y + endRadius * sin(Math.toRadians(angle.toDouble())).toFloat()

            drawLine(
                color = Color.Black,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx()
            )
        }

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

@Composable
fun RealTimeChart(
    speedRecords: List<SpeedRecord>,
    maxSpeed: Float,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            if (speedRecords.size >= 2) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val padding = 20.dp.toPx()

                    // Lignes de vitesse
                    val speedPath = Path()
                    speedRecords.forEachIndexed { index, record ->
                        val x = padding + (width - 2 * padding) * index / (speedRecords.size - 1)
                        val y = height - padding - (height - 2 * padding) * (record.speed / maxSpeed)

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

                    // Ligne de batterie
                    val batteryPath = Path()
                    speedRecords.forEachIndexed { index, record ->
                        val x = padding + (width - 2 * padding) * index / (speedRecords.size - 1)
                        val y = height - padding - (height - 2 * padding) * (record.battery / 100f)

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
    speedRecords: List<SpeedRecord>
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy\nHH:mm:ss", Locale.getDefault())

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            // En-têtes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("IdTrajet", "Départ", "Actuel", "Arrivée").forEach { header ->
                    Text(
                        text = header,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            Divider()

            // Ligne de données
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = session.id.take(8),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp
                )
                Text(
                    text = dateFormat.format(Date(session.startTime)),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp
                )
                Text(
                    text = dateFormat.format(Date()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = Color.Blue
                )
                Text(
                    text = if (session.endTime != null) dateFormat.format(Date(session.endTime)) else "",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Données de batterie et vitesse
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("Bat: ${session.startBattery}%", fontSize = 10.sp)
                Text("Bat: ${currentData.battery}%", fontSize = 10.sp, color = Color.Blue)
                Text("Bat: ${session.endBattery?.let { "$it%" } ?: ""}", fontSize = 10.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("Km Départ\n${session.startOdometer}", fontSize = 10.sp)
                Text("Km Actuel\n${currentData.odometer}", fontSize = 10.sp, color = Color.Blue)
                Text("Km Arrivée\n${session.endOdometer ?: ""}", fontSize = 10.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("")
                Text("Vmoy: ${speedRecords.map { it.speed }.average().toInt()}\nkm/h",
                    fontSize = 10.sp, color = Color.Blue)
                Text("Vmoy Arrivée:", fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun ActionsSection(
    headlights: Boolean,
    neonLights: Boolean,
    wheelMode: WheelMode,
    speedUnit: SpeedUnit,
    isDebrided: Boolean,
    isLocked: Boolean,
    onHeadlightsToggle: () -> Unit,
    onNeonToggle: () -> Unit,
    onWheelModeToggle: () -> Unit,
    onSpeedUnitToggle: () -> Unit,
    onDebridToggle: () -> Unit,
    onLockToggle: () -> Unit,
    onHornPress: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF006064))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "ACTIONS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Phares
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

                // Néon
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

                // Mode roues
                IconButton(
                    onClick = onWheelModeToggle,
                    modifier = Modifier.size(48.dp)
                ) {
                    Text(if (wheelMode == WheelMode.ONE_WHEEL) "🛴" else "🏍", fontSize = 20.sp)
                }

                // Unité de vitesse
                Button(
                    onClick = onSpeedUnitToggle,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(speedUnit.name)
                }

                // Bridage
                IconButton(
                    onClick = onDebridToggle,
                    modifier = Modifier.size(48.dp)
                ) {
                    Text(if (isDebrided) "🔓" else "🔒", fontSize = 20.sp)
                }

                // Klaxon
                IconButton(
                    onClick = onHornPress,
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("📯", fontSize = 20.sp)
                }

                // Lock parking
                IconButton(
                    onClick = onLockToggle,
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("🔑", fontSize = 20.sp, color = if (isLocked) Color.Red else Color.Black)
                }
            }
        }
    }
}