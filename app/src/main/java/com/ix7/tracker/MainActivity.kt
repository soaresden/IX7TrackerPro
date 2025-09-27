package com.ix7.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var bluetoothManager: BluetoothManager

    // Gestionnaire des permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            bluetoothManager.startDiscovery()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialiser le gestionnaire Bluetooth
        bluetoothManager = BluetoothManager(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TrackerApp(bluetoothManager)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.cleanup()
    }

    /**
     * Demande les permissions nécessaires
     */
    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            bluetoothManager.startDiscovery()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TrackerApp(bluetoothManager: BluetoothManager) {
        var selectedTab by remember { mutableIntStateOf(0) }

        // Observer les états du Bluetooth
        val discoveredDevices by bluetoothManager.discoveredDevices.collectAsState()
        val connectionState by bluetoothManager.connectionState.collectAsState()
        val receivedData by bluetoothManager.receivedData.collectAsState()

        Column {
            // Navigation en haut
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Connexion") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Informations") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Logs") }
                )
            }

            // Contenu selon l'onglet sélectionné
            when (selectedTab) {
                0 -> BluetoothConnectionScreen(
                    bluetoothManager = bluetoothManager,
                    discoveredDevices = discoveredDevices,
                    connectionState = connectionState,
                    onRequestPermissions = { requestPermissions() }
                )
                1 -> {
                    val scooterData = receivedData ?: ScooterData()
                    InformationScreen(
                        scooterData = scooterData,
                        isConnected = connectionState == ConnectionState.CONNECTED
                    )
                }
                2 -> LogScreen()
            }
        }
    }

    @Composable
    fun BluetoothConnectionScreen(
        bluetoothManager: BluetoothManager,
        discoveredDevices: List<BluetoothDeviceInfo>,
        connectionState: ConnectionState,
        onRequestPermissions: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // État de connexion
            ConnectionStatusCard(connectionState)

            // Vérifier si le Bluetooth est activé
            if (!bluetoothManager.isBluetoothEnabled()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = "Bluetooth désactivé. Veuillez l'activer dans les paramètres.",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Boutons de contrôle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onRequestPermissions() },
                        enabled = connectionState != ConnectionState.SCANNING,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (connectionState == ConnectionState.SCANNING) "Recherche..." else "Rechercher")
                    }

                    Button(
                        onClick = { bluetoothManager.stopDiscovery() },
                        enabled = connectionState == ConnectionState.SCANNING,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Arrêter")
                    }
                }

                if (connectionState == ConnectionState.CONNECTED) {
                    Button(
                        onClick = { bluetoothManager.disconnect() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Déconnecter", color = Color.White)
                    }
                }

                // Liste des appareils découverts
                if (discoveredDevices.isNotEmpty()) {
                    Text(
                        text = "Appareils trouvés:",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    LazyColumn {
                        items(discoveredDevices) { device ->
                            DeviceCard(
                                device = device,
                                onConnect = { bluetoothManager.connectToDevice(device.address) },
                                isConnectable = connectionState == ConnectionState.DISCONNECTED
                            )
                        }
                    }
                } else if (connectionState == ConnectionState.SCANNING) {
                    Card {
                        Text(
                            text = "Recherche d'appareils M0Robot en cours...",
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ConnectionStatusCard(connectionState: ConnectionState) {
        val (statusText, statusColor) = when (connectionState) {
            ConnectionState.DISCONNECTED -> "Déconnecté" to Color.Gray
            ConnectionState.SCANNING -> "Recherche en cours..." to Color.Blue
            ConnectionState.CONNECTING -> "Connexion..." to Color.Orange
            ConnectionState.CONNECTED -> "Connecté" to Color.Green
            ConnectionState.ERROR -> "Erreur de connexion" to Color.Red
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f))
        ) {
            Text(
                text = "État: $statusText",
                modifier = Modifier.padding(16.dp),
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    fun DeviceCard(
        device: BluetoothDeviceInfo,
        onConnect: () -> Unit,
        isConnectable: Boolean
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = device.name ?: "Appareil inconnu",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = device.address,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = onConnect,
                    enabled = isConnectable
                ) {
                    Text("Connecter")
                }
            }
        }
    }

    @Composable
    fun InformationScreen(
        scooterData: ScooterData,
        isConnected: Boolean
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // État de connexion
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) Color.Green.copy(alpha = 0.1f)
                    else Color.Red.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = if (isConnected) "Scooter connecté" else "Scooter déconnecté",
                    modifier = Modifier.padding(16.dp),
                    color = if (isConnected) Color.Green else Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            if (isConnected) {
                // Données principales
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Données en temps réel",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        InfoRow("Vitesse actuelle", Utils.formatSpeed(scooterData.speed))
                        InfoRow("Batterie restante", "${scooterData.battery}%")
                        InfoRow("Tension", Utils.formatVoltage(scooterData.voltage))
                        InfoRow("Courant", Utils.formatCurrent(scooterData.current))
                        InfoRow("Puissance", Utils.formatPower(scooterData.power))
                        InfoRow("Température", Utils.formatTemperature(scooterData.temperature))
                        InfoRow("Kilométrage total", Utils.formatDistance(scooterData.odometer))
                        InfoRow("Temps de conduite", scooterData.totalRideTime)
                        InfoRow("Source de données", scooterData.dataSource)
                    }
                }

                // Données système
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Informations système",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        InfoRow("État batterie", scooterData.batteryState.toString())
                        InfoRow("Codes d'erreur", scooterData.errorCodes.toString())
                        InfoRow("Codes d'avertissement", scooterData.warningCodes.toString())
                        InfoRow("Version firmware", scooterData.firmwareVersion)
                        InfoRow("Version Bluetooth", scooterData.bluetoothVersion)
                        InfoRow("Version app", scooterData.appVersion)
                    }
                }

                // Données brutes si disponibles
                scooterData.rawData?.let { rawData ->
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Données brutes",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Hex: ${Utils.bytesToHex(rawData)}",
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            } else {
                Card {
                    Text(
                        text = "Connectez-vous à un scooter M0Robot pour voir les informations de télémétrie.",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    @Composable
    fun InfoRow(label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    @Composable
    fun LogScreen() {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Logs système",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Les logs détaillés sont disponibles dans Logcat (Android Studio) avec le tag 'BluetoothManager'",
                    fontSize = 14.sp
                )
            }
        }
    }
}