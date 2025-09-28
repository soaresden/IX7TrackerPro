// RideScreen.kt - Redesigné et divisé
package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.ScooterData
import com.ix7.tracker.ui.components.ride.*

enum class RideMode {
    PEDESTRIAN, ECO, RACE, POWER
}

enum class WheelMode {
    ONE_WHEEL, TWO_WHEELS
}

enum class SpeedUnit {
    KMH, MPH
}

@Composable
fun RideScreen(
    scooterData: ScooterData,
    isConnected: Boolean
) {
    var wheelMode by remember { mutableStateOf(WheelMode.ONE_WHEEL) }
    var speedUnit by remember { mutableStateOf(SpeedUnit.KMH) }
    var rideMode by remember { mutableStateOf(RideMode.ECO) }
    var leftTurn by remember { mutableStateOf(false) }
    var rightTurn by remember { mutableStateOf(false) }
    var warningLights by remember { mutableStateOf(false) }
    var headlights by remember { mutableStateOf(false) }
    var neonLights by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var isRiding by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header avec contrôles principaux
        HeaderControls(
            wheelMode = wheelMode,
            speedUnit = speedUnit,
            isLocked = isLocked,
            onWheelModeChange = { wheelMode = if (wheelMode == WheelMode.ONE_WHEEL) WheelMode.TWO_WHEELS else WheelMode.ONE_WHEEL },
            onSpeedUnitChange = { speedUnit = if (speedUnit == SpeedUnit.KMH) SpeedUnit.MPH else SpeedUnit.KMH },
            onLockToggle = { isLocked = !isLocked }
        )

        // Compteur de vitesse
        SpeedCounter(
            speed = if (isConnected) scooterData.speed else 0f,
            speedUnit = speedUnit
        )

        // Mode de conduite
        RideModeSelector(
            currentMode = rideMode,
            onModeChange = { rideMode = it }
        )

        // Graphique temps réel
        RideGraph(isRiding = isRiding)

        // Clignotants et warning (sur une ligne)
        TurnSignalsRow(
            leftTurn = leftTurn,
            rightTurn = rightTurn,
            warningLights = warningLights,
            onLeftTurnToggle = {
                leftTurn = !leftTurn
                if (leftTurn) { rightTurn = false; warningLights = false }
            },
            onRightTurnToggle = {
                rightTurn = !rightTurn
                if (rightTurn) { leftTurn = false; warningLights = false }
            },
            onWarningToggle = {
                warningLights = !warningLights
                if (warningLights) { leftTurn = false; rightTurn = false }
            }
        )

        // Boutons de contrôle de trajet
        RideControlButtons(
            isRiding = isRiding,
            isPaused = isPaused,
            onStartRide = { isRiding = true; isPaused = false },
            onPauseRide = { isPaused = !isPaused },
            onStopRide = { isRiding = false; isPaused = false }
        )

        // Tableau de données
        RideDataTable(
            scooterData = scooterData,
            isConnected = isConnected,
            isRiding = isRiding
        )

        // Barre d'actions
        ActionsBar(
            headlights = headlights,
            neonLights = neonLights,
            onHeadlightsToggle = { headlights = !headlights },
            onNeonToggle = { neonLights = !neonLights },
            onHornPress = { /* TODO: Klaxon */ }
        )
    }
}