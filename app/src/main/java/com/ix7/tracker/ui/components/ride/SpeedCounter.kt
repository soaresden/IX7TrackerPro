// Fichier: ui/components/ride/SpeedCounter.kt
package com.ix7.tracker.ui.components.ride

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.ui.screens.SpeedUnit

@Composable
fun SpeedCounter(
    speed: Float,
    speedUnit: SpeedUnit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${speed.toInt()}",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = if (speed > 0) Color.Red else Color.Gray
            )
            Text(
                text = speedUnit.name.lowercase(),
                fontSize = 18.sp,
                color = Color.Gray
            )
        }
    }
}