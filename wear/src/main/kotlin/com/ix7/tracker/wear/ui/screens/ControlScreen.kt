package com.ix7.tracker.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.wear.bluetooth.WearScooterManager
import com.ix7.tracker.wear.bluetooth.LockManager
import com.ix7.tracker.wear.bluetooth.ConnectionState
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay

@Composable
fun ControlScreen(
    scooterManager: WearScooterManager,
    lockManager: LockManager,
    context: Context,
    scooterName: String = "M0Robot",
    lockName: String = "Iphone9",
    onBackClick: () -> Unit,
    onTripRecorder: () -> Unit = {}
) {
    val connectionState by scooterManager.connectionState.collectAsState()
    val lockState by lockManager.lockState.collectAsState()
    val scooterData by scooterManager.scooterData.collectAsState()

    val scooterConnected = connectionState == ConnectionState.CONNECTED
    val lockConnected = lockState.isConnected
    val lockDetected = lockState.isDetected

    var lastScooterAddress by remember { mutableStateOf<String?>(null) }

    // Setup: define password and start lock scanning
    LaunchedEffect(Unit) {
        lockManager.setPassword("896647")

        try {
            val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
            if (bluetoothAdapter != null) {
                lockManager.startScanning(bluetoothAdapter)
            }
        } catch (e: Exception) {
            Log.e("CONTROL_SCREEN", "Error starting lock scan: ${e.message}")
        }
    }

    // Auto-reconnect if scooter disconnects
    LaunchedEffect(scooterConnected) {
        if (!scooterConnected && lastScooterAddress != null) {
            delay(1000)
            try {
                val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                if (bluetoothAdapter != null && lastScooterAddress != null) {
                    scooterManager.connectToDevice(lastScooterAddress!!, bluetoothAdapter)
                }
            } catch (e: Exception) {
                Log.e("CONTROL_SCREEN", "Auto-reconnect error: ${e.message}")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 0.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // ESPACE EN HAUT
        Spacer(modifier = Modifier.height(24.dp))

        // ========== TROTTINETTE ==========
        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .align(Alignment.CenterHorizontally)
                .background(Color(0xFF1a1a1a), RoundedCornerShape(4.dp))
                .padding(horizontal = 3.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🛴 $scooterName",
                        color = Color(0xFFFFD700),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (scooterConnected) "🟢" else "🔴",
                    fontSize = 16.sp
                )
            }

            // 3 boutons ronds - Trottinette
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .align(Alignment.CenterHorizontally)
                    .height(38.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Statut
                Button(
                    onClick = { },
                    enabled = false,
                    modifier = Modifier
                        .size(38.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        disabledBackgroundColor = if (scooterConnected) Color(0xFFFFD700) else Color(0xFF555555)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("⚡", fontSize = 16.sp, color = if (scooterConnected) Color.Black else Color.White)
                }

                // Lock
                Button(
                    onClick = {
                        scooterManager.lockScooter()
                    },
                    enabled = scooterConnected,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (scooterConnected) Color(0xFF8B0000) else Color(0xFF4a2d2d)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("🔒", fontSize = 16.sp)
                }

                // Unlock
                Button(
                    onClick = {
                        scooterManager.unlockScooter()
                    },
                    enabled = scooterConnected,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (scooterConnected) Color(0xFF2d5a2d) else Color(0xFF2d3a2d)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("🔓", fontSize = 16.sp)
                }
            }
        }

        // ========== CADENAS ==========
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .align(Alignment.CenterHorizontally)
                .background(Color(0xFF1a1a1a), RoundedCornerShape(4.dp))
                .padding(horizontal = 3.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔐 Cadenas $lockName",
                        color = Color(0xFFFF6B6B),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (lockConnected) "🟢 ${if (lockState.isLocked) "🔒" else "🔓"}" else "🔴",
                    fontSize = 14.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .align(Alignment.CenterHorizontally)
                    .height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Éclair cadenas
                Button(
                    onClick = {
                        if (!lockConnected && lockDetected) {
                            try {
                                val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                                if (bluetoothAdapter != null) {
                                    lockManager.connect(bluetoothAdapter)
                                }
                            } catch (e: Exception) {
                                Log.e("CONTROL", "Error: ${e.message}")
                            }
                        } else if (lockConnected) {
                            lockManager.disconnect()
                        }
                    },
                    enabled = lockDetected,
                    modifier = Modifier
                        .size(44.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = when {
                            lockConnected -> Color(0xFFFFD700)
                            lockDetected -> Color(0xFF555555)
                            else -> Color(0xFF1a1a1a)
                        },
                        disabledBackgroundColor = Color(0xFF1a1a1a)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("⚡", fontSize = 18.sp, color = when {
                        lockConnected -> Color.Black
                        else -> Color.White
                    })
                }

                // Lock cadenas
                Button(
                    onClick = { lockManager.lock() },
                    enabled = lockConnected,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (lockConnected) Color(0xFF8B0000) else Color(0xFF4a2d2d)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("🔒", fontSize = 16.sp)
                }

                // Unlock cadenas
                Button(
                    onClick = { lockManager.unlock() },
                    enabled = lockConnected,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (lockConnected) Color(0xFF2d5a2d) else Color(0xFF2d3a2d)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("🔓", fontSize = 16.sp)
                }
            }
        }

        // Spacer pour remonter les boutons
        Spacer(modifier = Modifier.height(20.dp))

        // Bottom buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp)
                .height(28.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Bouton Trip Recorder
            Button(
                onClick = onTripRecorder,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1a4d1a)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                shape = RoundedCornerShape(3.dp)
            ) {
                Text("📍 Trip", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }

            // Bouton Retour
            Button(
                onClick = {
                    // Disconnect safely
                    if (lockConnected) {
                        lockManager.disconnect()
                    }
                    scooterManager.disconnect()
                    onBackClick()
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                shape = RoundedCornerShape(3.dp)
            ) {
                Text("← Retour", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}