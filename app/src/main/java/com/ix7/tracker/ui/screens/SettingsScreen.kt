package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.utils.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }

    var autoConnect by remember { mutableStateOf(preferencesManager.autoConnectEnabled) }
    var keepScreenOn by remember { mutableStateOf(preferencesManager.keepScreenOn) }
    var vibrationEnabled by remember { mutableStateOf(preferencesManager.vibrationEnabled) }
    var soundEnabled by remember { mutableStateOf(preferencesManager.soundEnabled) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Paramètres",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Section Connexion
        item {
            SectionHeader("Connexion")
        }

        item {
            SettingSwitch(
                title = "Connexion automatique",
                description = "Se connecter automatiquement au dernier scooter",
                checked = autoConnect,
                onCheckedChange = {
                    autoConnect = it
                    preferencesManager.autoConnectEnabled = it
                }
            )
        }

        // Section Interface
        item {
            SectionHeader("Interface")
        }

        item {
            SettingSwitch(
                title = "Garder l'écran allumé",
                description = "Empêcher la mise en veille pendant l'utilisation",
                checked = keepScreenOn,
                onCheckedChange = {
                    keepScreenOn = it
                    preferencesManager.keepScreenOn = it
                }
            )
        }

        // Section Notifications
        item {
            SectionHeader("Notifications")
        }

        item {
            SettingSwitch(
                title = "Vibrations",
                description = "Activer les vibrations pour les notifications",
                checked = vibrationEnabled,
                onCheckedChange = {
                    vibrationEnabled = it
                    preferencesManager.vibrationEnabled = it
                }
            )
        }

        item {
            SettingSwitch(
                title = "Sons",
                description = "Activer les sons de notification",
                checked = soundEnabled,
                onCheckedChange = {
                    soundEnabled = it
                    preferencesManager.soundEnabled = it
                }
            )
        }

        // Section Informations
        item {
            SectionHeader("Informations")
        }

        item {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "IX7TrackerPro",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Version 1.0.0",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Application de suivi pour scooters M0Robot",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = "•",
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "•",
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}