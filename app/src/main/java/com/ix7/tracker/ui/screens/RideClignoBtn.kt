package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.ix7.tracker.ui.components.IconActionButton

@Composable
fun RideClignoBtn(
    modifier: Modifier = Modifier
) {
    var leftSignalOn by remember { mutableStateOf(false) }
    var rightSignalOn by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconActionButton(
            icon = "⬅️",
            label = "Left",
            isActive = leftSignalOn,
            onClick = { leftSignalOn = !leftSignalOn },
            modifier = Modifier.weight(1f)
        )

        IconActionButton(
            icon = "➡️",
            label = "Right",
            isActive = rightSignalOn,
            onClick = { rightSignalOn = !rightSignalOn },
            modifier = Modifier.weight(1f)
        )
    }
}