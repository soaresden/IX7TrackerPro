package com.ix7.tracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.ix7.tracker.utils.TripUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TripCard(
    trip: Trip,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF0A84FF).copy(alpha = 0.3f) else Color(0xFF2C2C2E)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (selectionMode) onToggleSelection() else expanded = !expanded
                    }
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectionMode) {
                        Checkbox(checked = isSelected, onCheckedChange = { onToggleSelection() })
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Column {
                        Text(
                            text = dateFormat.format(trip.startDate),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${timeFormat.format(trip.startDate)} → ${timeFormat.format(trip.endDate)}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "${getModeIcon(trip.settings)} ${getModeText(trip.settings)}",
                            fontSize = 10.sp,
                            color = Color(0xFF0A84FF)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoChip("🛣️", "%.1f km".format(trip.distance))
                    InfoChip("⏱️", TripUtils.formatDurationSimple(trip.duration))
                    InfoChip("🔋", "${trip.startBattery}% → ${trip.endBattery}%")
                }

                if (!selectionMode) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded && !selectionMode) {
                TripDetailsContent(trip = trip)
            }
        }
    }
}

@Composable
private fun TripDetailsContent(trip: Trip) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("🚀 Départ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.height(6.dp))
                DetailLine("📅", SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(trip.startDate))
                DetailLine("🕐", SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(trip.startDate))
                DetailLine("🔋", "${trip.startBattery}%")
                DetailLine("📏", "%.2f km".format(trip.startOdometer))
                DetailLine("📍", trip.startLocation.address.ifEmpty { "%.4f, %.4f".format(trip.startLocation.latitude, trip.startLocation.longitude) })
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("⏱️", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = TripUtils.formatDurationSimple(trip.duration),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9500)
                )
                Text("Temps de trajet", fontSize = 9.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "🔋 -${trip.startBattery - trip.endBattery}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9500)
                )
                Text("Consommée", fontSize = 9.sp, color = Color.Gray)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("🏁 Arrivée", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                Spacer(modifier = Modifier.height(6.dp))
                DetailLine("📅", SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(trip.endDate))
                DetailLine("🕐", SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(trip.endDate))
                DetailLine("🔋", "${trip.endBattery}%")
                DetailLine("📏", "%.2f km".format(trip.endOdometer))
                DetailLine("📍", trip.endLocation.address.ifEmpty { "%.4f, %.4f".format(trip.endLocation.latitude, trip.endLocation.longitude) })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = Color.Gray.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(2f)) {
                Text("📊 Répartition des vitesses", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))

                SpeedHistogram(
                    speedStats = trip.speedStats,
                    totalDuration = trip.duration,
                    height = 200.dp
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("🏃 Vitesses", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                StatRow("Max", "%.0f km/h".format(trip.maxSpeed))
                StatRow("Moy", "%.0f km/h".format(trip.avgSpeed))
                Divider(color = Color.Gray.copy(alpha = 0.3f))
                StatRow("⚡ Énergie", "%.0f Wh".format(trip.energyUsed))
            }
        }
    }
}

// PUBLIC - utilisable partout
@Composable
fun InfoChip(icon: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = icon, fontSize = 13.sp)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}

@Composable
private fun DetailLine(icon: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(icon, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
    Spacer(modifier = Modifier.height(3.dp))
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

fun getModeIcon(settings: TripSettings): String {
    return when (settings.ridingMode) {
        RidingMode.PEDESTRIAN -> "🚶"
        RidingMode.ECO -> "🍃"
        RidingMode.SPORT -> "⚡"
        RidingMode.RACE -> "🏁"
    }
}

fun getModeText(settings: TripSettings): String {
    val mode = when (settings.ridingMode) {
        RidingMode.PEDESTRIAN -> "Piéton"
        RidingMode.ECO -> "Eco"
        RidingMode.SPORT -> "Sport"
        RidingMode.RACE -> "Race"
    }
    val drive = when (settings.driveMode) {
        DriveMode.ONE_WHEEL -> "1R"
        DriveMode.TWO_WHEELS -> "2R"
    }
    val lock = when (settings.speedLock) {
        SpeedLock.LOCKED -> "🔒"
        SpeedLock.UNLOCKED -> "🔓"
    }
    return "$mode $drive $lock"
}