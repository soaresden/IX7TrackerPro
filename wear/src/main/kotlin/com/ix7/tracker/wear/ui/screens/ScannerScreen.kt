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

@Composable
fun ScannerScreen(
    scooterManager: WearScooterManager,
    context: Context,
    onConnected: () -> Unit
) {
    val connectionState by scooterManager.connectionState.collectAsState()
    val isScanning by scooterManager.isScanning.collectAsState()
    val discoveredDevices by scooterManager.discoveredDevices.collectAsState()

    // ✅ Auto-basculer vers ControlScreen quand connecté
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            Log.d("SCANNER", "✅ CONNECTÉ! Basculement automatique...")
            onConnected()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ========== HEADER ==========
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🛴 Sélectionner Trottinette",
                color = Color(0xFFFFD700),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = when {
                    connectionState == ConnectionState.CONNECTED -> "✅ Connectée"
                    connectionState == ConnectionState.CONNECTING -> "⏳ Connexion..."
                    isScanning -> "🔍 Recherche..."
                    else -> "❌ Déconnectée"
                },
                color = when {
                    connectionState == ConnectionState.CONNECTED -> Color.Green
                    connectionState == ConnectionState.CONNECTING -> Color.Yellow
                    isScanning -> Color.Cyan
                    else -> Color.Red
                },
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
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
                    text = "Aucun device trouvé\nClique sur 'Rechercher'",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        } else if (discoveredDevices.isNotEmpty()) {
            // Liste des devices trouvés
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(discoveredDevices) { device ->
                    DeviceItem(
                        device = device,
                        isConnecting = connectionState == ConnectionState.CONNECTING,
                        onConnect = {
                            Log.d("SCANNER", "🔗 Connexion à ${device.name} (${device.address})")
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
                    fontSize = 48.sp
                )
            }
        }

        // ========== BOUTONS ==========
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Bouton Rechercher
            Button(
                onClick = {
                    try {
                        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                        val bluetoothAdapter = bluetoothManager.adapter
                        if (bluetoothAdapter != null) {
                            if (isScanning) {
                                Log.d("SCANNER", "Arrêt du scan")
                                scooterManager.stopScanning(bluetoothAdapter)
                            } else {
                                Log.d("SCANNER", "Démarrage du scan")
                                scooterManager.startScanning(bluetoothAdapter)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SCANNER", "Error toggling scan: ${e.message}")
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
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (isScanning) "🛑 Arrêter" else "🔍 Rechercher",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Bouton Actualiser (pour rafraîchir la liste)
            Button(
                onClick = {
                    Log.d("SCANNER", "Rafraîchissement liste")
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
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "🔄 Rafra.",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
            .height(40.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(0xFF2C2C2E),
            disabledBackgroundColor = Color(0xFF555555)
        ),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = device.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = device.address,
                    color = Color.Gray,
                    fontSize = 9.sp
                )
            }

            Text(
                text = "${device.rssi} dBm",
                color = Color(0xFFFFD700),
                fontSize = 10.sp
            )
        }
    }
}