package com.example.scrolltrek.data.model

data class Landmark(
    val id: String,
    val name: String,
    val location: String,
    val distanceMeters: Double,
    val tier: LandmarkTier,
    val orientation: LandmarkOrientation,
    val funFact: String,
    val shareMessage: String,
    val illustrationRes: String,
    val cardGradientStart: String,
    val cardGradientEnd: String
)
