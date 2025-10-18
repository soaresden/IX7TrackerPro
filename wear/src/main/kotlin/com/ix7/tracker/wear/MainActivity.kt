package com.ix7.tracker.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ix7.tracker.wear.bluetooth.WearScooterManager
import com.ix7.tracker.wear.bluetooth.LockManager
import com.ix7.tracker.wear.ui.screens.ScannerScreen
import com.ix7.tracker.wear.ui.screens.ControlScreen
import android.util.Log

class MainActivity : ComponentActivity() {

    private lateinit var scooterManager: WearScooterManager
    private lateinit var lockManager: LockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scooterManager = WearScooterManager(this)
        lockManager = LockManager(this)

        Log.d("MAIN", "✅ Gestionnaires initialisés")

        setContent {
            val currentScreen = remember { mutableStateOf("scanner") }

            when (currentScreen.value) {
                "scanner" -> {
                    ScannerScreen(
                        scooterManager = scooterManager,
                        context = this@MainActivity,
                        onConnected = {
                            Log.d("MAIN", "Navigation vers ControlScreen")
                            currentScreen.value = "control"
                        }
                    )
                }
                "control" -> {
                    ControlScreen(
                        scooterManager = scooterManager,
                        lockManager = lockManager,
                        context = this@MainActivity,
                        scooterName = "M0Robot",
                        onBackClick = {
                            Log.d("MAIN", "Retour au scanner")
                            currentScreen.value = "scanner"
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scooterManager.disconnect()
        lockManager.disconnect()
        scooterManager.cleanup()
        lockManager.cleanup()
        Log.d("MAIN", "Cleanup terminé")
    }
}