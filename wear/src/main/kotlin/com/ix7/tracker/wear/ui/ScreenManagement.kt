package com.ix7.tracker.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ix7.tracker.wear.bluetooth.WearBluetoothManager
import com.ix7.tracker.wear.ui.screens.MenuScreen
import com.ix7.tracker.wear.ui.screens.ScannerScreen
import com.ix7.tracker.wear.ui.screens.ControlScreen

sealed class Screen {
    object Menu : Screen()
    object Scanner : Screen()
    object Control : Screen()
}

@Composable
fun ScreenManagement() {
    val context = LocalContext.current
    val bluetoothManager = remember { WearBluetoothManager(context) }

    val currentScreen = remember { mutableStateOf<Screen>(Screen.Menu) }
    val selectedScooterAddress = remember { mutableStateOf<String?>(null) }
    val selectedScooterName = remember { mutableStateOf<String?>(null) }

    when (currentScreen.value) {
        is Screen.Menu -> MenuScreen(
            onScannerClick = { currentScreen.value = Screen.Scanner }
        )

        is Screen.Scanner -> ScannerScreen(
            bluetoothManager = bluetoothManager,
            onScooterSelected = { address ->
                selectedScooterAddress.value = address
                currentScreen.value = Screen.Control
            },
            onBackClick = { currentScreen.value = Screen.Menu }
        )

        is Screen.Control -> ControlScreen(
            bluetoothManager = bluetoothManager,
            scooterName = selectedScooterName.value ?: "M0Robot",
            scooterAddress = selectedScooterAddress.value ?: "",
            onBackClick = {
                currentScreen.value = Screen.Menu
                selectedScooterAddress.value = null
                selectedScooterName.value = null
            }
        )
    }
}