package com.ix7.tracker.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class TripData(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0,
    val startBattery: Int = 0,
    val endBattery: Int = 0,
    val startOdometer: Float = 0f,
    val endOdometer: Float = 0f,
    val maxSpeed: Float = 0f,
    val avgSpeed: Float = 0f
)

@Composable
fun TripRecorderScreen(
    onBackClick: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var currentTrip by remember { mutableStateOf<TripData?>(null) }
    var trips by remember { mutableStateOf<List<TripData>>(emptyList()) }

    var currentSpeed by remember { mutableStateOf(0f) }
    var currentBattery by remember { mutableStateOf(75) }
    var currentOdometer by remember { mutableStateOf(123.45f) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // Simulation des données
    LaunchedEffect(isRecording) {
        while (isRecording || true) {
            currentSpeed = if (isRecording) (Math.random() * 50).toFloat() else 0f
            if (isRecording) {
                currentBattery = (currentBattery - 0.1f).toInt().coerceAtLeast(0)
                currentOdometer += (Math.random() * 0.05).toFloat()
            }
            currentTime = System.currentTimeMillis()
            delay(500)
        }
    }

    if (showHistory) {
        TripHistoryScreen(
            trips = trips,
            onBackClick = { showHistory = false }
        )
    } else {
        // ========== ENREGISTREMENT - MONTRE RONDE ==========
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // VITESSE ÉNORME AU CENTRE
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = currentSpeed.toInt().toString(),
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    Text(
                        text = "km/h",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.offset(y = (-10).dp)
                    )
                }
            }

            // Boutons EN HAUT DE LA ZONE CENTRALE
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(28.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // REC / STOP
                Button(
                    onClick = {
                        if (!isRecording) {
                            currentTrip = TripData(
                                startTime = System.currentTimeMillis(),
                                startBattery = currentBattery,
                                startOdometer = currentOdometer
                            )
                            isRecording = true
                        } else {
                            currentTrip?.let { trip ->
                                val completed = trip.copy(
                                    endTime = System.currentTimeMillis(),
                                    endBattery = currentBattery,
                                    endOdometer = currentOdometer,
                                    avgSpeed = currentSpeed
                                )
                                trips = trips + completed
                            }
                            isRecording = false
                            currentTrip = null
                        }
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isRecording) Color(0xFF8B0000) else Color(0xFF2d5a2d)
                    ),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        text = if (isRecording) "⏹" else "⏺️",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                // Historique
                Button(
                    onClick = { showHistory = true },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1a4d1a)),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text("📊", fontSize = 12.sp)
                }

                // Retour
                Button(
                    onClick = onBackClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333)),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text("←", fontSize = 12.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Infos essentielles - MÊME LARGEUR QUE LES BOUTONS
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .background(Color(0xFF1a1a1a), RoundedCornerShape(6.dp))
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timeFormat.format(Date(currentTime)),
                    color = Color(0xFFFFD700),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "🔋 $currentBattery%",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "🏁 %.1f km".format(currentOdometer),
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}