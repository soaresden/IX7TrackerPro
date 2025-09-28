package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.ScooterData
import com.ix7.tracker.core.ConnectionState

@Composable
fun CompactDataScreen(
    scooterData: ScooterData,
    isConnected: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // État de connexion
        item {
            StatusCard(
                text = if (isConnected) "🟢 Connecté" else "🔴 Déconnecté",
                color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }

        // Section Données Temps Réel
        item {
            SectionHeader("⚡ Données Temps Réel")
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DataCard("🏃 Vitesse", "${scooterData.speed.toInt()} km/h", Color(0xFF2196F3), Modifier.weight(1f))
                DataCard("🔋 Batterie", "${scooterData.battery.toInt()}%", getBatteryColor(scooterData.battery), Modifier.weight(1f))
            }
        }

        // Section Électrique
        item {
            SectionHeader("⚡ Système Électrique")
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DataCard("⚡ Voltage", "${scooterData.voltage}V", Color(0xFFFF9800), Modifier.weight(1f))
                DataCard("🔌 Courant", "${scooterData.current}A", Color(0xFF00BCD4), Modifier.weight(1f))
            }
        }

        item {
            DataCard("💪 Puissance", "${scooterData.power.toInt()}W", Color(0xFFE91E63))
        }

        // Section Températures
        item {
            SectionHeader("🌡️ Températures")
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DataCard("🌡️ Scooter", "${scooterData.temperature.toInt()}°C", getTemperatureColor(scooterData.temperature), Modifier.weight(1f))
                DataCard("🔥 Batterie", "${scooterData.batteryTemperature.toInt()}°C", getTemperatureColor(scooterData.batteryTemperature), Modifier.weight(1f))
            }
        }

        // Section Historique
        item {
            SectionHeader("📊 Historique")
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DataCard("🛣️ Total", "${scooterData.odometer}km", Color(0xFF9C27B0), Modifier.weight(1f))
                DataCard("📍 Trajet", "${scooterData.tripDistance}km", Color(0xFF795548), Modifier.weight(1f))
            }
        }

        item {
            DataCard("⏱️ Temps Total", scooterData.totalRideTime, Color(0xFF607D8B))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun StatusCard(text: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth().height(60.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DataCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = modifier.height(80.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = color,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getBatteryColor(battery: Float): Color {
    return when {
        battery > 50f -> Color(0xFF4CAF50)
        battery > 20f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

private fun getTemperatureColor(temperature: Float): Color {
    return when {
        temperature > 60f -> Color(0xFFF44336)
        temperature > 45f -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }
}