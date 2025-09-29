package com.ix7.tracker.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.ix7.tracker.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BluetoothManagerImpl(private val context: Context) : BluetoothRepository {

    private val scanner = BluetoothScanner(context) { devices ->
        _discoveredDevices.value = devices}

    private val connector = BluetoothConnector(context) { state ->
        _connectionState.value = state
    }

    private val dataHandler = BluetoothDataHandler { data ->
        _scooterData.value = data
    }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    private val _scooterData = MutableStateFlow(ScooterData())
    private val _isScanning = MutableStateFlow(false)

    // Implémentation des états observables de BluetoothRepository
    override val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices
    override val connectionState: StateFlow<ConnectionState> = _connectionState
    override val scooterData: StateFlow<ScooterData> = _scooterData
    override val isScanning: StateFlow<Boolean> = _isScanning

    // Implémentation du Scan BLE de BluetoothRepository
    override suspend fun startScan(): Result<Unit> {
        _isScanning.value = true
        return scanner.startScan()
    }

    override suspend fun stopScan(): Result<Unit> {
        _isScanning.value = false
        return scanner.stopScan()
    }

    // Implémentation des méthodes de Connexion de BluetoothRepository

    /**
     * Se connecte à un appareil Bluetooth en utilisant son adresse MAC.
     */
    override suspend fun connectToDevice(address: String): Result<Unit> {
        // Logique pour se connecter en utilisant l'adresse
        // Vous devrez peut-être ajuster votre BluetoothConnector pour gérer cela,
        // ou obtenir l'objet BluetoothDevice à partir de l'adresse d'abord.
        // Pour l'instant, je vais supposer que votre BluetoothConnector peut prendre une adresse.
        return connector.connect(address) { data ->
            dataHandler.handleData(data)
        }
    }

    /**
     * Se connecte à un appareil Bluetooth en utilisant un objet BluetoothDevice.
     */
    override suspend fun connect(device: BluetoothDevice): Result<Unit> {
        // Logique pour se connecter en utilisant l'objet BluetoothDevice
        // Votre BluetoothConnector prend probablement une adresse MAC, donc nous l'extrayons.
        return connector.connect(device.address) { data ->
            dataHandler.handleData(data)
        }
    }

    override suspend fun disconnect(): Result<Unit> {
        return connector.disconnect()
    }

    // Implémentation des Commandes de BluetoothRepository
    override suspend fun unlockScooter(): Result<Unit> {
        // Exemple de commande. Adaptez le tableau de bytes à votre protocole.
        val unlockCommand = "UNLOCK_COMMAND_BYTES".toByteArray() // REMPLACEZ CECI
        return sendCommand(unlockCommand)
    }

    override suspend fun sendCommand(command: ByteArray): Result<Unit> {
        return connector.sendCommand(command)
    }

    // Implémentation de la Gestion du cycle de vie de BluetoothRepository
    override fun initialize(): Result<Unit> {
        // Vérifier si Bluetooth est supporté et activé pourrait être une bonne idée ici aussi.
        // Pour l'instant, on se fie à ce que scanner.initialize() pourrait faire.
        return scanner.initialize()
    }

    override fun cleanup() {
        scanner.cleanup()
        connector.cleanup()
        // Vous pourriez aussi vouloir réinitialiser vos StateFlows ici si nécessaire.
    }

    // Implémentation des Utilitaires de BluetoothRepository
    override fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter?.isEnabled == true
    }

    override fun hasNecessaryPermissions(): Boolean {
        // Assurez-vous que PermissionHelper est correctement implémenté
        return PermissionHelper.hasAllBluetoothPermissions(context)
    }

    override fun clearDiscoveredDevices() {
        _discoveredDevices.value = emptyList()
    }

    // Vous pouvez supprimer la méthode `connectWithDetails` si elle n'est plus nécessaire,
    // car l'interface définit déjà deux façons de se connecter.
    // suspend fun connectWithDetails(deviceName: String, deviceAddress: String): Result<Unit> {
    //     return connectToDevice(deviceAddress)
    // }
}
