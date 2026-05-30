package com.example.scrolltrek

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.scrolltrek.ui.navigation.ScrollTrekNavHost
import com.example.scrolltrek.ui.theme.ScrollTrekTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var landmarkIdToNavigate by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val sharedPreferences = remember { context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE) }
            var appTheme by remember {
                mutableStateOf(sharedPreferences.getString("app_theme", "system") ?: "system")
            }

            DisposableEffect(sharedPreferences) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == "app_theme") {
                        appTheme = sharedPreferences.getString("app_theme", "system") ?: "system"
                    }
                }
                sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            ScrollTrekTheme(themeSetting = appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    val navController = rememberNavController()

                    val navigateId = landmarkIdToNavigate
                    LaunchedEffect(navigateId) {
                        if (navigateId != null) {
                            navController.navigate("landmark_detail/$navigateId")
                            landmarkIdToNavigate = null
                        }
                    }

                    ScrollTrekNavHost(
                        navController = navController
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            if (it.hasExtra("KEY_NAVIGATE_LANDMARK_ID")) {
                val landmarkId = it.getStringExtra("KEY_NAVIGATE_LANDMARK_ID")
                if (landmarkId != null) {
                    landmarkIdToNavigate = landmarkId
                }
                it.removeExtra("KEY_NAVIGATE_LANDMARK_ID")
            }
        }
    }
}