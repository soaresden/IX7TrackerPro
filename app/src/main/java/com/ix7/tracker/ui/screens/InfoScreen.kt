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
 * 🎯 Écran d'informations CORRIGÉ avec les vrais offsets
 *
 * Valeurs validées:
 * - Batterie: 66% (0x20[45], 0x3E, 0xD3[43])
 * - Voltage: 49.0V (0x3E[6-7] BE/1000)
 * - Odomètre: 102.9km (0x03[2-3] LE/100 ou 0x30[35-36] LE/10)
 * - Température: 26-27°C (0x3E[49], 0xD3[17,29])
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ℹ️ Informations",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Indicateur de connexion
            Text(
                text = if (isConnected) "✅ Connecté" else "❌ Déconnecté",
                fontSize = 12.sp,
                color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }

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
                    InfoLine(
                        "Distance",
                        if (isConnected) "%.1f km".format(scooterData.tripDistance) else "-"
                    )
                    InfoLine(
                        "Vitesse",
                        if (isConnected) "%.1f km/h".format(scooterData.speed) else "-"
                    )
                    InfoLine(
                        "Total",
                        if (isConnected) "%.1f km".format(scooterData.odometer) else "-",
                        Color(0xFF2196F3)
                    )
                    InfoLine(
                        "Temps",
                        if (isConnected) scooterData.totalRideTime else "-"
                    )
                }

                // BATTERIE (avec les vrais offsets)
                val batteryColor = when {
                    !isConnected -> Color.Gray
                    scooterData.battery > 50 -> Color(0xFF4CAF50)
                    scooterData.battery > 20 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
                CompactInfoCard(title = "Batterie", titleColor = batteryColor) {
                    InfoLine(
                        "Charge",
                        if (isConnected) "${scooterData.battery.toInt()}%" else "-",
                        batteryColor
                    )
                    InfoLine(
                        "Tension",
                        if (isConnected) "%.2f V".format(scooterData.voltage) else "-",
                        if (scooterData.voltage > 40.0) Color(0xFF4CAF50) else Color(0xFFFFC107)
                    )
                    InfoLine(
                        "Courant",
                        if (isConnected) "%.1f A".format(scooterData.current) else "-"
                    )
                    InfoLine(
                        "Puissance",
                        if (isConnected) "%.0f W".format(scooterData.power) else "-"
                    )
                }

                // TEMPÉRATURE (avec les vrais offsets)
                val tempColor = when {
                    !isConnected -> Color.Gray
                    scooterData.temperature > 70 -> Color(0xFFF44336)
                    scooterData.temperature > 50 -> Color(0xFFFF9800)
                    else -> Color(0xFF4CAF50)
                }
                CompactInfoCard(title = "Température", titleColor = tempColor) {
                    InfoLine(
                        "Scooter",
                        if (isConnected) "%.1f°C".format(scooterData.temperature) else "-",
                        tempColor
                    )
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
                        if (!isConnected) {
                            "-"
                        } else {
                            when (scooterData.currentMode.name) {
                                "PIETON" -> "🚶 Piéton"
                                "ECO" -> "🌱 Eco"
                                "SPORT" -> "⚡ Sport"
                                "RACE" -> "🏎️ Race"
                                else -> "-"
                            }
                        }
                    )
                    InfoLine(
                        "Limite",
                        if (!isConnected) {
                            "-"
                        } else {
                            when (scooterData.speedLimitMode.name) {
                                "LIMITED" -> "🚧 Bridé"
                                "UNLIMITED" -> "⚡ Débridé"
                                else -> "-"
                            }
                        }
                    )
                }

                // VERSIONS
                CompactInfoCard(title = "Versions") {
                    InfoLine("Électrique", scooterData.firmwareVersion.ifEmpty { "N/A" })
                    InfoLine("Bluetooth", scooterData.bluetoothVersion.ifEmpty { "N/A" })
                }

                // DIAGNOSTIC DES OFFSETS
                if (isConnected) {
                    CompactInfoCard(title = "🔍 Diagnostic", titleColor = Color(0xFF64B5F6)) {
                        Text(
                            "Offsets validés :",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        InfoLine(
                            "Batterie",
                            "0x20[45], 0x3E, 0xD3[43]",
                            Color(0xFF64B5F6)
                        )
                        InfoLine(
                            "Voltage",
                            "0x3E[6-7] BE/1000",
                            Color(0xFF64B5F6)
                        )
                        InfoLine(
                            "Odomètre",
                            "0x03[2-3] LE/100",
                            Color(0xFF64B5F6)
                        )
                        InfoLine(
                            "Temp",
                            "0x3E[49], 0xD3",
                            Color(0xFF64B5F6)
                        )
                    }
                } else {
                    CompactInfoCard(title = "❌ Déconnecté", titleColor = Color(0xFFF44336)) {
                        Text(
                            "Connecte-toi pour voir les données en temps réel",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
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