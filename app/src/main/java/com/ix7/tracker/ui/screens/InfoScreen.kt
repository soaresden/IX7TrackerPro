package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.ScooterData

/**
 * Écran d'informations complètes du scooter
 * Affiche TOUTES les données disponibles, organisées par catégories
 * PAS de boutons, uniquement de l'information
 */
@Composable
fun InfoScreen(scooterData: ScooterData, isConnected: Boolean) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Titre
        item {
            Text(
                text = "ℹ️ Informations",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // TRAJET EN COURS
        // ═══════════════════════════════════════════════════════════════════
        item {
            CategoryHeader("Trajet en cours")
        }

        item {
            InfoCard {
                InfoRow("Kilométrage pour ce trajet", "%.1f km".format(scooterData.tripDistance))
                InfoRow("Vitesse actuelle", "%.1f km/h".format(scooterData.speed))
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // COMPTEURS TOTAUX
        // ═══════════════════════════════════════════════════════════════════
        item {
            CategoryHeader("Compteurs totaux")
        }

        item {
            InfoCard {
                InfoRow("Kilométrage total", "%.1f km".format(scooterData.odometer))
                InfoRow("Temps de conduite total", scooterData.totalRideTime)
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // BATTERIE
        // ═══════════════════════════════════════════════════════════════════
        item {
            CategoryHeader("Batterie")
        }

        item {
            InfoCard {
                val batteryColor = when {
                    scooterData.battery > 50 -> Color(0xFF4CAF50)
                    scooterData.battery > 20 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }

                InfoRow(
                    label = "Puissance restante",
                    value = "${scooterData.battery.toInt()}%",
                    valueColor = batteryColor,
                    highlighted = true
                )
                InfoRow("Tension", "%.1f V".format(scooterData.voltage))
                InfoRow("Courant", "%.1f A".format(scooterData.current))
                InfoRow("Puissance", "%.1f W".format(scooterData.power))
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // TEMPÉRATURES
        // ═══════════════════════════════════════════════════════════════════
        item {
            CategoryHeader("Températures")
        }

        item {
            InfoCard {
                val tempColor = when {
                    scooterData.temperature > 70 -> Color(0xFFF44336)
                    scooterData.temperature > 50 -> Color(0xFFFF9800)
                    else -> Color(0xFF4CAF50)
                }

                InfoRow(
                    label = "Température du scooter",
                    value = "%.1f°C".format(scooterData.temperature),
                    valueColor = tempColor
                )
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // VERSIONS SYSTÈME
        // ═══════════════════════════════════════════════════════════════════
        item {
            CategoryHeader("Versions système")
        }

        item {
            InfoCard {
                InfoRow("Version électrique", scooterData.firmwareVersion.ifEmpty { "N/A" })
                InfoRow("Version Bluetooth", scooterData.bluetoothVersion.ifEmpty { "N/A" })
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // CONFIGURATION
        // ═══════════════════════════════════════════════════════════════════
        item {
            CategoryHeader("Configuration actuelle")
        }

        item {
            InfoCard {
                InfoRow(
                    label = "Mode de conduite",
                    value = when (scooterData.currentMode?.name) {
                        "PEDESTRIAN" -> "🚶 Piéton"
                        "ECO" -> "🌱 Eco"
                        "SPORT" -> "⚡ Sport"
                        "RACE" -> "🏎️ Race"
                        else -> "Inconnu"
                    }
                )

                InfoRow(
                    label = "Limitation de vitesse",
                    value = when (scooterData.speedLimitMode?.name) {
                        "LIMITED" -> "🚧 Bridé"
                        "UNLIMITED" -> "⚡ Débridé"
                        else -> "Inconnu"
                    }
                )
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // CONNEXION
        // ═══════════════════════════════════════════════════════════════════
        item {
            CategoryHeader("Connexion")
        }

        item {
            InfoCard {
                InfoRow(
                    label = "État de la connexion",
                    value = if (isConnected) "✅ Connecté" else "❌ Déconnecté",
                    valueColor = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }

        // Espace en bas
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(Color(0xFFFF0000), shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF0000)
        )
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color.White,
    highlighted: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (highlighted) 16.sp else 15.sp,
            color = if (highlighted) Color.White else Color.Gray,
            fontWeight = if (highlighted) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            fontSize = if (highlighted) 18.sp else 16.sp,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }

    if (highlighted) {
        Divider(
            color = Color(0xFF3A3A3C),
            thickness = 1.dp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}