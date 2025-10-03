package com.ix7.tracker.ui.components

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
import com.ix7.tracker.ui.screens.RideMode

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
            .height(200.dp)  // AJOUTE CETTE LIGNE
            .background(Color.Red)  // CHANGE EN ROUGE VIF
            .border(2.dp, lcdBlue)
            .padding(16.dp)
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Vitesse au centre
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%02d", scooterData.speed.toInt()),
                        color = lcdBlue,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 8.sp
                    )
                    Text(
                        text = "KPH",
                        color = lcdBlue,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Mode roues à droite
                Text(
                    text = "1WD", // TODO: détecter 2WD
                    color = lcdBlue,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ligne du bas: ODO
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ODO",
                    color = lcdBlue,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("%05d", scooterData.odometer.toInt()),
                    color = lcdBlue,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "KM",
                    color = lcdBlue,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}