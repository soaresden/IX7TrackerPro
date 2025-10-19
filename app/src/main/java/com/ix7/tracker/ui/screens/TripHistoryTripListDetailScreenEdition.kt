package com.ix7.tracker.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Écran complet d'édition - Redesigné
 * - Odomètre départ / arrivée
 * - Date et heure départ / arrivée avec calendrier
 * - Gauche: départ, Droite: arrivée
 * - Batterie seulement
 */
@Composable
fun TripEditionScreen(
    trip: Trip,
    onSave: (Trip) -> Unit = {},
    onDelete: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    // États pour départ
    var startOdometer by remember { mutableStateOf(trip.startOdometer.toString()) }
    var startDate by remember { mutableStateOf(trip.startDate) }
    var startBattery by remember { mutableStateOf(trip.startBattery.toString()) }

    // États pour arrivée
    var endOdometer by remember { mutableStateOf(trip.endOdometer.toString()) }
    var endDate by remember { mutableStateOf(trip.endDate) }
    var endBattery by remember { mutableStateOf(trip.endBattery.toString()) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    // Dialog suppression
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer ce trajet ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(trip.id)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F11))
    ) {
        // ===== HEADER =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1E))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
            Text("Éditer le trajet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, null, tint = Color.Red)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ===== GAUCHE: DÉPART | DROITE: ARRIVÉE =====
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ===== GAUCHE: DÉPART =====
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF1C1C1E), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text("📍 DÉPART", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A84FF))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Date/Heure départ
                        Text("Date & Heure", fontSize = 12.sp, color = Color.Gray)
                        Button(
                            onClick = {
                                val calendar = Calendar.getInstance().apply { time = startDate }
                                DatePickerDialog(context, { _, year, month, day ->
                                    calendar.set(year, month, day)
                                    TimePickerDialog(context, { _, hour, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                                        calendar.set(Calendar.MINUTE, minute)
                                        calendar.set(Calendar.SECOND, 0)
                                        startDate = calendar.time
                                    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))
                        ) {
                            Text(dateFormat.format(startDate), fontSize = 11.sp, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Odomètre départ
                        Text("Odomètre (km)", fontSize = 12.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = startOdometer,
                            onValueChange = { startOdometer = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFF2C2C2E),
                                focusedContainerColor = Color(0xFF2C2C2E),
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            ),
                            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(fontSize = 12.sp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Batterie départ
                        Text("Batterie (%)", fontSize = 12.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = startBattery,
                            onValueChange = { startBattery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFF2C2C2E),
                                focusedContainerColor = Color(0xFF2C2C2E),
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            ),
                            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(fontSize = 12.sp)
                        )
                    }

                    // ===== DROITE: ARRIVÉE =====
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF1C1C1E), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text("🏁 ARRIVÉE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9500))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Date/Heure arrivée
                        Text("Date & Heure", fontSize = 12.sp, color = Color.Gray)
                        Button(
                            onClick = {
                                val calendar = Calendar.getInstance().apply { time = endDate }
                                DatePickerDialog(context, { _, year, month, day ->
                                    calendar.set(year, month, day)
                                    TimePickerDialog(context, { _, hour, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                                        calendar.set(Calendar.MINUTE, minute)
                                        calendar.set(Calendar.SECOND, 0)
                                        endDate = calendar.time
                                    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))
                        ) {
                            Text(dateFormat.format(endDate), fontSize = 11.sp, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Odomètre arrivée
                        Text("Odomètre (km)", fontSize = 12.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = endOdometer,
                            onValueChange = { endOdometer = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFF2C2C2E),
                                focusedContainerColor = Color(0xFF2C2C2E),
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            ),
                            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(fontSize = 12.sp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Batterie arrivée
                        Text("Batterie (%)", fontSize = 12.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = endBattery,
                            onValueChange = { endBattery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFF2C2C2E),
                                focusedContainerColor = Color(0xFF2C2C2E),
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            ),
                            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(fontSize = 12.sp)
                        )
                    }
                }
            }

            // ===== CALCULS AUTOMATIQUES =====
            item {
                val distance = (endOdometer.toFloatOrNull() ?: trip.endOdometer) - (startOdometer.toFloatOrNull() ?: trip.startOdometer)
                val durationMs = endDate.time - startDate.time
                val durationMin = durationMs / (1000 * 60)
                val durationHour = durationMin / 60
                val batteryUsed = (startBattery.toIntOrNull() ?: trip.startBattery) - (endBattery.toIntOrNull() ?: trip.endBattery)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("📊 Résumé du trajet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Distance: ${"%.1f".format(distance)} km", fontSize = 12.sp, color = Color.White)
                        Text("Durée: $durationHour h ${"${durationMin % 60}".padStart(2, '0')} min", fontSize = 12.sp, color = Color.White)
                        Text("Batterie utilisée: $batteryUsed%", fontSize = 12.sp, color = Color.White)
                    }
                }
            }

            // ===== BOUTONS =====
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Annuler", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            try {
                                val updatedTrip = trip.copy(
                                    startDate = startDate,
                                    endDate = endDate,
                                    startOdometer = startOdometer.toFloatOrNull() ?: trip.startOdometer,
                                    endOdometer = endOdometer.toFloatOrNull() ?: trip.endOdometer,
                                    startBattery = startBattery.toIntOrNull() ?: trip.startBattery,
                                    endBattery = endBattery.toIntOrNull() ?: trip.endBattery,
                                    // Recalculer distance et duration
                                    distance = (endOdometer.toFloatOrNull() ?: trip.endOdometer) - (startOdometer.toFloatOrNull() ?: trip.startOdometer),
                                    duration = endDate.time - startDate.time
                                )
                                onSave(updatedTrip)
                            } catch (e: Exception) {
                                android.util.Log.e("TripEditionScreen", "Erreur: ${e.message}")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
                    ) {
                        Text("Enregistrer", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}