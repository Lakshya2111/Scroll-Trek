package com.example.scrolltrek.data.model

data class WeeklyAnalytics(
    val days: List<DailySummary>,    // last 7 days
    val totalDistanceM: Double,
    val averageDailyM: Double,
    val mostScrolledApp: String,
    val peakHour: Int                // 0–23
)
