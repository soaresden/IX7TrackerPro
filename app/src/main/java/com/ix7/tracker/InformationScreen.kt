package com.ix7.tracker

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

@Composable
fun InformationScreen(
    scooterData: ScooterData,
    isConnected: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Titre principal
            Text(
                text = if (isConnected) "Informations M0Robot" else "Informations (Déconnecté)",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isConnected) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }

        item {
            // Section Données temps réel
            InfoCard(
                title = "Données temps réel",
                items = listOf(
                    InfoItem("Kilométrage total", formatDistance(scooterData.odometer)),
                    InfoItem("Température du scooter", "${scooterData.temperature}°C : ${scooterData.temperature}°C"),
                    InfoItem("Temps de conduite total", scooterData.totalRideTime),
                    InfoItem("Vitesse actuelle", formatSpeed(scooterData.speed)),
                    InfoItem("Puissance restante", formatPercentage(scooterData.battery))
                )
            )
        }

        item {
            // Section Batterie
            InfoCard(
                title = "Batterie",
                items = listOf(
                    InfoItem("Température de la batterie", "--"),
                    InfoItem("État de la batterie", "${scooterData.batteryState}"),
                    InfoItem("Courant", "${formatCurrent(scooterData.current)} : ${formatCurrent(scooterData.current)}"),
                    InfoItem("Tension", formatVoltage(scooterData.voltage)),
                    InfoItem("Puissance", formatPower(scooterData.power))
                )
            )
        }

        item {
            // Section Système
            InfoCard(
                title = "Système",
                items = listOf(
                    InfoItem("Codes d'erreur", "${scooterData.errorCodes}"),
                    InfoItem("Code d'avertissement", "${scooterData.warningCodes}"),
                    InfoItem("Version électrique", scooterData.firmwareVersion),
                    InfoItem("Version Bluetooth", scooterData.bluetoothVersion),
                    InfoItem("Numéro de version de l'application", scooterData.appVersion)
                )
            )
        }

        if (isConnected) {
            item {
                // Section Statistiques calculées
                val totalHours = parseTotalHours(scooterData.totalRideTime)
                val avgSpeed = calculateAverageSpeed(scooterData.odometer, totalHours)
                val estimatedSavings = calculateEstimatedSavings(scooterData.odometer)

                InfoCard(
                    title = "Statistiques",
                    items = listOf(
                        InfoItem("Vitesse moyenne", formatSpeed(avgSpeed)),
                        InfoItem("Économies estimées", estimatedSavings),
                        InfoItem("Consommation", "${formatPower(calculateConsumption(scooterData.power, scooterData.odometer))}/km")
                    )
                )
            }
        }

        if (!isConnected) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Non connecté",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Connectez-vous à un scooter pour voir les données en temps réel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    items: List<InfoItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            items.forEachIndexed { index, item ->
                InfoRow(
                    label = item.label,
                    value = item.value,
                    isHighlighted = item.isHighlighted
                )

                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isHighlighted) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isHighlighted) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

data class InfoItem(
    val label: String,
    val value: String,
    val isHighlighted: Boolean = false
)