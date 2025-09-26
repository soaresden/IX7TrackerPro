package com.ix7.tracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationScreen(
    scooterData: ScooterData,
    isConnected: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // En-tête compact
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isConnected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "M0Robot",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isConnected) "Connecté" else "Déconnecté",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Données principales - 2 colonnes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactDataCard(
                title = "Vitesse",
                value = formatSpeed(scooterData.speed),
                modifier = Modifier.weight(1f)
            )
            CompactDataCard(
                title = "Batterie",
                value = formatPercentage(scooterData.battery),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactDataCard(
                title = "Température",
                value = "${scooterData.temperature}°C",
                modifier = Modifier.weight(1f)
            )
            CompactDataCard(
                title = "Kilométrage",
                value = formatDistance(scooterData.odometer),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactDataCard(
                title = "Tension",
                value = formatVoltage(scooterData.voltage),
                modifier = Modifier.weight(1f)
            )
            CompactDataCard(
                title = "Puissance",
                value = formatPower(scooterData.power),
                modifier = Modifier.weight(1f)
            )
        }

        // Temps de conduite en pleine largeur
        CompactDataCard(
            title = "Temps total",
            value = scooterData.totalRideTime,
            modifier = Modifier.fillMaxWidth()
        )

        // Informations système compactes
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Système",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Erreurs: ${scooterData.errorCodes}", fontSize = 12.sp)
                    Text("Avertissements: ${scooterData.warningCodes}", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CompactDataCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Fonctions de formatage (identiques à avant)
fun formatPercentage(value: Float): String {
    return "${String.format("%.1f", value)}%"
}

fun formatVoltage(value: Float): String {
    return "${String.format("%.1f", value)}V"
}

fun formatCurrent(value: Float): String {
    return "${String.format("%.1f", value)}A"
}

fun formatPower(value: Float): String {
    return "${String.format("%.1f", value)}W"
}

fun formatDistance(value: Float): String {
    return "${String.format("%.1f", value)} km"
}

fun formatSpeed(value: Float): String {
    return "${String.format("%.1f", value)} km/h"
}