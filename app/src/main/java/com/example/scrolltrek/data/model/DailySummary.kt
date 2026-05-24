package com.example.scrolltrek.data.model

data class DailySummary(
    val dateKey: String,             // "2026-05-24"
    val totalDistanceM: Double,
    val sessionCount: Int,
    val topApp: String,
    val topAppDistanceM: Double,
    val goalMeters: Float,           // 0 if no goal set
    val goalProgressFraction: Float  // 0.0–1.0+
)
