package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

// Modèle de données pour un trajet
data class Trip(
    val id: String,
    val date: Date,
    val distance: Float, // en km
    val duration: Long, // en secondes
    val maxSpeed: Float, // en km/h
    val avgSpeed: Float, // en km/h
    val startBattery: Int, // en %
    val endBattery: Int, // en %
    val energyUsed: Float // en Wh
)

@Composable
fun TripHistoryScreen() {
    // TODO: Récupérer les trajets depuis la base de données
    val trips = remember {
        mutableStateOf(
            listOf(
                // Exemples de trajets
                Trip(
                    id = "1",
                    date = Date(System.currentTimeMillis() - 86400000),
                    distance = 12.5f,
                    duration = 1860, // 31 minutes
                    maxSpeed = 42f,
                    avgSpeed = 24f,
                    startBattery = 100,
                    endBattery = 68,
                    energyUsed = 245f
                ),
                Trip(
                    id = "2",
                    date = Date(System.currentTimeMillis() - 172800000),
                    distance = 8.3f,
                    duration = 1200,
                    maxSpeed = 38f,
                    avgSpeed = 25f,
                    startBattery = 68,
                    endBattery = 45,
                    energyUsed = 180f
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // En-tête
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📊 Historique des trajets",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${trips.value.size} trajet${if (trips.value.size > 1) "s" else ""}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            // Bouton d'export
            OutlinedButton(
                onClick = { /* TODO: Exporter */ },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF0A84FF)
                )
            ) {
                Text("📤 Exporter")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Statistiques globales
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = "🛣️",
                    value = "%.1f km".format(trips.value.sumOf { it.distance.toDouble() }),
                    label = "Total"
                )
                StatItem(
                    icon = "⏱️",
                    value = formatTotalDuration(trips.value.sumOf { it.duration }),
                    label = "Temps"
                )
                StatItem(
                    icon = "⚡",
                    value = "%.0f Wh".format(trips.value.sumOf { it.energyUsed.toDouble() }),
                    label = "Énergie"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Liste des trajets
        if (trips.value.isEmpty()) {
            // État vide
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🛴",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aucun trajet enregistré",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vos trajets apparaîtront ici",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trips.value) { trip ->
                    TripCard(trip = trip)
                }
            }
        }
    }
}

@Composable
private fun StatItem(icon: String, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun TripCard(trip: Trip) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // En-tête avec date et durée
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📅",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = dateFormat.format(trip.date),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = timeFormat.format(trip.date),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Text(
                    text = formatDuration(trip.duration),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0A84FF),
                    modifier = Modifier
                        .background(
                            color = Color(0xFF0A84FF).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Statistiques du trajet
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TripStat(icon = "🛣️", value = "%.1f km".format(trip.distance), label = "Distance")
                TripStat(icon = "🏃", value = "%.0f km/h".format(trip.maxSpeed), label = "Max")
                TripStat(icon = "📊", value = "%.0f km/h".format(trip.avgSpeed), label = "Moyenne")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Batterie et énergie
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔋", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${trip.startBattery}% → ${trip.endBattery}%",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(-${trip.startBattery - trip.endBattery}%)",
                        fontSize = 14.sp,
                        color = Color(0xFFFF9500)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⚡", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%.0f Wh".format(trip.energyUsed),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun TripStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}min"
        else -> "${minutes}min"
    }
}

private fun formatTotalDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return "${hours}h ${minutes}m"
}