package com.example.scrolltrek.ui.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.scrolltrek.ui.common.ServiceUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val todaySummary by viewModel.todaySummary.collectAsStateWithLifecycle()
    val landmarkProgress by viewModel.landmarkProgress.collectAsStateWithLifecycle()
    val streakState by viewModel.streakState.collectAsStateWithLifecycle()
    val weeklyAnalytics by viewModel.weekly.collectAsStateWithLifecycle()

    var isServiceRunning by remember { mutableStateOf(false) }
    var trackingEnabled by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.milestoneUnlock.collect { landmark ->
            navController.navigate("milestone/${landmark.id}")
        }
    }

    LaunchedEffect(Unit) {
        val sharedPreferences = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        while (true) {
            val serviceEnabled = ServiceUtils.isAccessibilityServiceEnabled(context)
            isServiceRunning = serviceEnabled
            if (serviceEnabled) {
                val prefVal = sharedPreferences.getBoolean("KEY_TRACKING_ENABLED", true)
                if (!prefVal) {
                    sharedPreferences.edit().putBoolean("KEY_TRACKING_ENABLED", true).commit()
                    trackingEnabled = true
                    try {
                        val serviceIntent = Intent(context, com.example.scrolltrek.tracking.ScrollTrackingAccessibilityService::class.java).apply {
                            action = "ACTION_ENABLE_TRACKING"
                        }
                        context.startService(serviceIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                trackingEnabled = sharedPreferences.getBoolean("KEY_TRACKING_ENABLED", true)
            }
            delay(1000)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Scroll Trek",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                TodayDistanceCard(
                    todaySummary = todaySummary,
                    streakState = streakState,
                    lifetimeM = landmarkProgress.lifetimeDistanceM,
                    isServiceRunning = isServiceRunning,
                    trackingEnabled = trackingEnabled,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            if (!trackingEnabled) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("⏸️", fontSize = 24.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Scroll Tracking Paused",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tracking is completely disabled. You can re-enable it anytime in Settings.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                LandmarkProgressArc(
                    progress = landmarkProgress,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            item {
                RecentUnlocksCarousel(
                    recentlyUnlocked = landmarkProgress.recentlyUnlocked,
                    onLandmarkClick = { landmark ->
                        navController.navigate("landmark_detail/${landmark.id}")
                    }
                )
            }

            item {
                WeeklyBarChart(
                    weeklyAnalytics = weeklyAnalytics,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}
