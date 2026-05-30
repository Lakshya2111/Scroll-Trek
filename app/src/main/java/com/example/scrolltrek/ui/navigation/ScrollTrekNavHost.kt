package com.example.scrolltrek.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.scrolltrek.ui.onboarding.OnboardingFlow
import com.example.scrolltrek.ui.milestone.MilestoneRevealScreen
import com.example.scrolltrek.ui.journey.LandmarkDetailSheet
import com.example.scrolltrek.ui.settings.SettingsScreen
import com.example.scrolltrek.ui.settings.AppExclusionScreen

@Composable
fun ScrollTrekNavHost(navController: NavHostController) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE) }
    val onboardingComplete = remember { sharedPreferences.getBoolean("onboarding_complete", false) }

    NavHost(
        navController = navController,
        startDestination = if (onboardingComplete) "home" else "onboarding"
    ) {
        composable("onboarding") {
            OnboardingFlow(onComplete = {
                sharedPreferences.edit().putBoolean("onboarding_complete", true).apply()
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }

        // Main scaffold tabs
        composable("home") {
            MainScaffold(navController = navController, startTab = Tab.HOME)
        }
        composable("journey") {
            MainScaffold(navController = navController, startTab = Tab.JOURNEY)
        }
        composable("insights") {
            MainScaffold(navController = navController, startTab = Tab.INSIGHTS)
        }

        // Modal/Full-screen destinations (no bottom nav)
        composable(
            route = "milestone/{landmarkId}",
            arguments = listOf(navArgument("landmarkId") { type = NavType.StringType })
        ) { backStackEntry ->
            val landmarkId = backStackEntry.arguments?.getString("landmarkId") ?: ""
            MilestoneRevealScreen(navController = navController, landmarkId = landmarkId)
        }

        composable(
            route = "landmark_detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            LandmarkDetailSheet(navController = navController, landmarkId = id)
        }

        composable("settings") {
            SettingsScreen(navController = navController)
        }

        composable("app_exclusions") {
            AppExclusionScreen(navController = navController)
        }
    }
}
