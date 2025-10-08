package com.ix7.tracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.bluetooth.BluetoothRepository
import com.ix7.tracker.core.TemperatureThresholds
import com.ix7.tracker.core.ScooterData
import com.ix7.tracker.core.RideMode
import com.ix7.tracker.core.WheelMode
import com.ix7.tracker.core.SpeedUnit
import com.ix7.tracker.core.SpeedLimits
import com.ix7.tracker.core.SpeedLimitMode
import com.ix7.tracker.protocol.ProtocolConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.*
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.util.Log

/**
 * ✅ CORRECTIONS APPLIQUÉES :
 * - Affichage vitesse depuis scooterData.speed (trame 0x37)
 * - Klaxon maintenu = bip continu
 * - Barrière et éclair en BLEU quand actifs
 * - Regroupements visuels par paires
 */
@Composable
fun RideScreen(
    scooterData: ScooterData,
    isConnected: Boolean,
    bluetoothManager: BluetoothRepository
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // ===== ÉTATS LOCAUX =====
    var wheelMode by remember { mutableStateOf(WheelMode.ONE_WHEEL) }
    var speedUnit by remember { mutableStateOf(SpeedUnit.KMH) }
    var isRiding by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var cruiseControl by remember { mutableStateOf(false) }
    var headlightsOn by remember { mutableStateOf(false) }
    var neonOn by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(true) }

    // Utiliser le mode depuis scooterData si disponible, sinon état local
    val currentMode by remember(scooterData.currentMode) {
        mutableStateOf(scooterData.currentMode ?: RideMode.ECO)
    }

    // Utiliser speedLimitMode depuis scooterData
    val isUnlimited = scooterData.speedLimitMode == SpeedLimitMode.UNLIMITED

    // ===== VITESSES LIMITES SELON MODE =====
    val speedLimits = when {
        // Mode DÉBRIDÉ 1 roue
        isUnlimited && wheelMode == WheelMode.ONE_WHEEL -> SpeedLimits(20, 30, 40, 50)
        // Mode DÉBRIDÉ 2 roues
        isUnlimited && wheelMode == WheelMode.TWO_WHEELS -> SpeedLimits(15, 30, 45, 60)
        // Mode BRIDÉ (1 ou 2 roues)
        else -> SpeedLimits(5, 10, 15, 25)
    }

    // Vitesse max du mode actif
    val maxSpeed = when (currentMode) {
        RideMode.PEDESTRIAN -> speedLimits.pedestrian
        RideMode.ECO -> speedLimits.eco
        RideMode.SPORT -> speedLimits.sport
        RideMode.RACE -> speedLimits.race
    }

    val displayMaxSpeed = if (speedUnit == SpeedUnit.MPH) (maxSpeed * 0.621371).toInt() else maxSpeed

    // ⭐ VITESSE DEPUIS scooterData (trame 0x37) ⭐
    val currentSpeed = if (speedUnit == SpeedUnit.MPH) {
        scooterData.speed * 0.621371f
    } else {
        scooterData.speed
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ═══════════════════════════════════════════════════════════════════
        // 1. COMPTEUR DE VITESSE + CADENAS + EMOJIS
        // ═══════════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compteur vitesse
            CompactSpeedometer(
                speed = if (isConnected) currentSpeed else 0f,
                maxSpeed = displayMaxSpeed.toFloat(),
                speedUnit = speedUnit,
                onUnitClick = {
                    scope.launch {
                        if (speedUnit == SpeedUnit.KMH) {
                            bluetoothManager.sendCommand(ProtocolConstants.CMD_UNIT_MPH)
                            speedUnit = SpeedUnit.MPH
                        } else {
                            bluetoothManager.sendCommand(ProtocolConstants.CMD_UNIT_KMH)
                            speedUnit = SpeedUnit.KMH
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            )

            // Colonne: 2 cadenas + 3 emojis - GROUPÉS
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 2 CADENAS - GROUPÉS DANS UNE CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    bluetoothManager.sendCommand(ProtocolConstants.CMD_LOCK)
                                    isLocked = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLocked) Color.Red else Color.DarkGray
                            ),
                            modifier = Modifier.size(50.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("🔒", fontSize = 20.sp)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    bluetoothManager.sendCommand(ProtocolConstants.CMD_UNLOCK)
                                    isLocked = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isLocked) Color.Green else Color.DarkGray
                            ),
                            modifier = Modifier.size(50.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("🔓", fontSize = 20.sp)
                        }
                    }
                }

                // 3 EMOJIS - GROUPÉS DANS UNE CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        // PHARES
                        Button(
                            onClick = {
                                scope.launch {
                                    if (headlightsOn) {
                                        bluetoothManager.sendCommand(ProtocolConstants.CMD_LIGHTS_OFF)
                                        headlightsOn = false
                                    } else {
                                        bluetoothManager.sendCommand(ProtocolConstants.CMD_LIGHTS_ON)
                                        headlightsOn = true
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (headlightsOn) Color(0xFFFFEB3B) else Color.DarkGray
                            ),
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(if (headlightsOn) "💡" else "⚫", fontSize = 16.sp)
                        }

                        // NÉON
                        Button(
                            onClick = {
                                scope.launch {
                                    if (neonOn) {
                                        bluetoothManager.sendCommand(ProtocolConstants.CMD_NEON_OFF)
                                        neonOn = false
                                    } else {
                                        bluetoothManager.sendCommand(ProtocolConstants.CMD_NEON_ON)
                                        neonOn = true
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (neonOn) Color(0xFF9C27B0) else Color.DarkGray
                            ),
                            modifier = Modifier.size(36.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(if (neonOn) "🟣" else "⚫", fontSize = 16.sp)
                        }

                        // KLAXON - MAINTIEN = BIP CONTINU
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5722))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            scope.launch {
                                                bluetoothManager.sendCommand(ProtocolConstants.CMD_HORN_TRY_1)
                                                Log.i("HORN", "🔊 Klaxon activé (maintien)")
                                            }
                                            tryAwaitRelease()
                                            scope.launch {
                                                bluetoothManager.sendCommand(ProtocolConstants.CMD_HORN_TRY_1)
                                                Log.i("HORN", "🔊 Klaxon désactivé (relâché)")
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔊", fontSize = 18.sp)
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // 2. MODE DE CONDUITE
        // ═══════════════════════════════════════════════════════════════════
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mode roues - 2 boutons séparés
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Roues:", fontSize = 12.sp, color = Color.White)

                        // Bouton 1 ROUE
                        Button(
                            onClick = {
                                wheelMode = WheelMode.ONE_WHEEL
                                scope.launch {
                                    bluetoothManager.connector?.setWheelMode(WheelMode.ONE_WHEEL)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (wheelMode == WheelMode.ONE_WHEEL)
                                    Color.Blue else Color.DarkGray
                            ),
                            modifier = Modifier.size(45.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🛴", fontSize = 14.sp)
                                Text("1", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Bouton 2 ROUES
                        Button(
                            onClick = {
                                wheelMode = WheelMode.TWO_WHEELS
                                scope.launch {
                                    bluetoothManager.connector?.setWheelMode(WheelMode.TWO_WHEELS)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (wheelMode == WheelMode.TWO_WHEELS)
                                    Color.Blue else Color.DarkGray
                            ),
                            modifier = Modifier.size(45.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🏍️️", fontSize = 14.sp)
                                Text("2", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // CRUISE CONTROL - GROUPÉ DANS UNE CARD
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                // CROIX ROUGE ❌ - Régulateur OFF
                                Button(
                                    onClick = {
                                        scope.launch {
                                            bluetoothManager.sendCommand(ProtocolConstants.CMD_CRUISE_OFF)
                                            cruiseControl = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!cruiseControl) Color.Blue else Color.DarkGray
                                    ),
                                    modifier = Modifier.size(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("❌", fontSize = 16.sp)
                                }

                                // CIBLE 🎯 - Régulateur ON
                                Button(
                                    onClick = {
                                        scope.launch {
                                            bluetoothManager.sendCommand(ProtocolConstants.CMD_CRUISE_ON)
                                            cruiseControl = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (cruiseControl) Color.Blue else Color.DarkGray
                                    ),
                                    modifier = Modifier.size(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("🎯", fontSize = 16.sp)
                                }
                            }
                        }

                        // LIMITATION VITESSE - GROUPÉ DANS UNE CARD
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                // BRIDÉ 🚧 - BLEU QUAND ACTIF
                                Button(
                                    onClick = {
                                        scope.launch {
                                            bluetoothManager.connector?.setSpeedLimitMode(SpeedLimitMode.LIMITED)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!isUnlimited) Color.Blue else Color.DarkGray
                                    ),
                                    modifier = Modifier.size(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("🚧", fontSize = 16.sp)
                                }

                                // DÉBRIDÉ ⚡ - BLEU QUAND ACTIF
                                Button(
                                    onClick = {
                                        scope.launch {
                                            bluetoothManager.connector?.setSpeedLimitMode(SpeedLimitMode.UNLIMITED)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isUnlimited) Color.Blue else Color.DarkGray
                                    ),
                                    modifier = Modifier.size(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("⚡", fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // BOUTONS MODES - CORRIGÉ selon ProtocolConstants
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // PIÉTON 🚶
                    Button(
                        onClick = {
                            scope.launch {
                                bluetoothManager.sendCommand(ProtocolConstants.CMD_MODE_PEDESTRIAN)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentMode == RideMode.PEDESTRIAN) Color.Blue else Color.DarkGray
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🚶", fontSize = 14.sp)
                            Text("${speedLimits.pedestrian}", fontSize = 10.sp)
                        }
                    }

                    // ECO 🌱
                    Button(
                        onClick = {
                            scope.launch {
                                bluetoothManager.sendCommand(ProtocolConstants.CMD_MODE_ECO)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentMode == RideMode.ECO) Color.Blue else Color.DarkGray
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌱", fontSize = 14.sp)
                            Text("${speedLimits.eco}", fontSize = 10.sp)
                        }
                    }

                    // RACE 🏎️
                    Button(
                        onClick = {
                            scope.launch {
                                bluetoothManager.sendCommand(ProtocolConstants.CMD_MODE_RACE)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentMode == RideMode.RACE) Color.Blue else Color.DarkGray
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏎️", fontSize = 14.sp)
                            Text("${speedLimits.race}", fontSize = 10.sp)
                        }
                    }

                    // SPORT ⚡
                    Button(
                        onClick = {
                            scope.launch {
                                bluetoothManager.sendCommand(ProtocolConstants.CMD_MODE_SPORT)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentMode == RideMode.SPORT) Color.Blue else Color.DarkGray
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚡", fontSize = 14.sp)
                            Text("${speedLimits.sport}", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // 3. CLIGNOTANTS (désactivés pour l'instant)
        // ═══════════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                enabled = false,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("⬅️", fontSize = 20.sp)
            }

            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                enabled = false,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("⚠️", fontSize = 20.sp)
            }

            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                enabled = false,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("➡️", fontSize = 20.sp)
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // 4. GRAPHIQUE EN TEMPS RÉEL
        // ═══════════════════════════════════════════════════════════════════
        Card(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            RealTimeGraph(
                isRiding = isRiding,
                currentSpeed = currentSpeed,
                currentBattery = scooterData.battery,
                maxSpeed = displayMaxSpeed.toFloat()
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // 5. CONTRÔLES DE TRAJET
        // ═══════════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { isRiding = true; isPaused = false },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                enabled = !isRiding || isPaused,
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
            ) {
                Text("▶", fontSize = 20.sp)
            }

            Button(
                onClick = { isPaused = !isPaused },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                enabled = isRiding,
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
            ) {
                Text(if (isPaused) "▶" else "⏸", fontSize = 20.sp)
            }

            Button(
                onClick = { isRiding = false; isPaused = false },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                enabled = isRiding,
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
            ) {
                Text("⏹", fontSize = 20.sp)
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // 6. DONNÉES COMPACTES
        // ═══════════════════════════════════════════════════════════════════
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DataItem("Batterie", "${scooterData.battery.toInt()}%")
                DataItem("Voltage", "%.1fV".format(scooterData.voltage))
                DataItem("Odomètre", "%.1fkm".format(scooterData.odometer))
                TemperatureIndicator(temperature = scooterData.temperature)
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // 7. STATISTIQUES DE TRAJET
        // ═══════════════════════════════════════════════════════════════════
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DataItem("Trajet", "%.1fkm".format(scooterData.tripDistance))
                DataItem("Temps", scooterData.totalRideTime)
                DataItem("Puissance", "%.0fW".format(scooterData.power))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// COMPOSANTS AUXILIAIRES
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun TemperatureIndicator(temperature: Float) {
    val (emoji, color, warning) = when {
        temperature > TemperatureThresholds.MOTOR_CRITICAL -> Triple("🔥", Color(0xFFF44336), true)
        temperature > TemperatureThresholds.MOTOR_WARNING -> Triple("🌡️", Color(0xFFFF9800), true)
        else -> Triple("🌡️", Color(0xFF4CAF50), false)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { }
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Text(
            text = "${temperature.toInt()}°C",
            fontSize = 14.sp,
            fontWeight = if (warning) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
        Text(text = "Temp", fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
private fun DataItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
private fun CompactSpeedometer(
    speed: Float,
    maxSpeed: Float,
    speedUnit: SpeedUnit,
    onUnitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // ⭐ AFFICHAGE VITESSE (depuis trame 0x37)
            Text(
                text = "${speed.toInt()}",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = speedUnit.name.lowercase(),
                    fontSize = 14.sp,
                    color = Color.Cyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onUnitClick() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${maxSpeed.toInt()}",
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RealTimeGraph(
    isRiding: Boolean,
    currentSpeed: Float,
    currentBattery: Float,
    maxSpeed: Float
) {
    var speedHistory by remember { mutableStateOf(listOf<Float>()) }
    var batteryHistory by remember { mutableStateOf(listOf<Float>()) }

    LaunchedEffect(isRiding, currentSpeed, currentBattery) {
        if (isRiding) {
            speedHistory = speedHistory.takeLast(49) + currentSpeed
            batteryHistory = batteryHistory.takeLast(49) + currentBattery
        } else {
            speedHistory = emptyList()
            batteryHistory = emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        if (speedHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("▶ pour démarrer", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val pointSpacing = width / speedHistory.size.coerceAtLeast(1)

                // Courbe vitesse (bleu)
                val speedPath = Path()
                speedHistory.forEachIndexed { index, speed ->
                    val x = index * pointSpacing
                    val y = height - (speed / maxSpeed * height).coerceIn(0f, height)
                    if (index == 0) speedPath.moveTo(x, y) else speedPath.lineTo(x, y)
                }
                drawPath(path = speedPath, color = Color(0xFF2196F3), style = Stroke(width = 3f))

                // Courbe batterie (vert)
                val batteryPath = Path()
                batteryHistory.forEachIndexed { index, battery ->
                    val x = index * pointSpacing
                    val y = height - (battery / 100f * height)
                    if (index == 0) batteryPath.moveTo(x, y) else batteryPath.lineTo(x, y)
                }
                drawPath(path = batteryPath, color = Color(0xFF4CAF50), style = Stroke(width = 2f))
            }
        }
    }
}