package com.example.scrolltrek.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.ui.graphics.vector.ImageVector

enum class Tab(val icon: ImageVector, val label: String, val route: String) {
    HOME(Icons.Rounded.Home, "Home", "home"),
    JOURNEY(Icons.Rounded.Map, "Journey", "journey"),
    INSIGHTS(Icons.Rounded.BarChart, "Insights", "insights")
}
