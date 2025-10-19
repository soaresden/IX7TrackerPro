package com.ix7.tracker.ui.components

import androidx.compose.foundation.layout.*
import com.ix7.tracker.core.RideMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.SpeedLimits

@Composable
fun ModSwitchBtn(
    currentMode: RideMode,
    speedLimits: SpeedLimits,
    onPIETONClick: () -> Unit,
    onEcoClick: () -> Unit,
    onRaceClick: () -> Unit,
    onSportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // PIÉTON 🚶
        Button(
            onClick = onPIETONClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (currentMode == RideMode.PIETON)
                    Color(0xFF007AFF) else Color(0xFF3C3C3E)
            ),
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🚶", fontSize = 14.sp, color = Color.White)
                Text("${speedLimits.PIETON}", fontSize = 10.sp, color = Color.White)
            }
        }

        // ECO 🌱
        Button(
            onClick = onEcoClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (currentMode == RideMode.ECO)
                    Color(0xFF007AFF) else Color(0xFF3C3C3E)
            ),
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🌱", fontSize = 14.sp, color = Color.White)
                Text("${speedLimits.eco}", fontSize = 10.sp, color = Color.White)
            }
        }

        // RACE 🏎️
        Button(
            onClick = onRaceClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (currentMode == RideMode.RACE)
                    Color(0xFF007AFF) else Color(0xFF3C3C3E)
            ),
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏎️", fontSize = 14.sp, color = Color.White)
                Text("${speedLimits.race}", fontSize = 10.sp, color = Color.White)
            }
        }

        // SPORT ⚡
        Button(
            onClick = onSportClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (currentMode == RideMode.SPORT)
                    Color(0xFF007AFF) else Color(0xFF3C3C3E)
            ),
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚡", fontSize = 14.sp, color = Color.White)
                Text("${speedLimits.sport}", fontSize = 10.sp, color = Color.White)
            }
        }
    }
}
