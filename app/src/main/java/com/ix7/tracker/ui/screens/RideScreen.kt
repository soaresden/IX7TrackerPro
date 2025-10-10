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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically

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
    var cruiseThreshold by remember { mutableStateOf(25f) } // Valeur par défaut du seuil
    var headlightsOn by remember { mutableStateOf(false) }
    var neonOn by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(true) }

    // État local pour le mode actuel (mis à jour par les boutons)
    var localCurrentMode by remember { mutableStateOf(scooterData.currentMode ?: RideMode.ECO) }

    // Utiliser le mode depuis scooterData si disponible, sinon état local
    val currentMode = scooterData.currentMode ?: localCurrentMode

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

    // ===== FONCTION POUR ENVOYER LE SEUIL DU RÉGULATEUR (AVEC MAPPING CORRECT) =====
    fun sendCruiseThreshold(speedKmh: Int) {
        scope.launch {
            // IMPORTANT: La valeur hex n'est PAS la vitesse directe !
            // D'après vos tests :
            // - 20 km/h → hex 0x14 (20) ✓ Direct
            // - 10 km/h → hex 0x24 (36)
            // - 21 km/h → hex 0x3C (60)

            // Table de conversion vitesse réelle → valeur hex
            val speedToHexValue = when(speedKmh) {
                10 -> 0x24  // Confirmé par vos tests
                11 -> 0x27
                12 -> 0x2A
                13 -> 0x2D
                14 -> 0x30
                15 -> 0x33
                16 -> 0x36
                17 -> 0x39
                18 -> 0x10
                19 -> 0x12
                20 -> 0x14  // Confirmé par vos tests
                21 -> 0x3C  // Confirmé par vos tests
                22 -> 0x3F
                23 -> 0x42
                24 -> 0x45
                25 -> 0x48
                26 -> 0x4B
                27 -> 0x4E
                28 -> 0x51
                29 -> 0x54
                30 -> 0x57
                35 -> 0x60
                40 -> 0x70
                45 -> 0x80
                50 -> 0x90
                55 -> 0xA0
                60 -> 0xB0
                else -> speedKmh  // Par défaut, utiliser la valeur directe
            }

            // Checksums connus pour certaines valeurs
            val knownChecksums = mapOf(
                0x14 to Pair(0x7A.toByte(), 0x43.toByte()),  // 20 km/h
                0x24 to Pair(0x13.toByte(), 0x9A.toByte()),  // 10 km/h (hex 36)
                0x3C to Pair(0x66.toByte(), 0xBF.toByte())   // 21 km/h (hex 60)
            )

            val baseCmd = byteArrayOf(
                0x61, 0x9E.toByte(),
                0x30, 0x14, 0x37,
                0xC7.toByte(),            // Commande cruise threshold
                speedToHexValue.toByte(),  // VALEUR ENCODÉE (pas la vitesse directe !)
                0x00,                     // Checksum 1
                0x00,                     // Checksum 2
                0xCA.toByte()            // Fin
            )

            // Utiliser les checksums connus ou calculer
            val checksums = knownChecksums[speedToHexValue]
            if (checksums != null) {
                baseCmd[7] = checksums.first
                baseCmd[8] = checksums.second
                Log.i("CRUISE", "✅ Checksums connus pour $speedKmh km/h (hex: 0x${speedToHexValue.toString(16)})")
            } else {
                // Calcul approximatif
                var xor = 0
                for (i in 2..6) {
                    xor = xor xor baseCmd[i].toInt()
                }
                baseCmd[7] = xor.toByte()
                baseCmd[8] = (xor xor 0xFF).toByte()
                Log.w("CRUISE", "⚠️ Checksums calculés pour $speedKmh km/h (hex: 0x${speedToHexValue.toString(16)})")
            }

            // Envoyer la commande
            bluetoothManager.sendCommand(baseCmd)

            val hex = baseCmd.joinToString(" ") { "%02X".format(it) }
            Log.i("CRUISE", "🎯 Seuil régulateur: $speedKmh km/h → hex 0x${speedToHexValue.toString(16)}")
            Log.i("CRUISE", "📤 Commande: $hex")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ═══════════════════════════════════════════════════════════════════
        // 1. COMPTEUR DE VITESSE + SLIDER RÉGULATEUR
        // ═══════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp), // Réduit la hauteur pour faire de la place au slider
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
                                modifier = Modifier.size(45.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("🔒", fontSize = 18.sp)
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
                                modifier = Modifier.size(45.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("🔓", fontSize = 18.sp)
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
                                modifier = Modifier.size(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (headlightsOn) "💡" else "⚫", fontSize = 14.sp)
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
                                modifier = Modifier.size(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (neonOn) "🟣" else "⚫", fontSize = 14.sp)
                            }

                            // KLAXON - MAINTIEN = BIP CONTINU
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
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
                                Text("🔊", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // 🎯 SLIDER RÉGULATEUR - EN DESSOUS DU COMPTEUR
            AnimatedVisibility(
                visible = cruiseControl,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2C2C2E).copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Titre et valeur actuelle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🎯 Seuil régulateur",
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${cruiseThreshold.toInt()} km/h",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Cyan
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Slider avec marqueurs
                        Column {
                            Slider(
                                value = cruiseThreshold,
                                onValueChange = { cruiseThreshold = it },
                                onValueChangeFinished = {
                                    sendCruiseThreshold(cruiseThreshold.toInt())
                                },
                                valueRange = 10f..60f,
                                steps = 49, // Pour avoir des valeurs entières
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.Cyan,
                                    activeTrackColor = Color.Cyan,
                                    inactiveTrackColor = Color(0xFF505050)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Marqueurs de référence
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("10", fontSize = 10.sp, color = Color.Gray)
                                Text("25", fontSize = 10.sp, color = Color.Gray)
                                Text("40", fontSize = 10.sp, color = Color.Gray)
                                Text("60", fontSize = 10.sp, color = Color.Gray)
                            }
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
                        // RÉGULATEUR - GROUPÉ DANS UNE CARD
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                // DÉSACTIVER RÉGULATEUR ❌
                                Button(
                                    onClick = {
                                        scope.launch {
                                            // Commande qui MARCHE pour OFF
                                            val command = byteArrayOf(
                                                0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                                0x48, 0x34, 0x34, 0x68, 0xCB.toByte()
                                            )
                                            bluetoothManager.sendCommand(command)
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

                                // ACTIVER RÉGULATEUR 🎯
                                Button(
                                    onClick = {
                                        scope.launch {
                                            // Commande qui MARCHE pour ON
                                            val command = byteArrayOf(
                                                0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                                0x48, 0x35, 0x34, 0x6F, 0xCB.toByte()
                                            )
                                            bluetoothManager.sendCommand(command)
                                            cruiseControl = true

                                            // Envoyer aussi le seuil actuel
                                            delay(100)
                                            sendCruiseThreshold(cruiseThreshold.toInt())
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

                // BOUTONS MODES - CORRIGÉ : État local + couleurs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // PIÉTON 🚶
                    Button(
                        onClick = {
                            localCurrentMode = RideMode.PEDESTRIAN
                            scope.launch {
                                bluetoothManager.sendCommand(ProtocolConstants.CMD_MODE_PEDESTRIAN)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (localCurrentMode == RideMode.PEDESTRIAN)
                                Color(0xFF007AFF) else Color(0xFF3C3C3E)
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🚶", fontSize = 14.sp, color = Color.White)
                            Text("${speedLimits.pedestrian}", fontSize = 10.sp, color = Color.White)
                        }
                    }

                    // ECO 🌱
                    Button(
                        onClick = {
                            localCurrentMode = RideMode.ECO
                            scope.launch {
                                bluetoothManager.sendCommand(ProtocolConstants.CMD_MODE_ECO)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (localCurrentMode == RideMode.ECO)
                                Color(0xFF007AFF) else Color(0xFF3C3C3E)
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌱", fontSize = 14.sp, color = Color.White)
                            Text("${speedLimits.eco}", fontSize = 10.sp, color = Color.White)
                        }
                    }


                    // RACE 🏎️
                    Button(
                        onClick = {
                            localCurrentMode = RideMode.RACE
                            scope.launch {
                                bluetoothManager.sendCommand(ProtocolConstants.CMD_MODE_RACE)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (localCurrentMode == RideMode.RACE)
                                Color(0xFF007AFF) else Color(0xFF3C3C3E)
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏎️", fontSize = 14.sp, color = Color.White)
                            Text("${speedLimits.race}", fontSize = 10.sp, color = Color.White)
                        }
                    }

                    // SPORT ⚡
                    Button(
                        onClick = {
                            localCurrentMode = RideMode.SPORT
                            scope.launch {
                                bluetoothManager.sendCommand(ProtocolConstants.CMD_MODE_SPORT)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (localCurrentMode == RideMode.SPORT)
                                Color(0xFF007AFF) else Color(0xFF3C3C3E)
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚡", fontSize = 14.sp, color = Color.White)
                            Text("${speedLimits.sport}", fontSize = 10.sp, color = Color.White)
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
// COMPOSANTS AUXILIAIRES (inchangés)
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