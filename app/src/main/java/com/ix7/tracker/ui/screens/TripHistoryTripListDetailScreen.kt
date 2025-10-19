package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.data.Trip
import java.text.SimpleDateFormat
import java.util.*

// ════════════════════════════════════════════════════════════════
// 📋 TRIP DETAIL SCREEN
// ════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryTripListDetailScreen(
    trip: Trip,
    tripNumber: Int,
    onBack: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEE dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val batteryUsed = trip.startBattery - trip.endBattery
    val durationHours = trip.duration / (1000 * 60 * 60)
    val durationMins = (trip.duration / (1000 * 60)) % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
    ) {
        // Header avec bouton retour
        TopAppBar(
            title = { Text("Trajet n°$tripNumber", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF2C2C2E)
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DetailSection("📊 Informations générales") {
                    DetailRow("Date de début", dateFormat.format(trip.startDate))
                    DetailRow("Date de fin", dateFormat.format(trip.endDate))
                    DetailRow("Durée", "$durationHours h ${"$durationMins".padStart(2, '0')} min")
                }
            }

            item {
                DetailSection("📏 Distance et vitesse") {
                    DetailRow("Distance", "${"%.2f".format(trip.distance)} km")
                    DetailRow("Vitesse moyenne", "${"%.1f".format(trip.avgSpeed)} km/h")
                    DetailRow("Vitesse max", "${"%.1f".format(trip.maxSpeed)} km/h")
                }
            }

            item {
                DetailSection("🔋 Batterie") {
                    DetailRow("Batterie initiale", "${trip.startBattery}%")
                    DetailRow("Batterie finale", "${trip.endBattery}%")
                    DetailRow("Consommation", "$batteryUsed%")
                    DetailRow("Énergie utilisée", "${"%.1f".format(trip.energyUsed)} Wh")
                }
            }

            item {
                DetailSection("📍 Localisation") {
                    DetailRow(
                        "Départ",
                        "${trip.startLocation.address}\n${trip.startLocation.latitude}, ${trip.startLocation.longitude}"
                    )
                    DetailRow(
                        "Arrivée",
                        "${trip.endLocation.address}\n${trip.endLocation.latitude}, ${trip.endLocation.longitude}"
                    )
                }
            }

            item {
                DetailSection("⚙️ Paramètres") {
                    DetailRow("Mode de conduite", trip.settings.ridingMode.name)
                    DetailRow("Mode de conduite", trip.settings.driveMode.name)
                    DetailRow("Limite de vitesse", trip.settings.speedLock.name)
                }
            }

            item {
                DetailSection("📈 Statistiques de vitesse") {
                    DetailRow("0 km/h", "${trip.speedStats.range0} fois")
                    DetailRow("0-10 km/h", "${trip.speedStats.range0_10} fois")
                    DetailRow("10-20 km/h", "${trip.speedStats.range10_20} fois")
                    DetailRow("20-30 km/h", "${trip.speedStats.range20_30} fois")
                    DetailRow("30-40 km/h", "${trip.speedStats.range30_40} fois")
                    DetailRow("40-50 km/h", "${trip.speedStats.range40_50} fois")
                    DetailRow("50-60 km/h", "${trip.speedStats.range50_60} fois")
                    DetailRow("> 60 km/h", "${trip.speedStats.rangeAbove60} fois")
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0A84FF),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 13.sp, color = Color.White)
    }
}