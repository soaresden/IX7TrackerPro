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

@Composable
fun SettingsScreen(
    bluetoothManager: BluetoothRepository,
    isConnected: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "⚙️ Réglages",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
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
                    // TODO: Sauvegarder dans les préférences
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
                    // TODO: Sauvegarder dans les préférences
                }
            )
        }

        item {
            var disconnectAlert by remember { mutableStateOf(true) }
            SettingSwitch(
                icon = "📡",
                title = "Alerte déconnexion",
                description = "Notification en cas de perte de connexion",
                checked = disconnectAlert,
                onCheckedChange = {
                    disconnectAlert = it
                    // TODO: Sauvegarder dans les préférences
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
                    // TODO: Sauvegarder dans les préférences
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
                    // TODO: Sauvegarder dans les préférences
                }
            )
        }

        item {
            var autoConnect by remember { mutableStateOf(false) }
            SettingSwitch(
                icon = "🔗",
                title = "Connexion automatique",
                description = "Se reconnecter au dernier scooter",
                checked = autoConnect,
                onCheckedChange = {
                    autoConnect = it
                    // TODO: Sauvegarder dans les préférences
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // UNITÉS
        // ═══════════════════════════════════════════════════════════════════
        item {
            SectionHeader("Unités")
        }

        item {
            var useKmh by remember { mutableStateOf(true) }
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
                        text = "📏",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 16.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unité de vitesse",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (useKmh) "Kilomètres/heure" else "Miles/heure",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = { useKmh = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (useKmh) Color(0xFF0A84FF) else Color.Gray
                            )
                        ) {
                            Text("km/h", fontSize = 12.sp, fontWeight = if (useKmh) FontWeight.Bold else FontWeight.Normal)
                        }
                        TextButton(
                            onClick = { useKmh = false },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (!useKmh) Color(0xFF0A84FF) else Color.Gray
                            )
                        ) {
                            Text("mph", fontSize = 12.sp, fontWeight = if (!useKmh) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
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