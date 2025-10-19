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
 * ModComparison - Affiche les statistiques de mode
 */
@Composable
fun ModComparison(modeStats: List<ModeStatsData>) {
    if (modeStats.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucune donnée de mode disponible", color = Color.Gray, fontSize = 14.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(modeStats) { stat ->
            ModComparisonCard(stat)
        }
    }
}

@Composable
private fun ModComparisonCard(stat: ModeStatsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Mode name header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stat.mode.uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "${stat.count} trajets",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatItem(
                        label = "Distance moy",
                        value = "${"%.1f".format(stat.avgDistance)} km",
                        color = Color(0xFFFF9500),
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        label = "Durée moy",
                        value = "${stat.avgDuration / 60000} min",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatItem(
                        label = "Vitesse moy",
                        value = "${"%.1f".format(stat.avgSpeed)} km/h",
                        color = Color(0xFFBB86FC),
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        label = "Batterie utilisée",
                        value = "${"%.1f".format(stat.avgBatteryUsed)}%",
                        color = Color(0xFFFF5252),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatItem(
                        label = "Efficacité",
                        value = "${"%.2f".format(stat.efficiency)} km/%",
                        color = Color(0xFF76FF03),
                        modifier = Modifier.weight(1f)
                    )
                    StatItem(
                        label = "Vitesse max",
                        value = "${"%.1f".format(stat.maxSpeed)} km/h",
                        color = Color(0xFFFF1744),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}