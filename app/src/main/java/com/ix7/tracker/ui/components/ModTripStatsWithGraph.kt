package com.ix7.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
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
 * Affiche les stats détaillées d'un trajet avec graphique de vitesse
 */
@Composable
fun TripStatsWithGraph(trip: Trip) {
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val durationHours = trip.duration / (1000 * 60 * 60)
    val durationMins = (trip.duration / (1000 * 60)) % 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // En-tête avec dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        dateFormat.format(trip.startDate),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "→ ${dateFormat.format(trip.endDate)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color(0xFF4CAF50).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        "${trip.startBattery - trip.endBattery}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text("batterie", fontSize = 10.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grille de stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox(
                    label = "Distance",
                    value = "${"%.1f".format(trip.distance)} km",
                    color = Color(0xFF0A84FF),
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Durée",
                    value = "$durationHours h ${"%02d".format(durationMins)} min",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                StatBox(
                    label = "Vitesse moy",
                    value = "${"%.1f".format(trip.avgSpeed)} km/h",
                    color = Color(0xFFBB86FC),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Graphique de vitesse
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C1C1E), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    "Distribution des vitesses",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                SpeedHistogram(
                    speedStats = trip.speedStats,
                    totalDuration = trip.duration,
                    height = 160.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}