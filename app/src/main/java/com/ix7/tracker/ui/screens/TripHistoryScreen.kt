package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen() {
    // TODO: Récupérer les trajets depuis la base de données
    val trips = remember {
        mutableStateOf(
            listOf(
                // DUMMY DATA - À remplacer par vraies données de la BDD
                Trip(
                    id = "1",
                    date = Date(System.currentTimeMillis() - 86400000),
                    distance = 12.5f,
                    duration = 1860,
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
                ),
                Trip(
                    id = "3",
                    date = Date(System.currentTimeMillis() - 259200000),
                    distance = 15.7f,
                    duration = 2100,
                    maxSpeed = 45f,
                    avgSpeed = 27f,
                    startBattery = 85,
                    endBattery = 50,
                    energyUsed = 310f
                )
            )
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }

    // Filtrer les trajets selon les dates
    val filteredTrips = remember(trips.value, startDate, endDate) {
        if (startDate == null && endDate == null) {
            trips.value
        } else {
            trips.value.filter { trip ->
                val tripTime = trip.date.time
                val start = startDate?.time ?: 0L
                val end = endDate?.time ?: Long.MAX_VALUE
                tripTime in start..end
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // En-tête compact
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📊 Trajets",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${filteredTrips.size} trajet${if (filteredTrips.size > 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Filtre date
                IconButton(
                    onClick = { showDatePicker = !showDatePicker },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (startDate != null || endDate != null) Color(0xFF0A84FF) else Color(0xFF2C2C2E)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Filtrer",
                        tint = Color.White
                    )
                }

                // Export
                TextButton(
                    onClick = { /* TODO: Exporter */ },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF0A84FF)
                    )
                ) {
                    Text("Exporter", fontSize = 12.sp)
                }
            }
        }

        // Filtres de date
        if (showDatePicker) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Filtrer par période", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Boutons période rapide
                        TextButton(onClick = {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, -7)
                            startDate = cal.time
                            endDate = Date()
                        }) {
                            Text("7j", fontSize = 11.sp)
                        }
                        TextButton(onClick = {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.MONTH, -1)
                            startDate = cal.time
                            endDate = Date()
                        }) {
                            Text("30j", fontSize = 11.sp)
                        }
                        TextButton(onClick = {
                            startDate = null
                            endDate = null
                        }) {
                            Text("Tout", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Statistiques globales compactes
        if (filteredTrips.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = "🛣️",
                        value = "%.1f km".format(filteredTrips.sumOf { it.distance.toDouble() }),
                        label = "Total",
                        compact = true
                    )
                    StatItem(
                        icon = "⏱️",
                        value = formatTotalDuration(filteredTrips.sumOf { it.duration }),
                        label = "Temps",
                        compact = true
                    )
                    StatItem(
                        icon = "⚡",
                        value = "%.0f Wh".format(filteredTrips.sumOf { it.energyUsed.toDouble() }),
                        label = "Énergie",
                        compact = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Liste des trajets
        if (filteredTrips.isEmpty()) {
            // État vide
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🛴",
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aucun trajet",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Vos trajets apparaîtront ici",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredTrips) { trip ->
                    CompactTripCard(trip = trip)
                }
            }
        }
    }
}

@Composable
private fun StatItem(icon: String, value: String, label: String, compact: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            fontSize = if (compact) 16.sp else 24.sp
        )
        Text(
            text = value,
            fontSize = if (compact) 12.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = if (compact) 10.sp else 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun CompactTripCard(trip: Trip) {
    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date et heure
            Column {
                Text(
                    text = dateFormat.format(trip.date),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = timeFormat.format(trip.date),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // Stats principales
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distance
                TripStat("🛣️", "%.1f km".format(trip.distance))
                // Vitesse max
                TripStat("🏃", "%.0f".format(trip.maxSpeed))
                // Durée
                TripStat("⏱️", formatDuration(trip.duration))
            }

            // Batterie
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(text = "🔋", fontSize = 12.sp)
                Text(
                    text = "-${trip.startBattery - trip.endBattery}%",
                    fontSize = 11.sp,
                    color = Color(0xFFFF9500)
                )
            }
        }
    }
}

@Composable
private fun TripStat(icon: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = icon, fontSize = 11.sp)
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h${minutes}m"
        else -> "${minutes}m"
    }
}

private fun formatTotalDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return "${hours}h${minutes}m"
}