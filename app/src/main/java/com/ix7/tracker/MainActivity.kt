package com.ix7.tracker

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var bluetoothLe: BluetoothLe
    private lateinit var bluetoothDeviceScanner: BluetoothDeviceScanner
    private lateinit var protocolHandler: M0RobotProtocolHandler

    // État de l'application
    private var currentScreen by mutableStateOf("main")
    private var scooterData by mutableStateOf(ScooterData())
    private var isScanning by mutableStateOf(false)
    private var isConnecting by mutableStateOf(false)
    private var connectionStatus by mutableStateOf("Déconnecté")

    // Launcher pour activer le Bluetooth
    private val bluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            LogManager.logInfo("Bluetooth activé")
        }
    }

    // Launcher pour les permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            checkBluetoothAndStart()
        } else {
            connectionStatus = "Permissions refusées"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeBluetoothComponents()
        requestPermissions()

        setContent {
            IX7TrackerTheme {
                when (currentScreen) {
                    "main" -> MainScreen()
                    "information" -> InformationScreen(
                        scooterData = scooterData,
                        onBack = { currentScreen = "main" }
                    )
                }
            }
        }
    }

    private fun initializeBluetoothComponents() {
        bluetoothLe = BluetoothLe(this)
        bluetoothDeviceScanner = BluetoothDeviceScanner(this)
        protocolHandler = M0RobotProtocolHandler()

        // Configuration des callbacks
        bluetoothLe.setOnDataReceivedListener { data ->
            lifecycleScope.launch {
                LogManager.logInfo("Données reçues (${data.size} bytes): ${protocolHandler.dataToHex(data)}")

                val parsedData = protocolHandler.parseIncomingData(data)
                LogManager.logInfo("Données parsées: $parsedData")

                if (parsedData.isNotEmpty()) {
                    updateScooterData(parsedData)
                    LogManager.logInfo("ScooterData mis à jour: batterie=${scooterData.batteryLevel}%, vitesse=${scooterData.currentSpeed}km/h")
                } else {
                    LogManager.logInfo("Aucune donnée reconnue dans le packet")

                    // Essayer aussi le protocole M365
                    val m365Data = protocolHandler.parseM365Protocol(data)
                    if (m365Data.isNotEmpty()) {
                        LogManager.logInfo("Données M365 parsées: $m365Data")
                        updateScooterData(m365Data)
                    }
                }
            }
        }

        bluetoothLe.setOnConnectionStateChangedListener { isConnected ->
            lifecycleScope.launch {
                scooterData = scooterData.copy(isConnected = isConnected)
                connectionStatus = if (isConnected) {
                    "Connecté"
                } else {
                    "Déconnecté"
                }
                isConnecting = false

                if (isConnected) {
                    currentScreen = "main"
                    // Demander les informations de la trottinette
                    requestScooterInfo()
                }
            }
        }
    }

    private fun updateScooterData(data: Map<String, Any>) {
        scooterData = scooterData.copy(
            // Batterie
            batteryLevel = data["batteryLevel"] as? Int ?: scooterData.batteryLevel,
            voltage = data["voltage"] as? Double ?: scooterData.voltage,
            current = data["current"] as? Double ?: scooterData.current,
            batteryTemperature = data["batteryTemp"] as? Int ?: scooterData.batteryTemperature,
            batteryStatus = data["batteryStatus"] as? Int ?: scooterData.batteryStatus,

            // Vitesse et distance
            currentSpeed = data["speed"] as? Double ?: scooterData.currentSpeed,
            totalDistance = data["totalDistance"] as? Double ?: scooterData.totalDistance,

            // Températures
            scooterTemperature = data["scooterTemp"] as? Double ?: scooterData.scooterTemperature,

            // Puissance
            power = data["power"] as? Double ?: scooterData.power,

            // Codes d'erreur
            errorCode = data["errorCode"] as? Int ?: scooterData.errorCode,
            warningCode = data["warningCode"] as? Int ?: scooterData.warningCode,

            // Versions
            firmwareVersion = data["firmware"] as? String ?: scooterData.firmwareVersion,
            electricVersion = data["electricVersion"] as? String ?: scooterData.electricVersion,
            bluetoothVersion = data["bluetoothVersion"] as? String ?: scooterData.bluetoothVersion,

            // Temps de conduite
            ridingTime = data["ridingTime"] as? String ?: scooterData.ridingTime,

            timestamp = System.currentTimeMillis()
        )
    }

    private fun requestScooterInfo() {
        lifecycleScope.launch {
            try {
                LogManager.logInfo("Demande des informations de la trottinette...")

                // Attendre un peu après la connexion
                kotlinx.coroutines.delay(1000)

                // Envoyer des commandes pour récupérer les infos
                val commands = protocolHandler.createFullInfoRequest()
                LogManager.logInfo("Envoi de ${commands.size} commandes...")

                commands.forEachIndexed { index, command ->
                    LogManager.logInfo("Envoi commande ${index + 1}: ${protocolHandler.dataToHex(command)}")
                    bluetoothLe.sendData(command)
                    kotlinx.coroutines.delay(500) // Délai plus long entre les commandes
                }

                // Essayer aussi le protocole M365 standard
                kotlinx.coroutines.delay(1000)
                sendM365Commands()

            } catch (e: Exception) {
                LogManager.logError("Erreur lors de la demande d'infos", e)
            }
        }
    }

    private fun sendM365Commands() {
        lifecycleScope.launch {
            try {
                // Commandes M365 standard
                val m365BatteryCmd = byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x03.toByte(), 0x22.toByte(), 0x01.toByte(), 0x0E.toByte(), 0xFF.toByte(), 0xDD.toByte())
                val m365InfoCmd = byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x03.toByte(), 0x22.toByte(), 0x01.toByte(), 0x10.toByte(), 0xFF.toByte(), 0xDD.toByte())
                val m365SpeedCmd = byteArrayOf(0x55.toByte(), 0xAA.toByte(), 0x03.toByte(), 0x22.toByte(), 0x01.toByte(), 0x0D.toByte(), 0xFF.toByte(), 0xDD.toByte())

                LogManager.logInfo("Envoi commandes M365...")
                bluetoothLe.sendData(m365BatteryCmd)
                kotlinx.coroutines.delay(300)
                bluetoothLe.sendData(m365InfoCmd)
                kotlinx.coroutines.delay(300)
                bluetoothLe.sendData(m365SpeedCmd)

            } catch (e: Exception) {
                LogManager.logError("Erreur envoi commandes M365", e)
            }
        }
    }

    @Composable
    fun MainScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "IX7 Tracker Pro",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // État de connexion
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (scooterData.isConnected)
                        Color.Green.copy(alpha = 0.1f)
                    else
                        Color.Red.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = connectionStatus,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (scooterData.isConnected) Color.Green else Color.Red
                    )
                    if (scooterData.isConnected) {
                        Text(
                            text = "Batterie: ${scooterData.batteryLevel}%",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Vitesse: ${scooterData.currentSpeed} km/h",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Boutons d'action
            if (scooterData.isConnected) {
                Button(
                    onClick = { disconnectFromScooter() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Déconnecter")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { currentScreen = "information" },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Voir les informations")
                }
            } else {
                Button(
                    onClick = { startScanAndConnect() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isScanning && !isConnecting
                ) {
                    if (isScanning || isConnecting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        when {
                            isScanning -> "Recherche..."
                            isConnecting -> "Connexion..."
                            else -> "Connecter à M0Robot"
                        }
                    )
                }
            }
        }
    }

    private fun startScanAndConnect() {
        isScanning = true
        connectionStatus = "Recherche de M0Robot..."

        lifecycleScope.launch {
            bluetoothDeviceScanner.startScan { device, rssi ->
                if (device.name?.startsWith("M0Robot", ignoreCase = true) == true) {
                    LogManager.logInfo("M0Robot trouvé: ${device.name} (${device.address}) - Signal: ${rssi}dBm")

                    // Arrêter le scan et se connecter automatiquement
                    stopScanning()
                    connectToDevice(device.address)
                }
            }

            // Arrêter le scan après 10 secondes si rien n'est trouvé
            kotlinx.coroutines.delay(10000)
            if (isScanning) {
                stopScanning()
                connectionStatus = "Aucun M0Robot trouvé"
            }
        }
    }

    private fun stopScanning() {
        isScanning = false
        bluetoothDeviceScanner.stopScan()
        if (!scooterData.isConnected) {
            connectionStatus = "Déconnecté"
        }
    }

    private fun connectToDevice(deviceAddress: String) {
        isConnecting = true
        connectionStatus = "Connexion..."

        lifecycleScope.launch {
            try {
                bluetoothLe.connect(deviceAddress)
            } catch (e: Exception) {
                connectionStatus = "Erreur de connexion"
                isConnecting = false
                LogManager.logError("Erreur de connexion", e)
            }
        }
    }

    private fun disconnectFromScooter() {
        bluetoothLe.disconnect()
        scooterData = ScooterData() // Reset des données
        connectionStatus = "Déconnecté"
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.addAll(listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ))
        } else {
            permissions.addAll(listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            ))
        }

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        val missingPermissions = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            checkBluetoothAndStart()
        }
    }

    private fun checkBluetoothAndStart() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            connectionStatus = "Bluetooth non disponible"
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableBtIntent)
        }
    }

    @Composable
    fun IX7TrackerTheme(content: @Composable () -> Unit) {
        MaterialTheme(
            colorScheme = darkColorScheme(),
            content = content
        )
    }
}