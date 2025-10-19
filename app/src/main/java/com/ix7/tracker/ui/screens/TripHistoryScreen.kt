package com.ix7.tracker.ui.screens

import android.content.Context
import com.ix7.tracker.core.SpeedLimitMode
import com.ix7.tracker.core.WheelMode
import com.ix7.tracker.core.RideMode
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.data.*
import com.ix7.tracker.ui.components.*
import com.ix7.tracker.utils.TripUtils
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { TripRepository(context) }
    val trips by repository.allTrips.collectAsState(initial = emptyList())

    var selectionMode by remember { mutableStateOf(false) }
    var selectedTrips by remember { mutableStateOf(setOf<String>()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var showBatteryCycles by remember { mutableStateOf(false) }
    var showModeComparison by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val filteredTrips = remember(trips, startDate, endDate) {
        if (startDate == null && endDate == null) trips
        else trips.filter { it.startDate.time in (startDate?.time ?: 0L)..(endDate?.time ?: Long.MAX_VALUE) }
    }

    val batteryCycles = remember(filteredTrips) {
        TripUtils.detectBatteryCycles(filteredTrips)
    }

    val modeStats = remember(filteredTrips) {
        TripUtils.analyzeModeStats(filteredTrips)
    }

    // 🗑️ Dialogue de confirmation de suppression
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer les trajets ?") },
            text = { Text("Voulez-vous vraiment supprimer ${selectedTrips.size} trajet(s) ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.deleteTrips(selectedTrips.toList())
                            selectedTrips = emptySet()
                            selectionMode = false
                            showDeleteDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {

        ModSyncWithWear()
        Spacer(modifier = Modifier.height(12.dp))


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (selectionMode) "${selectedTrips.size} sél."
                    else if (showBatteryCycles) "🔋 ${batteryCycles.size} cycles"
                    else if (showModeComparison) "📊 ${modeStats.size} modes"
                    else "📊 ${filteredTrips.size} trajets",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (selectionMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { selectionMode = false; selectedTrips = emptySet() }) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = selectedTrips.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, null, tint = if (selectedTrips.isNotEmpty()) Color.Red else Color.Gray)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { selectionMode = true }) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                    }
                    IconButton(onClick = { showDatePicker = !showDatePicker }) {
                        Icon(Icons.Default.DateRange, null, tint = if (startDate != null || endDate != null) Color(0xFF0A84FF) else Color.White)
                    }
                    IconButton(onClick = { exportToJson(context, filteredTrips) }) {
                        Icon(Icons.Default.Share, null, tint = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 🎯 3 BOUTONS DE MODE ALIGNÉS (mis en valeur)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Bouton Trajets
            Button(
                onClick = {
                    showBatteryCycles = false
                    showModeComparison = false
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!showBatteryCycles && !showModeComparison) Color(0xFF0A84FF) else Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📍", fontSize = 16.sp)
                    Text("Trajets", fontSize = 10.sp)
                }
            }

            // Bouton Cycles
            Button(
                onClick = {
                    showBatteryCycles = true
                    showModeComparison = false
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showBatteryCycles) Color(0xFF4CAF50) else Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔋", fontSize = 16.sp)
                    Text("Cycles", fontSize = 10.sp)
                }
            }

            // Bouton Comparaison
            Button(
                onClick = {
                    showBatteryCycles = false
                    showModeComparison = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showModeComparison) Color(0xFFFF9500) else Color.Transparent
                ),
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚙️", fontSize = 16.sp)
                    Text("Modes", fontSize = 10.sp)
                }
            }
        }

        AnimatedVisibility(visible = showDatePicker) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(selected = false, onClick = {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    startDate = cal.time
                    endDate = Date()
                }, label = { Text("Aujourd'hui", fontSize = 11.sp) })
                FilterChip(selected = false, onClick = {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    startDate = cal.time
                    endDate = Date()
                }, label = { Text("7j", fontSize = 11.sp) })
                FilterChip(selected = false, onClick = {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.MONTH, -1)
                    startDate = cal.time
                    endDate = Date()
                }, label = { Text("30j", fontSize = 11.sp) })
                FilterChip(selected = startDate == null && endDate == null, onClick = {
                    startDate = null
                    endDate = null
                }, label = { Text("Tout", fontSize = 11.sp) })
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (showBatteryCycles) {
            BatteryCyclesView(cycles = batteryCycles)
        } else if (showModeComparison) {
            ModeComparisonScreen(modeStats = modeStats)
        } else {
            if (filteredTrips.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Aucun trajet enregistré", color = Color.Gray, fontSize = 14.sp)
                        Text("Appuyez sur ▶ pour démarrer", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredTrips) { trip ->
                        TripCard(
                            trip = trip,
                            selectionMode = selectionMode,
                            isSelected = trip.id in selectedTrips,
                            onToggleSelection = {
                                selectedTrips = if (trip.id in selectedTrips) {
                                    selectedTrips - trip.id
                                } else {
                                    selectedTrips + trip.id
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// 📤 Export JSON
private fun exportToJson(context: Context, trips: List<Trip>) {
    try {
        val jsonArray = JSONArray()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

        trips.forEach { trip ->
            val tripJson = JSONObject().apply {
                put("id", trip.id)
                put("startDate", dateFormat.format(trip.startDate))
                put("endDate", dateFormat.format(trip.endDate))
                put("startBattery", trip.startBattery)
                put("endBattery", trip.endBattery)
                put("startOdometer", trip.startOdometer)
                put("endOdometer", trip.endOdometer)
                put("distance", trip.distance)
                put("duration", trip.duration)
                put("maxSpeed", trip.maxSpeed)
                put("avgSpeed", trip.avgSpeed)
                put("energyUsed", trip.energyUsed)

                put("startLocation", JSONObject().apply {
                    put("latitude", trip.startLocation.latitude)
                    put("longitude", trip.startLocation.longitude)
                    put("address", trip.startLocation.address)
                })

                put("endLocation", JSONObject().apply {
                    put("latitude", trip.endLocation.latitude)
                    put("longitude", trip.endLocation.longitude)
                    put("address", trip.endLocation.address)
                })

                put("speedStats", JSONObject().apply {
                    put("range0", trip.speedStats.range0)
                    put("range0_10", trip.speedStats.range0_10)
                    put("range10_20", trip.speedStats.range10_20)
                    put("range20_30", trip.speedStats.range20_30)
                    put("range30_40", trip.speedStats.range30_40)
                    put("range40_50", trip.speedStats.range40_50)
                    put("range50_60", trip.speedStats.range50_60)
                    put("rangeAbove60", trip.speedStats.rangeAbove60)
                })

                put("settings", JSONObject().apply {
                    put("RideMode", trip.settings.ridingMode.name)
                    put("driveMode", trip.settings.driveMode.name)
                    put("SpeedLimitMode", trip.settings.speedLock.name)
                })
            }
            jsonArray.put(tripJson)
        }

        val jsonString = jsonArray.toString(2) // Indentation de 2 espaces

        // Partager le JSON
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, jsonString)
            putExtra(Intent.EXTRA_SUBJECT, "IX7 Tracker - Export ${trips.size} trajets")
            type = "application/json"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Partager les trajets")
        context.startActivity(shareIntent)

    } catch (e: Exception) {
        android.util.Log.e("TripHistoryScreen", "Erreur export JSON: ${e.message}")
    }
}