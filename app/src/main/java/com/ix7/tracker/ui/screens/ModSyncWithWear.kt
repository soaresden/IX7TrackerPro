package com.ix7.tracker.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.data.TripRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ModSyncWithWear() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { TripRepository(context) }

    var expanded by remember { mutableStateOf(false) }

    // État de la montre
    var wearDeviceName by remember { mutableStateOf("Montre IX7") }
    var wearAppInstalled by remember { mutableStateOf(true) }
    var wearTripCount by remember { mutableStateOf(30) }
    var wearLastTripDate by remember { mutableStateOf(Date()) }

    // État du téléphone
    var phoneTripsCount by remember { mutableStateOf(0) }
    val trips by repository.allTrips.collectAsState(initial = emptyList())
    val last30Trips by repository.last30Trips.collectAsState(initial = emptyList())

    // États de synchronisation
    var syncInProgress by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf("") }
    var syncDirection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(trips) {
        phoneTripsCount = trips.size
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(12.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        // ===== HEADER CLICKABLE =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Infos compactes gauche + droite
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Montre
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⌚", fontSize = 14.sp)
                    Text("$wearTripCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A84FF))
                }

                Text("|", fontSize = 12.sp, color = Color.Gray)

                // Téléphone
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📱", fontSize = 14.sp)
                    Text("$phoneTripsCount", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9500))
                }
            }

            // Chevron
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }

        // ===== CONTENU EXPANDABLE =====
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ===== GAUCHE: MONTRE =====
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⌚", fontSize = 20.sp, modifier = Modifier.padding(bottom = 6.dp))
                        Text(wearDeviceName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            if (wearAppInstalled) "✅ App" else "❌ App",
                            fontSize = 8.sp,
                            color = if (wearAppInstalled) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("$wearTripCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A84FF))
                        Text("trajets", fontSize = 8.sp, color = Color.Gray)
                    }

                    // ===== CENTRE: BOUTONS =====
                    Column(
                        modifier = Modifier.width(60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                syncInProgress = true
                                syncDirection = "to_phone"
                                syncStatus = "Récupération..."
                                scope.launch {
                                    try {
                                        kotlinx.coroutines.delay(2000)
                                        wearTripCount = 28
                                        syncStatus = "✅ Reçu"
                                        kotlinx.coroutines.delay(1500)
                                        syncStatus = ""
                                    } catch (e: Exception) {
                                        syncStatus = "❌ Erreur"
                                    } finally {
                                        syncInProgress = false
                                        syncDirection = null
                                    }
                                }
                            },
                            enabled = !syncInProgress && wearAppInstalled,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                null,
                                tint = if (wearAppInstalled && !syncInProgress) Color(0xFF0A84FF) else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        IconButton(
                            onClick = {
                                syncInProgress = true
                                syncDirection = "to_wear"
                                syncStatus = "Envoi..."
                                scope.launch {
                                    try {
                                        kotlinx.coroutines.delay(2000)
                                        syncStatus = "✅ Envoyé"
                                        kotlinx.coroutines.delay(1500)
                                        syncStatus = ""
                                    } catch (e: Exception) {
                                        syncStatus = "❌ Erreur"
                                    } finally {
                                        syncInProgress = false
                                        syncDirection = null
                                    }
                                }
                            },
                            enabled = !syncInProgress && wearAppInstalled && phoneTripsCount > 0,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowForward,
                                null,
                                tint = if (wearAppInstalled && phoneTripsCount > 0 && !syncInProgress) Color(0xFF4CAF50) else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // ===== DROITE: TÉLÉPHONE =====
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📱", fontSize = 20.sp, modifier = Modifier.padding(bottom = 6.dp))
                        Text("Téléphone", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("✅ Connecté", fontSize = 8.sp, color = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("$phoneTripsCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9500))
                        Text("trajets", fontSize = 8.sp, color = Color.Gray)
                    }
                }

                // Status
                if (syncStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (syncInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.5.dp,
                                color = Color(0xFF0A84FF)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            syncStatus,
                            fontSize = 9.sp,
                            color = when {
                                syncStatus.contains("✅") -> Color(0xFF4CAF50)
                                syncStatus.contains("❌") -> Color(0xFFF44336)
                                else -> Color.White
                            }
                        )
                    }
                }
            }
        }
    }
}