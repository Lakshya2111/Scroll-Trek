package com.example.scrolltrek.data.model

data class LiveSessionState(
    val sessionDistanceM: Double,
    val sessionStartMs: Long,
    val paceMetersPerMinute: Double,
    val currentApp: String
)
