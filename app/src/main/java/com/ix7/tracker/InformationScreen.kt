package com.ix7.tracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InformationScreen(
    scooterData: ScooterData,
    isConnected: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Titre
        Text(
            text = if (isConnected) "Informations M0Robot" else "Informations (Déconnecté)",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (isConnected) MaterialTheme.colorScheme.primary else Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Section principale compacte
        CompactInfoCard(
            title = "Données temps réel",
            items = listOf(
                "Kilométrage total" to formatDistance(scooterData.odometer),
                "Température" to "${scooterData.temperature}°C : ${scooterData.temperature}°C",
                "Temps de conduite" to scooterData.totalRideTime,
                "Vitesse actuelle" to formatSpeed(scooterData.speed),
                "Puissance restante" to formatPercentage(scooterData.battery)
            )
        )

        // Batterie compacte
        CompactInfoCard(
            title = "Batterie",
            items = listOf(
                "Température batterie" to "--",
                "État batterie" to "${scooterData.batteryState}",
                "Courant" to "${formatCurrent(scooterData.current)} : ${formatCurrent(scooterData.current)}",
                "Tension" to formatVoltage(scooterData.voltage),
                "Puissance" to formatPower(scooterData.power)
            )
        )

        // Système compacte
        CompactInfoCard(
            title = "Système",
            items = listOf(
                "Codes d'erreur" to "${scooterData.errorCodes}",
                "Code d'avertissement" to "${scooterData.warningCodes}",
                "Version électrique" to scooterData.firmwareVersion,
                "Version Bluetooth" to scooterData.bluetoothVersion,
                "Version application" to scooterData.appVersion
            )
        )

        if (!isConnected) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "Non connecté - Connectez-vous pour voir les données en temps réel",
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CompactInfoCard(
    title: String,
    items: List<Pair<String, String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            items.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}