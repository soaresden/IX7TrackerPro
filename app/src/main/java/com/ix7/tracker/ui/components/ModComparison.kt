package com.ix7.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Affiche la comparaison des modes (Eco, Sport, etc.)
 */
@Composable
fun ModComparison(modeStats: List<ModeStatsData>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (modeStats.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Aucune donnée de mode disponible", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            items(modeStats) { stat ->
                ModeStatCard(stat)
            }
        }
    }
}

@Composable
fun ModeStatCard(stat: ModeStatsData) {
    val durationHours = stat.totalDuration / (1000 * 60 * 60)
    val durationMins = (stat.totalDuration / (1000 * 60)) % 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "⚙️ ${stat.modeName}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "${stat.tripCount} trajet(s)",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color(0xFFFF9500).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        "${"%.1f".format(stat.avgBatteryUsage)}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9500)
                    )
                    Text("batterie/trajet", fontSize = 8.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grille 2x2 des stats
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(
                        label = "Distance",
                        value = "${"%.1f".format(stat.totalDistance)} km",
                        color = Color(0xFF0A84FF),
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = "Durée",
                        value = "$durationHours h ${"$durationMins".padStart(2, '0')} min",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(
                        label = "Vitesse moy",
                        value = "${"%.1f".format(stat.avgSpeed)} km/h",
                        color = Color(0xFFBB86FC),
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        label = "Trajets",
                        value = "${stat.tripCount}",
                        color = Color(0xFFFF9500),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}