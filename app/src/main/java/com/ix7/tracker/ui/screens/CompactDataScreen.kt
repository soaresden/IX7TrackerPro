package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
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
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // État de connexion
        StatusCard(
            text = if (isConnected) "🟢 Connecté" else "🔴 Déconnecté",
            color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
        )

        // Section principale - Données temps réel (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DataCard("🏃 Vitesse", "${if (isConnected) scooterData.speed.toInt() else 0} km/h", Color(0xFF2196F3), Modifier.weight(1f))
            DataCard("🔋 Batterie", "${if (isConnected) scooterData.battery.toInt() else 0}%", getBatteryColor(if (isConnected) scooterData.battery else 0f), Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DataCard("⚡ Voltage", "${if (isConnected) scooterData.voltage else 0}V", Color(0xFFFF9800), Modifier.weight(1f))
            DataCard("🌡️ Temp", "${if (isConnected) scooterData.temperature.toInt() else 0}°C", getTemperatureColor(if (isConnected) scooterData.temperature else 0f), Modifier.weight(1f))
        }

        // Section électrique (1x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DataCard("🔌 Courant", "${if (isConnected) scooterData.current else 0}A", Color(0xFF00BCD4), Modifier.weight(1f))
            DataCard("💪 Puissance", "${if (isConnected) scooterData.power.toInt() else 0}W", Color(0xFFE91E63), Modifier.weight(1f))
        }

        // Section historique (1x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DataCard("🛣️ Total", "${if (isConnected) scooterData.odometer else 0}km", Color(0xFF9C27B0), Modifier.weight(1f))
            DataCard("📍 Trajet", "${if (isConnected) scooterData.tripDistance else 0}km", Color(0xFF795548), Modifier.weight(1f))
        }

        // Dernière mise à jour
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF607D8B).copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⏱️",
                    fontSize = 24.sp,
                    color = Color(0xFF607D8B)
                )
                Text(
                    text = "Dernière MAJ",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isConnected && scooterData.lastUpdate != null) {
                        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(scooterData.lastUpdate)
                    } else "--:--:--",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF607D8B)
                )
            }
        }
    }
}

@Composable
private fun StatusCard(text: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth().height(50.dp)
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
                fontSize = 10.sp,
                color = color,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 14.sp,
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