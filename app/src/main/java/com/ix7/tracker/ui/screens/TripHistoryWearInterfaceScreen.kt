package com.ix7.tracker.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.List
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
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WearInterfaceScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { TripRepository(context) }
    val trips by repository.allTrips.collectAsState(initial = emptyList())

    var showSyncDialog by remember { mutableStateOf(false) }
    var showLeftPanel by remember { mutableStateOf(false) }
    var showRightPanel by remember { mutableStateOf(false) }

    // 🔄 Dialogue de confirmation Sync
    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("Synchroniser avec Wear OS ?") },
            text = { Text("Cela va synchroniser ${trips.size} trajets avec votre montre connectée.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            syncWithWear(context, trips)
                        }
                        showSyncDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF4CAF50))
                ) {
                    Text("Synchroniser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1C1C1E))) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ◀️ PANNEAU GAUCHE
            LeftPanel(
                trips = trips,
                visible = showLeftPanel,
                onExport = { exportToJson(context, trips) }
            )

            // 🎯 CENTRE - BOUTONS VERTICAUX
            Column(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF2C2C2E))
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // 🔼 Bouton HAUT avec flèche à gauche
                IconButton(
                    onClick = { showLeftPanel = !showLeftPanel },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF0A84FF), CircleShape)
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowLeft,
                        contentDescription = "Gauche",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 🔄 Bouton SYNC au MILIEU
                IconButton(
                    onClick = { showSyncDialog = true },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF4CAF50), CircleShape)
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "Sync",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 🔽 Bouton BAS avec flèche à droite
                IconButton(
                    onClick = { showRightPanel = !showRightPanel },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFFF9500), CircleShape)
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowRight,
                        contentDescription = "Droite",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // ▶️ PANNEAU DROIT
            RightPanel(
                trips = trips,
                visible = showRightPanel,
                onExport = { exportToJson(context, trips) }
            )
        }
    }
}

@Composable
fun LeftPanel(
    trips: List<Trip>,
    visible: Boolean,
    onExport: () -> Unit
) {
    if (!visible) return

    Column(
        modifier = Modifier
            .width(160.dp)
            .fillMaxHeight()
            .background(Color(0xFF2C2C2E))
            .border(1.dp, Color.Gray.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Titre
        Text(
            "📊 Trajets",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Stats globales
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatLineCompact("Total", trips.size.toString(), Color(0xFF0A84FF))
                StatLineCompact("Distance", "${"%.0f".format(trips.sumOf { it.distance.toDouble() })} km", Color(0xFF4CAF50))
                StatLineCompact("Batterie", "${"%.0f".format(trips.map { it.startBattery - it.endBattery }.average())} % moy", Color(0xFFFF9500))
            }
        }

        // Bouton Export
        Button(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A84FF)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Icon(Icons.Default.Share, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Exporter", fontSize = 10.sp)
        }

        // Liste rapide
        Text("Récents:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(trips.take(5)) { trip ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            "${"%.1f".format(trip.distance)} km",
                            fontSize = 9.sp,
                            color = Color(0xFF0A84FF),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${trip.startBattery - trip.endBattery}%",
                            fontSize = 8.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RightPanel(
    trips: List<Trip>,
    visible: Boolean,
    onExport: () -> Unit
) {
    if (!visible) return

    Column(
        modifier = Modifier
            .width(160.dp)
            .fillMaxHeight()
            .background(Color(0xFF2C2C2E))
            .border(1.dp, Color.Gray.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Titre
        Text(
            "🔋 Cycles",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Stats cycles
        val avgBattery = if (trips.isNotEmpty()) {
            trips.map { it.startBattery - it.endBattery }.average()
        } else {
            0.0
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StatLineCompact("Autonomie", "${"%.0f".format(avgBattery)} %", Color(0xFF4CAF50))
                StatLineCompact("Sessions", trips.size.toString(), Color(0xFFBB86FC))
                StatLineCompact("Temps total", "${trips.sumOf { it.duration } / (1000 * 3600)} h", Color(0xFF0A84FF))
            }
        }

        // Bouton Export
        Button(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Icon(Icons.Rounded.List, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Télécharger", fontSize = 10.sp)
        }

        // Stats avancées
        Text("Statistiques:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                StatLineCompact("Distance/trajet", "${"%.1f".format(if (trips.isNotEmpty()) trips.map { it.distance }.average() else 0.0)} km", Color(0xFFFF9500), size = 8)
                StatLineCompact("Vitesse max", "${"%.0f".format(if (trips.isNotEmpty()) trips.maxOf { it.maxSpeed } else 0.0)} km/h", Color(0xFFFF3B30), size = 8)
            }
        }
    }
}

@Composable
fun StatLineCompactLegacy(
    label: String,
    value: String,
    color: Color,
    size: Int = 9
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = size.sp, color = Color.Gray)
        Text(value, fontSize = size.sp, fontWeight = FontWeight.Bold, color = color)
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
            putExtra(Intent.EXTRA_SUBJECT, "IX7 Wear Sync - ${trips.size} trajets")
            type = "application/json"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Partager les données")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        android.util.Log.e("WearInterface", "Erreur export: ${e.message}")
    }
}

// 🔄 Sync avec Wear OS
private suspend fun syncWithWear(context: Context, trips: List<Trip>) {
    try {
        // Ici tu implémenteras la sync réelle avec Wear OS
        android.util.Log.d("WearSync", "Sync lancée avec ${trips.size} trajets")
    } catch (e: Exception) {
        android.util.Log.e("WearSync", "Erreur sync: ${e.message}")
    }
}