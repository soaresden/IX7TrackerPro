package com.ix7.tracker.ui.screens

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.data.Trip
import kotlinx.coroutines.launch

// ════════════════════════════════════════════════════════════════
// 🔄 SYNC WITH WEAR - COLLAPSABLE (PARAMÉTRE)
// ════════════════════════════════════════════════════════════════

@Composable
fun TripHistoryWearInterfaceScreen(
    trips: List<Trip>,
    context: Context,
    onSyncPhone: suspend (List<Trip>) -> Unit = { },
    onSyncWatch: suspend (List<Trip>) -> Unit = { }
) {
    var isExpanded by remember { mutableStateOf(true) }
    var showSyncToPhoneDialog by remember { mutableStateOf(false) }
    var showSyncToWatchDialog by remember { mutableStateOf(false) }
    var isSyncingToPhone by remember { mutableStateOf(false) }
    var isSyncingToWatch by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Dialog Sync vers téléphone
    if (showSyncToPhoneDialog) {
        SyncConfirmationDialogWear(
            title = "Synchroniser vers le téléphone ?",
            message = "Cela va synchroniser ${trips.size} trajets vers votre téléphone.",
            icon = "📱",
            onConfirm = {
                showSyncToPhoneDialog = false
                isSyncingToPhone = true
                scope.launch {
                    try {
                        onSyncPhone(trips)
                        syncMessage = "✅ Synchronisation téléphone réussie"
                        isSyncingToPhone = false
                    } catch (e: Exception) {
                        syncMessage = "❌ Erreur: ${e.message}"
                        isSyncingToPhone = false
                    }
                }
            },
            onDismiss = { showSyncToPhoneDialog = false }
        )
    }

    // Dialog Sync vers montre
    if (showSyncToWatchDialog) {
        SyncConfirmationDialogWear(
            title = "Synchroniser vers la montre ?",
            message = "Cela va envoyer ${trips.size} trajets à votre Wear OS.",
            icon = "⌚",
            onConfirm = {
                showSyncToWatchDialog = false
                isSyncingToWatch = true
                scope.launch {
                    try {
                        onSyncWatch(trips)
                        syncMessage = "✅ Synchronisation montre réussie"
                        isSyncingToWatch = false
                    } catch (e: Exception) {
                        syncMessage = "❌ Erreur: ${e.message}"
                        isSyncingToWatch = false
                    }
                }
            },
            onDismiss = { showSyncToWatchDialog = false }
        )
    }

    // Message de feedback
    if (syncMessage.isNotEmpty()) {
        LaunchedEffect(syncMessage) {
            kotlinx.coroutines.delay(3000)
            syncMessage = ""
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
    ) {
        Column(modifier = Modifier.padding(0.dp)) {
            // 🔽 HEADER CLICKABLE - Déplier/Replier
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp)
            ) {
                // Titre + Icône
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "🔄",
                        fontSize = 24.sp,
                        color = Color(0xFF0A84FF)
                    )
                    Column {
                        Text(
                            "🔄 Synchronisation",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "${trips.size} trajets",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Chevron animé
                Icon(
                    imageVector = if (isExpanded)
                        Icons.Rounded.KeyboardArrowUp
                    else
                        Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Replier" else "Déplier",
                    tint = Color(0xFF0A84FF),
                    modifier = Modifier.size(28.dp)
                )
            }

            // 📋 CONTENU COLLAPSABLE
            if (isExpanded) {
                HorizontalDivider(
                    color = Color.Gray.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    // Deux boutons côte à côte
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Bouton Téléphone
                        SyncButtonWear(
                            icon = "📱",
                            label = "Vers téléphone",
                            isLoading = isSyncingToPhone,
                            onClick = { showSyncToPhoneDialog = true },
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF0A84FF)
                        )

                        // Bouton Montre
                        SyncButtonWear(
                            icon = "⌚",
                            label = "Vers montre",
                            isLoading = isSyncingToWatch,
                            onClick = { showSyncToWatchDialog = true },
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF4CAF50)
                        )
                    }

                    // Message de feedback
                    if (syncMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (syncMessage.startsWith("✅")) Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    else Color.Red.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                syncMessage,
                                fontSize = 12.sp,
                                color = if (syncMessage.startsWith("✅")) Color(0xFF4CAF50) else Color.Red,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncButtonWear(
    icon: String,
    label: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0A84FF)
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.2f),
            contentColor = color,
            disabledContainerColor = Color.Gray.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        if (isLoading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = color
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text("Syncing...", fontSize = 9.sp, color = color.copy(alpha = 0.7f))
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SyncConfirmationDialogWear(
    title: String,
    message: String,
    icon: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF0A84FF).copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 28.sp)
            }
        },
        title = {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    message,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "⚠️ Assurez-vous que la connexion Bluetooth est active",
                    fontSize = 12.sp,
                    color = Color(0xFFFF9500)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF0A84FF))
            ) {
                Text("Synchroniser", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
            ) {
                Text("Annuler")
            }
        },
        containerColor = Color(0xFF2C2C2E),
        textContentColor = Color.White
    )
}