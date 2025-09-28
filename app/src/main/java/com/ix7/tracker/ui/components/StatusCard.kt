package com.ix7.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.ConnectionState

@Composable
fun StatusCard(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor, statusIcon) = when (connectionState) {
        ConnectionState.DISCONNECTED -> Triple(
            "Déconnecté",
            MaterialTheme.colorScheme.outline,
            null
        )
        ConnectionState.SCANNING -> Triple(
            "Recherche en cours...",
            MaterialTheme.colorScheme.primary,
            null
        )
        ConnectionState.CONNECTING -> Triple(
            "Connexion...",
            Color(0xFFFF9800),
            null
        )
        ConnectionState.CONNECTED -> Triple(
            "Connecté",
            Color(0xFF4CAF50),
            null
        )
        ConnectionState.ERROR -> Triple(
            "Erreur de connexion",
            MaterialTheme.colorScheme.error,
            null
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "•",
                color = statusColor
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "État de la connexion",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = statusText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Indicateur d'activité pour le scan et la connexion
            if (connectionState == ConnectionState.SCANNING || connectionState == ConnectionState.CONNECTING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = statusColor
                )
            }
        }
    }
}