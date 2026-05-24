package com.example.scrolltrek.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.scrolltrek.ui.home.HomeScreen
import com.example.scrolltrek.ui.insights.InsightsScreen
import com.example.scrolltrek.ui.journey.JourneyScreen

@Composable
fun MainScaffold(
    navController: NavHostController,
    startTab: Tab
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = tab == startTab,
                        onClick = {
                            if (tab != startTab) {
                                navController.navigate(tab.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (startTab) {
            Tab.HOME -> HomeScreen(navController = navController, modifier = modifier)
            Tab.JOURNEY -> JourneyScreen(navController = navController, modifier = modifier)
            Tab.INSIGHTS -> InsightsScreen(navController = navController, modifier = modifier)
        }
    }
}
