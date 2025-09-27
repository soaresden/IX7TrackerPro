package com.ix7.tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationScreen(
    scooterData: ScooterData,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("←", color = Color.White, fontSize = 18.sp)
            }

            Text(
                text = "Informations",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(60.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Points de connexion
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (scooterData.isConnected) Color.Red else Color.Gray,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Toutes les informations
        InformationItem(
            label = "Kilométrage total",
            value = "${String.format("%.1f", scooterData.totalDistance)}Km",
            isHighlighted = true
        )

        InformationItem(
            label = "Température du scooter",
            value = "${String.format("%.1f", scooterData.scooterTemperature)}°C : ${String.format("%.1f", scooterData.scooterTemperature)}°C"
        )

        InformationItem(
            label = "Temps de conduite total",
            value = scooterData.ridingTime
        )

        InformationItem(
            label = "Vitesse actuelle",
            value = "${String.format("%.1f", scooterData.currentSpeed)}Km/h"
        )

        InformationItem(
            label = "Puissance restante",
            value = "${scooterData.batteryLevel}%"
        )

        InformationItem(
            label = "Température de la batterie",
            value = if (scooterData.batteryTemperature == 0) "--" else "${scooterData.batteryTemperature}°C"
        )

        InformationItem(
            label = "État de la batterie",
            value = "${scooterData.batteryStatus}"
        )

        InformationItem(
            label = "Courant",
            value = "${String.format("%.1f", scooterData.current)}A : ${String.format("%.1f", scooterData.current)}A"
        )

        InformationItem(
            label = "Tension",
            value = "${String.format("%.1f", scooterData.voltage)}V"
        )

        InformationItem(
            label = "Puissance",
            value = "${String.format("%.1f", scooterData.power)}W"
        )

        // Espacement pour les codes d'erreur
        Spacer(modifier = Modifier.height(16.dp))

        InformationItem(
            label = "Codes d'erreur",
            value = "${scooterData.errorCode}"
        )

        InformationItem(
            label = "Code d'avertissement",
            value = "${scooterData.warningCode}"
        )

        // Espacement pour les versions
        Spacer(modifier = Modifier.height(16.dp))

        InformationItem(
            label = "Version électrique",
            value = scooterData.electricVersion
        )

        InformationItem(
            label = "Version Bluetooth",
            value = scooterData.bluetoothVersion
        )

        InformationItem(
            label = "Numéro de version de l'application",
            value = scooterData.appVersion
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun InformationItem(
    label: String,
    value: String,
    isHighlighted: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isHighlighted) Color.Red else Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }

    // Ligne de séparation
    HorizontalDivider(
        color = Color.Gray.copy(alpha = 0.3f),
        thickness = 1.dp
    )
}