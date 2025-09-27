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
                InfoItem("Kilométrage total", formatDistance(scooterData.odometer)),
                InfoItem("Température", "${scooterData.temperature}°C : ${scooterData.temperature}°C"),
                InfoItem("Temps de conduite", scooterData.totalRideTime),
                InfoItem("Vitesse actuelle", formatSpeed(scooterData.speed), true), // COULEUR
                InfoItem("Puissance restante", formatPercentage(scooterData.battery), true) // COULEUR
            ),
            isConnected = isConnected
        )

        // Batterie compacte
        CompactInfoCard(
            title = "Batterie",
            items = listOf(
                InfoItem("Température batterie", "--"),
                InfoItem("État batterie", "${scooterData.batteryState}"),
                InfoItem("Courant", "${formatCurrent(scooterData.current)} : ${formatCurrent(scooterData.current)}", true), // COULEUR
                InfoItem("Tension", formatVoltage(scooterData.voltage), true), // COULEUR
                InfoItem("Puissance", formatPower(scooterData.power), true) // COULEUR
            ),
            isConnected = isConnected
        )

        // Système compacte
        CompactInfoCard(
            title = "Système",
            items = listOf(
                InfoItem("Codes d'erreur", "${scooterData.errorCodes}"),
                InfoItem("Code d'avertissement", "${scooterData.warningCodes}"),
                InfoItem("Version électrique", scooterData.firmwareVersion),
                InfoItem("Version Bluetooth", scooterData.bluetoothVersion),
                InfoItem("Version application", scooterData.appVersion)
            ),
            isConnected = isConnected
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

// Classe helper pour éviter la complexité avec les types
data class InfoItem(
    val label: String,
    val value: String,
    val isDynamic: Boolean = false
)

@Composable
fun CompactInfoCard(
    title: String,
    items: List<InfoItem>,
    isConnected: Boolean = true
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

            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            !isConnected -> Color.Gray
                            item.isDynamic -> MaterialTheme.colorScheme.primary // BLEU pour données dynamiques
                            else -> MaterialTheme.colorScheme.onSurface
                        }
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