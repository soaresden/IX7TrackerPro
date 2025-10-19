package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
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
// ✅ Import pour l'extension border
import androidx.compose.material3.Divider
import androidx.compose.foundation.border

/**
 * ✅ Composable réutilisable pour afficher une ligne de stat (label + valeur)
 */
@Composable
fun StatRowComponent(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/**
 * ✅ Composable pour afficher une plage de vitesse avec la durée
 */
@Composable
fun SpeedRangeRowComponent(label: String, seconds: Long) {
    val minutes = seconds / 60
    val secs = seconds % 60

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(4.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Text(
            "${"$minutes".padStart(2, '0')}m ${"$secs".padStart(2, '0')}s",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0A84FF)
        )
    }
}

/**
 * ✅ Badge pour afficher une statistique avec fond coloré
 */
@Composable
fun StatBadgeComponent(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(6.dp)
    ) {
        Text(label, fontSize = 9.sp, color = Color.Gray)
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/**
 * ✅ Ligne de stat compacte pour les panneaux latéraux
 */
@Composable
fun StatLineCompact(
    label: String,
    value: String,
    color: Color,
    size: Int = 9
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = size.sp, color = Color.Gray)
        Text(value, fontSize = size.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

