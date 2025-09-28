package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ix7.tracker.bluetooth.BluetoothRepository
import com.ix7.tracker.core.ConnectionState
import com.ix7.tracker.utils.LogManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    bluetoothManager: BluetoothRepository,
    logManager: LogManager
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Observer les états
    val connectionState by bluetoothManager.connectionState.collectAsState()
    val discoveredDevices by bluetoothManager.discoveredDevices.collectAsState()
    val scooterData by bluetoothManager.scooterData.collectAsState()
    val isScanning by bluetoothManager.isScanning.collectAsState()

    Column {
        // Barre de navigation en haut
        NavigationBar {
            NavigationBarItem(
                icon = { Text("•") },
                label = { Text("Connexion") },
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 }
            )
            NavigationBarItem(
                icon = { Text("•") },
                label = { Text("Données") },
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 }
            )
            NavigationBarItem(
                icon = { Text("•") },
                label = { Text("Logs") },
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 }
            )
        }

        // Contenu selon l'onglet sélectionné
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> ConnectionScreen(
                    bluetoothManager = bluetoothManager,
                    discoveredDevices = discoveredDevices,
                    connectionState = connectionState,
                    isScanning = isScanning
                )
                1 -> DataScreen(
                    scooterData = scooterData,
                    isConnected = connectionState == ConnectionState.CONNECTED
                )
                2 -> LogScreen(logManager = logManager)
            }
        }
    }
}