package com.ix7.tracker.ui.components

import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.bluetooth.LockManager
import com.ix7.tracker.bluetooth.LockState

@Composable
fun RideLockView(
    lockManager: LockManager,
    lockState: LockState,
    context: Context,
    modifier: Modifier = Modifier
) {
    // ⏱️ DEBOUNCE : Empêcher les clics multiples
    var lastClickTime by remember { mutableStateOf(0L) }

    AnimatedVisibility(
        visible = lockState.isDetected,
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2C2C2E)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header avec état
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "🔐 ${lockState.name}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Indicateur de connexion
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (lockState.isConnected) Color.Green
                                    else Color.Yellow // Jaune = détecté mais pas connecté
                                )
                        )

                        // Afficher la batterie si disponible
                        if (lockState.batteryLevel > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "🔋 ${lockState.batteryLevel}%",
                                fontSize = 12.sp,
                                color = when {
                                    lockState.batteryLevel > 50 -> Color.Green
                                    lockState.batteryLevel > 20 -> Color.Yellow
                                    else -> Color.Red
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bouton connexion/déconnexion
                    Button(
                        onClick = {
                            if (lockState.isConnected) {
                                lockManager.disconnect()
                            } else {
                                try {
                                    val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
                                    lockManager.connect(bluetoothAdapter)
                                } catch (e: Exception) {
                                    Log.e("ModLockView", "❌ Erreur connexion: ${e.message}")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (lockState.isConnected)
                                Color.Red else Color.Blue
                        ),
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            if (lockState.isConnected) "Déconnecter" else "Connecter",
                            fontSize = 12.sp
                        )
                    }
                }

                // Afficher l'état actuel (locked/unlocked) si pas connecté
                if (!lockState.isConnected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (lockState.isLocked)
                                    Color(0xFF8B0000).copy(alpha = 0.3f)
                                else
                                    Color(0xFF006400).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lockState.isLocked) "🔒 Verrouillé" else "🔓 Déverrouillé",
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Boutons Lock/Unlock - Affichés uniquement si connecté
                if (lockState.isConnected) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 🔓 UNLOCK
                        Button(
                            onClick = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClickTime > 2000) {
                                    lastClickTime = currentTime
                                    lockManager.unlock()
                                    Log.d("ModLockView", "🔓 UNLOCK cliqué")
                                }
                            },
                            enabled = lockState.isConnected,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!lockState.isLocked)
                                    Color(0xFF2E7D32) // Vert foncé si déjà déverrouillé
                                else
                                    Color(0xFF4CAF50), // Vert clair si verrouillé
                                disabledContainerColor = Color.Gray
                            ),
                            modifier = Modifier.weight(1f).height(60.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (!lockState.isLocked) "✓" else "🔓",
                                    fontSize = 24.sp
                                )
                                Text("UNLOCK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // 🔒 LOCK
                        Button(
                            onClick = {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClickTime > 2000) {
                                    lastClickTime = currentTime
                                    lockManager.lock()
                                    Log.d("ModLockView", "🔒 LOCK cliqué")
                                }
                            },
                            enabled = lockState.isConnected,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lockState.isLocked)
                                    Color(0xFFC62828) // Rouge foncé si déjà verrouillé
                                else
                                    Color(0xFFF44336), // Rouge clair si déverrouillé
                                disabledContainerColor = Color.Gray
                            ),
                            modifier = Modifier.weight(1f).height(60.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (lockState.isLocked) "✓" else "🔒",
                                    fontSize = 24.sp
                                )
                                Text("LOCK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}