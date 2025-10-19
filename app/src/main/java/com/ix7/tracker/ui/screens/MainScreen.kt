package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.bluetooth.BluetoothRepository
import com.ix7.tracker.core.ConnectionState
import com.ix7.tracker.utils.LogManager
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.LaunchedEffect

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
    val framesState = remember { mutableStateMapOf<String, FrameMonitor>() }

    LaunchedEffect(Unit) {  // ✅ Démarre immédiatement, pas besoin d'attendre CONNECTED
        bluetoothManager.rawFrameFlow.collect { frame ->
            updateFrameMonitor(framesState, frame)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Barre de navigation avec 6 onglets
        NavigationBar(
            containerColor = Color(0xFF1C1C1E),
            contentColor = Color.White
        ) {
            // 0 - CONNEXION
            NavigationBarItem(
                icon = { Text("🔌", fontSize = 20.sp) },
                label = { Text("Connexion", fontSize = 10.sp) },
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF0A84FF),
                    selectedTextColor = Color(0xFF0A84FF),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color(0xFF2C2C2E)
                )
            )

            // 1 - INFORMATIONS
            NavigationBarItem(
                icon = { Text("ℹ️", fontSize = 20.sp) },
                label = { Text("Infos", fontSize = 10.sp) },
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF0A84FF),
                    selectedTextColor = Color(0xFF0A84FF),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color(0xFF2C2C2E)
                )
            )

            // 2 - RIDE
            NavigationBarItem(
                icon = { Text("🛴", fontSize = 20.sp) },
                label = { Text("Ride", fontSize = 10.sp) },
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF0A84FF),
                    selectedTextColor = Color(0xFF0A84FF),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color(0xFF2C2C2E)
                )
            )

            // 3 - TRIPS (Historique)
            NavigationBarItem(
                icon = { Text("📊", fontSize = 20.sp) },
                label = { Text("Trips", fontSize = 10.sp) },
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF0A84FF),
                    selectedTextColor = Color(0xFF0A84FF),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color(0xFF2C2C2E)
                )
            )

            // 4 - TEST
            NavigationBarItem(
                icon = { Text("🧪", fontSize = 20.sp) },
                label = { Text("Test", fontSize = 10.sp) },
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF0A84FF),
                    selectedTextColor = Color(0xFF0A84FF),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color(0xFF2C2C2E)
                )
            )

            // 5 - SETTINGS
            NavigationBarItem(
                icon = { Text("⚙️", fontSize = 20.sp) },
                label = { Text("Settings", fontSize = 10.sp) },
                selected = selectedTab == 5,
                onClick = { selectedTab = 5 },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF0A84FF),
                    selectedTextColor = Color(0xFF0A84FF),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color(0xFF2C2C2E)
                )
            )
        }

        // Contenu de l'écran sélectionné
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

                1 -> InfoScreen(
                    scooterData = scooterData,
                    isConnected = connectionState == ConnectionState.CONNECTED
                )

                2 -> Ride_Screen(
                    scooterData = scooterData,
                    isConnected = connectionState == ConnectionState.CONNECTED,
                    bluetoothManager = bluetoothManager
                )

                3 -> TripHistory_Screen()

                4 -> TestScreenNew(
                    bluetoothManager = bluetoothManager,
                    isConnected = connectionState == ConnectionState.CONNECTED,
                    framesState = framesState  // ✅ Passer le framesState au lieu du callback
                )

                5 -> SettingsScreen(
                    bluetoothManager = bluetoothManager,
                    isConnected = connectionState == ConnectionState.CONNECTED
                )
            }
        }
    }
}