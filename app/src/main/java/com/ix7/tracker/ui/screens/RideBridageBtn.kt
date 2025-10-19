package com.ix7.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ModBridageBtn(
    isUnlimited: Boolean,
    onLimitedClick: () -> Unit,
    onUnlimitedClick: () -> Unit,
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
            // BRIDÉ 🚧
            Button(
                onClick = onLimitedClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isUnlimited) Color.Blue else Color.DarkGray
                ),
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("🚧", fontSize = 16.sp)
            }

            // DÉBRIDÉ ⚡
            Button(
                onClick = onUnlimitedClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isUnlimited) Color.Blue else Color.DarkGray
                ),
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("⚡", fontSize = 16.sp)
            }
        }
    }
}