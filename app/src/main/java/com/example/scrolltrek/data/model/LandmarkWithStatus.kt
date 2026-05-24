package com.example.scrolltrek.data.model

data class LandmarkWithStatus(
    val landmark: Landmark,
    val isUnlocked: Boolean,
    val unlockedAtMs: Long? = null
)
