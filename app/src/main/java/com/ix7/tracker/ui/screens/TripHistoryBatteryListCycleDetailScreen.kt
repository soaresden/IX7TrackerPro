package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.ui.components.BatteryCycleData
import java.text.SimpleDateFormat
import java.util.*

// ════════════════════════════════════════════════════════════════
// 🔋 BATTERY CYCLE DETAIL SCREEN
// ════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryBatteryListCycleDetailScreen(
    batteryCycle: BatteryCycleData,
    onBack: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val healthPercentage = ((batteryCycle.endBattery - batteryCycle.cycleDifference) / batteryCycle.startBattery) * 100

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
    ) {
        TopAppBar(
            title = { Text("Cycle #${batteryCycle.cycleNumber}", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF2C2C2E)
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BatteryDetailSection("📊 Informations du cycle") {
                    BatteryDetailRow("Cycle", "#${batteryCycle.cycleNumber}")
                    BatteryDetailRow("Date de début", dateFormat.format(batteryCycle.startDate))
                    BatteryDetailRow("Date de fin", dateFormat.format(batteryCycle.endDate))
                    BatteryDetailRow("Durée", "${(batteryCycle.endDate.time - batteryCycle.startDate.time) / (1000 * 60 * 60)} heures")
                }
            }

            item {
                BatteryDetailSection("🔋 Niveaux de batterie") {
                    BatteryDetailRow("Batterie initiale", "${batteryCycle.startBattery}%", Color(0xFF0A84FF))
                    BatteryDetailRow("Batterie finale", "${batteryCycle.endBattery}%", Color(0xFFFF9500))
                    BatteryDetailRow("Différence", "${batteryCycle.cycleDifference}%", Color(0xFF4CAF50))
                    BatteryDetailRow("Consommation moyenne", "${"%.1f".format(batteryCycle.cycleDifference / batteryCycle.tripCount)}% par trajet")
                }
            }

            item {
                BatteryDetailSection("🏥 Santé de la batterie") {
                    BatteryDetailRow("Santé estimée", "${"%.1f".format(healthPercentage)}%")
                    LinearProgressIndicator(
                        progress = { healthPercentage.toFloat() / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = when {
                            healthPercentage >= 80f -> Color(0xFF4CAF50)
                            healthPercentage >= 60f -> Color(0xFF0A84FF)
                            healthPercentage >= 40f -> Color(0xFFFF9500)
                            else -> Color.Red
                        },
                        trackColor = Color(0xFF3A3A3C)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BatteryDetailRow("État", when {
                        healthPercentage >= 80f -> "Excellent"
                        healthPercentage >= 60f -> "Bon"
                        healthPercentage >= 40f -> "Acceptable"
                        else -> "Critique"
                    })
                }
            }

            item {
                BatteryDetailSection("📈 Statistiques") {
                    BatteryDetailRow("Nombre de trajets", "${batteryCycle.tripCount}")
                    BatteryDetailRow("Usure totale", "${batteryCycle.cycleDifference}%")
                    BatteryDetailRow("Usure moyenne par trajet", "${"%.2f".format(batteryCycle.cycleDifference.toFloat() / batteryCycle.tripCount)}%")
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BatteryDetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun BatteryDetailRow(
    label: String,
    value: String,
    color: Color = Color.White
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 13.sp, color = color, fontWeight = FontWeight.Bold)
    }
}