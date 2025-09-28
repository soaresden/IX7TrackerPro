// Fichier: ui/components/ride/RideGraph.kt
package com.ix7.tracker.ui.components.ride

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun RideGraph(isRiding: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isRiding) {
                Text(
                    text = "📈 Graphique temps réel\n(en cours de trajet)",
                    textAlign = TextAlign.Center,
                    color = Color.Blue
                )
            } else {
                Text(
                    text = "📈 Graphique temps réel\n(démarrez un trajet)",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        }
    }
}