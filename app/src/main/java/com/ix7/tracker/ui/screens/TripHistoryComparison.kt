package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.ix7.tracker.ui.components.ModeStatsData

// ════════════════════════════════════════════════════════════════
// ⚙️ MODE COMPARISON SCREEN
// ════════════════════════════════════════════════════════════════

@Composable
fun TripHistoryComparison(modeStats: List<ModeStatsData>) {
    if (modeStats.isEmpty()) {
        EmptyState("Aucune donnée de mode disponible")
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(modeStats) { stat ->
            ModeComparisonCard(stat)
        }
    }
}

@Composable
private fun ModeComparisonCard(stat: ModeStatsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stat.mode.replaceFirstChar { it.uppercase() },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "${stat.count} trajets",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Text(
                    getModeEmoji(stat.mode),
                    fontSize = 28.sp
                )
            }

            Divider(
                color = Color.Gray.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Stats Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModeStatItem(
                        label = "Distance moyenne",
                        value = "${"%.1f".format(stat.avgDistance)} km",
                        color = Color(0xFF0A84FF),
                        modifier = Modifier.weight(1f)
                    )
                    ModeStatItem(
                        label = "Durée moyenne",
                        value = "${"%.0f".format(stat.avgDuration / 60)} min",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModeStatItem(
                        label = "Vitesse moy",
                        value = "${"%.1f".format(stat.avgSpeed)} km/h",
                        color = Color(0xFFBB86FC),
                        modifier = Modifier.weight(1f)
                    )
                    ModeStatItem(
                        label = "Batterie utilisée",
                        value = "${"%.1f".format(stat.avgBatteryUsed)}%",
                        color = Color(0xFFFF9500),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModeStatItem(
                        label = "Efficacité",
                        value = "${"%.1f".format(stat.efficiency)} km/%",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    ModeStatItem(
                        label = "Vitesse max",
                        value = "${"%.1f".format(stat.maxSpeed)} km/h",
                        color = Color(0xFFE91E63),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeStatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📊", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, fontSize = 16.sp, color = Color.Gray)
        }
    }
}

private fun getModeEmoji(mode: String): String {
    return when (mode.lowercase()) {
        "eco" -> "🌱"
        "sport" -> "⚡"
        "race" -> "🏎️"
        "pedestrian" -> "🚶"
        else -> "⚙️"
    }
}