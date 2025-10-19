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
import com.ix7.tracker.data.Trip
import com.ix7.tracker.data.TripRepository
import com.ix7.tracker.ui.components.BatteryCycleData
import com.ix7.tracker.ui.components.BatteryCycles
import com.ix7.tracker.ui.components.ModComparison
import com.ix7.tracker.ui.components.ModTripFilters
import com.ix7.tracker.ui.components.ModeStatsData
import com.ix7.tracker.ui.components.toBatteryCyclesData
import com.ix7.tracker.ui.components.toModeStatsData
import com.ix7.tracker.utils.TripUtils
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import com.ix7.tracker.ui.screens.TripHistoryTripListDetailScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistory_Screen() {
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
        TripHistoryTripListDetailScreen(trip = selectedTripForDetail!!, tripNumber = tripNumber) {
            selectedTripForDetail = null
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {

        // 🔄 COMPOSABLE COLLAPSABLE DE SYNCHRONISATION
        TripHistoryWearInterfaceScreen(
            trips = sortedTrips,
            context = context,
            onSyncPhone = { tripsToSync ->
                syncWithWear(context, tripsToSync)
            },
            onSyncWatch = { tripsToSync ->
                exportToJson(context, tripsToSync)
            }
        )
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
            BatteryCycles(cycles = batteryCycles)
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
                            tripNumber = trips.indexOf(trip) + 1,
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

    val cal = Calendar.getInstance().apply { time = trip.startDate }
    val isNocturnal = cal.get(Calendar.HOUR_OF_DAY) < 4

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !selectionMode) { onDetailClick() }
            .border(
                2.dp,
                if (isSelected) Color(0xFF0A84FF) else Color.Gray.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isNocturnal) Color(0xFF1a1a2e) else Color(0xFF2C2C2E)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.size(24.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF0A84FF),
                        uncheckedColor = Color.Gray
                    )
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "#$tripNumber - ${dateFormat.format(trip.startDate)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0A84FF)
                )
                Text(
                    text = "${timeFormat.format(trip.startDate)} → ${timeFormat.format(trip.endDate)}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🛴 ${String.format("%.1f", trip.distance)} km",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                    Text(
                        text = "⏱️ ${durationHours}h ${durationMins}m",
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🔋 $batteryUsed% (${trip.startBattery}% → ${trip.endBattery}%)",
                        fontSize = 11.sp,
                        color = if (batteryUsed > 50) Color(0xFFFF3B30) else Color(0xFF4CAF50)
                    )
                    Text(
                        text = "⚡ ${String.format("%.1f", trip.avgSpeed)} km/h moy",
                        fontSize = 11.sp,
                        color = Color(0xFFFF9500)
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
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
                put("distance", trip.distance)
                put("duration", trip.duration)
                put("avgSpeed", trip.avgSpeed)
                put("settings", JSONObject().apply {
                    put("ridingMode", trip.settings.ridingMode.name)
                    put("driveMode", trip.settings.driveMode.name)
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

// 🔄 Sync avec Wear OS
private suspend fun syncWithWear(context: Context, trips: List<Trip>) {
    try {
        android.util.Log.d("WearSync", "Sync lancée avec ${trips.size} trajets")
        // TODO: Implémenter la sync réelle avec Wear OS
    } catch (e: Exception) {
        android.util.Log.e("WearSync", "Erreur sync: ${e.message}")
    }
}