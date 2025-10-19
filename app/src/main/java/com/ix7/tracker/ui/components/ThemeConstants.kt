package com.ix7.tracker.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ════════════════════════════════════════════════════════════════
// 🎨 THEME COLORS - Centralize all magic color values
// ════════════════════════════════════════════════════════════════

object ThemeColors {
    // ✅ Primary colors
    val PRIMARY_BLUE = Color(0xFF007AFF)
    val SECONDARY_BLUE = Color(0xFF5AC8FA)

    // ✅ Dark mode palette
    val DARK_BG = Color(0xFF1C1C1E)
    val DARK_CARD = Color(0xFF2C2C2E)
    val DARK_BUTTON = Color(0xFF3C3C3E)
    val DARK_SURFACE = Color(0xFF424245)

    // ✅ Status colors
    val SUCCESS_GREEN = Color(0xFF34C759)
    val WARNING_ORANGE = Color(0xFFFF9500)
    val ERROR_RED = Color(0xFFFF3B30)
    val INFO_BLUE = Color(0xFF007AFF)

    // ✅ Text colors
    val TEXT_PRIMARY = Color.White
    val TEXT_SECONDARY = Color(0xFF9CA3AF)
    val TEXT_TERTIARY = Color(0xFF6B7280)

    // ✅ Speed mode colors (per mode)
    val MODE_PIETON = Color(0xFF6366F1)      // Indigo
    val MODE_ECO = Color(0xFF34C759)         // Green
    val MODE_RACE = Color(0xFFFF9500)        // Orange
    val MODE_SPORT = Color( 0xFFFF3B30)       // Red

    // ✅ Battery status colors
    fun batteryColor(batteryPercent: Int): Color = when {
        batteryPercent > 50 -> Color.Green
        batteryPercent > 20 -> Color.Yellow
        else -> Color.Red
    }

    // ✅ Speed status colors
    fun speedColor(currentSpeed: Float, speedLimit: Float): Color = when {
        currentSpeed < speedLimit * 0.7f -> SUCCESS_GREEN
        currentSpeed < speedLimit * 0.9f -> WARNING_ORANGE
        currentSpeed >= speedLimit -> ERROR_RED
        else -> TEXT_PRIMARY
    }
}

// ════════════════════════════════════════════════════════════════
// 📏 THEME DIMENSIONS - Centralize padding, spacing, corner radius
// ════════════════════════════════════════════════════════════════

object ThemeDimensions {
    // ✅ Padding values (use consistently throughout app)
    val PADDING_EXTRA_SMALL = 2.dp
    val PADDING_SMALL = 4.dp
    val PADDING_SMALL_MEDIUM = 6.dp
    val PADDING_MEDIUM = 8.dp
    val PADDING_MEDIUM_LARGE = 10.dp
    val PADDING_LARGE = 12.dp
    val PADDING_XL = 16.dp
    val PADDING_XXL = 20.dp
    val PADDING_HUGE = 24.dp

    // ✅ Spacing for components
    val SPACER_TINY = 2.dp
    val SPACER_SMALL = 4.dp
    val SPACER_MEDIUM = 8.dp
    val SPACER_LARGE = 12.dp
    val SPACER_XL = 16.dp

    // ✅ Corner radius
    val CORNER_RADIUS_SMALL = 6.dp
    val CORNER_RADIUS_MEDIUM = 8.dp
    val CORNER_RADIUS_LARGE = 12.dp
    val CORNER_RADIUS_XL = 16.dp
    val CORNER_RADIUS_CIRCLE = 50.dp

    // ✅ Icon sizes
    val ICON_TINY = 12.dp
    val ICON_SMALL = 16.dp
    val ICON_MEDIUM = 20.dp
    val ICON_LARGE = 24.dp
    val ICON_XL = 32.dp

    // ✅ Button sizes
    val BUTTON_HEIGHT_SMALL = 32.dp
    val BUTTON_HEIGHT_MEDIUM = 40.dp
    val BUTTON_HEIGHT_LARGE = 48.dp

    // ✅ Card sizes
    val CARD_MIN_HEIGHT = 80.dp
    val CARD_MAX_HEIGHT = 200.dp
    val CARD_MIN_WIDTH = 100.dp

    // ✅ Elevation (shadow depth)
    val ELEVATION_SMALL = 2.dp
    val ELEVATION_MEDIUM = 4.dp
    val ELEVATION_LARGE = 8.dp
}

// ════════════════════════════════════════════════════════════════
// 📝 THEME TYPOGRAPHY SIZES (font sizes to maintain consistency)
// ════════════════════════════════════════════════════════════════

object ThemeTypography {
    val FONT_SIZE_CAPTION = 10.dp       // 10sp
    val FONT_SIZE_LABEL = 12.dp         // 12sp
    val FONT_SIZE_BODY = 14.dp          // 14sp
    val FONT_SIZE_SUBTITLE = 16.dp      // 16sp
    val FONT_SIZE_TITLE = 18.dp         // 18sp
    val FONT_SIZE_HEADING = 20.dp       // 20sp
    val FONT_SIZE_DISPLAY = 24.dp       // 24sp
}

// ════════════════════════════════════════════════════════════════
// ⏱️ ANIMATION DURATIONS
// ════════════════════════════════════════════════════════════════

object ThemeAnimations {
    const val DURATION_SHORT = 150      // ms - quick feedback
    const val DURATION_MEDIUM = 300     // ms - standard animation
    const val DURATION_LONG = 500       // ms - transitions
    const val DURATION_EXTRA_LONG = 800 // ms - complex animations
}

// ════════════════════════════════════════════════════════════════
// 🎯 USAGE EXAMPLES
// ════════════════════════════════════════════════════════════════

/*
// BEFORE (bad - magic values scattered everywhere):
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF007AFF)  // ← Magic value repeated 20+ times
    ),
    modifier = Modifier.padding(horizontal = 12.dp)  // ← Repeated pattern
) {
    Text("Click me", fontSize = 14.sp)  // ← Font size magic value
}

// AFTER (good - centralized, reusable):
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = ThemeColors.PRIMARY_BLUE
    ),
    modifier = Modifier.padding(horizontal = ThemeDimensions.PADDING_LARGE)
) {
    Text("Click me", fontSize = ThemeTypography.FONT_SIZE_BODY.value.sp)
}

// OR EVEN BETTER - use the helper components from RideButtonHelpers.kt:
ModeSelectorButton(
    emoji = "🏎️",
    speedLimit = "60 km/h",
    isSelected = currentMode == RideMode.RACE,
    onClick = { /* ... */ }
)
*/