package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ix7.tracker.ui.components.IconActionButton

@Composable
fun RideBridageBtn(
    isUnlimited: Boolean,
    onLimitedClick: () -> Unit,
    onUnlimitedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconActionButton(
            icon = "🔒",
            label = "Limited",
            isActive = !isUnlimited,
            onClick = onLimitedClick,
            modifier = Modifier.weight(1f)
        )

        IconActionButton(
            icon = "🔓",
            label = "Unlimited",
            isActive = isUnlimited,
            onClick = onUnlimitedClick,
            modifier = Modifier.weight(1f)
        )
    }
}