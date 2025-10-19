package com.ix7.tracker.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TripHistoryScreen(
    trips: List<TripData>,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(4.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "📊 Historique",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD700),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (trips.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucun trajet", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(0.9f)
                    .align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(trips) { trip ->
                    TripHistoryItem(trip)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.CenterHorizontally)
                .height(28.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333)),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(3.dp)
        ) {
            Text("← Retour", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TripHistoryItem(trip: TripData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1a1a1a), RoundedCornerShape(4.dp))
            .padding(6.dp)
    ) {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dateFormat.format(Date(trip.startTime)),
                color = Color(0xFFFFD700),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Vitesse moy: %.1f km/h".format(trip.avgSpeed),
                color = Color.White,
                fontSize = 12.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "🔋 ${trip.startBattery}% → ${trip.endBattery}%",
                color = Color(0xFF00FF00),
                fontSize = 11.sp
            )
            Text(
                text = "🏁 %.1f → %.1f km".format(trip.startOdometer, trip.endOdometer),
                color = Color(0xFF00FFFF),
                fontSize = 11.sp
            )
        }
    }
}