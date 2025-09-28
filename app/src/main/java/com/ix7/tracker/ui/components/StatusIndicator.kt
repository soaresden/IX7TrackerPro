package com.ix7.tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.core.ConnectionState

@Composable
fun StatusIndicator(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    val (color, isAnimated) = when (connectionState) {
        ConnectionState.DISCONNECTED -> Color.Gray to false
        ConnectionState.SCANNING -> MaterialTheme.colorScheme.primary to true
        ConnectionState.CONNECTING -> Color(0xFFFF9800) to true
        ConnectionState.CONNECTED -> Color(0xFF4CAF50) to false
        ConnectionState.ERROR -> MaterialTheme.colorScheme.error to false
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Indicateur visuel
        if (isAnimated) {
            AnimatedStatusDot(color = color)
        } else {
            StaticStatusDot(color = color)
        }

        // Texte (optionnel)
        if (showText) {
            Text(
                text = when (connectionState) {
                    ConnectionState.DISCONNECTED -> "Déconnecté"
                    ConnectionState.SCANNING -> "Recherche..."
                    ConnectionState.CONNECTING -> "Connexion..."
                    ConnectionState.CONNECTED -> "Connecté"
                    ConnectionState.ERROR -> "Erreur"
                },
                color = color,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun AnimatedStatusDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "status")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = size.minDimension / 2
            )
        }
    }
}

@Composable
private fun StaticStatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = color,
                radius = size.minDimension / 2
            )
        }
    }
}