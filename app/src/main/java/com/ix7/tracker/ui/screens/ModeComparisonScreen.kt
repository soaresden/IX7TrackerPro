package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.data.*
import com.ix7.tracker.ui.components.*
import com.ix7.tracker.utils.TripUtils

@Composable
fun ModeComparisonScreen(modeStats: List<ModeStats>) {
    if (modeStats.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📊", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Pas de données", color = Color.Gray, fontSize = 14.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Comparatif global
            item {
                ComparisonSummaryCard(modeStats)
            }

            // Chaque mode en détail
            items(modeStats) { stats ->
                ModeStatsCard(stats)
            }
        }
    }
}

@Composable
private fun ComparisonSummaryCard(modeStats: List<ModeStats>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("📊 Comparaison des modes", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))

            // Meilleur mode par critère
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val mostEconomical = modeStats.minByOrNull { it.avgBatteryConsumption }
                val fastest = modeStats.maxByOrNull { it.avgSpeed }
                val mostUsed = modeStats.maxByOrNull { it.tripCount }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🍃", fontSize = 20.sp)
                    Text("Plus éco", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        getModeText(mostEconomical?.settings ?: modeStats.first().settings),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚡", fontSize = 20.sp)
                    Text("Plus rapide", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        getModeText(fastest?.settings ?: modeStats.first().settings),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9500)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⭐", fontSize = 20.sp)
                    Text("Plus utilisé", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        getModeText(mostUsed?.settings ?: modeStats.first().settings),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A84FF)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeStatsCard(stats: ModeStats) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${getModeIcon(stats.settings)} ${getModeText(stats.settings)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "${stats.tripCount} trajet${if (stats.tripCount > 1) "s" else ""}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoChip("🛣️", "%.1f km".format(stats.totalDistance))
                    InfoChip("⚡", "%.1f%%/km".format(stats.avgBatteryConsumption))
                }

                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Réduire" else "Détails", fontSize = 11.sp)
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1C1E))
                        .padding(12.dp)
                ) {
                    // Stats rapides
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MiniStat("🏃 Max", "%.0f km/h".format(stats.maxSpeed))
                        MiniStat("📊 Moy", "%.0f km/h".format(stats.avgSpeed))
                        MiniStat("⏱️ Total", TripUtils.formatDurationFull(stats.totalDuration))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Histogramme
                    Text("📊 Répartition des vitesses", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))

                    SpeedHistogram(
                        speedStats = stats.aggregatedSpeedStats,
                        totalDuration = stats.totalDuration,
                        height = 180.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}