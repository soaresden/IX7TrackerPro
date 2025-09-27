package com.ix7.tracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Composable pour l'écran d'informations détaillées du scooter
 */
@Composable
fun InformationScreen(
    scooterData: ScooterData,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Titre et état de connexion
        item {
            ConnectionHeader(isConnected = isConnected)
        }

        if (isConnected) {
            // Section vitesse et mouvement
            item {
                SpeedSection(scooterData = scooterData)
            }

            // Section batterie
            item {
                BatterySection(scooterData = scooterData)
            }

            // Section électrique
            item {
                ElectricalSection(scooterData = scooterData)
            }

            // Section voyage
            item {
                TripSection(scooterData = scooterData)
            }

            // Section système
            item {
                SystemSection(scooterData = scooterData)
            }

            // Section données brutes (si disponibles)
            if (scooterData.rawData != null) {
                item {
                    RawDataSection(rawData = scooterData.rawData!!)
                }
            }

            // Section statistiques calculées
            item {
                StatisticsSection(scooterData = scooterData)
            }
        } else {
            item {
                DisconnectedMessage()
            }
        }
    }
}

@Composable
fun ConnectionHeader(isConnected: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                Color.Green.copy(alpha = 0.1f)
            else
                Color.Red.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isConnected) "🔗 Scooter M0Robot connecté" else "❌ Aucun scooter connecté",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isConnected) Color.Green else Color.Red
            )
        }
    }
}

@Composable
fun SpeedSection(scooterData: ScooterData) {
    InfoCard(
        title = "🏁 Vitesse et mouvement",
        backgroundColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        InfoRow("Vitesse actuelle", Utils.formatSpeed(scooterData.speed))
        InfoRow("Kilométrage total", Utils.formatDistance(scooterData.odometer))
        InfoRow("Temps de conduite total", scooterData.totalRideTime)

        val totalHours = Utils.parseTotalHours(scooterData.totalRideTime)
        val avgSpeed = Utils.calculateAverageSpeed(scooterData.odometer, totalHours)
        InfoRow("Vitesse moyenne", Utils.formatSpeed(avgSpeed))
    }
}

@Composable
fun BatterySection(scooterData: ScooterData) {
    val batteryColor = when {
        scooterData.battery > 50 -> Color.Green
        scooterData.battery > 20 -> Color.Yellow
        else -> Color.Red
    }

    InfoCard(
        title = "🔋 Batterie",
        backgroundColor = batteryColor.copy(alpha = 0.1f)
    ) {
        InfoRow("Charge restante", "${scooterData.battery}%", valueColor = batteryColor)
        InfoRow("État de la batterie", scooterData.batteryState.toString())
        InfoRow("Température batterie", Utils.formatTemperature(scooterData.temperature))

        val remainingRange = Utils.calculateRemainingRange(scooterData.battery)
        InfoRow("Autonomie estimée", Utils.formatDistance(remainingRange))
    }
}

@Composable
fun ElectricalSection(scooterData: ScooterData) {
    InfoCard(
        title = "⚡ Données électriques",
        backgroundColor = MaterialTheme.colorScheme.secondaryContainer
    ) {
        InfoRow("Tension", Utils.formatVoltage(scooterData.voltage))
        InfoRow("Courant", Utils.formatCurrent(scooterData.current))
        InfoRow("Puissance", Utils.formatPower(scooterData.power))

        val consumption = Utils.calculateConsumption(scooterData.power, scooterData.odometer)
        InfoRow("Consommation", "${String.format("%.1f", consumption)} W/km")
    }
}

@Composable
fun TripSection(scooterData: ScooterData) {
    InfoCard(
        title = "🛣️ Informations de trajet",
        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        InfoRow("Kilométrage total", Utils.formatDistance(scooterData.odometer))
        InfoRow("Temps de conduite total", scooterData.totalRideTime)

        val estimatedSavings = Utils.calculateEstimatedSavings(scooterData.odometer)
        InfoRow("Économies estimées", estimatedSavings)

        val co2Saved = scooterData.odometer * 0.12f // 120g CO2/km pour une voiture
        InfoRow("CO₂ économisé", "${String.format("%.1f", co2Saved)} kg")
    }
}

@Composable
fun SystemSection(scooterData: ScooterData) {
    InfoCard(
        title = "🔧 Informations système",
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        InfoRow("Codes d'erreur", if (scooterData.errorCodes == 0) "Aucun" else scooterData.errorCodes.toString())
        InfoRow("Codes d'avertissement", if (scooterData.warningCodes == 0) "Aucun" else scooterData.warningCodes.toString())
        InfoRow("Version firmware", scooterData.firmwareVersion)
        InfoRow("Version Bluetooth", scooterData.bluetoothVersion)
        InfoRow("Source de données", scooterData.dataSource)
    }
}

@Composable
fun RawDataSection(rawData: ByteArray) {
    InfoCard(
        title = "📊 Données brutes",
        backgroundColor = Color.Gray.copy(alpha = 0.1f)
    ) {
        Text(
            text = "Hex: ${Utils.bytesToHex(rawData)}",
            fontSize = 12.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Text(
            text = "Taille: ${rawData.size} octets",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            text = "Type: ${Utils.getScooterType(rawData)}",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun StatisticsSection(scooterData: ScooterData) {
    InfoCard(
        title = "📈 Statistiques calculées",
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        val totalHours = Utils.parseTotalHours(scooterData.totalRideTime)
        val avgSpeed = Utils.calculateAverageSpeed(scooterData.odometer, totalHours)
        val efficiency = if (scooterData.power > 0 && scooterData.speed > 0) {
            scooterData.power / scooterData.speed
        } else 0f

        InfoRow("Efficacité énergétique", "${String.format("%.1f", efficiency)} W/(km/h)")
        InfoRow("Temps de conduite (heures)", String.format("%.1f", totalHours))
        InfoRow("Vitesse moyenne", Utils.formatSpeed(avgSpeed))

        if (scooterData.battery > 0 && scooterData.speed > 0) {
            val timeRemaining = (scooterData.battery / 100f) * totalHours
            InfoRow("Temps restant estimé", Utils.formatRideTime(timeRemaining))
        }
    }
}

@Composable
fun DisconnectedMessage() {
    Card {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📱",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aucun scooter connecté",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connectez-vous à un scooter M0Robot dans l'onglet 'Connexion' pour voir les informations de télémétrie en temps réel.",
                fontSize = 14.sp,
                color = Color.Gray,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}