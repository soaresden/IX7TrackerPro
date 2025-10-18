package com.ix7.tracker.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.ix7.tracker.wear.bluetooth.WearScooterManager
import com.ix7.tracker.wear.bluetooth.ConnectionState
import com.ix7.tracker.wear.bluetooth.BluetoothDeviceInfo
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler

@Composable
fun ScannerScreen(
    scooterManager: WearScooterManager,
    context: Context,
    onConnected: () -> Unit,
    onExit: () -> Unit = {}
) {
    val connectionState by scooterManager.connectionState.collectAsState()
    val isScanning by scooterManager.isScanning.collectAsState()
    val discoveredDevices by scooterManager.discoveredDevices.collectAsState()

    // Auto-exit on back
    BackHandler {
        (context as? android.app.Activity)?.finish()
    }

    // Auto-switch to ControlScreen when connected
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            onConnected()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(4.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // ========== HEADER ==========
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🛴",
                fontSize = 28.sp
            )

            Text(
                text = when {
                    connectionState == ConnectionState.CONNECTED -> "✅ OK"
                    connectionState == ConnectionState.CONNECTING -> "⏳"
                    isScanning -> "🔍"
                    else -> "❌"
                },
                color = when {
                    connectionState == ConnectionState.CONNECTED -> Color.Green
                    connectionState == ConnectionState.CONNECTING -> Color.Yellow
                    isScanning -> Color.Cyan
                    else -> Color.Red
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ========== BOUTONS (en haut) ==========
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Bouton Rechercher
            Button(
                onClick = {
                    try {
                        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                        val bluetoothAdapter = bluetoothManager.adapter
                        if (bluetoothAdapter != null) {
                            if (isScanning) {
                                scooterManager.stopScanning(bluetoothAdapter)
                            } else {
                                scooterManager.startScanning(bluetoothAdapter)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SCANNER", "Error: ${e.message}")
                    }
                },
                enabled = connectionState != ConnectionState.CONNECTING,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isScanning) Color(0xFF8B0000) else Color(0xFF333333),
                    disabledBackgroundColor = Color(0xFF555555)
                ),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(3.dp)
            ) {
                Text(
                    text = if (isScanning) "STOP" else "🔍",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Bouton Actualiser
            Button(
                onClick = {
                    try {
                        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                        val bluetoothAdapter = bluetoothManager.adapter
                        if (bluetoothAdapter != null) {
                            scooterManager.stopScanning(bluetoothAdapter)
                            Thread.sleep(500)
                            scooterManager.startScanning(bluetoothAdapter)
                        }
                    } catch (e: Exception) {
                        Log.e("SCANNER", "Error: ${e.message}")
                    }
                },
                enabled = !isScanning && connectionState != ConnectionState.CONNECTING,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF333333),
                    disabledBackgroundColor = Color(0xFF555555)
                ),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(3.dp)
            ) {
                Text(
                    text = "🔄",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Bouton Quitter
            Button(
                onClick = {
                    (context as? android.app.Activity)?.finish()
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4a0000)),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(3.dp)
            ) {
                Text(
                    text = "✕",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // ========== LISTE DES DEVICES ==========
        if (discoveredDevices.isEmpty() && !isScanning) {
            // Pas de devices trouvés
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun\ndevice",
                    color = Color.Gray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (discoveredDevices.isNotEmpty()) {
            // Liste des devices trouvés
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(discoveredDevices) { device ->
                    DeviceItem(
                        device = device,
                        isConnecting = connectionState == ConnectionState.CONNECTING,
                        onConnect = {
                            try {
                                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                                val bluetoothAdapter = bluetoothManager.adapter
                                if (bluetoothAdapter != null) {
                                    scooterManager.connectToDevice(device.address, bluetoothAdapter)
                                }
                            } catch (e: Exception) {
                                Log.e("SCANNER", "Error: ${e.message}")
                            }
                        }
                    )
                }
            }
        } else {
            // Scan en cours, afficher l'animation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔍",
                    fontSize = 56.sp
                )
            }
        }
    }
}

@Composable
fun DeviceItem(
    device: BluetoothDeviceInfo,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
    Button(
        onClick = onConnect,
        enabled = !isConnecting,
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(0xFF2C2C2E),
            disabledBackgroundColor = Color(0xFF555555)
        ),
        shape = RoundedCornerShape(3.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = device.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = device.address,
                color = Color(0xFFFFD700),
                fontSize = 11.sp
            )
        }
    }
}