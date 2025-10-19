package com.ix7.tracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

/**
 * Affiche les filtres de date pour les trajets
 */
@Composable
fun ModTripFilters(
    visible: Boolean,
    startDate: Date?,
    endDate: Date?,
    onStartDateChange: (Date?) -> Unit,
    onEndDateChange: (Date?) -> Unit
) {
    AnimatedVisibility(visible = visible) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = false,
                onClick = {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    onStartDateChange(cal.time)
                    onEndDateChange(Date())
                },
                label = { Text("Aujourd'hui", fontSize = 11.sp) }
            )

            FilterChip(
                selected = false,
                onClick = {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    onStartDateChange(cal.time)
                    onEndDateChange(Date())
                },
                label = { Text("7j", fontSize = 11.sp) }
            )

            FilterChip(
                selected = false,
                onClick = {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.MONTH, -1)
                    onStartDateChange(cal.time)
                    onEndDateChange(Date())
                },
                label = { Text("30j", fontSize = 11.sp) }
            )

            FilterChip(
                selected = startDate == null && endDate == null,
                onClick = {
                    onStartDateChange(null)
                    onEndDateChange(null)
                },
                label = { Text("Tout", fontSize = 11.sp) }
            )
        }
    }
}