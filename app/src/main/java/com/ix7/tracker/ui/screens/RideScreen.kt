package com.ix7.tracker.ui.screens

import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ix7.tracker.bluetooth.BluetoothRepository
import com.ix7.tracker.bluetooth.LockManager
import com.ix7.tracker.core.*
import com.ix7.tracker.protocol.ProtocolConstants
import com.ix7.tracker.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ix7.tracker.tracker.TripRecorder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.location.Location

/**
 * 🎯 ÉCRAN RIDE CORRIGÉ
 *
 * Ce fichier utilise les données provenant de ScooterData qui sont maintenant
 * décodées avec les VRAIS offsets validés par l'analyse du log:
 *
 * - scooterData.battery → provient de 0x20[45], 0x3E ou 0xD3[43]
 * - scooterData.voltage → provient de 0x3E[6-7] BE/1000
 * - scooterData.odometer → provient de 0x03[2-3] LE/100 ou 0x30[35-36] LE/10
 * - scooterData.temperature → provient de 0x3E[49] ou 0xD3[17,29]
 * - scooterData.speed → provient de 0x32[5]
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
    var cruiseThreshold by remember { mutableStateOf(25f) }
    var headlightsOn by remember { mutableStateOf(false) }
    var neonOn by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(true) }

    // 🔐 LOCK MANAGER
    val lockManager = remember { LockManager(context) }
    val lockState by lockManager.lockState.collectAsState()

    // État local pour le mode actuel
    var localCurrentMode by remember { mutableStateOf(scooterData.currentMode ?: RideMode.ECO) }
    val currentMode = scooterData.currentMode ?: localCurrentMode
    val isUnlimited = scooterData.speedLimitMode == SpeedLimitMode.UNLIMITED

    val tripRecorder = remember { TripRecorder(context) }
    val isRecordingTrip by tripRecorder.isRecording.collectAsState()
    var lastLocation by remember { mutableStateOf<Location?>(null) }

    // Location provider
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // ===== VITESSES LIMITES =====
    val speedLimits = when {
        isUnlimited && wheelMode == WheelMode.ONE_WHEEL -> SpeedLimits(20, 30, 40, 50)
        isUnlimited && wheelMode == WheelMode.TWO_WHEELS -> SpeedLimits(15, 30, 45, 60)
        else -> SpeedLimits(5, 10, 15, 25)
    }

    val maxSpeed = when (currentMode) {
        RideMode.PEDESTRIAN -> speedLimits.pedestrian
        RideMode.ECO -> speedLimits.eco
        RideMode.SPORT -> speedLimits.sport
        RideMode.RACE -> speedLimits.race
    }

    val displayMaxSpeed = if (speedUnit == SpeedUnit.MPH) (maxSpeed * 0.621371).toInt() else maxSpeed

    // 🎯 VITESSE CORRIGÉE - provient de 0x32[5]
    val currentSpeed = if (speedUnit == SpeedUnit.MPH) {
        scooterData.speed * 0.621371f
    } else {
        scooterData.speed
    }

    // 🔍 INITIALISATION: Scanner la serrure au démarrage
    LaunchedEffect(Unit) {
        try {
            val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
            lockManager.startScanning(bluetoothAdapter)
            Log.d("RideScreen", "🔍 Scan de serrure démarré")
        } catch (e: Exception) {
            Log.e("RideScreen", "❌ Erreur démarrage scan: ${e.message}")
        }
    }

    // Mettre à jour la vitesse pendant l'enregistrement
    LaunchedEffect(isRecordingTrip, scooterData.speed) {
        if (isRecordingTrip) {
            tripRecorder.updateSpeed(scooterData.speed)
        }
    }

    // ===== FONCTION POUR ENVOYER LE SEUIL DU RÉGULATEUR =====
    fun sendCruiseThreshold(speedKmh: Int) {
        scope.launch {
            val speedToHexValue = when(speedKmh) {
                10 -> 0x24; 11 -> 0x27; 12 -> 0x2A; 13 -> 0x2D; 14 -> 0x30
                15 -> 0x33; 16 -> 0x36; 17 -> 0x39; 18 -> 0x10; 19 -> 0x12
                20 -> 0x14; 21 -> 0x3C; 22 -> 0x3F; 23 -> 0x42; 24 -> 0x45
                25 -> 0x48; 26 -> 0x4B; 27 -> 0x4E; 28 -> 0x51; 29 -> 0x54
                30 -> 0x57; 35 -> 0x60; 40 -> 0x70; 45 -> 0x80; 50 -> 0x90
                55 -> 0xA0; 60 -> 0xB0
                else -> speedKmh
            }

            val knownChecksums = mapOf(
                0x14 to Pair(0x7A.toByte(), 0x43.toByte()),
                0x24 to Pair(0x13.toByte(), 0x9A.toByte()),
                0x3C to Pair(0x66.toByte(), 0xBF.toByte())
            )

            val baseCmd = byteArrayOf(
                0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                0xC7.toByte(), speedToHexValue.toByte(), 0x00, 0x00, 0xCA.toByte()
            )

            val checksums = knownChecksums[speedToHexValue]
            if (checksums != null) {
                baseCmd[7] = checksums.first
                baseCmd[8] = checksums.second
            } else {
                var xor = 0
                for (i in 2..6) xor = xor xor baseCmd[i].toInt()
                baseCmd[7] = xor.toByte()
                baseCmd[8] = (xor xor 0xFF).toByte()
            }

            bluetoothManager.sendCommand(baseCmd)
            Log.i("CRUISE", "🎯 Seuil: $speedKmh km/h → 0x${speedToHexValue.toString(16)}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // INTERFACE PRINCIPALE
    // ═══════════════════════════════════════════════════════════════════
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1️⃣ COMPTEUR + CONTRÔLES LATÉRAUX
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 🎯 Compteur de vitesse - UTILISE LA VRAIE VITESSE de 0x32[5]
                ModCompteurView(
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

                // Contrôles latéraux
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Lock/Unlock Trottinette
                    ModParkingBtn(
                        isLocked = isLocked,
                        onLock = {
                            scope.launch {
                                bluetoothManager.sendCommand(ProtocolConstants.CMD_LOCK)
                                isLocked = true
                            }
                        },
                        onUnlock = {
                            scope.launch {
                                bluetoothManager.sendCommand(ProtocolConstants.CMD_UNLOCK)
                                isLocked = false
                            }
                        }
                    )

                    // Phares, Néon, Klaxon
                    ModPharesHornBtn(
                        headlightsOn = headlightsOn,
                        neonOn = neonOn,
                        onHeadlightsToggle = {
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
                        onNeonToggle = {
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
                        onHornPress = {
                            scope.launch {
                                bluetoothManager.sendCommand(ProtocolConstants.CMD_HORN_TRY_1)
                                Log.i("HORN", "🔊 Klaxon activé")
                            }
                        },
                        onHornRelease = {
                            scope.launch {
                                bluetoothManager.sendCommand(ProtocolConstants.CMD_HORN_TRY_1)
                                Log.i("HORN", "🔊 Klaxon désactivé")
                            }
                        }
                    )
                }
            }

            // Slider régulateur
            ModRegulatorSlider(
                visible = cruiseControl,
                cruiseThreshold = cruiseThreshold,
                onValueChange = { cruiseThreshold = it },
                onValueChangeFinished = {
                    sendCruiseThreshold(cruiseThreshold.toInt())
                }
            )
        }

        // 2️⃣ MODES DE CONDUITE
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Roues
                    ModWheelsBtn(
                        wheelMode = wheelMode,
                        onOneWheelClick = {
                            wheelMode = WheelMode.ONE_WHEEL
                            scope.launch {
                                bluetoothManager.connector?.setWheelMode(WheelMode.ONE_WHEEL)
                            }
                        },
                        onTwoWheelsClick = {
                            wheelMode = WheelMode.TWO_WHEELS
                            scope.launch {
                                bluetoothManager.connector?.setWheelMode(WheelMode.TWO_WHEELS)
                            }
                        }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Régulateur
                        ModRegulatorBtn(
                            cruiseControl = cruiseControl,
                            onDisable = {
                                scope.launch {
                                    val command = byteArrayOf(
                                        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                        0x48, 0x34, 0x34, 0x68, 0xCB.toByte()
                                    )
                                    bluetoothManager.sendCommand(command)
                                    cruiseControl = false
                                }
                            },
                            onEnable = {
                                scope.launch {
                                    val command = byteArrayOf(
                                        0x61, 0x9E.toByte(), 0x30, 0x14, 0x37,
                                        0x48, 0x35, 0x34, 0x6F, 0xCB.toByte()
                                    )
                                    bluetoothManager.sendCommand(command)
                                    cruiseControl = true
                                    delay(100)
                                    sendCruiseThreshold(cruiseThreshold.toInt())
                                }
                            }
                        )

                        // Bridage
                        ModBridageBtn(
                            isUnlimited = isUnlimited,
                            onLimitedClick = {
                                scope.launch {
                                    bluetoothManager.connector?.setSpeedLimitMode(SpeedLimitMode.LIMITED)
                                }
                            },
                            onUnlimitedClick = {
                                scope.launch {
                                    bluetoothManager.connector?.setSpeedLimitMode(SpeedLimitMode.UNLIMITED)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Boutons de mode
                ModSwitchBtn(
                    currentMode = localCurrentMode,
                    speedLimits = speedLimits,
                    onPedestrianClick = {
                        localCurrentMode = RideMode.PEDESTRIAN
                        scope.launch {
                            bluetoothManager.sendCommand(ProtocolConstants.CMD_MODE_PEDESTRIAN)
                        }
                    },
                    onEcoClick = {
                        localCurrentMode = RideMode.ECO
                        scope.launch {
                            bluetoothManager.sendCommand(ProtocolConstants.CMD_MODE_ECO)
                        }
                    },
                    onRaceClick = {
                        localCurrentMode = RideMode.RACE
                        scope.launch {
                            bluetoothManager.sendCommand(ProtocolConstants.CMD_MODE_RACE)
                        }
                    },
                    onSportClick = {
                        localCurrentMode = RideMode.SPORT
                        scope.launch {
                            bluetoothManager.sendCommand(ProtocolConstants.CMD_MODE_SPORT)
                        }
                    }
                )
            }
        }

        // 3️⃣ SERRURE (si détectée)
        ModLockView(
            lockManager = lockManager,
            lockState = lockState,
            context = context
        )

        // 4️⃣ CLIGNOTANTS
        ModClignoBtn()

        // 5️⃣ GRAPHIQUE + CONTRÔLES
        // 🎯 UTILISE LES VRAIES VALEURS:
        // - currentSpeed → de 0x32[5]
        // - scooterData.battery → de 0x20[45], 0x3E ou 0xD3[43]
        ModGraphView(
            isRiding = isRiding,
            isPaused = isPaused,
            currentSpeed = currentSpeed,
            currentBattery = scooterData.battery,
            maxSpeed = displayMaxSpeed.toFloat(),
            scooterData = scooterData,
            currentMode = localCurrentMode,
            wheelMode = wheelMode,
            isUnlimited = isUnlimited,
            onStart = { isRiding = true; isPaused = false },
            onPauseToggle = { isPaused = !isPaused },
            onStop = { isRiding = false; isPaused = false }
        )

        // 6️⃣ INFOS
        // 🎯 AFFICHE LES VRAIES VALEURS de scooterData (décodées avec les bons offsets)
        ModInfoRideView(scooterData = scooterData)
    }
}