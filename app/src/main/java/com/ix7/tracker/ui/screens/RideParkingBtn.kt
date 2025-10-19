package com.ix7.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RideParkingBtn(
    isLocked: Boolean,
    onLock: () -> Unit,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            // LOCK 🔒
            Button(
                onClick = onLock,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLocked) Color.Red else Color.DarkGray
                ),
                modifier = Modifier.size(45.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("🔒", fontSize = 18.sp)
            }

            // UNLOCK 🔓
            Button(
                onClick = onUnlock,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isLocked) Color.Green else Color.DarkGray
                ),
                modifier = Modifier.size(45.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("🔓", fontSize = 18.sp)
            }
        }
    }
}