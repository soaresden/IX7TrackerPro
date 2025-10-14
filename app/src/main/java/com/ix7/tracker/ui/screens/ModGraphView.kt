package com.ix7.tracker.ui.components

import android.location.Location
import com.ix7.tracker.core.SpeedLimitMode
import com.ix7.tracker.core.WheelMode
import com.ix7.tracker.core.RideMode
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.data.*
import com.ix7.tracker.tracker.TripRecorder
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

@Composable
fun ModGraphView(
    isRiding: Boolean,
    isPaused: Boolean,
    currentSpeed: Float,
    currentBattery: Float,
    maxSpeed: Float,
    scooterData: com.ix7.tracker.core.ScooterData,
    currentMode: com.ix7.tracker.core.RideMode,
    wheelMode: com.ix7.tracker.core.WheelMode,
    isUnlimited: Boolean,
    onStart: () -> Unit,
    onPauseToggle: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tripRecorder = remember { TripRecorder(context) }
    val isRecordingTrip by tripRecorder.isRecording.collectAsState()
    var lastLocation by remember { mutableStateOf<Location?>(null) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var speedHistory by remember { mutableStateOf(listOf<Float>()) }
    var batteryHistory by remember { mutableStateOf(listOf<Float>()) }

    // Mettre à jour l'historique ET le recorder
    LaunchedEffect(isRiding, currentSpeed, currentBattery) {
        if (isRiding && !isPaused) {
            speedHistory = speedHistory.takeLast(49) + currentSpeed
            batteryHistory = batteryHistory.takeLast(49) + currentBattery

            // Enregistrer la vitesse dans le trip
            if (isRecordingTrip) {
                tripRecorder.updateSpeed(currentSpeed)
            }
        }
        if (!isRiding) {
            speedHistory = emptyList()
            batteryHistory = emptyList()
        }
    }

    Column(modifier = modifier) {
        // Graphique
        Card(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
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

                        // Courbe vitesse (bleu)
                        val speedPath = Path()
                        speedHistory.forEachIndexed { index, speed ->
                            val x = index * pointSpacing
                            val y = height - (speed / maxSpeed * height).coerceIn(0f, height)
                            if (index == 0) speedPath.moveTo(x, y) else speedPath.lineTo(x, y)
                        }
                        drawPath(path = speedPath, color = Color(0xFF2196F3), style = Stroke(width = 3f))

                        // Courbe batterie (vert)
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

        Spacer(modifier = Modifier.height(6.dp))

        // Boutons de contrôle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // PLAY ▶
            Button(
                onClick = {
                    onStart()

                    // Démarrer l'enregistrement du trajet
                    try {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            lastLocation = location

                            tripRecorder.startTrip(
                                battery = scooterData.battery.toInt(),
                                odometer = scooterData.odometer,
                                location = location,
                                settings = TripSettings(
                                    ridingMode = when(currentMode) {
                                        com.ix7.tracker.core.RideMode.PEDESTRIAN -> RideMode.PEDESTRIAN
                                        com.ix7.tracker.core.RideMode.ECO -> RideMode.ECO
                                        com.ix7.tracker.core.RideMode.SPORT -> RideMode.SPORT
                                        com.ix7.tracker.core.RideMode.RACE -> RideMode.RACE
                                    },
                                    driveMode = when(wheelMode) {
                                        com.ix7.tracker.core.WheelMode.ONE_WHEEL -> WheelMode.ONE_WHEEL
                                        com.ix7.tracker.core.WheelMode.TWO_WHEELS -> WheelMode.TWO_WHEELS
                                    },
                                    speedLock = if (isUnlimited) SpeedLimitMode.UNLIMITED else SpeedLimitMode.LIMITED
                                )
                            )
                            Log.i("ModGraphView", "🟢 Enregistrement démarré")
                        }
                    } catch (e: Exception) {
                        Log.e("ModGraphView", "❌ Erreur location: ${e.message}")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                enabled = !isRiding || isPaused,
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
            ) {
                Text("▶", fontSize = 20.sp)
            }

            // PAUSE ⏸
            Button(
                onClick = {
                    onPauseToggle()

                    if (!isPaused) {
                        tripRecorder.pauseTrip()
                        Log.i("ModGraphView", "⏸️ Trajet en pause")
                    } else {
                        tripRecorder.resumeTrip()
                        Log.i("ModGraphView", "▶️ Trajet repris")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                enabled = isRiding,
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
            ) {
                Text(if (isPaused) "▶" else "⏸", fontSize = 20.sp)
            }

            // STOP ⏹
            Button(
                onClick = {
                    onStop()

                    // Arrêter et sauvegarder le trajet
                    scope.launch {
                        try {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                lastLocation = location

                                scope.launch {
                                    val savedTrip = tripRecorder.stopTrip(
                                        battery = scooterData.battery.toInt(),
                                        odometer = scooterData.odometer,
                                        location = location
                                    )

                                    if (savedTrip != null) {
                                        Log.i("ModGraphView", "✅ Trajet sauvegardé: ${savedTrip.distance}km en ${savedTrip.duration}s")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ModGraphView", "❌ Erreur sauvegarde: ${e.message}")
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                enabled = isRiding,
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
            ) {
                Text("⏹", fontSize = 20.sp)
            }
        }
    }
}