// Fichier: ui/components/ride/RideControlButtons.kt
package com.ix7.tracker.ui.components.ride

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RideControlButtons(
    isRiding: Boolean,
    isPaused: Boolean,
    onStartRide: () -> Unit,
    onPauseRide: () -> Unit,
    onStopRide: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onStartRide,
                enabled = !isRiding,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                modifier = Modifier.size(48.dp)
            ) {
                Text("▶️", fontSize = 20.sp)
            }

            Button(
                onClick = onPauseRide,
                enabled = isRiding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) Color.Blue else Color(0xFFFF9800)
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Text(if (isPaused) "▶️" else "⏸️", fontSize = 20.sp)
            }

            Button(
                onClick = onStopRide,
                enabled = isRiding,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.size(48.dp)
            ) {
                Text("⏹️", fontSize = 20.sp)
            }
        }
    }
}
