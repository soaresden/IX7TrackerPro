package com.ix7.tracker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ix7.tracker.bluetooth.BluetoothManagerImpl
import com.ix7.tracker.bluetooth.BluetoothRepository
import com.ix7.tracker.bluetooth.PermissionHelper
import com.ix7.tracker.ui.screens.MainScreen
import com.ix7.tracker.ui.theme.IX7TrackerTheme
import com.ix7.tracker.utils.LogManager

class MainActivity : ComponentActivity() {

    private lateinit var bluetoothManager: BluetoothRepository
    private lateinit var logManager: LogManager

    // Gestionnaire de permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            logManager.info("Toutes les permissions accordées", "PERMISSIONS")
            initializeBluetooth()
        } else {
            logManager.warning("Permissions manquantes: ${permissions.filterValues { !it }.keys}", "PERMISSIONS")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialiser les gestionnaires
        bluetoothManager = BluetoothManagerImpl(this)
        logManager = LogManager.getInstance()

        logManager.info("Application démarrée", "MAIN")

        // Vérifier et demander les permissions
        checkAndRequestPermissions()

        setContent {
            IX7TrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        bluetoothManager = bluetoothManager,
                        logManager = logManager
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.cleanup()
        logManager.info("Application fermée", "MAIN")
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = PermissionHelper.getMissingPermissions(this)

        if (missingPermissions.isEmpty()) {
            logManager.info("Toutes les permissions sont accordées", "PERMISSIONS")
            initializeBluetooth()
        } else {
            logManager.info("Demande de permissions: $missingPermissions", "PERMISSIONS")
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun initializeBluetooth() {
        val result = bluetoothManager.initialize()
        if (result.isSuccess) {
            logManager.info("Bluetooth initialisé avec succès", "BLUETOOTH")
        } else {
            logManager.error("Échec d'initialisation Bluetooth: ${result.exceptionOrNull()?.message}", "BLUETOOTH")
        }
    }
}