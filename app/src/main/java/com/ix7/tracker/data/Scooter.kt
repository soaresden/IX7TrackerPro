package com.ix7.tracker.data

data class Scooter(
    val name: String,
    val address: String,
    val rssi: Int,
    val type: Int = 1
)