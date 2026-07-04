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
) {
    val fullShareMessage: String
        get() = "$shareMessage\n\nDownload scrollTrek: https://play.google.com/store/apps/details?id=io.github.lakshya2111.scrolltrek"
}

