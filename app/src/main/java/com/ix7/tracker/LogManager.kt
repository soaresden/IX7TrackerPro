package com.ix7.tracker

import android.util.Log

object LogManager {
    private const val TAG = "IX7Tracker"

    fun logInfo(message: String) {
        Log.i(TAG, message)
    }

    fun logError(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
    }

    fun logWarning(message: String) {
        Log.w(TAG, message)
    }

    fun logDebug(message: String) {
        Log.d(TAG, message)
    }
}