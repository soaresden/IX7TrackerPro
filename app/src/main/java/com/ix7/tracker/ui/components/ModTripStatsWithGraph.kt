package com.ix7.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ModTripStatsWithGraph - Affiche les statistiques de trajet
 */
@Composable
fun ModTripStatsWithGraph(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/**
 * StatsRow - Affiche une ligne de statistiques
 */
@Composable
fun StatsRow(
    stat1Label: String = "",
    stat1Value: String = "",
    stat1Color: Color = Color(0xFF0A84FF),
    stat2Label: String = "",
    stat2Value: String = "",
    stat2Color: Color = Color(0xFF4CAF50),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (stat1Label.isNotEmpty() && stat1Value.isNotEmpty()) {
            ModTripStatsWithGraph(
                label = stat1Label,
                value = stat1Value,
                color = stat1Color,
                modifier = Modifier.weight(1f)
            )
        }

        if (stat2Label.isNotEmpty() && stat2Value.isNotEmpty()) {
            ModTripStatsWithGraph(
                label = stat2Label,
                value = stat2Value,
                color = stat2Color,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * TripStatsDisplay - Affiche toutes les stats d'un trajet
 */
@Composable
fun TripStatsDisplay(
    distance: Float,
    duration: Long,
    avgSpeed: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModTripStatsWithGraph(
            label = "Distance",
            value = "${"%.1f".format(distance)} km",
            color = Color(0xFFFF9500),
            modifier = Modifier.fillMaxWidth()
        )

        ModTripStatsWithGraph(
            label = "Durée",
            value = "${duration / 60000} min",
            color = Color(0xFF0A84FF),
            modifier = Modifier.fillMaxWidth()
        )

        ModTripStatsWithGraph(
            label = "Vitesse moy",
            value = "${"%.1f".format(avgSpeed)} km/h",
            color = Color(0xFFBB86FC),
            modifier = Modifier.fillMaxWidth()
        )
    }
}