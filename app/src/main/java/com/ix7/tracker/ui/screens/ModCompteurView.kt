package com.ix7.tracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.SpeedUnit

@Composable
fun ModCompteurView(
    speed: Float,
    maxSpeed: Float,
    speedUnit: SpeedUnit,
    onUnitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Vitesse actuelle
            Text(
                text = "${speed.toInt()}",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Unité (cliquable) + Max
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = speedUnit.name.lowercase(),
                    fontSize = 14.sp,
                    color = Color.Cyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onUnitClick() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${maxSpeed.toInt()}",
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}