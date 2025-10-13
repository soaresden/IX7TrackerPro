package com.ix7.tracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.data.*
import com.ix7.tracker.ui.components.*
import com.ix7.tracker.utils.TripUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen() {
    val trips = remember { mutableStateOf(TripUtils.generateDummyTrips()) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedTrips by remember { mutableStateOf(setOf<String>()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var showBatteryCycles by remember { mutableStateOf(false) }
    var showModeComparison by remember { mutableStateOf(false) }

    val filteredTrips = remember(trips.value, startDate, endDate) {
        if (startDate == null && endDate == null) trips.value
        else trips.value.filter { it.startDate.time in (startDate?.time ?: 0L)..(endDate?.time ?: Long.MAX_VALUE) }
    }

    val batteryCycles = remember(filteredTrips) {
        TripUtils.detectBatteryCycles(filteredTrips)
    }

    val modeStats = remember(filteredTrips) {
        TripUtils.analyzeModeStats(filteredTrips)
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
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

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (selectionMode) {
                    IconButton(onClick = { selectionMode = false; selectedTrips = emptySet() }) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            trips.value = trips.value.filterNot { it.id in selectedTrips }
                            selectedTrips = emptySet()
                            selectionMode = false
                        },
                        enabled = selectedTrips.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, null, tint = if (selectedTrips.isNotEmpty()) Color.Red else Color.Gray)
                    }
                } else {
                    // Bouton Cycles Batterie
                    IconButton(
                        onClick = {
                            showBatteryCycles = !showBatteryCycles
                            if (showBatteryCycles) showModeComparison = false
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (showBatteryCycles) Color(0xFF4CAF50) else Color.Transparent
                        )
                    ) {
                        Text("🔋", fontSize = 20.sp)
                    }

                    // Bouton Comparaison Modes
                    IconButton(
                        onClick = {
                            showModeComparison = !showModeComparison
                            if (showModeComparison) showBatteryCycles = false
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (showModeComparison) Color(0xFF0A84FF) else Color.Transparent
                        )
                    ) {
                        Text("⚙️", fontSize = 20.sp)
                    }

                    IconButton(onClick = { selectionMode = true }) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                    }
                    IconButton(onClick = { showDatePicker = !showDatePicker }) {
                        Icon(Icons.Default.DateRange, null, tint = if (startDate != null || endDate != null) Color(0xFF0A84FF) else Color.White)
                    }
                    IconButton(onClick = { /* Export */ }) {
                        Icon(Icons.Default.Share, null, tint = Color.White)
                    }
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
                    Text("Aucun trajet", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredTrips) { trip ->
                        TripCard(
                            trip = trip,
                            selectionMode = selectionMode,
                            isSelected = trip.id in selectedTrips,
                            onToggleSelection = {
                                selectedTrips = if (trip.id in selectedTrips) selectedTrips - trip.id else selectedTrips + trip.id
                            }
                        )
                    }
                }
            }
        }
    }
}