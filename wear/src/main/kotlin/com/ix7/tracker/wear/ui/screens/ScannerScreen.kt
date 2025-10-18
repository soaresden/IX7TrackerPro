package com.ix7.tracker.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ScannerScreen(
    onScooterSelected: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val isScanning = remember { mutableStateOf(true) }
    val scootersList = remember { mutableStateOf<List<String>>(emptyList()) }

    // Simulation du scan (en vrai, ce serait une vraie recherche BLE)
    LaunchedEffect(Unit) {
        delay(2000)
        scootersList.value = listOf("M0Robot", "IX7-2024")
        isScanning.value = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isScanning.value) "🔍 Scan..." else "Trottinettes trouvées",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isScanning.value) {
            Text(
                text = "Recherche en cours...",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 20.dp)
            )
        } else {
            scootersList.value.forEach { scooter ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.DarkGray)
                        .clickable { onScooterSelected(scooter) }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🛴 $scooter",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBackClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Text("← Retour", fontSize = 12.sp)
        }
    }
}