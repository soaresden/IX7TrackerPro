package com.ix7.tracker.ui.screens

import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.SpeedUnit
import com.ix7.tracker.core.WheelMode

/**
 * 📊 État GROUPÉ du RideScreen
 *
 * Centralise tous les états pour:
 * - Meilleure lisibilité
 * - Faciliter la sérialisation
 * - Passer à ViewModel plus tard
 */
data class RideScreenState(
    // ===== CONFIGURATION =====
    val wheelMode: WheelMode = WheelMode.TWO_WHEELS,
    val speedUnit: SpeedUnit = SpeedUnit.KMH,

    // ===== ÉTAT DE CONDUITE =====
    val isRiding: Boolean = false,
    val isPaused: Boolean = false,

    // ===== RÉGULATEUR =====
    val cruiseControl: Boolean = false,
    val cruiseThreshold: Float = 25f,

    // ===== ACCESSOIRES =====
    val headlightsOn: Boolean = false,
    val neonOn: Boolean = false,
    val isLocked: Boolean = true,
)

/**
 * 🎬 Actions disponibles sur le state
 */
sealed class RideScreenAction {
    // Configuration
    data class SetWheelMode(val mode: WheelMode) : RideScreenAction()
    data class SetSpeedUnit(val unit: SpeedUnit) : RideScreenAction()

    // Conduite
    object StartRide : RideScreenAction()
    object PauseRide : RideScreenAction()
    object ResumeRide : RideScreenAction()
    object StopRide : RideScreenAction()

    // Régulateur
    object EnableCruise : RideScreenAction()
    object DisableCruise : RideScreenAction()
    data class SetCruiseThreshold(val speedKmh: Float) : RideScreenAction()

    // Accessoires
    object ToggleHeadlights : RideScreenAction()
    object ToggleNeon : RideScreenAction()
    object ToggleLock : RideScreenAction()
}

/**
 * 🔄 Reducer pour transformer l'état
 */
fun rideScreenReducer(state: RideScreenState, action: RideScreenAction): RideScreenState {
    return when (action) {
        // Configuration
        is RideScreenAction.SetWheelMode -> state.copy(wheelMode = action.mode)
        is RideScreenAction.SetSpeedUnit -> state.copy(speedUnit = action.unit)

        // Conduite
        RideScreenAction.StartRide -> state.copy(isRiding = true, isPaused = false)
        RideScreenAction.PauseRide -> state.copy(isPaused = !state.isPaused)
        RideScreenAction.ResumeRide -> state.copy(isPaused = false)
        RideScreenAction.StopRide -> state.copy(isRiding = false, isPaused = false)

        // Régulateur
        RideScreenAction.EnableCruise -> state.copy(cruiseControl = true)
        RideScreenAction.DisableCruise -> state.copy(cruiseControl = false)
        is RideScreenAction.SetCruiseThreshold -> state.copy(cruiseThreshold = action.speedKmh)

        // Accessoires
        RideScreenAction.ToggleHeadlights -> state.copy(headlightsOn = !state.headlightsOn)
        RideScreenAction.ToggleNeon -> state.copy(neonOn = !state.neonOn)
        RideScreenAction.ToggleLock -> state.copy(isLocked = !state.isLocked)
    }
}