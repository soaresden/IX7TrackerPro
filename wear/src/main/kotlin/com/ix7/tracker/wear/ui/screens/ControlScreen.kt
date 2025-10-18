package com.ix7.tracker.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun ControlScreen(
    scooterManager: WearScooterManager,
    lockManager: LockManager,
    context: Context,
    scooterName: String = "M0Robot",
    onBackClick: () -> Unit
) {
    val connectionState by scooterManager.connectionState.collectAsState()
    val lockState by lockManager.lockState.collectAsState()
    val scooterData by scooterManager.scooterData.collectAsState()

    val scooterConnected = connectionState == ConnectionState.CONNECTED
    val lockConnected = lockState.isConnected
    val lockDetected = lockState.isDetected

    // ✅ Setup: définir le password du cadenas et démarrer le scan
    LaunchedEffect(Unit) {
        lockManager.setPassword("896647")
        Log.d("CONTROL_SCREEN", "Password set")

        try {
            val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
            if (bluetoothAdapter != null) {
                lockManager.startScanning(bluetoothAdapter)
                Log.d("CONTROL_SCREEN", "Lock scanning started")
            }
        } catch (e: Exception) {
            Log.e("CONTROL_SCREEN", "Error starting lock scan: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(top = 40.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ========== TROTTINETTE ==========
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "🛴 $scooterName",
                        color = Color(0xFFFFD700),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "⚡ ${scooterData.battery}% | 🌡️ ${scooterData.temperature.toInt()}°C",
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }
                Text(
                    text = if (scooterConnected) "🟢" else "🔴",
                    fontSize = 11.sp
                )
            }

            // 3 boutons ronds - Trottinette
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Statut trottinette
                Button(
                    onClick = { },
                    enabled = false,
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        disabledBackgroundColor = if (scooterConnected) Color(0xFFFFD700) else Color(0xFF555555)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text(
                        "⚡",
                        fontSize = 16.sp,
                        color = if (scooterConnected) Color.Black else Color.White
                    )
                }

                // Lock scooter
                Button(
                    onClick = {
                        Log.d("CONTROL", "🔒 Lock scooter cliqué")
                        Log.d("CONTROL", "   txCharacteristic disponible")
                        Log.d("CONTROL", "   connectionState: $connectionState")
                        scooterManager.lockScooter()
                    },
                    enabled = scooterConnected,
                    modifier = Modifier
                        .weight(1f)
                        .size(44.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (scooterConnected) Color(0xFF8B0000) else Color(0xFF4a2d2d)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("🔒", fontSize = 14.sp)
                }

                // Unlock scooter
                Button(
                    onClick = {
                        Log.d("CONTROL", "🔓 Unlock scooter cliqué")
                        Log.d("CONTROL", "   txCharacteristic disponible")
                        Log.d("CONTROL", "   connectionState: $connectionState")
                        scooterManager.unlockScooter()
                    },
                    enabled = scooterConnected,
                    modifier = Modifier
                        .weight(1f)
                        .size(44.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (scooterConnected) Color(0xFF2d5a2d) else Color(0xFF2d3a2d)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("🔓", fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ========== CADENAS ==========
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🔐 Cadenas",
                    color = Color(0xFFFF6B6B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (lockConnected) "🟢 ${if (lockState.isLocked) "🔒" else "🔓"}" else "🔴",
                    fontSize = 11.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Éclair cadenas - NOIR si pas détecté, GRIS si détecté mais pas connecté, JAUNE si connecté
                Button(
                    onClick = {
                        Log.d("CONTROL", "⚡ Éclair cadenas cliqué")
                        Log.d("CONTROL", "   Detected: $lockDetected, Connected: $lockConnected")

                        if (!lockConnected && lockDetected) {
                            Log.d("CONTROL", "Connexion au cadenas...")
                            try {
                                val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                                if (bluetoothAdapter != null) {
                                    lockManager.connect(bluetoothAdapter)
                                }
                            } catch (e: Exception) {
                                Log.e("CONTROL", "Error connecting lock: ${e.message}")
                            }
                        } else if (lockConnected) {
                            Log.d("CONTROL", "Déconnexion du cadenas...")
                            lockManager.disconnect()
                        }
                    },
                    enabled = lockDetected,
                    modifier = Modifier.size(44.dp),
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
                    Text(
                        "⚡",
                        fontSize = 16.sp,
                        color = when {
                            lockConnected -> Color.Black
                            else -> Color.White
                        }
                    )
                }

                // Lock cadenas
                Button(
                    onClick = {
                        Log.d("CONTROL", "🔒 Lock cadenas cliqué")
                        lockManager.lock()
                    },
                    enabled = lockConnected,
                    modifier = Modifier
                        .weight(1f)
                        .size(44.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (lockConnected) Color(0xFF8B0000) else Color(0xFF4a2d2d)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("🔒", fontSize = 14.sp)
                }

                // Unlock cadenas
                Button(
                    onClick = {
                        Log.d("CONTROL", "🔓 Unlock cadenas cliqué")
                        lockManager.unlock()
                    },
                    enabled = lockConnected,
                    modifier = Modifier
                        .weight(1f)
                        .size(44.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (lockConnected) Color(0xFF2d5a2d) else Color(0xFF2d3a2d)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("🔓", fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333)),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            Text("Retour", fontSize = 9.sp, color = Color.White)
        }
    }
}