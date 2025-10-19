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
                title = { Text("Cycle $cycleNumber", color = Color.White) },
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
            // 🔋 EN-TÊTE DU CYCLE
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Chargée à ${cycle.startBattery}% le ${dateFormat.format(cycle.startDate)} ${timeFormat.format(cycle.startDate)}",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "au ${dateFormat.format(cycle.endDate)} ${timeFormat.format(cycle.endDate)} à ${cycle.endBattery}%",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // 📊 STATS DU CYCLE
            Text(
                "Statistiques",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatRowComponent("Batterie Utilisée", "$batteryUsed%", Color(0xFFFF3B30))
                    StatRowComponent("Temps écoulé", "$durationHours h ${"$durationMins".padStart(2, '0')} min", Color(0xFF0A84FF))
                    StatRowComponent("Distance Parcourue", "${"%.2f".format(cycle.distance)} km", Color(0xFF4CAF50))

                    if (cycle.duration > 0) {
                        val avgSpeed = cycle.distance / (cycle.duration / (1000f * 3600f))
                        StatRowComponent("Vitesse moyenne", "${"%.2f".format(avgSpeed)} km/h", Color(0xFFBB86FC))

                        val autonomieIntegrale = if (batteryUsed > 0) {
                            (cycle.distance / batteryUsed) * 100
                        } else 0f
                        StatRowComponent("Autonomie à 100%", "${"%.1f".format(autonomieIntegrale)} km", Color(0xFF34C759))
                    }
                }
            }

            // 📈 GRAPHIQUE BATTERIE VS TEMPS
            Text(
                "Décharge de batterie",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            BatteryGraphComponent(cycle)

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun BatteryGraphComponent(cycle: BatteryCycleData) {
    val durationMinutes = cycle.duration / (1000 * 60)
    val startBattery = cycle.startBattery
    val endBattery = cycle.endBattery

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Titre axes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text("$startBattery%", fontSize = 10.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            Text("Batterie", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        }

        // Zone graphique
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF1C1C1E), RoundedCornerShape(4.dp))
        ) {
            // Grille de référence
            repeat(5) { i ->
                val y = (1f - i / 4f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color.Gray.copy(alpha = 0.1f))
                        .align(Alignment.TopStart)
                        .offset(y = (240.dp - 24.dp - 12.dp) * y - 2.dp)
                )
            }

            // Courbe linéaire simple (décharge)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Point de départ (haut)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF4CAF50), RoundedCornerShape(3.dp))
                    )
                }

                // Point d'arrivée (bas)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFFFF3B30), RoundedCornerShape(3.dp))
                    )
                }
            }

            // Texte informatif
            Column(
                modifier = Modifier
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "📉 Graphique batterie",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Text(
                    "${cycle.startBattery}% → ${cycle.endBattery}%",
                    fontSize = 10.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Axe X
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("0 min", fontSize = 10.sp, color = Color.Gray)
            Text("Temps", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text("$durationMinutes min", fontSize = 10.sp, color = Color.Gray)
        }
    }
}