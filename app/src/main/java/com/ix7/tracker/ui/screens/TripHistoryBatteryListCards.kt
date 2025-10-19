package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.ui.components.BatteryCycleData
import java.text.SimpleDateFormat
import java.util.*

// ════════════════════════════════════════════════════════════════
// 🔋 BATTERY LIST CARDS
// ════════════════════════════════════════════════════════════════

@Composable
fun TripHistoryBatteryListCards(
    batteryCycles: List<BatteryCycleData>,
    onItemClick: (BatteryCycleData) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = batteryCycles,
            key = { it.cycleNumber }
        ) { cycle ->
            BatteryCycleCard(
                cycle = cycle,
                onDetailClick = { onItemClick(cycle) }
            )
        }
    }
}

@Composable
private fun BatteryCycleCard(
    cycle: BatteryCycleData,
    onDetailClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailClick() }
            .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 📌 Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Cycle #${cycle.cycleNumber}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    dateFormat.format(cycle.startDate),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 📊 Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BatteryStatBadge(
                    label = "Début",
                    value = "${cycle.startBattery}%",
                    color = Color(0xFF0A84FF),
                    modifier = Modifier.weight(1f)
                )
                BatteryStatBadge(
                    label = "Fin",
                    value = "${cycle.endBattery}%",
                    color = Color(0xFFFF9500),
                    modifier = Modifier.weight(1f)
                )
                BatteryStatBadge(
                    label = "Usure",
                    value = "${cycle.cycleDifference}%",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                BatteryStatBadge(
                    label = "Trajets",
                    value = "${cycle.tripCount}",
                    color = Color(0xFFBB86FC),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BatteryStatBadge(
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