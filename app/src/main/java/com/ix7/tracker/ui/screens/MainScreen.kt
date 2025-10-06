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

    val connectionState by bluetoothManager.connectionState.collectAsState()
    val discoveredDevices by bluetoothManager.discoveredDevices.collectAsState()
    val scooterData by bluetoothManager.scooterData.collectAsState()
    val isScanning by bluetoothManager.isScanning.collectAsState()

    Column {
        NavigationBar {
            NavigationBarItem(
                icon = { Text("🔗") },
                label = { Text("Connexion") },
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 }
            )
            NavigationBarItem(
                icon = { Text("📊") },
                label = { Text("Données") },
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 }
            )
            NavigationBarItem(
                icon = { Text("🛴") },
                label = { Text("Conduite") },
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 }
            )
            NavigationBarItem(
                icon = { Text("📝") },
                label = { Text("Logs") },
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 }
            )

            NavigationBarItem(
                icon = { Text("🧪") },
                label = { Text("Test") },
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 }
            )
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (selectedTab) {
                0 -> ConnectionScreen(bluetoothManager, discoveredDevices, connectionState, isScanning)
                1 -> CompactDataScreen(bluetoothManager, scooterData, connectionState)
                2 -> RideScreen(scooterData, connectionState == ConnectionState.CONNECTED, bluetoothManager)  // AJOUTE bluetoothManager
                3 -> LogScreen(logManager)
                4 -> TestScreen(bluetoothManager, connectionState == ConnectionState.CONNECTED)
            }
        }
    }
}