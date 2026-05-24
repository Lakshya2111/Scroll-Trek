package com.example.scrolltrek.ui.common

import kotlin.math.roundToInt

object DistanceFormatter {
    fun format(meters: Double): String = when {
        meters < 1.0    -> "${(meters * 100).roundToInt()} cm"
        meters < 1000.0 -> "${meters.roundToInt()} m"
        meters < 10000.0  -> String.format(java.util.Locale.US, "%.1f km", meters / 1000.0)
        else            -> String.format(java.util.Locale.US, "%.0f km", meters / 1000.0)
    }
}
