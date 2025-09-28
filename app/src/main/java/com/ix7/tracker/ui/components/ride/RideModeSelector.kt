// Fichier: ui/components/ride/RideModeSelector.kt
package com.ix7.tracker.ui.components.ride

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.ui.screens.RideMode

@Composable
fun RideModeSelector(
    currentMode: RideMode,
    onModeChange: (RideMode) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Mode de conduite",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RideMode.values().forEach { mode ->
                    Button(
                        onClick = { onModeChange(mode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentMode == mode) Color.Yellow else Color.Gray
                        ),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = mode.name.take(3),
                            fontSize = 12.sp,
                            fontWeight = if (currentMode == mode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
