package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.ScooterData

@Composable
fun CompactDataScreen(
    scooterData: ScooterData,
    isConnected: Boolean
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Ligne 1
        item { StatusCard(if (isConnected) "Connecté" else "Déconnecté", if (isConnected) Color.Green else Color.Red) }
        item { DataCard("Batterie", "${scooterData.battery.toInt()}%", getBatteryColor(scooterData.battery)) }

        // Ligne 2
        item { DataCard("Vitesse", "${scooterData.speed.toInt()} km/h", Color.Blue) }
        item { DataCard("Voltage", "${scooterData.voltage} V", Color(0xFFFF9800)) }

        // Ligne 3
        item { DataCard("Température", "${scooterData.temperature.toInt()}°C", getTemperatureColor(scooterData.temperature)) }
        item { DataCard("Kilomètrage", "${scooterData.odometer} km", Color(0xFF9C27B0)) }

        // Ligne 4
        item { DataCard("Courant", "${scooterData.current} A", Color(0xFF00BCD4)) }
        item { DataCard("Puissance", "${scooterData.power.toInt()} W", Color(0xFFE91E63)) }
    }
}

@Composable
private fun StatusCard(text: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = Modifier.height(80.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DataCard(title: String, value: String, color: Color = Color.Gray) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = Modifier.height(80.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
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