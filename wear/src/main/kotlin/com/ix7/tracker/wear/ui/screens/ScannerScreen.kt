package com.ix7.tracker.wear.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
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

    // Filter only scooters (M0Robot devices)
    val scooters = discoveredDevices.filter { device ->
        device.name.contains("M0Robot", ignoreCase = true) ||
                device.name.contains("m0robot", ignoreCase = true)
    }

    // Auto-switch to ControlScreen when connected
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            onConnected()
        }
    }

    // Back handler to exit
    BackHandler {
        (context as? android.app.Activity)?.finish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(4.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ========== TOP (peu d'espace) ==========
        Spacer(modifier = Modifier.height(2.dp))

        // ========== CENTRE (contenu principal) ==========
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "🛴",
                fontSize = 36.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "M0 Tracker",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Contenu: Liste ou Animation
            if (isScanning && scooters.isEmpty()) {
                // Scanning animation
                ScanningAnimation()
                Text(
                    text = "Recherche...",
                    color = Color.Cyan,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            } else if (scooters.isEmpty() && !isScanning) {
                // No scooters found
                Text(
                    text = "Aucune trottinette",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                // Display scooters
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(scooters) { device ->
                            ScooterItem(
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
                }
            }
        }

        // ========== BOTTOM (2 petits boutons centrés) ==========
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bouton Rechercher/Stop (petit)
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
                    .size(28.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isScanning) Color(0xFF8B0000) else Color(0xFF2a4a2a),
                    disabledBackgroundColor = Color(0xFF555555)
                ),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (isScanning) "⏹" else "🔍",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Bouton Quitter (petit)
            Button(
                onClick = {
                    (context as? android.app.Activity)?.finish()
                },
                modifier = Modifier
                    .size(28.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4a0000)),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "✕",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ScanningAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")

    val width by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "width"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        // Ligne qui se raccourcit
        Box(
            modifier = Modifier
                .width((40 + width).dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color(0xFFFFD700))
        )

        // Ondes radar
        Text(
            text = "📡",
            fontSize = 36.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun ScooterItem(
    device: BluetoothDeviceInfo,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
    val signalStrength = device.rssi?.let { rssi ->
        when {
            rssi > -50 -> "Très proche"
            rssi > -70 -> "Proche"
            rssi > -85 -> "Moyen"
            else -> "Loin"
        }
    } ?: "?"

    val signalEmoji = device.rssi?.let { rssi ->
        when {
            rssi > -50 -> "⚡"
            rssi > -70 -> "📍"
            rssi > -85 -> "📡"
            else -> "❌"
        }
    } ?: "?"

    Button(
        onClick = onConnect,
        enabled = !isConnecting,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .height(40.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(0xFF1a1a2e),
            disabledBackgroundColor = Color(0xFF555555)
        ),
        shape = RoundedCornerShape(5.dp),
        contentPadding = PaddingValues(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = device.name,
                    color = Color(0xFFFFD700),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = signalEmoji,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = device.address,
                    color = Color(0xFF999999),
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = signalStrength,
                    color = Color(0xFF00FF00),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}