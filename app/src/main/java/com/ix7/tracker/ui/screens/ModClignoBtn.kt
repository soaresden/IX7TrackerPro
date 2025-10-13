package com.ix7.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ModClignoBtn(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // GAUCHE ⬅️
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            enabled = false,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
        ) {
            Text("⬅️", fontSize = 20.sp)
        }

        // WARNING ⚠️
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            enabled = false,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
        ) {
            Text("⚠️", fontSize = 20.sp)
        }

        // DROITE ➡️
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            enabled = false,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
        ) {
            Text("➡️", fontSize = 20.sp)
        }
    }
}