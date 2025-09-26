package com.ix7.tracker

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.ui.theme.IX7TrackerProTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var bluetoothLe: BluetoothLe

    // UUID corrects pour M0Robot (basés sur l'app originale)
    private val SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb"
    private val CHARACTERISTIC_UUID_WRITE = "0000ffe1-0000-1000-8000-00805f9b34fb"
    private val CHARACTERISTIC_UUID_NOTIFY = "0000ffe1-0000-1000-8000-00805f9b34fb"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothLe = BluetoothLe(this)
        enableEdgeToEdge()

        setContent {
            IX7TrackerProTheme {
                var isScanning by remember { mutableStateOf(false) }
                var foundDevices by remember { mutableStateOf(listOf<BluetoothDevice>()) }
                var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
                var isConnected by remember { mutableStateOf(false) }
                var connectionStatus by remember { mutableStateOf("Non connecté") }

                // Stockage des données de la trottinette
                var speedKmh by remember { mutableStateOf(0f) }
                var batteryLevel by remember { mutableStateOf(0) }
                var totalDistance by remember { mutableStateOf(0f) }
                var currentDistance by remember { mutableStateOf(0f) }
                var isLightOn by remember { mutableStateOf(false) }
                var powerMode by remember { mutableStateOf(1) } // 1: Low, 2: Med, 3: High

                // État pour l'enregistrement
                var isRecording by remember { mutableStateOf(false) }
                var recordedData by remember { mutableStateOf(listOf<Map<String, Any>>()) }
                var recordingStartTime by remember { mutableStateOf(0L) }
                var elapsedTime by remember { mutableStateOf(0L) }

                val coroutineScope = rememberCoroutineScope()

                // Timer pour demander les données régulièrement
                LaunchedEffect(isConnected) {
                    if (isConnected && selectedDevice != null) {
                        while (isConnected) {
                            // Envoyer la commande pour obtenir les données
                            bluetoothLe.writeCommand(
                                SERVICE_UUID,
                                CHARACTERISTIC_UUID_WRITE,
                                byteArrayOf(0x5A.toByte(), 0xA5.toByte(), 0x01, 0x3C, 0x20, 0x01, 0x7B.toByte(), 0xFF.toByte(), 0xFF.toByte())
                            )
                            delay(500) // Demander les données toutes les 500ms
                        }
                    }
                }

                // Timer pour l'enregistrement
                LaunchedEffect(isRecording) {
                    if (isRecording) {
                        recordingStartTime = System.currentTimeMillis()
                        while (isRecording) {
                            delay(1000)
                            elapsedTime = System.currentTimeMillis() - recordingStartTime

                            // Ajouter les données actuelles à l'enregistrement
                            val dataPoint = mapOf(
                                "time" to System.currentTimeMillis(),
                                "speed" to speedKmh,
                                "battery" to batteryLevel,
                                "distance" to currentDistance
                            )
                            recordedData = recordedData + dataPoint
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    "IX7 Tracker Pro",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                            .fillMaxSize()
                    ) {
                        // Section Connexion Bluetooth
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = connectionStatus,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Button(
                                        onClick = {
                                            if (!isConnected) {
                                                if (!isScanning) {
                                                    isScanning = true
                                                    bluetoothLe.startScan { devices ->
                                                        foundDevices = devices
                                                    }
                                                } else {
                                                    isScanning = false
                                                    bluetoothLe.stopScan()
                                                }
                                            } else {
                                                // Déconnexion
                                                bluetoothLe.disconnect()
                                                isConnected = false
                                                connectionStatus = "Non connecté"
                                                selectedDevice = null
                                                speedKmh = 0f
                                                batteryLevel = 0
                                                currentDistance = 0f
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isConnected) Color.Red else MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text(
                                            if (isConnected) "Déconnecter"
                                            else if (isScanning) "Arrêter scan"
                                            else "Scanner"
                                        )
                                    }
                                }

                                if (isScanning && foundDevices.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyColumn(
                                        modifier = Modifier.height(120.dp)
                                    ) {
                                        items(foundDevices) { device ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedDevice = device
                                                        isScanning = false
                                                        bluetoothLe.stopScan()

                                                        // Connexion au dispositif
                                                        connectionStatus = "Connexion..."

                                                        bluetoothLe.connect(
                                                            mac = device.address,
                                                            onConnectionStateChange = { state ->
                                                                when (state) {
                                                                    BluetoothLe.STATE_CONNECTED -> {
                                                                        isConnected = true
                                                                        connectionStatus = "Connecté à ${device.name}"

                                                                        // Démarrer les notifications
                                                                        bluetoothLe.startNotifications(
                                                                            mac = device.address,
                                                                            serviceUUID = SERVICE_UUID,
                                                                            characteristicUUID = CHARACTERISTIC_UUID_NOTIFY,
                                                                            onNotification = { data ->
                                                                                // Parser les données reçues
                                                                                val parsedData = parseM0RobotData(data)
                                                                                speedKmh = parsedData["speed"] as? Float ?: 0f
                                                                                batteryLevel = parsedData["battery"] as? Int ?: 0
                                                                                currentDistance = parsedData["currentDistance"] as? Float ?: 0f
                                                                                totalDistance = parsedData["totalDistance"] as? Float ?: 0f
                                                                                isLightOn = parsedData["light"] as? Boolean ?: false
                                                                                powerMode = parsedData["mode"] as? Int ?: 1

                                                                                Log.d("BLE", "Vitesse: $speedKmh km/h, Batterie: $batteryLevel%")
                                                                            }
                                                                        )
                                                                    }
                                                                    BluetoothLe.STATE_DISCONNECTED -> {
                                                                        isConnected = false
                                                                        connectionStatus = "Déconnecté"
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    }
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(device.name ?: "Appareil inconnu")
                                                Text(
                                                    device.address,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Affichage du compteur de vitesse
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                SpeedometerGauge(
                                    currentSpeed = speedKmh,
                                    maxSpeed = 25f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Informations principales
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    InfoCard(
                                        title = "Batterie",
                                        value = "$batteryLevel%",
                                        color = when {
                                            batteryLevel > 60 -> Color.Green
                                            batteryLevel > 30 -> Color.Yellow
                                            else -> Color.Red
                                        }
                                    )
                                    InfoCard(
                                        title = "Distance",
                                        value = String.format("%.2f km", currentDistance),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    InfoCard(
                                        title = "Mode",
                                        value = when(powerMode) {
                                            1 -> "ECO"
                                            2 -> "NORMAL"
                                            3 -> "SPORT"
                                            else -> "?"
                                        },
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Contrôles
                                if (isConnected) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        // Bouton lumière
                                        Button(
                                            onClick = {
                                                val lightCmd = if (isLightOn) {
                                                    // Commande pour éteindre
                                                    byteArrayOf(0x5A.toByte(), 0xA5.toByte(), 0x02, 0x3C, 0x20, 0x01, 0x00, 0x79.toByte(), 0xFF.toByte())
                                                } else {
                                                    // Commande pour allumer
                                                    byteArrayOf(0x5A.toByte(), 0xA5.toByte(), 0x02, 0x3C, 0x20, 0x01, 0x01, 0x78.toByte(), 0xFF.toByte())
                                                }
                                                bluetoothLe.writeCommand(SERVICE_UUID, CHARACTERISTIC_UUID_WRITE, lightCmd)
                                                isLightOn = !isLightOn
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isLightOn) Color.Yellow else Color.Gray
                                            )
                                        ) {
                                            Text(if (isLightOn) "Lumière ON" else "Lumière OFF")
                                        }

                                        // Bouton mode
                                        Button(
                                            onClick = {
                                                powerMode = if (powerMode >= 3) 1 else powerMode + 1
                                                val modeCmd = when(powerMode) {
                                                    1 -> byteArrayOf(0x5A.toByte(), 0xA5.toByte(), 0x03, 0x3C, 0x20, 0x01, 0x01, 0x77.toByte(), 0xFF.toByte())
                                                    2 -> byteArrayOf(0x5A.toByte(), 0xA5.toByte(), 0x03, 0x3C, 0x20, 0x01, 0x02, 0x76.toByte(), 0xFF.toByte())
                                                    3 -> byteArrayOf(0x5A.toByte(), 0xA5.toByte(), 0x03, 0x3C, 0x20, 0x01, 0x03, 0x75.toByte(), 0xFF.toByte())
                                                    else -> byteArrayOf()
                                                }
                                                bluetoothLe.writeCommand(SERVICE_UUID, CHARACTERISTIC_UUID_WRITE, modeCmd)
                                            }
                                        ) {
                                            Text("Mode: ${when(powerMode) {
                                                1 -> "ECO"
                                                2 -> "NORMAL"
                                                3 -> "SPORT"
                                                else -> "?"
                                            }}")
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Section Enregistrement
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isRecording) {
                                            "Enregistrement: ${formatTime(elapsedTime)}"
                                        } else {
                                            "Enregistrement arrêté"
                                        },
                                        fontWeight = FontWeight.Medium
                                    )
                                    Button(
                                        onClick = {
                                            if (!isRecording) {
                                                isRecording = true
                                                recordedData = emptyList()
                                            } else {
                                                isRecording = false
                                                // Sauvegarder les données ici
                                                saveRecordedData(recordedData)
                                            }
                                        },
                                        enabled = isConnected,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text(if (isRecording) "Arrêter" else "Démarrer")
                                    }
                                }

                                if (recordedData.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val avgSpeed = recordedData.map { it["speed"] as Float }.average()
                                    val maxSpeed = recordedData.map { it["speed"] as Float }.maxOrNull() ?: 0f
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Text("Moy: ${String.format("%.1f", avgSpeed)} km/h")
                                        Text("Max: ${String.format("%.1f", maxSpeed)} km/h")
                                        Text("Points: ${recordedData.size}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Parser les données M0Robot basé sur le protocole de l'app originale
    private fun parseM0RobotData(data: ByteArray): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        // Le protocole M0Robot envoie des paquets de 20 octets
        if (data.size >= 20) {
            // Structure basée sur l'app originale :
            // data[0-1] : Header (0x5A, 0xA5)
            // data[2] : Type de commande
            // data[6-7] : Vitesse (en 0.01 km/h)
            // data[8] : Batterie (%)
            // data[10-13] : Distance totale (en mètres)
            // data[14-15] : Distance actuelle (en 0.01 km)
            // data[16] : État lumière (0/1)
            // data[17] : Mode puissance (1/2/3)

            if (data[0] == 0x5A.toByte() && data[1] == 0xA5.toByte()) {
                // Vitesse
                val speedRaw = ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)
                result["speed"] = speedRaw / 100f // Convertir en km/h

                // Batterie
                result["battery"] = data[8].toInt() and 0xFF

                // Distance totale
                val totalDistRaw = ((data[10].toInt() and 0xFF) shl 24) or
                        ((data[11].toInt() and 0xFF) shl 16) or
                        ((data[12].toInt() and 0xFF) shl 8) or
                        (data[13].toInt() and 0xFF)
                result["totalDistance"] = totalDistRaw / 1000f // Convertir en km

                // Distance actuelle
                val currentDistRaw = ((data[14].toInt() and 0xFF) shl 8) or (data[15].toInt() and 0xFF)
                result["currentDistance"] = currentDistRaw / 100f // Convertir en km

                // État lumière
                result["light"] = data[16] == 0x01.toByte()

                // Mode puissance
                result["mode"] = data[17].toInt() and 0xFF
            }
        }

        Log.d("M0Robot", "Data parsed: $result")
        return result
    }

    private fun formatTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / 1000 / 60) % 60
        val hours = millis / 1000 / 3600
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun saveRecordedData(data: List<Map<String, Any>>) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val filename = "trajet_$timestamp.csv"

        // TODO: Implémenter la sauvegarde réelle dans un fichier
        Log.d("MainActivity", "Saving ${data.size} data points to $filename")
    }
}

@Composable
fun SpeedometerGauge(
    currentSpeed: Float,
    maxSpeed: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = size.center

            // Arc de fond
            drawArc(
                color = Color.Gray.copy(alpha = 0.3f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            // Arc de vitesse
            val speedPercentage = (currentSpeed / maxSpeed).coerceIn(0f, 1f)
            val speedColor = when {
                speedPercentage < 0.5f -> Color.Green
                speedPercentage < 0.8f -> Color.Yellow
                else -> Color.Red
            }

            drawArc(
                color = speedColor,
                startAngle = 135f,
                sweepAngle = 270f * speedPercentage,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Affichage de la vitesse
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = 30.dp)
        ) {
            Text(
                text = String.format("%.1f", currentSpeed),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "km/h",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}