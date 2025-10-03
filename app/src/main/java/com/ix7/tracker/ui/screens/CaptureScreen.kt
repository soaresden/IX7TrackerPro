package com.ix7.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

data class CapturedCommand(
    val timestamp: String,
    val hex: String,
    val type: String
)

@Composable
fun CaptureScreen() {
    var capturedCommands by remember { mutableStateOf(listOf<CapturedCommand>()) }
    var isCapturing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Capture des commandes physiques", style = MaterialTheme.typography.headlineMedium)
        Text("Appuie sur les boutons de la trottinette pour voir les commandes", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (isCapturing) {
                    capturedCommands = emptyList()
                }
                isCapturing = !isCapturing
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isCapturing) "⏹ Arrêter capture" else "▶ Démarrer capture")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(capturedCommands) { cmd ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(cmd.timestamp, style = MaterialTheme.typography.bodySmall)
                        Text(cmd.hex, style = MaterialTheme.typography.bodyMedium)
                        Text(cmd.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}