package com.ix7.tracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ModRegulatorSlider(
    visible: Boolean,
    cruiseThreshold: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2C2C2E).copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Titre et valeur actuelle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🎯 Seuil régulateur",
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${cruiseThreshold.toInt()} km/h",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Cyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Slider avec marqueurs
                Column {
                    Slider(
                        value = cruiseThreshold,
                        onValueChange = onValueChange,
                        onValueChangeFinished = onValueChangeFinished,
                        valueRange = 10f..60f,
                        steps = 49, // Pour avoir des valeurs entières
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Cyan,
                            activeTrackColor = Color.Cyan,
                            inactiveTrackColor = Color(0xFF505050)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Marqueurs de référence
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("10", fontSize = 10.sp, color = Color.Gray)
                        Text("25", fontSize = 10.sp, color = Color.Gray)
                        Text("40", fontSize = 10.sp, color = Color.Gray)
                        Text("60", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}