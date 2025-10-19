package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ix7.tracker.core.WheelMode
import com.ix7.tracker.ui.components.IconActionButton

@Composable
fun RideWheelsBtn(
    wheelMode: WheelMode,
    onOneWheelClick: () -> Unit,
    onTwoWheelsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconActionButton(
            icon = "🛞",
            label = "",
            isActive = wheelMode == WheelMode.ONE_WHEEL,
            onClick = onOneWheelClick,
            modifier = Modifier.weight(1f)
        )

        IconActionButton(
            icon = "🛞🛞",
            label = "",
            isActive = wheelMode == WheelMode.TWO_WHEELS,
            onClick = onTwoWheelsClick,
            modifier = Modifier.weight(1f)
        )
    }
}