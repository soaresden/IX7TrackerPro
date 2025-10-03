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


        // Tableau de bord LCD
        DashboardDisplay(
            scooterData = scooterData,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

// États et indicateurs
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phares
                StateIndicator(
                    label = "Phares",
                    isActive = scooterData.headlightsOn,
                    activeIcon = "💡",
                    inactiveIcon = "⚫"
                )

                // Néon
                StateIndicator(
                    label = "Néon",
                    isActive = scooterData.neonOn,
                    activeIcon = "🟣",
                    inactiveIcon = "⚫"
                )

                // Clignotant gauche
                StateIndicator(
                    label = "Cligno G",
                    isActive = scooterData.leftBlinker,
                    activeIcon = "⬅️",
                    inactiveIcon = "⚫"
                )

                // Clignotant droit
                StateIndicator(
                    label = "Cligno D",
                    isActive = scooterData.rightBlinker,
                    activeIcon = "➡️",
                    inactiveIcon = "⚫"
                )
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode roues (TODO: détecter 2WD)
                StateIndicator(
                    label = "Mode",
                    isActive = false, // TODO: déterminer si 2WD
                    activeIcon = "🏍️",
                    inactiveIcon = "🛴"
                )

                // Régulateur
                StateIndicator(
                    label = "Régulateur",
                    isActive = scooterData.cruiseControl,
                    activeIcon = "👮",
                    inactiveIcon = "✖️"
                )

                // Démarrage zéro
                StateIndicator(
                    label = "Start",
                    isActive = scooterData.zeroStart,
                    activeIcon = "🐇",
                    inactiveIcon = "🐢"
                )
            }
        }


        Spacer(modifier = Modifier.height(8.dp))
        // Section principale - Données temps réel (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {}

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
// Fonction helper
@Composable
private fun StateIndicator(
    label: String,
    isActive: Boolean,
    activeIcon: String,
    inactiveIcon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isActive) activeIcon else inactiveIcon,
            fontSize = 24.sp
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray
        )
    }
}
private fun getTemperatureColor(temperature: Float): Color {
    return when {
        temperature > 60f -> Color(0xFFF44336)
        temperature > 45f -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }


}