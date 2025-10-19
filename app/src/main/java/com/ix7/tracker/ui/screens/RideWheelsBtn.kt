package com.ix7.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.WheelMode

@Composable
fun ModWheelsBtn(
    wheelMode: WheelMode,
    onOneWheelClick: () -> Unit,
    onTwoWheelsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text("Roues:", fontSize = 12.sp, color = Color.White)

        // 1 ROUE 🛴
        Button(
            onClick = onOneWheelClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (wheelMode == WheelMode.ONE_WHEEL)
                    Color.Blue else Color.DarkGray
            ),
            modifier = Modifier.size(45.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🛴", fontSize = 14.sp)
                Text("1", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 2 ROUES 🏍️
        Button(
            onClick = onTwoWheelsClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (wheelMode == WheelMode.TWO_WHEELS)
                    Color.Blue else Color.DarkGray
            ),
            modifier = Modifier.size(45.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏍️️", fontSize = 14.sp)
                Text("2", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}