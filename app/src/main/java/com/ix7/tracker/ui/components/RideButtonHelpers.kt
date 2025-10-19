package com.ix7.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ════════════════════════════════════════════════════════════════
// 🎨 REUSABLE BUTTON COMPONENTS
// ════════════════════════════════════════════════════════════════

/**
 * Generic mode selector button (replaces repetitive code in RideSwitchBtn)
 * ⚠️ Use inside a Row { } block and apply .weight(1f) from parent
 */
@Composable
fun RowScope.ModeSelectorButton(
    emoji: String,
    speedLimit: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected)
                ThemeColors.PRIMARY_BLUE else ThemeColors.DARK_BUTTON
        ),
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 2.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 14.sp, color = ThemeColors.TEXT_PRIMARY)
            Text(speedLimit, fontSize = 10.sp, color = ThemeColors.TEXT_PRIMARY)
        }
    }
}

/**
 * Generic icon action button
 */
@Composable
fun IconActionButton(
    icon: String,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive)
                ThemeColors.SUCCESS_GREEN else ThemeColors.DARK_BUTTON
        ),
        modifier = modifier,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 10.sp, color = ThemeColors.TEXT_PRIMARY, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Unified state card component (replaces StatusCard + StatusIndicator)
 */
@Composable
fun StateCard(
    title: String,
    icon: String,
    status: String,
    statusColor: Color,
    batteryLevel: Int = -1,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColors.DARK_CARD
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$icon $title",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColors.TEXT_PRIMARY
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Status indicator dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, shape = CircleShape)
                    )
                }

                // Optional battery indicator
                if (batteryLevel > 0) {
                    Text(
                        "🔋 $batteryLevel%",
                        fontSize = 12.sp,
                        color = when {
                            batteryLevel > 50 -> Color.Green
                            batteryLevel > 20 -> Color.Yellow
                            else -> Color.Red
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(status, fontSize = 12.sp, color = statusColor)

            if (content != null) {
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    }
}

/**
 * Horizontal scrollable button row
 */
@Composable
fun HorizontalButtonRow(
    buttons: List<Pair<String, () -> Unit>>,
    selectedIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        buttons.forEachIndexed { index, (label, onClick) ->
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (index == selectedIndex)
                        ThemeColors.PRIMARY_BLUE else ThemeColors.DARK_BUTTON
                ),
                modifier = Modifier.padding(4.dp)
            ) {
                Text(label, fontSize = 12.sp, color = ThemeColors.TEXT_PRIMARY)
            }
        }
    }
}

/**
 * Info badge (small info display)
 */
@Composable
fun InfoBadge(
    label: String,
    value: String,
    icon: String = "",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(ThemeColors.DARK_BUTTON, shape = RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon.isNotEmpty()) {
            Text(icon, fontSize = 12.sp)
        }
        Text(label, fontSize = 10.sp, color = ThemeColors.TEXT_SECONDARY)
        Text(value, fontSize = 10.sp, color = ThemeColors.TEXT_PRIMARY, fontWeight = FontWeight.Bold)
    }
}