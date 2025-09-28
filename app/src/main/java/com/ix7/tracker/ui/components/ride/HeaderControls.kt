// ui/components/ride/HeaderControls.kt
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
import com.ix7.tracker.ui.screens.WheelMode

@Composable
fun HeaderControls(
    wheelMode: WheelMode,
    speedUnit: SpeedUnit,
    isLocked: Boolean,
    onWheelModeChange: () -> Unit,
    onSpeedUnitChange: () -> Unit,
    onLockToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onWheelModeChange,
            modifier = Modifier.size(60.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(if (wheelMode == WheelMode.ONE_WHEEL) "🛴" else "🏍️", fontSize = 24.sp)
        }

        Button(
            onClick = onSpeedUnitChange,
            modifier = Modifier.height(40.dp)
        ) {
            Text(speedUnit.name, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onLockToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLocked) Color.Red else Color.Green
            ),
            modifier = Modifier.size(80.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = if (isLocked) "🔒" else "🔓",
                fontSize = 28.sp
            )
        }
    }
}