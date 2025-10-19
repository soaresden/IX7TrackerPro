package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.data.Trip

// ════════════════════════════════════════════════════════════════
// ✏️ TRIP DETAIL EDITION SCREEN
// ════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryTripListDetailScreenEdition(
    trip: Trip,
    tripNumber: Int,
    onBack: () -> Unit,
    onSave: (Trip) -> Unit
) {
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
    ) {
        // Header
        TopAppBar(
            title = { Text("Édition Trajet n°$tripNumber", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = {
                    // TODO: Sauvegarder les modifications
                    onSave(trip)
                }) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF0A84FF))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF2C2C2E)
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                EditSection("📝 Notes") {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("Ajouter des notes sur ce trajet...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0A84FF),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            }

            item {
                EditSection("⚙️ Paramètres") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EditableField("Mode", trip.settings.ridingMode.name)
                        EditableField("Drive Mode", trip.settings.driveMode.name)
                        EditableField("Limit Mode", trip.settings.speedLock.name)
                    }
                }
            }

            item {
                EditSection("📍 Localisation") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EditableField("Adresse de départ", trip.startLocation.address)
                        EditableField("Adresse d'arrivée", trip.endLocation.address)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun EditSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0A84FF),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun EditableField(
    label: String,
    defaultValue: String
) {
    var value by remember { mutableStateOf(defaultValue) }

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF0A84FF),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}