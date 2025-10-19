package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ix7.tracker.ui.components.IconActionButton

@Composable
fun RidePharesHornBtn(
    headlightsOn: Boolean,
    neonOn: Boolean,
    onHeadlightsToggle: () -> Unit,
    onNeonToggle: () -> Unit,
    onHornPress: () -> Unit,
    onHornRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconActionButton(
            icon = "💡",
            label = "Lights",
            isActive = headlightsOn,
            onClick = onHeadlightsToggle,
            modifier = Modifier.weight(1f)
        )

        IconActionButton(
            icon = "✨",
            label = "Neon",
            isActive = neonOn,
            onClick = onNeonToggle,
            modifier = Modifier.weight(1f)
        )

        IconActionButton(
            icon = "📢",
            label = "Horn",
            isActive = false,
            onClick = {
                onHornPress()
                // In a real app, you'd use onPress/onRelease properly
                onHornRelease()
            },
            modifier = Modifier.weight(1f)
        )
    }
}