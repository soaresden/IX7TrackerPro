package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.ScooterData
import com.ix7.tracker.ui.components.DataCard
import com.ix7.tracker.utils.FormatUtils

@Composable
fun DataScreen(
    scooterData: ScooterData,
    isConnected: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // État de connexion
        item {
            ConnectionStatusCard(isConnected = isConnected)
        }

        // Données temps réel
        item {
            SectionHeader(
                title = "Données temps réel"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DataCard(
                    title = "Vitesse",
                    value = FormatUtils.formatSpeed(scooterData.speed),
                    modifier = Modifier.weight(1f)
                )
                DataCard(
                    title = "Batterie",
                    value = FormatUtils.formatBattery(scooterData.battery),
                    color = getBatteryColor(scooterData.battery),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            DataCard(
                title = "Température du scooter",
                value = FormatUtils.formatTemperature(scooterData.temperature),
                color = getTemperatureColor(scooterData.temperature)
            )
        }

        // Données de trajet
        item {
            SectionHeader(
                title = "Historique"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DataCard(
                    title = "Kilométrage total",
                    value = FormatUtils.formatDistance(scooterData.odometer),
                    modifier = Modifier.weight(1f)
                )
                DataCard(
                    title = "Trajet actuel",
                    value = FormatUtils.formatDistance(scooterData.tripDistance),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            DataCard(
                title = "Temps de conduite total",
                value = FormatUtils.formatRideTime(scooterData.totalRideTime)
            )
        }

        // Données électriques
        item {
            SectionHeader(
                title = "Système électrique"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DataCard(
                    title = "Tension",
                    value = FormatUtils.formatVoltage(scooterData.voltage),
                    modifier = Modifier.weight(1f)
                )
                DataCard(
                    title = "Courant",
                    value = FormatUtils.formatCurrent(scooterData.current),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            DataCard(
                title = "Puissance",
                value = FormatUtils.formatPower(scooterData.power)
            )
        }

        // Températures
        item {
            SectionHeader(
                title = "Températures"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DataCard(
                    title = "Scooter",
                    value = FormatUtils.formatTemperature(scooterData.temperature),
                    color = getTemperatureColor(scooterData.temperature),
                    modifier = Modifier.weight(1f)
                )
                DataCard(
                    title = "Batterie",
                    value = if (scooterData.batteryTemperature > 0)
                        FormatUtils.formatTemperature(scooterData.batteryTemperature)
                    else "N/A",
                    color = getTemperatureColor(scooterData.batteryTemperature),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Diagnostics
        item {
            SectionHeader(
                title = "Diagnostics"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DataCard(
                    title = "Codes d'erreur",
                    value = FormatUtils.formatErrorCodes(scooterData.errorCodes),
                    color = if (scooterData.errorCodes > 0) MaterialTheme.colorScheme.error else Color.Unspecified,
                    modifier = Modifier.weight(1f)
                )
                DataCard(
                    title = "Avertissements",
                    value = FormatUtils.formatWarningCodes(scooterData.warningCodes),
                    color = if (scooterData.warningCodes > 0) Color(0xFFFF9800) else Color.Unspecified,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Informations système
        item {
            SectionHeader(
                title = "Informations système"
            )
        }

        item {
            DataCard(
                title = "Version firmware",
                value = scooterData.firmwareVersion
            )
        }

        item {
            DataCard(
                title = "Version Bluetooth",
                value = scooterData.bluetoothVersion
            )
        }

        item {
            DataCard(
                title = "Dernière mise à jour",
                value = FormatUtils.formatDate(scooterData.lastUpdate)
            )
        }

        // Calculs dérivés
        item {
            SectionHeader(
                title = "Statistiques calculées"
            )
        }

        item {
            val totalHours = FormatUtils.parseTimeToHours(scooterData.totalRideTime)
            val avgSpeed = FormatUtils.calculateAverageSpeed(scooterData.odometer, totalHours)
            val savings = FormatUtils.calculateEstimatedSavings(scooterData.odometer)

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DataCard(
                        title = "Vitesse moyenne",
                        value = FormatUtils.formatSpeed(avgSpeed),
                        modifier = Modifier.weight(1f)
                    )
                    DataCard(
                        title = "Économies estimées",
                        value = savings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(isConnected: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "•",
                color = if (isConnected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isConnected) "Scooter connecté" else "Scooter déconnecté",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isConnected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: String = "•"
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = icon,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun getBatteryColor(battery: Float): Color {
    return when {
        battery > 50f -> Color(0xFF4CAF50) // Vert
        battery > 20f -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Rouge
    }
}

private fun getTemperatureColor(temperature: Float): Color {
    return when {
        temperature > 60f -> Color(0xFFF44336) // Rouge - Très chaud
        temperature > 45f -> Color(0xFFFF9800) // Orange - Chaud
        temperature < 0f -> Color(0xFF2196F3) // Bleu - Froid
        else -> Color.Unspecified // Normal
    }
}