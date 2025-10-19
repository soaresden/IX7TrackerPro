package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.ui.components.BatteryCycleData
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryCycleDetailScreen(
    cycle: BatteryCycleData,
    cycleNumber: Int,
    onBackClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    val batteryUsed = cycle.startBattery - cycle.endBattery
    val durationHours = cycle.duration / (1000 * 60 * 60)
    val durationMins = (cycle.duration / (1000 * 60)) % 60

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cycle n°$cycleNumber", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2C2C2E)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 🔋 INFO CYCLE
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Chargée à ${cycle.startBattery}% le ${dateFormat.format(cycle.startDate)} ${timeFormat.format(cycle.startDate)}",
                        fontSize = 12.sp,
                        color = Color.White
                    )

                    Text(
                        "au ${dateFormat.format(cycle.endDate)} ${timeFormat.format(cycle.endDate)} à ${cycle.endBattery}%",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // 📊 STATS DU CYCLE
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatRow("Batterie Utilisée", "$batteryUsed%", Color(0xFFFF3B30))
                    StatRow("Temps écoulé", "$durationHours h ${"$durationMins".padStart(2, '0')} min", Color(0xFF0A84FF))
                    StatRow("Distance Parcourue", "${"%.2f".format(cycle.distance)} km", Color(0xFF4CAF50))

                    if (cycle.duration > 0) {
                        val autonomyPercent = cycle.distance / (cycle.duration / (1000f * 3600f))
                        StatRow("Autonomie moyenne", "${"%.2f".format(autonomyPercent)} km/h", Color(0xFFBB86FC))
                    }
                }
            }

            // 📈 GRAPHIQUE BATTERIE vs TEMPS (placeholder)
            Text(
                "Graphique de batterie",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            BatteryGraphPlaceholder(cycle)

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun BatteryGraphPlaceholder(cycle: BatteryCycleData) {
    val durationMinutes = cycle.duration / (1000 * 60)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Axe Y (Batterie)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text("${cycle.startBattery}%", fontSize = 10.sp, color = Color.Gray)
            Text("📉 Batterie", fontSize = 10.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        }

        // Graphique simplifié (ligne diagonale)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1C1C1E), RoundedCornerShape(4.dp))
        ) {
            // Simulation d'une courbe de décharge
            Text(
                "📊 Graphique à implémenter avec Recharts",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Axe X (Temps)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("0 min", fontSize = 10.sp, color = Color.Gray)
            Text("Temps ⏱️", fontSize = 10.sp, color = Color(0xFF0A84FF), fontWeight = FontWeight.Bold)
            Text("$durationMinutes min", fontSize = 10.sp, color = Color.Gray)
        }

        // Label axes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${cycle.endBattery}%", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun StatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}