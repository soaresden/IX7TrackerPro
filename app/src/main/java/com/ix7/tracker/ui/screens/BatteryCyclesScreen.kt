package com.ix7.tracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BatteryCyclesView(cycles: List<BatteryCycle>) {
    if (cycles.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔋", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Pas assez de données", color = Color.Gray, fontSize = 14.sp)
                Text("Effectuez plusieurs trajets", color = Color.Gray, fontSize = 12.sp)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                BatteryDegradationCard(cycles)
            }

            items(cycles) { cycle ->
                BatteryCycleCard(cycle)
            }
        }
    }
}

@Composable
private fun BatteryDegradationCard(cycles: List<BatteryCycle>) {
    if (cycles.size < 2) return

    val capacityPerKm = cycles.map { cycle ->
        val batteryUsed = cycle.startBattery - cycle.endBattery
        if (cycle.totalDistance > 0) batteryUsed.toFloat() / cycle.totalDistance else 0f
    }

    val avgFirst3 = capacityPerKm.take(3).average()
    val avgLast3 = capacityPerKm.takeLast(3).average()
    val degradation = ((avgLast3 - avgFirst3) / avgFirst3 * 100).toFloat()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("📈 Analyse de dégradation", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${cycles.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Cycles", fontSize = 11.sp, color = Color.Gray)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (degradation > 5) "%.1f%%".format(degradation) else "Stable",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (degradation > 10) Color(0xFFF44336) else if (degradation > 5) Color(0xFFFF9500) else Color(0xFF4CAF50)
                    )
                    Text("Dégradation", fontSize = 11.sp, color = Color.Gray)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%.1f%%/km".format(avgLast3), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Conso actuelle", fontSize = 11.sp, color = Color.Gray)
                }
            }

            if (degradation > 10) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "⚠️ Batterie en dégradation significative",
                    fontSize = 11.sp,
                    color = Color(0xFFF44336),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BatteryCycleCard(cycle: BatteryCycle) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())

    val modeStats = calculateCycleModeStats(cycle)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "🔋 Cycle #${cycle.cycleNumber}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "${cycle.startBattery}% → ${cycle.endBattery}% (-${cycle.startBattery - cycle.endBattery}%)",
                        fontSize = 12.sp,
                        color = Color(0xFFFF9500)
                    )
                    val hoursUsed = cycle.totalDuration / 3600f
                    Text(
                        "⏱️ %.1fh d'utilisation".format(hoursUsed),
                        fontSize = 10.sp,
                        color = Color(0xFF0A84FF)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoChip("🛣️", "%.1f km".format(cycle.totalDistance))
                    InfoChip("📊", "${cycle.trips.size} trajets")
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1C1E))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🚀 Début", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                            Text("${cycle.startBattery}%", fontSize = 12.sp, color = Color.White)
                            Text(dateFormat.format(cycle.startDate), fontSize = 10.sp, color = Color.Gray)
                        }

                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("%.1f km".format(cycle.totalDistance), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9500))
                            Text("parcourus", fontSize = 9.sp, color = Color.Gray)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("🏁 Fin", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                            Text("${cycle.endBattery}%", fontSize = 12.sp, color = Color.White)
                            Text(dateFormat.format(cycle.endDate), fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("⚙️ Répartition des modes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModeStatsColumn("Mode conduite", modeStats.ridingModeStats, Modifier.weight(1f))
                        ModeStatsColumn("Roues", modeStats.driveModeStats, Modifier.weight(1f))
                        ModeStatsColumn("Bridage", modeStats.speedLockStats, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("📊 Répartition des vitesses", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))

                    SpeedHistogram(
                        speedStats = cycle.aggregatedSpeedStats,
                        totalDuration = cycle.totalDuration,
                        height = 180.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeStatsColumn(
    title: String,
    stats: Map<String, Float>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(title, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))

        // Afficher TOUS les modes, même à 0%
        stats.entries.forEach { (mode, percentage) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    mode,
                    fontSize = 9.sp,
                    color = if (percentage > 0) Color.White else Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "%.0f%%".format(percentage),
                    fontSize = 9.sp,
                    color = if (percentage > 0) Color(0xFF0A84FF) else Color.Gray,
                    fontWeight = if (percentage > 0) FontWeight.Bold else FontWeight.Normal
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

private data class CycleModeStats(
    val ridingModeStats: Map<String, Float>,
    val driveModeStats: Map<String, Float>,
    val speedLockStats: Map<String, Float>
)

private fun calculateCycleModeStats(cycle: BatteryCycle): CycleModeStats {
    val totalTrips = cycle.trips.size.toFloat()

    // Stats mode de conduite - TOUS les modes
    val ridingModeCount = cycle.trips.groupBy { it.settings.ridingMode }.mapValues { it.value.size }
    val ridingModeStats = mutableMapOf<String, Float>()

    // Ajouter TOUS les modes, même à 0%
    ridingModeStats["🚶 Piéton"] = (ridingModeCount[RidingMode.PEDESTRIAN] ?: 0) / totalTrips * 100
    ridingModeStats["🍃 Eco"] = (ridingModeCount[RidingMode.ECO] ?: 0) / totalTrips * 100
    ridingModeStats["⚡ Sport"] = (ridingModeCount[RidingMode.SPORT] ?: 0) / totalTrips * 100
    ridingModeStats["🏁 Race"] = (ridingModeCount[RidingMode.RACE] ?: 0) / totalTrips * 100

    // Stats roues - TOUS les modes
    val driveModeCount = cycle.trips.groupBy { it.settings.driveMode }.mapValues { it.value.size }
    val driveModeStats = mutableMapOf<String, Float>()

    driveModeStats["1 roue"] = (driveModeCount[DriveMode.ONE_WHEEL] ?: 0) / totalTrips * 100
    driveModeStats["2 roues"] = (driveModeCount[DriveMode.TWO_WHEELS] ?: 0) / totalTrips * 100

    // Stats bridage - TOUS les modes
    val speedLockCount = cycle.trips.groupBy { it.settings.speedLock }.mapValues { it.value.size }
    val speedLockStats = mutableMapOf<String, Float>()

    speedLockStats["🔒 Bridé"] = (speedLockCount[SpeedLock.LOCKED] ?: 0) / totalTrips * 100
    speedLockStats["🔓 Débridé"] = (speedLockCount[SpeedLock.UNLOCKED] ?: 0) / totalTrips * 100

    return CycleModeStats(ridingModeStats, driveModeStats, speedLockStats)
}