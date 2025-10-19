package com.ix7.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RideRegulatorBtn(
    cruiseControl: Boolean,
    onDisable: () -> Unit,
    onEnable: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            // DÉSACTIVER ❌
            Button(
                onClick = onDisable,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!cruiseControl) Color.Blue else Color.DarkGray
                ),
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("❌", fontSize = 16.sp)
            }

            // ACTIVER 🎯
            Button(
                onClick = onEnable,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (cruiseControl) Color.Blue else Color.DarkGray
                ),
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("🎯", fontSize = 16.sp)
            }
        }
    }
}