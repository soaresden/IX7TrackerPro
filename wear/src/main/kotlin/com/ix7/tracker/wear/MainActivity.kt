package com.ix7.tracker.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ix7.tracker.wear.bluetooth.WearScooterManager
import com.ix7.tracker.wear.bluetooth.LockManager
import com.ix7.tracker.wear.ui.screens.SplashScreen
import com.ix7.tracker.wear.ui.screens.ScannerScreen
import com.ix7.tracker.wear.ui.screens.ControlScreen
import com.ix7.tracker.wear.ui.screens.TripRecorderScreen
import android.util.Log

class MainActivity : ComponentActivity() {

    private lateinit var scooterManager: WearScooterManager
    private lateinit var lockManager: LockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scooterManager = WearScooterManager(this)
        lockManager = LockManager(this)

        setContent {
            val currentScreen = remember { mutableStateOf("splash") }

            when (currentScreen.value) {
                "splash" -> {
                    SplashScreen(
                        onSplashComplete = {
                            currentScreen.value = "scanner"
                        }
                    )
                }
                "scanner" -> {
                    ScannerScreen(
                        scooterManager = scooterManager,
                        context = this@MainActivity,
                        onConnected = {
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
                        lockName = "Iphone9",
                        onBackClick = {
                            currentScreen.value = "scanner"
                        },
                        onTripRecorder = {
                            currentScreen.value = "trip_recorder"
                        }
                    )
                }
                "trip_recorder" -> {
                    TripRecorderScreen(
                        onBackClick = {
                            currentScreen.value = "control"
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
    }
}