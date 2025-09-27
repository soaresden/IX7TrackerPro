package com.ix7.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class MainActivity : ComponentActivity() {

    private lateinit var bluetoothManager: BluetoothManager

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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerApp(bluetoothManager: BluetoothManager) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Observer les états du Bluetooth - UTILISE LES BONNES PROPRIÉTÉS
    val discoveredDevices by bluetoothManager.discoveredDevices.collectAsState()
    val connectionState by bluetoothManager.connectionState.collectAsState()
    val receivedData by bluetoothManager.receivedData.collectAsState()

    // Démarrer la découverte au lancement si pas connecté
    LaunchedEffect(Unit) {
        if (connectionState == ConnectionState.DISCONNECTED) {
            bluetoothManager.startDiscovery()
        }
    }

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
        }

        // Contenu selon l'onglet sélectionné
        when (selectedTab) {
            0 -> BluetoothConnectionScreen(
                bluetoothManager = bluetoothManager,
                discoveredDevices = discoveredDevices,
                connectionState = connectionState
            )
            1 -> {
                val scooterData = receivedData ?: ScooterData()
                InformationScreen(
                    scooterData = scooterData,
                    isConnected = connectionState == ConnectionState.CONNECTED
                )
            }
        }
    }
}

@Composable
fun BluetoothConnectionScreen(
    bluetoothManager: BluetoothManager,
    discoveredDevices: List<BluetoothDeviceInfo>,
    connectionState: ConnectionState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // État de connexion
        ConnectionStatusCard(connectionState)

        // Boutons de contrôle - UTILISE LES BONNES MÉTHODES
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { bluetoothManager.startDiscovery() },
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
        }
    }
}

@Composable
fun ConnectionStatusCard(connectionState: ConnectionState) {
    val (statusText, statusColor) = when (connectionState) {
        ConnectionState.DISCONNECTED -> "Déconnecté" to Color.Gray
        ConnectionState.SCANNING -> "Recherche en cours..." to Color.Blue
        ConnectionState.CONNECTING -> "Connexion..." to Color(0xFFFFA500)
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