package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.ix7.tracker.ui.components.HistogrammeComponent
import com.ix7.tracker.ui.components.HistogrammeData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeComparisonDetailScreen(
    trips: List<Trip>,
    onBackClick: () -> Unit
) {
    val modeStats = remember(trips) {
        calculateAutonomyByMode(trips)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analyse des modes", color = Color.White) },
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
            // 📊 GRAPHIQUE 8 COURBES D'AUTONOMIE
            Text(
                "Autonomie par mode",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            AutonomyCurvesGraphic(modeStats)

            Spacer(modifier = Modifier.height(8.dp))

            // 📋 HISTOGRAMME TEMPS PAR MODE
            Text(
                "Temps d'utilisation par mode",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            ModeTimeHistogram(modeStats)

            Spacer(modifier = Modifier.height(8.dp))

            // 📈 DÉTAIL PAR MODE
            Text(
                "Détails par mode",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    Triple("Piéton", Color(0xFF0A84FF), listOf("Bridé", "Débridé")),
                    Triple("Eco", Color(0xFF4CAF50), listOf("Bridé", "Débridé")),
                    Triple("Race", Color(0xFFFF9500), listOf("Bridé", "Débridé")),
                    Triple("Sport", Color(0xFFFF3B30), listOf("Bridé", "Débridé"))
                ).forEach { (modeName, color, variants) ->
                    ModeDetailExpandableCard(modeName, color, variants, modeStats)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun AutonomyCurvesGraphic(modeStats: Map<String, ModeAutonomyData>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // En-tête
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("100% → 0%", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text("Autonomie - 8 courbes", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }

        // Zone graphique
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1C1C1E), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("📊 Courbes d'autonomie", fontSize = 11.sp, color = Color.Gray)
                Text("(Piéton/Eco/Race/Sport)", fontSize = 10.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                // Légende compacte
                Column(
                    modifier = Modifier
                        .background(Color(0xFF2C2C2E), RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ModeLegendItem("Piéton Bridé / Débridé", Color(0xFF0A84FF))
                    ModeLegendItem("Eco Bridé / Débridé", Color(0xFF4CAF50))
                    ModeLegendItem("Race Bridé / Débridé", Color(0xFFFF9500))
                    ModeLegendItem("Sport Bridé / Débridé", Color(0xFFFF3B30))
                }
            }
        }

        // Axe X
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("0 km", fontSize = 10.sp, color = Color.Gray)
            Text("Distance parcourue", fontSize = 10.sp, color = Color(0xFF0A84FF), fontWeight = FontWeight.Bold)
            Text("X km", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ModeLegendItem(label: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(label, fontSize = 9.sp, color = Color.Gray)
    }
}

@Composable
fun ModeTimeHistogram(modeStats: Map<String, ModeAutonomyData>) {
    val totalTime = modeStats.values.sumOf { it.totalDuration }

    val histoData = listOf(
        "Piéton" to (modeStats["pieton_bridé"]?.totalDuration ?: 0L),
        "Piéton ⚡" to (modeStats["pieton_débridé"]?.totalDuration ?: 0L),
        "Eco" to (modeStats["eco_bridé"]?.totalDuration ?: 0L),
        "Eco ⚡" to (modeStats["eco_débridé"]?.totalDuration ?: 0L),
        "Race" to (modeStats["race_bridé"]?.totalDuration ?: 0L),
        "Race ⚡" to (modeStats["race_débridé"]?.totalDuration ?: 0L),
        "Sport" to (modeStats["sport_bridé"]?.totalDuration ?: 0L),
        "Sport ⚡" to (modeStats["sport_débridé"]?.totalDuration ?: 0L)
    ).filter { it.second > 0 }.map { (label, duration) ->
        val percentage = if (totalTime > 0) (duration.toFloat() / totalTime * 100) else 0f
        val color = when {
            label.contains("Piéton") -> Color(0xFF0A84FF)
            label.contains("Eco") -> Color(0xFF4CAF50)
            label.contains("Race") -> Color(0xFFFF9500)
            label.contains("Sport") -> Color(0xFFFF3B30)
            else -> Color.Gray
        }
        HistogrammeData(label, duration, percentage, color)
    }

    HistogrammeComponent(
        data = histoData,
        maxValue = histoData.maxOfOrNull { it.value } ?: 0L,
        displayFormat = { seconds -> "${seconds / 60}m" }
    )
}

@Composable
fun ModeDetailExpandableCard(
    modeName: String,
    color: Color,
    variants: List<String>,
    modeStats: Map<String, ModeAutonomyData>
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modeName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )

                Text(
                    if (expanded) "▼" else "▶",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))

                variants.forEach { variant ->
                    val key = "${modeName.lowercase()}_${if (variant == "Débridé") "débridé" else "bridé"}"
                    val data = modeStats[key]

                    if (data != null && data.tripCount > 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1C1C1E), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                variant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Trajets: ${data.tripCount}", fontSize = 10.sp, color = Color.Gray)
                                Text("${"%.1f".format(data.totalDistance)} km", fontSize = 10.sp, color = Color.Gray)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Consommation: ${"%.2f".format(data.avgConsumption)} %/km", fontSize = 10.sp, color = Color(0xFFFF9500))
                                Text("Autonomie: ${"%.0f".format(data.autonomyRange)} km", fontSize = 10.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

// DATA CLASS
data class ModeAutonomyData(
    val tripCount: Int,
    val totalDistance: Float,
    val totalDuration: Long,
    val avgConsumption: Float, // % par km
    val autonomyRange: Float  // km autonomy à 100%
)

// CALCUL D'AUTONOMIE
fun calculateAutonomyByMode(trips: List<Trip>): Map<String, ModeAutonomyData> {
    val modes = listOf(
        "pieton_bridé" to Pair("Pieton", "Bridé"),
        "pieton_débridé" to Pair("Pieton", "Débridé"),
        "eco_bridé" to Pair("Eco", "Bridé"),
        "eco_débridé" to Pair("Eco", "Débridé"),
        "race_bridé" to Pair("Race", "Bridé"),
        "race_débridé" to Pair("Race", "Débridé"),
        "sport_bridé" to Pair("Sport", "Bridé"),
        "sport_débridé" to Pair("Sport", "Débridé")
    )

    return modes.associate { (key, modeFilter) ->
        val filtered = trips.filter { trip ->
            trip.settings.ridingMode.name.equals(modeFilter.first, ignoreCase = true) &&
                    trip.settings.driveMode.name.contains(if (modeFilter.second == "Débridé") "Débridé" else "Bridé", ignoreCase = true)
        }

        val data = if (filtered.isEmpty()) {
            ModeAutonomyData(0, 0f, 0, 0f, 0f)
        } else {
            val totalDistance = filtered.sumOf { it.distance.toDouble() }.toFloat()
            val totalDuration = filtered.sumOf { it.duration }
            val totalBatteryUsed = filtered.sumOf { (it.startBattery - it.endBattery).toDouble() }.toFloat()
            val tripCount = filtered.size

            val avgConsumption = if (totalDistance > 0) totalBatteryUsed / totalDistance else 0f
            val autonomyRange = if (avgConsumption > 0) 100f / avgConsumption else 0f

            ModeAutonomyData(
                tripCount = tripCount,
                totalDistance = totalDistance,
                totalDuration = totalDuration,
                avgConsumption = avgConsumption,
                autonomyRange = autonomyRange
            )
        }

        key to data
    }
}