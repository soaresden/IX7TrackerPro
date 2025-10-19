package com.ix7.tracker.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.ix7.tracker.ui.components.ModBatteryCycles
import com.ix7.tracker.ui.components.ModComparison
import com.ix7.tracker.ui.components.ModTripFilters
import com.ix7.tracker.ui.components.toBatteryCyclesData
import com.ix7.tracker.ui.components.toModeStatsData
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
    var selectedTripForDetail by remember { mutableStateOf<Trip?>(null) }

    val filteredTrips = remember(trips, startDate, endDate) {
        if (startDate == null && endDate == null) trips
        else trips.filter { it.startDate.time in (startDate?.time ?: 0L)..(endDate?.time ?: Long.MAX_VALUE) }
    }

    // ✅ Tri DÉCROISSANT par ID (plus récent en haut)
    val sortedTrips = remember(filteredTrips) {
        filteredTrips.sortedByDescending { it.id.toIntOrNull() ?: 0 }
    }

    val batteryCycles = remember(sortedTrips) {
        TripUtils.detectBatteryCycles(sortedTrips).toBatteryCyclesData()
    }

    val modeStats = remember(sortedTrips) {
        TripUtils.analyzeModeStats(sortedTrips).toModeStatsData()
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

    // 📋 Affichage du détail du trajet si sélectionné
    if (selectedTripForDetail != null) {
        val tripNumber = sortedTrips.indexOf(selectedTripForDetail) + 1
        TripDetailScreen(trip = selectedTripForDetail!!, tripNumber = tripNumber) {
            selectedTripForDetail = null
        }
        return
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
                    else if (showModeComparison) "⚙️ ${modeStats.size} modes"
                    else "📊 ${sortedTrips.size} trajets",
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
                    IconButton(onClick = { exportToJson(context, sortedTrips) }) {
                        Icon(Icons.Default.Share, null, tint = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 🎯 3 BOUTONS DE MODE ALIGNÉS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
                    Text("📊", fontSize = 16.sp)
                    Text("Trajets", fontSize = 10.sp)
                }
            }

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

        ModTripFilters(
            visible = showDatePicker,
            startDate = startDate,
            endDate = endDate,
            onStartDateChange = { startDate = it },
            onEndDateChange = { endDate = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (showBatteryCycles) {
            ModBatteryCycles(cycles = batteryCycles)
        } else if (showModeComparison) {
            ModComparison(modeStats = modeStats)
        } else {
            if (sortedTrips.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Aucun trajet enregistré", color = Color.Gray, fontSize = 14.sp)
                        Text("Appuyez sur ▶ pour démarrer", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sortedTrips) { trip ->
                        TripCardEnhanced(
                            trip = trip,
                            tripNumber = trips.indexOf(trip) + 1,  // Numérotation 1-based
                            selectionMode = selectionMode,
                            isSelected = trip.id in selectedTrips,
                            onToggleSelection = {
                                selectedTrips = if (trip.id in selectedTrips) {
                                    selectedTrips - trip.id
                                } else {
                                    selectedTrips + trip.id
                                }
                            },
                            onDetailClick = { selectedTripForDetail = trip }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 📱 Carte de trajet améliorée avec plus d'infos
 */
@Composable
fun TripCardEnhanced(
    trip: Trip,
    tripNumber: Int,
    selectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onDetailClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEE dd/MM/yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    val batteryUsed = trip.startBattery - trip.endBattery
    val durationHours = trip.duration / (1000 * 60 * 60)
    val durationMins = (trip.duration / (1000 * 60)) % 60

    // 🌙 Vérifier si c'est un trajet nocturne (après minuit)
    val cal = Calendar.getInstance().apply { time = trip.startDate }
    val isNocturnal = cal.get(Calendar.HOUR_OF_DAY) < 4

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !selectionMode) { onDetailClick() }
            .border(
                if (isSelected) 2.dp else 0.dp,
                if (isSelected) Color(0xFF0A84FF) else Color.Transparent,
                RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 📌 Header avec sélection et numéro
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggleSelection() })
                }

                Text(
                    "Trajet n°$tripNumber",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    dateFormat.format(trip.startDate),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ⏰ Temps avec vérification nocturne
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Jour : ", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(
                    "${timeFormat.format(trip.startDate)} → ${timeFormat.format(trip.endDate)}",
                    fontSize = 11.sp,
                    color = Color.White
                )
                if (isNocturnal) {
                    Text(" (nocturne)", fontSize = 10.sp, color = Color(0xFF9933FF))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 📊 Stats en grille
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBadge(
                    label = "Distance",
                    value = "${"%.1f".format(trip.distance)} km",
                    color = Color(0xFF0A84FF),
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "Durée",
                    value = "$durationHours h ${"$durationMins".padStart(2, '0')} min",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "Vitesse moy",
                    value = "${"%.1f".format(trip.avgSpeed)} km/h",
                    color = Color(0xFFBB86FC),
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    label = "Batterie",
                    value = "$batteryUsed%",
                    color = Color(0xFFFF9500),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(6.dp)
    ) {
        Text(label, fontSize = 9.sp, color = Color.Gray)
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
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
        val jsonString = jsonArray.toString(2)
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

@Composable
fun ModSyncWithWear() {
    // Placeholder - à implémenter
}