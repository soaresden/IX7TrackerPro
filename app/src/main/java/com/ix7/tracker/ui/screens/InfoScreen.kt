package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.ScooterData

/**
 * Écran d'informations complètes du scooter
 * TOUT sur UNE SEULE PAGE - Non scrollable
 */
@Composable
fun InfoScreen(scooterData: ScooterData, isConnected: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Titre compact
        Text(
            text = "ℹ️ Informations",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Grille d'informations 2 colonnes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // COLONNE GAUCHE
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // TRAJET & COMPTEURS
                CompactInfoCard(title = "Trajet") {
                    InfoLine("Distance", "%.1f km".format(scooterData.tripDistance))
                    InfoLine("Vitesse", "%.1f km/h".format(scooterData.speed))
                    InfoLine("Total", "%.1f km".format(scooterData.odometer))
                    InfoLine("Temps", scooterData.totalRideTime)
                }

                // BATTERIE
                val batteryColor = when {
                    scooterData.battery > 50 -> Color(0xFF4CAF50)
                    scooterData.battery > 20 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
                CompactInfoCard(title = "Batterie", titleColor = batteryColor) {
                    InfoLine("Charge", "${scooterData.battery.toInt()}%", batteryColor)
                    InfoLine("Tension", "%.1f V".format(scooterData.voltage))
                    InfoLine("Courant", "%.1f A".format(scooterData.current))
                    InfoLine("Puissance", "%.0f W".format(scooterData.power))
                }

                // TEMPÉRATURE
                val tempColor = when {
                    scooterData.temperature > 70 -> Color(0xFFF44336)
                    scooterData.temperature > 50 -> Color(0xFFFF9800)
                    else -> Color(0xFF4CAF50)
                }
                CompactInfoCard(title = "Température", titleColor = tempColor) {
                    InfoLine("Scooter", "%.1f°C".format(scooterData.temperature), tempColor)
                }
            }

            // COLONNE DROITE
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // CONFIGURATION
                CompactInfoCard(title = "Configuration") {
                    InfoLine(
                        "Mode",
                        when (scooterData.currentMode?.name) {
                            "PEDESTRIAN" -> "🚶 Piéton"
                            "ECO" -> "🌱 Eco"
                            "SPORT" -> "⚡ Sport"
                            "RACE" -> "🏎️ Race"
                            else -> "N/A"
                        }
                    )
                    InfoLine(
                        "Limite",
                        when (scooterData.speedLimitMode?.name) {
                            "LIMITED" -> "🚧 Bridé"
                            "UNLIMITED" -> "⚡ Débridé"
                            else -> "N/A"
                        }
                    )
                }

                // VERSIONS
                CompactInfoCard(title = "Versions") {
                    InfoLine("Électrique", scooterData.firmwareVersion.ifEmpty { "N/A" })
                    InfoLine("Bluetooth", scooterData.bluetoothVersion.ifEmpty { "N/A" })
                }

                // CONNEXION
                val connColor = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
                CompactInfoCard(title = "Connexion", titleColor = connColor) {
                    InfoLine(
                        "État",
                        if (isConnected) "✅ Connecté" else "❌ Déconnecté",
                        connColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactInfoCard(
    title: String,
    titleColor: Color = Color(0xFFFF0000),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
            content()
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}