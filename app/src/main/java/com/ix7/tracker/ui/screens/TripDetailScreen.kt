package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.data.Trip
import com.ix7.tracker.data.TripLocation
import com.ix7.tracker.ui.components.HistogrammeComponent
import com.ix7.tracker.ui.components.HistogrammeData
import com.ix7.tracker.ui.screens.StatRowComponent
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(trip: Trip, tripNumber: Int, onBackClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    val batteryUsed = trip.startBattery - trip.endBattery
    val durationHours = trip.duration / (1000 * 60 * 60)
    val durationMins = (trip.duration / (1000 * 60)) % 60

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trajet $tripNumber", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2C2C2E)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 📍 HORAIRES
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(dateFormat.format(trip.startDate), fontSize = 11.sp, color = Color.Gray)
                            Text(
                                timeFormat.format(trip.startDate),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0A84FF)
                            )
                        }
                        Text("→", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(dateFormat.format(trip.endDate), fontSize = 11.sp, color = Color.Gray)
                            Text(
                                timeFormat.format(trip.endDate),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatRowComponent("Durée", "$durationHours h ${"$durationMins".padStart(2, '0')} min", Color(0xFF0A84FF), modifier = Modifier.weight(1f))
                        StatRowComponent("Distance", "${"%.1f".format(trip.distance)} km", Color(0xFF4CAF50), modifier = Modifier.weight(1f))
                        StatRowComponent("Batterie", "$batteryUsed%", Color(0xFFFF3B30), modifier = Modifier.weight(1f))
                    }
                }
            }

            // 📈 MODE & PARAMÈTRES (Simplifié)
            Text(
                "Mode & Paramètres",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            ModeParametersCard(trip)

            // 🏎️ VITESSES
            Text(
                "Statistiques de vitesse",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatRowComponent("Moyenne", "${"%.1f".format(trip.avgSpeed)} km/h", Color(0xFFBB86FC), modifier = Modifier.weight(1f))
                    StatRowComponent("Maximale", "${"%.1f".format(trip.maxSpeed)} km/h", Color(0xFFFF9500), modifier = Modifier.weight(1f))
                }
            }

            // 📊 DISTRIBUTION VITESSES
            Text(
                "Temps par plage de vitesse",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            VitessDistributionHistogram(trip)

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ModeParametersCard(trip: Trip) {
    val rideModeName = trip.settings.ridingMode.name
    val isDebride = trip.settings.driveMode.name.contains("Débridé", ignoreCase = true)

    // Couleur selon mode
    val modeColor = when (rideModeName.lowercase()) {
        "pieton" -> Color(0xFF0A84FF) // Bleu
        "eco" -> Color(0xFF4CAF50) // Vert
        "race" -> Color(0xFFFF9500) // Orange
        "sport" -> Color(0xFFFF3B30) // Rouge
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Mode principal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(modeColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .border(1.dp, modeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    rideModeName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = modeColor
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Trait bridé/débridé
                    Box(
                        modifier = Modifier
                            .width(if (isDebride) 8.dp else 4.dp)
                            .height(2.dp)
                            .background(modeColor, RoundedCornerShape(1.dp))
                    )
                    if (isDebride) {
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .height(2.dp)
                                .background(modeColor, RoundedCornerShape(1.dp))
                        )
                    }
                }
            }

            // Détails
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatRowComponent(
                    "État",
                    if (isDebride) "Débridé ⚡" else "Bridé",
                    modeColor,
                    modifier = Modifier.weight(1f)
                )
                StatRowComponent(
                    "Speedlock",
                    trip.settings.speedLock.name,
                    Color(0xFFBB86FC),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun VitessDistributionHistogram(trip: Trip) {
    val totalSeconds = trip.duration / 1000

    val ranges = listOf(
        "0 km/h" to trip.speedStats.range0,
        "0-10" to trip.speedStats.range0_10,
        "10-20" to trip.speedStats.range10_20,
        "20-30" to trip.speedStats.range20_30,
        "30-40" to trip.speedStats.range30_40,
        "40-50" to trip.speedStats.range40_50,
        "50-60" to trip.speedStats.range50_60,
        ">60" to trip.speedStats.rangeAbove60
    )

    val maxSeconds = ranges.maxOf { it.second }

    val histoData = ranges.map { (label, seconds) ->
        val percentage = if (totalSeconds > 0) (seconds.toFloat() / totalSeconds * 100) else 0f
        HistogrammeData(
            label = label,
            value = seconds,
            percentage = percentage,
            color = when {
                label.contains("0 km/h") -> Color(0xFF999999)
                label.contains("0-10") -> Color(0xFF0A84FF)
                label.contains("10-20") -> Color(0xFF4CAF50)
                label.contains("20-30") -> Color(0xFFFF9500)
                label.contains("30-40") -> Color(0xFFFF3B30)
                label.contains("40-50") -> Color(0xFFBB86FC)
                label.contains("50-60") -> Color(0xFF34C759)
                else -> Color(0xFF5856D6)
            }
        )
    }

    HistogrammeComponent(
        data = histoData,
        maxValue = maxSeconds,
        displayFormat = { seconds -> "${seconds / 60}m ${seconds % 60}s" }
    )
}

// ✅ StatRowComponent importé de ScreenUtils.kt