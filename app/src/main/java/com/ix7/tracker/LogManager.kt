package com.ix7.tracker

import android.util.Log

object LogManager {
    private const val TAG = "IX7Tracker"

    fun logInfo(message: String) {
        Log.i(TAG, message)
    }

    fun logError(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }

    fun logDebug(message: String) {
        Log.d(TAG, message)
    }
}