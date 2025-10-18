package com.ix7.tracker.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.wear.bluetooth.WearBluetoothManager
import kotlinx.coroutines.launch

@Composable
fun ControlScreen(
    bluetoothManager: WearBluetoothManager,
    scooterName: String,
    scooterAddress: String,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val connectionState by bluetoothManager.connectionState.collectAsState()
    val scooterData by bluetoothManager.scooterData.collectAsState()

    var scooterLocked by remember { mutableStateOf(false) }
    var lastAction by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(scrollState)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "🛴 $scooterName",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = scooterAddress,
            color = Color.Gray,
            fontSize = 9.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Status
        Text(
            text = if (connectionState.name == "CONNECTED") "Connecté ✓" else "Déconnexion...",
            color = if (connectionState.name == "CONNECTED") Color.Green else Color.Red,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Données scooter
        scooterData?.let { data ->
            Text(
                text = "🔋 ${data.battery}% | 🌡️ ${data.temperature}°C | ⚡ ${data.speed} km/h",
                color = Color.LightGray,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bouton Lock/Unlock
        Button(
            onClick = {
                scooterLocked = !scooterLocked
                if (scooterLocked) {
                    bluetoothManager.lockScooter()
                    lastAction = "🔒 Trottinette verrouillée"
                } else {
                    bluetoothManager.unlockScooter()
                    lastAction = "🔓 Trottinette déverrouillée"
                }
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (scooterLocked) Color(0xFF2d5a2d) else Color(0xFF5a2d2d)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Text(
                text = if (scooterLocked) "🔒 Verrouillée" else "🔓 Déverrouiller",
                fontSize = 11.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dernier action
        if (lastAction.isNotEmpty()) {
            Text(
                text = lastAction,
                color = Color.Yellow,
                fontSize = 10.sp,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bouton Retour
        Button(
            onClick = {
                scope.launch {
                    bluetoothManager.disconnect()
                    onBackClick()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Text("← Retour", fontSize = 11.sp)
        }
    }
}