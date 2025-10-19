package com.ix7.tracker.ui.components

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

data class HistogrammeData(
    val label: String,
    val value: Long,
    val percentage: Float,
    val color: Color
)

/**
 * 📊 Histogramme VERTICAL correct - Barres verticales comme une vraie distribution
 */
@Composable
fun HistogrammeComponent(
    data: List<HistogrammeData>,
    maxValue: Long = data.maxOfOrNull { it.value } ?: 0L,
    modifier: Modifier = Modifier,
    displayFormat: (Long) -> String = { "${it / 60}m" }
) {
    if (data.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // GRAPHIQUE - Zone principale
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // AXE Y - Étiquettes (haut = max, bas = 0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    displayFormat(maxValue),
                    fontSize = 8.sp,
                    color = Color.Gray,
                    modifier = Modifier.width(30.dp)
                )
                Text("", fontSize = 8.sp)
            }

            // ZONE DES BARRES
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF1C1C1E), RoundedCornerShape(4.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { item ->
                    VerticalBar(
                        data = item,
                        maxValue = maxValue,
                        displayFormat = displayFormat,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp)
                    )
                }
            }

            // AXE Y - 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("0", fontSize = 8.sp, color = Color.Gray, modifier = Modifier.width(30.dp))
                Text("", fontSize = 8.sp)
            }
        }

        // LABELS EN X (noms des catégories)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            data.forEach { item ->
                Text(
                    item.label,
                    fontSize = 9.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    maxLines = 2
                )
            }
        }

        // LÉGENDE (valeurs et %)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2C2C2E), RoundedCornerShape(6.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            data.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(item.color, RoundedCornerShape(2.dp))
                        )
                        Text(
                            item.label,
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            displayFormat(item.value),
                            fontSize = 9.sp,
                            color = item.color,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${item.percentage.toInt()}%",
                            fontSize = 9.sp,
                            color = Color.Gray,
                            modifier = Modifier.width(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalBar(
    data: HistogrammeData,
    maxValue: Long,
    displayFormat: (Long) -> String,
    modifier: Modifier = Modifier
) {
    val heightRatio = if (maxValue > 0) data.value.toFloat() / maxValue else 0f

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Valeur au-dessus de la barre
        Text(
            displayFormat(data.value),
            fontSize = 8.sp,
            color = data.color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Barre verticale
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(heightRatio)
                .background(data.color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
        )

        // Espace pour l'alignement
        if (heightRatio < 0.5f) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(1f - heightRatio)
            )
        }
    }
}

/**
 * Helper pour créer les données d'histogramme
 */
fun createHistogrammeData(
    data: Map<String, Pair<Long, Float>>,
    colors: Map<String, Color>
): List<HistogrammeData> {
    return data.map { (label, valuePercent) ->
        HistogrammeData(
            label = label,
            value = valuePercent.first,
            percentage = valuePercent.second,
            color = colors[label] ?: Color.Gray
        )
    }
}