package com.ix7.tracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun ModGraphView(
    isRiding: Boolean,
    isPaused: Boolean,
    currentSpeed: Float,
    currentBattery: Float,
    maxSpeed: Float,
    onStart: () -> Unit,
    onPauseToggle: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    var speedHistory by remember { mutableStateOf(listOf<Float>()) }
    var batteryHistory by remember { mutableStateOf(listOf<Float>()) }

    LaunchedEffect(isRiding, currentSpeed, currentBattery) {
        if (isRiding && !isPaused) {
            speedHistory = speedHistory.takeLast(49) + currentSpeed
            batteryHistory = batteryHistory.takeLast(49) + currentBattery
        }
        if (!isRiding) {
            speedHistory = emptyList()
            batteryHistory = emptyList()
        }
    }

    Column(modifier = modifier) {
        // Graphique
        Card(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                if (speedHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("▶ pour démarrer", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val pointSpacing = width / speedHistory.size.coerceAtLeast(1)

                        // Courbe vitesse (bleu)
                        val speedPath = Path()
                        speedHistory.forEachIndexed { index, speed ->
                            val x = index * pointSpacing
                            val y = height - (speed / maxSpeed * height).coerceIn(0f, height)
                            if (index == 0) speedPath.moveTo(x, y) else speedPath.lineTo(x, y)
                        }
                        drawPath(path = speedPath, color = Color(0xFF2196F3), style = Stroke(width = 3f))

                        // Courbe batterie (vert)
                        val batteryPath = Path()
                        batteryHistory.forEachIndexed { index, battery ->
                            val x = index * pointSpacing
                            val y = height - (battery / 100f * height)
                            if (index == 0) batteryPath.moveTo(x, y) else batteryPath.lineTo(x, y)
                        }
                        drawPath(path = batteryPath, color = Color(0xFF4CAF50), style = Stroke(width = 2f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Boutons de contrôle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // PLAY ▶
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                enabled = !isRiding || isPaused,
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
            ) {
                Text("▶", fontSize = 20.sp)
            }

            // PAUSE ⏸
            Button(
                onClick = onPauseToggle,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                enabled = isRiding,
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
            ) {
                Text(if (isPaused) "▶" else "⏸", fontSize = 20.sp)
            }

            // STOP ⏹
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                enabled = isRiding,
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
            ) {
                Text("⏹", fontSize = 20.sp)
            }
        }
    }
}