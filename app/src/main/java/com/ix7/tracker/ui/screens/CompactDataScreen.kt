package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import com.ix7.tracker.core.RideMode
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.bluetooth.BluetoothRepository
import com.ix7.tracker.core.ConnectionState
import com.ix7.tracker.core.ScooterData
import kotlinx.coroutines.launch

@Composable
fun CompactDataScreen(
    bluetoothManager: BluetoothRepository,
    scooterData: ScooterData,
    connectionState: ConnectionState
) {
    val scope = rememberCoroutineScope()  // ✅ AJOUTÉ
    val connector = bluetoothManager.connector

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Indicateur de connexion
        if (connectionState != ConnectionState.CONNECTED) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Non connecté",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Vitesse et batterie
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vitesse
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format("%.1f", scooterData.speed),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "km/h",
                        fontSize = 14.sp
                    )
                }
            }

            // Batterie
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format("%.0f%%", scooterData.battery),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Batterie",
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Mode actuel
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mode actuel",
                    fontSize = 16.sp
                )
                Text(
                    text = when (scooterData.currentMode) {
                        RideMode.PEDESTRIAN -> "PIÉTON"
                        RideMode.ECO -> "ECO"
                        RideMode.SPORT -> "SPORT"
                        RideMode.RACE -> "RACE"
                        else -> "ECO"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Boutons de contrôle - ✅ CORRIGÉS avec scope.launch
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Contrôles",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // PHARES - ✅ Avec scope.launch
                    ControlButton(
                        icon = Icons.Default.Lock,
                        label = "Phares",
                        isActive = scooterData.headlightsOn,
                        enabled = connectionState == ConnectionState.CONNECTED,
                        modifier = Modifier.weight(1f)
                    ) {
                        scope.launch {  // ✅ CORRIGÉ
                            connector?.setLights(!scooterData.headlightsOn)
                        }
                    }

                    // NÉON - ✅ Avec scope.launch
                    ControlButton(
                        icon = Icons.Default.Star,
                        label = "Néon",
                        isActive = scooterData.neonOn,
                        enabled = connectionState == ConnectionState.CONNECTED,
                        modifier = Modifier.weight(1f)
                    ) {
                        scope.launch {  // ✅ CORRIGÉ
                            connector?.setNeon(!scooterData.neonOn)
                        }
                    }
                }
            }
        }

        // Infos supplémentaires
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoRow("Température", "${scooterData.temperature}°C")
                InfoRow("Kilométrage", String.format("%.1f km", scooterData.odometer))
                InfoRow("Trajet actuel", String.format("%.2f km", scooterData.tripDistance))
            }
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(
                color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isActive) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
