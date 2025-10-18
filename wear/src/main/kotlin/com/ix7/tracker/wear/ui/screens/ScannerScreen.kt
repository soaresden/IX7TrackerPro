package com.ix7.tracker.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.wear.bluetooth.WearBluetoothManager
import kotlinx.coroutines.launch

@Composable
fun ScannerScreen(
    bluetoothManager: WearBluetoothManager,
    onScooterSelected: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val isScanning by bluetoothManager.isScanning.collectAsState()
    val scooters by bluetoothManager.discoveredScooters.collectAsState()

    LaunchedEffect(Unit) {
        scope.launch {
            bluetoothManager.startScan()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isScanning) "Scan..." else "Trottinettes",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isScanning) {
            Text(
                text = "Recherche en cours...",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 20.dp)
            )
        } else if (scooters.isEmpty()) {
            Text(
                text = "Aucune trottinette trouvée",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 20.dp)
            )
        } else {
            Column(modifier = Modifier.weight(1f)) {
                scooters.forEach { scooter ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.DarkGray)
                            .clickable {
                                scope.launch {
                                    bluetoothManager.stopScan()
                                    bluetoothManager.connectToScooter(scooter.address, scooter.name)
                                    onScooterSelected(scooter.address)
                                }
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(scooter.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${scooter.distance} (${scooter.rssi} dBm)", color = Color.LightGray, fontSize = 9.sp)
                            Text(scooter.address, color = Color.LightGray, fontSize = 8.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Button(
            onClick = {
                scope.launch {
                    bluetoothManager.stopScan()
                    onBackClick()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Text("Retour", fontSize = 12.sp)
        }
    }
}