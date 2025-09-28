
// Fichier: ui/components/ride/ActionsBar.kt
package com.ix7.tracker.ui.components.ride

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActionsBar(
    headlights: Boolean,
    neonLights: Boolean,
    onHeadlightsToggle: () -> Unit,
    onNeonToggle: () -> Unit,
    onHornPress: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF006064))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "🎛️ ACTIONS",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Lumières
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💡 Lumières", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onHeadlightsToggle,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text("💡", fontSize = 18.sp)
                        }

                        IconButton(
                            onClick = onNeonToggle,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text("✨", fontSize = 18.sp)
                        }
                    }
                }

                // Klaxon
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔊 Son", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = onHornPress,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Text("📯", fontSize = 24.sp)
                    }
                }

                // Autres actions
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚙️ Divers", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { /* TODO: Caméra */ },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text("📷", fontSize = 18.sp)
                        }
                        IconButton(
                            onClick = { /* TODO: Paramètres */ },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text("⚙️", fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}