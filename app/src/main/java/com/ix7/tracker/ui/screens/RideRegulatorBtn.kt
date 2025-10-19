package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ix7.tracker.ui.components.IconActionButton

@Composable
fun RideRegulatorBtn(
    cruiseControl: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconActionButton(
        icon = "⚙️",
        label = "Cruise",
        isActive = cruiseControl,
        onClick = if (cruiseControl) onDisable else onEnable,
        modifier = modifier
    )
}