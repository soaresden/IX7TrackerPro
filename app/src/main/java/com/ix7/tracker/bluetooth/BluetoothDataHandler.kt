package com.ix7.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ix7.tracker.bluetooth.BluetoothRepository
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun findMode(frames: MutableMap<String, FrameMonitor>): String {
    frames["0x37"]?.decoded?.get("mode")?.value?.let { return it }
    frames["0x30"]?.decoded?.get("mode")?.value?.let { return it }
    return "❓ Non trouvé"
}

fun findLockState(frames: MutableMap<String, FrameMonitor>): String {
    return "❓ À chercher"
}

fun findLightState(frames: MutableMap<String, FrameMonitor>): String {
    return "❓ À chercher"
}