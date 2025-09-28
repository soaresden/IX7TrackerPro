// Fichier: ui/components/ride/TurnSignalsRow.kt
package com.ix7.tracker.ui.components.ride

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TurnSignalsRow(
    leftTurn: Boolean,
    rightTurn: Boolean,
    warningLights: Boolean,
    onLeftTurnToggle: () -> Unit,
    onRightTurnToggle: () -> Unit,
    onWarningToggle: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onLeftTurnToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (leftTurn) Color(0xFFFF8C00) else Color.Gray
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Text("⬅️", fontSize = 20.sp)
            }

            Button(
                onClick = onWarningToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (warningLights) Color.Red else Color.Gray
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Text("⚠️", fontSize = 20.sp)
            }

            Button(
                onClick = onRightTurnToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (rightTurn) Color(0xFFFF8C00) else Color.Gray
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Text("➡️", fontSize = 20.sp)
            }
        }
    }
}