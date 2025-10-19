package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.SpeedLimits
import com.ix7.tracker.ui.components.ModeSelectorButton

@Composable
fun RideSwitchBtn(
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
        ModeSelectorButton("🚶", "${speedLimits.PIETON} km/h", currentMode == RideMode.PIETON, onPIETONClick)
        ModeSelectorButton("🌱", "${speedLimits.ECO} km/h", currentMode == RideMode.ECO, onEcoClick)
        ModeSelectorButton("🏎️", "${speedLimits.RACE} km/h", currentMode == RideMode.RACE, onRaceClick)
        ModeSelectorButton("⚡", "${speedLimits.SPORT} km/h", currentMode == RideMode.SPORT, onSportClick)   }
}