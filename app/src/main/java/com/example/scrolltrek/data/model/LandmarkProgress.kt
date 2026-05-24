package com.example.scrolltrek.data.model

data class LandmarkProgress(
    val currentLandmark: Landmark,
    val nextLandmark: Landmark,
    val lifetimeDistanceM: Double,
    val distanceToNextM: Double,
    val progressFraction: Float,     // 0.0–1.0
    val recentlyUnlocked: List<Landmark>  // unlocked in last 24h
)
