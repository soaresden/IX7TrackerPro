package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.ix7.tracker.data.Trip
import java.text.SimpleDateFormat
import java.util.*

// ════════════════════════════════════════════════════════════════
// 📱 TRIP LIST CARDS
// ════════════════════════════════════════════════════════════════

@Composable
fun TripHistoryTripListCards(
    trips: List<Trip>,
    selectionMode: Boolean,
    selectedTrips: Set<String>,
    onToggleSelection: (String) -> Unit,
    onDetailClick: (Trip) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = trips,
            key = { it.id }
        ) { trip ->
            val tripNumber = trips.indexOf(trip) + 1
            val isSelected = trip.id in selectedTrips

            TripCard(
                trip = trip,
                tripNumber = tripNumber,
                selectionMode = selectionMode,
                isSelected = isSelected,
                onToggleSelection = { onToggleSelection(trip.id) },
                onDetailClick = { onDetailClick(trip) }
            )
        }
    }
}

@Composable
private fun TripCard(
    trip: Trip,
    tripNumber: Int,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onDetailClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    val batteryUsed = trip.startBattery - trip.endBattery
    val durationHours = trip.duration / (1000 * 60 * 60)
    val durationMins = (trip.duration / (1000 * 60)) % 60

    val cal = Calendar.getInstance().apply { time = trip.startDate }
    val isNocturnal = cal.get(Calendar.HOUR_OF_DAY) < 4

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !selectionMode) { onDetailClick() }
            .border(
                if (isSelected) 2.dp else 0.dp,
                if (isSelected) Color(0xFF0A84FF) else Color.Transparent,
                RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 📌 Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggleSelection() })
                }

                Text(
                    "Trajet n°$tripNumber",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    dateFormat.format(trip.startDate),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ⏰ Temps
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Jour : ", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(
                    "${timeFormat.format(trip.startDate)} → ${timeFormat.format(trip.endDate)}",
                    fontSize = 11.sp,
                    color = Color.White
                )
                if (isNocturnal) {
                    Text(" (nocturne)", fontSize = 10.sp, color = Color(0xFF9933FF))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 📊 Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBadge(
                    label = "Distance",
                    value = "${"%.1f".format(trip.distance)} km",
                    color = Color(0xFF0A84FF),
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "Durée",
                    value = "$durationHours h ${"$durationMins".padStart(2, '0')} min",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "Vitesse moy",
                    value = "${"%.1f".format(trip.avgSpeed)} km/h",
                    color = Color(0xFFBB86FC),
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "Batterie",
                    value = "$batteryUsed%",
                    color = Color(0xFFFF9500),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(6.dp)
    ) {
        Text(label, fontSize = 9.sp, color = Color.Gray)
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}