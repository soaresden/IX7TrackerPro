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
import com.ix7.tracker.bluetooth.BluetoothRepository
import com.ix7.tracker.protocol.ProtocolConstants
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    bluetoothManager: BluetoothRepository,
    isConnected: Boolean
) {
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "⚙️ Paramètres",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // RÉGLAGES SCOOTER
        // ═══════════════════════════════════════════════════════════════════
        item {
            SectionHeader("Réglages du scooter")
        }

        item {
            SettingButton(
                icon = "🔧",
                title = "Calibrer le scooter",
                description = "Recalibrer les capteurs du scooter",
                enabled = isConnected,
                onClick = {
                    // TODO: Ajouter commande de calibration
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // ÉCLAIRAGE PAR DÉFAUT
        // ═══════════════════════════════════════════════════════════════════
        item {
            SectionHeader("Éclairage au démarrage")
        }

        item {
            var defaultLights by remember { mutableStateOf(false) }
            SettingSwitch(
                icon = "💡",
                title = "Phares automatiques",
                description = "Allumer les phares au démarrage",
                checked = defaultLights,
                onCheckedChange = {
                    defaultLights = it
                    // Sauvegarder la préférence
                }
            )
        }

        item {
            var defaultNeon by remember { mutableStateOf(false) }
            SettingSwitch(
                icon = "🟣",
                title = "Néon automatique",
                description = "Allumer le néon au démarrage",
                checked = defaultNeon,
                onCheckedChange = {
                    defaultNeon = it
                    // Sauvegarder la préférence
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // SÉCURITÉ
        // ═══════════════════════════════════════════════════════════════════
        item {
            SectionHeader("Sécurité")
        }

        item {
            var autoLock by remember { mutableStateOf(false) }
            SettingSwitch(
                icon = "🔒",
                title = "Verrouillage automatique",
                description = "Verrouiller le scooter à la déconnexion",
                checked = autoLock,
                onCheckedChange = {
                    autoLock = it
                    // Sauvegarder la préférence
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // ALERTES
        // ═══════════════════════════════════════════════════════════════════
        item {
            SectionHeader("Alertes")
        }

        item {
            var batteryAlert by remember { mutableStateOf(true) }
            SettingSwitch(
                icon = "🔋",
                title = "Alerte batterie faible",
                description = "Notification à 20% et 10% de batterie",
                checked = batteryAlert,
                onCheckedChange = {
                    batteryAlert = it
                }
            )
        }

        item {
            var tempAlert by remember { mutableStateOf(true) }
            SettingSwitch(
                icon = "🌡️",
                title = "Alerte température élevée",
                description = "Notification en cas de surchauffe",
                checked = tempAlert,
                onCheckedChange = {
                    tempAlert = it
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // APPLICATION
        // ═══════════════════════════════════════════════════════════════════
        item {
            SectionHeader("Application")
        }

        item {
            var keepScreenOn by remember { mutableStateOf(true) }
            SettingSwitch(
                icon = "📱",
                title = "Garder l'écran allumé",
                description = "Empêcher la mise en veille pendant la conduite",
                checked = keepScreenOn,
                onCheckedChange = {
                    keepScreenOn = it
                }
            )
        }

        item {
            var darkMode by remember { mutableStateOf(true) }
            SettingSwitch(
                icon = "🌙",
                title = "Mode sombre",
                description = "Utiliser le thème sombre",
                checked = darkMode,
                onCheckedChange = {
                    darkMode = it
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // INFORMATIONS
        // ═══════════════════════════════════════════════════════════════════
        item {
            SectionHeader("À propos")
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🛴",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "IX7 Tracker Pro",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Version 2.0.0",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Application de contrôle pour scooter M0Robot",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "© 2025 IX7 Technologies",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Espace en bas
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(
            text = "▸",
            color = Color(0xFF0A84FF),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0A84FF)
        )
    }
}

@Composable
private fun SettingButton(
    icon: String,
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFF2C2C2E) else Color(0xFF1C1C1E)
        )
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = icon,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 16.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = if (enabled) Color.White else Color.Gray
                    )
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                if (enabled) {
                    Text(
                        text = "▶",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    icon: String,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF0A84FF),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFF3A3A3C)
                )
            )
        }
    }
}