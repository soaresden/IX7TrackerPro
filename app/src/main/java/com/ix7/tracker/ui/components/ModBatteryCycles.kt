package com.ix7.tracker.ui.components

import androidx.compose.foundation.background
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Affiche les cycles de batterie détectés
 */
@Composable
fun ModBatteryCycles(cycles: List<BatteryCycleData>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (cycles.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Aucun cycle batterie détecté", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            items(cycles) { cycle ->
                BatteryCycleCard(cycle)
            }
        }
    }
}

@Composable
fun BatteryCycleCard(cycle: BatteryCycleData) {
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val durationHours = cycle.duration / (1000 * 60 * 60)
    val durationMins = (cycle.duration / (1000 * 60)) % 60
    val avgSpeedIfMoving = if (durationHours > 0) cycle.distance / (cycle.duration / (1000f * 3600f)) else 0f

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
                        dateFormat.format(cycle.startDate),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "→ ${dateFormat.format(cycle.endDate)}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color(0xFF4CAF50).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        "${cycle.startBattery - cycle.endBattery}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text("batterie", fontSize = 9.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatColumn(label = "Distance", value = "${"%.1f".format(cycle.distance)} km", color = Color(0xFFFF9500))
                StatColumn(label = "Durée", value = "$durationHours h ${"$durationMins".padStart(2, '0')} min", color = Color(0xFF0A84FF))
                StatColumn(label = "Vitesse moy", value = "${"%.1f".format(avgSpeedIfMoving)} km/h", color = Color(0xFFBB86FC))
            }
        }
    }
}