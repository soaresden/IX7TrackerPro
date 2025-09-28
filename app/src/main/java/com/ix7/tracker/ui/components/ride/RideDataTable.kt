
// Fichier: ui/components/ride/RideDataTable.kt
package com.ix7.tracker.ui.components.ride

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.ScooterData

@Composable
fun RideDataTable(
    scooterData: ScooterData,
    isConnected: Boolean,
    isRiding: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "📊 Données de trajet",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("🚀 Départ", "📍 Actuel", "🏁 Arrivée").forEach { header ->
                    Text(
                        text = header,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Batterie
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("🔋 --", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("🔋 ${if (isConnected) "${scooterData.battery.toInt()}%" else "--"}", fontSize = 11.sp, color = Color.Yellow, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("🔋 --", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }

            // Kilométrage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("🛣️ --", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("🛣️ ${if (isConnected) "${scooterData.odometer}km" else "--"}", fontSize = 11.sp, color = Color.Yellow, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("🛣️ --", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }

            // Vitesse moyenne
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text("", modifier = Modifier.weight(1f))
                Text("🏃 Vmoy: ${if (isConnected && isRiding) "${scooterData.speed.toInt()}km/h" else "--"}",
                    fontSize = 11.sp, color = Color.Yellow, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("", modifier = Modifier.weight(1f))
            }
        }
    }
}
