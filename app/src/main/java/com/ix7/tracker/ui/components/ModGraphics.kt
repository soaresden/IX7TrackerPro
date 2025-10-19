package com.ix7.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.data.SpeedStats
import com.ix7.tracker.utils.TripUtils

/**
 * Affiche un histogramme des vitesses
 */
@Composable
fun SpeedHistogram(
    speedStats: SpeedStats,
    totalDuration: Long,
    height: Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    val maxValue = maxOf(
        speedStats.range0,
        speedStats.range0_10,
        speedStats.range10_20,
        speedStats.range20_30,
        speedStats.range30_40,
        speedStats.range40_50,
        speedStats.range50_60,
        speedStats.rangeAbove60
    ).coerceAtLeast(1)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        VerticalBar("0", speedStats.range0, totalDuration, maxValue)
        VerticalBar("0-10", speedStats.range0_10, totalDuration, maxValue)
        VerticalBar("10-20", speedStats.range10_20, totalDuration, maxValue)
        VerticalBar("20-30", speedStats.range20_30, totalDuration, maxValue)
        VerticalBar("30-40", speedStats.range30_40, totalDuration, maxValue)
        VerticalBar("40-50", speedStats.range40_50, totalDuration, maxValue)
        VerticalBar("50-60", speedStats.range50_60, totalDuration, maxValue)
        VerticalBar("60+", speedStats.rangeAbove60, totalDuration, maxValue)
    }
}

@Composable
private fun RowScope.VerticalBar(label: String, time: Long, totalTime: Long, maxValue: Long) {
    val percentage = if (totalTime > 0) (time.toFloat() / totalTime * 100).toInt() else 0

    val heightPercentage = if (maxValue > 0) {
        val ratio = time.toFloat() / maxValue
        ratio.coerceIn(0.05f, 1f)
    } else 0.05f

    val colorRatio = if (maxValue > 0) (time.toFloat() / maxValue).coerceIn(0f, 1f) else 0f
    val barColor = lerp(Color(0xFF4CAF50), Color(0xFFF44336), colorRatio)
    val textColor = lerp(Color(0xFF4CAF50), Color(0xFFF44336), colorRatio)

    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (time > 0) {
            Text("$percentage%", fontSize = 9.sp, color = textColor, fontWeight = FontWeight.Bold)
        } else {
            Spacer(modifier = Modifier.height(11.dp))
        }

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .width(32.dp)
                .weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (time > 0) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight(heightPercentage)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(barColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

        if (time > 0) {
            Text(TripUtils.formatDurationMMSS(time), fontSize = 8.sp, color = textColor, fontWeight = FontWeight.Bold)
        } else {
            Text("--", fontSize = 8.sp, color = Color.Gray)
        }
    }
}