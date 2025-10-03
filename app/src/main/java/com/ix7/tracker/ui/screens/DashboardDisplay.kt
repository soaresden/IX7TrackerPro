package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.ScooterData

@Composable
fun DashboardDisplay(
    scooterData: ScooterData,
    modifier: Modifier = Modifier
) {
    val lcdBlue = Color(0xFF00D4FF)
    val lcdBg = Color(0xFF001A33)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)  // Réduit de 200 à 140
            .background(lcdBg)
            .border(2.dp, lcdBlue)
            .padding(12.dp)  // Réduit aussi le padding
    ) {
        Column {
            // Ligne du haut: Mode + Vitesse + Drive mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode à gauche
                Text(
                    text = when(scooterData.currentMode) {
                        RideMode.ECO -> "ECO"
                        RideMode.RACE -> "RACE"
                        RideMode.PEDESTRIAN -> "PEDES"
                        RideMode.POWER -> "POWER"
                    },
                    color = lcdBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Vitesse au centre
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%02d", scooterData.speed.toInt()),
                        color = lcdBlue,
                        fontSize = 40.sp,  // Réduit de 48 à 40
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 6.sp
                    )
                    Text(
                        text = "KPH",
                        color = lcdBlue,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Mode roues à droite
                Text(
                    text = "1WD",
                    color = lcdBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Ligne du bas: ODO + Batterie
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ODO à gauche
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ODO",
                        color = lcdBlue,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format("%05d", scooterData.odometer.toInt()),
                        color = lcdBlue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KM",
                        color = lcdBlue,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Batterie à droite
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format("%d%%", scooterData.battery.toInt()),
                        color = lcdBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}