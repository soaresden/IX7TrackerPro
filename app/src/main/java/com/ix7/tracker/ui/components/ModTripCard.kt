package com.ix7.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.data.Trip
import java.text.SimpleDateFormat
import java.util.*

/**
 * Affiche une carte d'un trajet avec option de sélection
 */
@Composable
fun TripCard(
    trip: Trip,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelection: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val durationHours = trip.duration / (1000 * 60 * 60)
    val durationMins = (trip.duration / (1000 * 60)) % 60

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = selectionMode) { if (selectionMode) onToggleSelection() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected && selectionMode)
                Color(0xFF0A84FF).copy(alpha = 0.2f)
            else
                Color(0xFF2C2C2E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Conteneur principal
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            dateFormat.format(trip.startDate),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "→ ${dateFormat.format(trip.endDate)}",
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
                            "${trip.startBattery - trip.endBattery}%",
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatColumn(
                        label = "Distance",
                        value = "${"%.1f".format(trip.distance)} km",
                        color = Color(0xFFFF9500)
                    )
                    StatColumn(
                        label = "Durée",
                        value = "$durationHours h ${"$durationMins".padStart(2, '0')} min",
                        color = Color(0xFF0A84FF)
                    )
                    StatColumn(
                        label = "Vitesse moy",
                        value = "${"%.1f".format(trip.avgSpeed)} km/h",
                        color = Color(0xFFBB86FC)
                    )
                }
            }

            // Checkbox de sélection
            if (selectionMode) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Sélectionner",
                    tint = if (isSelected) Color(0xFF0A84FF) else Color.Gray,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onToggleSelection() }
                )
            }
        }
    }
}