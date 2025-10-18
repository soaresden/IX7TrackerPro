package com.ix7.tracker.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ControlScreen(
    scooterName: String,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    var scooterLocked by remember { mutableStateOf(false) }
    var lockLocked by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(scrollState)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "🛴 $scooterName",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Status
        Text(
            text = "Connecté ✓",
            color = Color.Green,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Bouton Lock Trottinette
        Button(
            onClick = {
                scooterLocked = !scooterLocked
                actionMessage = if (scooterLocked) "🔒 Trottinette verrouillée" else "🔓 Trottinette déverrouillée"
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (scooterLocked) Color(0xFF2d5a2d) else Color(0xFF5a2d2d)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
        ) {
            Text(
                text = if (scooterLocked) "🔒 Déverrouiller" else "🔓 Verrouiller",
                fontSize = 11.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bouton Unlock Trottinette
        Button(
            onClick = {
                scooterLocked = !scooterLocked
                actionMessage = if (scooterLocked) "🔒 Trottinette verrouillée" else "🔓 Trottinette déverrouillée"
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (!scooterLocked) Color(0xFF2d5a2d) else Color(0xFF5a2d2d)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
        ) {
            Text(
                text = if (!scooterLocked) "✓ Déverrouillée" else "Verrouiller",
                fontSize = 11.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bouton Lock Cadenas
        Button(
            onClick = {
                lockLocked = !lockLocked
                actionMessage = if (lockLocked) "🔐 Cadenas verrouillé" else "🔓 Cadenas déverrouillé"
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (lockLocked) Color(0xFF2d5a2d) else Color(0xFF5a2d2d)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
        ) {
            Text(
                text = if (lockLocked) "🔐 Déverrouiller" else "🔓 Verrouiller",
                fontSize = 11.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bouton Unlock Cadenas
        Button(
            onClick = {
                lockLocked = !lockLocked
                actionMessage = if (lockLocked) "🔐 Cadenas verrouillé" else "🔓 Cadenas déverrouillé"
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (!lockLocked) Color(0xFF2d5a2d) else Color(0xFF5a2d2d)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
        ) {
            Text(
                text = if (!lockLocked) "✓ Déverrouillé" else "Verrouiller",
                fontSize = 11.sp,
                color = Color.White
            )
        }

        // Message d'action
        if (actionMessage.isNotEmpty()) {
            Text(
                text = actionMessage,
                color = Color.Yellow,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bouton Retour
        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Text("← Retour", fontSize = 11.sp)
        }
    }
}