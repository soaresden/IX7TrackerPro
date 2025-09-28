package com.ix7.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.BluetoothDeviceInfo
import com.ix7.tracker.protocol.ScooterDetector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCard(
    device: BluetoothDeviceInfo,
    onConnect: () -> Unit,
    isConnectable: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = if (isConnectable) onConnect else { {} },
        enabled = isConnectable,
        colors = CardDefaults.cardColors(
            containerColor = if (device.isScooter)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Informations de l'appareil
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "•",
                        color = if (device.isScooter)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = device.name ?: "Appareil inconnu",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = device.address,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )

                if (device.isScooter) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = device.scooterType.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Informations de signal et bouton
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Force du signal
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "•",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${device.rssi} dBm",
                        fontSize = 12.sp,
                        color = getSignalColor(device.rssi)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Distance estimée
                Text(
                    text = device.distance,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bouton de connexion
                Button(
                    onClick = onConnect,
                    enabled = isConnectable && ScooterDetector.isSignalStrongEnough(device.rssi),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (ScooterDetector.isSignalStrongEnough(device.rssi))
                            "Connecter"
                        else
                            "Signal faible",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

private fun getSignalColor(rssi: Int): Color {
    return when {
        rssi > -50 -> Color(0xFF4CAF50) // Vert - Excellent
        rssi > -60 -> Color(0xFF8BC34A) // Vert clair - Bon
        rssi > -70 -> Color(0xFFFF9800) // Orange - Moyen
        rssi > -80 -> Color(0xFFFF5722) // Rouge orange - Faible
        else -> Color(0xFFF44336) // Rouge - Très faible
    }
}