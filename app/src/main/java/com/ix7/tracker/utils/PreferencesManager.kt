package com.ix7.tracker.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestionnaire des préférences utilisateur
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ix7_tracker_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        // Clés des préférences
        private const val KEY_LAST_CONNECTED_DEVICE = "last_connected_device"
        private const val KEY_AUTO_CONNECT_ENABLED = "auto_connect_enabled"
        private const val KEY_SCAN_TIMEOUT = "scan_timeout"
        private const val KEY_CONNECTION_TIMEOUT = "connection_timeout"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_SOUND_ENABLED = "sound_enabled"

        // Valeurs par défaut
        private const val DEFAULT_SCAN_TIMEOUT = 30000L
        private const val DEFAULT_CONNECTION_TIMEOUT = 10000L
        private const val DEFAULT_AUTO_CONNECT = false
        private const val DEFAULT_KEEP_SCREEN_ON = false
        private const val DEFAULT_VIBRATION = true
        private const val DEFAULT_SOUND = true
    }

    // Dernier appareil connecté
    var lastConnectedDevice: String
        get() = prefs.getString(KEY_LAST_CONNECTED_DEVICE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_CONNECTED_DEVICE, value).apply()

    // Connexion automatique
    var autoConnectEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT_ENABLED, DEFAULT_AUTO_CONNECT)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT_ENABLED, value).apply()

    // Timeout de scan (en millisecondes)
    var scanTimeout: Long
        get() = prefs.getLong(KEY_SCAN_TIMEOUT, DEFAULT_SCAN_TIMEOUT)
        set(value) = prefs.edit().putLong(KEY_SCAN_TIMEOUT, value).apply()

    // Timeout de connexion (en millisecondes)
    var connectionTimeout: Long
        get() = prefs.getLong(KEY_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT)
        set(value) = prefs.edit().putLong(KEY_CONNECTION_TIMEOUT, value).apply()

    // Mode thème (0 = auto, 1 = clair, 2 = sombre)
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    // Garder l'écran allumé
    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEY_KEEP_SCREEN_ON, DEFAULT_KEEP_SCREEN_ON)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()

    // Vibrations activées
    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, DEFAULT_VIBRATION)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

    // Sons activés
    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    /**
     * Sauvegarde les informations de connexion réussie
     */
    fun saveSuccessfulConnection(deviceAddress: String) {
        lastConnectedDevice = deviceAddress
    }

    /**
     * Réinitialise toutes les préférences
     */
    fun resetAllPreferences() {
        prefs.edit().clear().apply()
    }

    /**
     * Exporte les préférences en format texte
     */
    fun exportPreferences(): String {
        val builder = StringBuilder()
        builder.appendLine("=== IX7TrackerPro - Préférences ===")
        builder.appendLine("Dernier appareil: $lastConnectedDevice")
        builder.appendLine("Connexion auto: $autoConnectEnabled")
        builder.appendLine("Timeout scan: ${scanTimeout}ms")
        builder.appendLine("Timeout connexion: ${connectionTimeout}ms")
        builder.appendLine("Mode thème: $themeMode")
        builder.appendLine("Écran allumé: $keepScreenOn")
        builder.appendLine("Vibrations: $vibrationEnabled")
        builder.appendLine("Sons: $soundEnabled")
        return builder.toString()
    }
}