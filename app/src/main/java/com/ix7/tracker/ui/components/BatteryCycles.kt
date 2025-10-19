package com.ix7.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Affiche les cycles de batterie détectés
 */
@Composable
fun BatteryCycles(cycles: List<BatteryCycleData>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (cycles.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Aucun cycle batterie détecté", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            items(cycles) { cycle ->
                BatteryCycleCard(cycle)
            }
        }
    }
}
