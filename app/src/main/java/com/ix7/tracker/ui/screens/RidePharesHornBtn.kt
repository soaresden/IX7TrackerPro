package com.ix7.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            // PHARES 💡
            Button(
                onClick = onHeadlightsToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (headlightsOn) Color(0xFFFFEB3B) else Color.DarkGray
                ),
                modifier = Modifier.size(32.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(if (headlightsOn) "💡" else "⚫", fontSize = 14.sp)
            }

            // NÉON 🟣
            Button(
                onClick = onNeonToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (neonOn) Color(0xFF9C27B0) else Color.DarkGray
                ),
                modifier = Modifier.size(32.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(if (neonOn) "🟣" else "⚫", fontSize = 14.sp)
            }

            // KLAXON 🔊 (maintien)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5722))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onHornPress()
                                tryAwaitRelease()
                                onHornRelease()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("🔊", fontSize = 14.sp)
            }
        }
    }
}