package com.example.scrolltrek.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.milestoneUnlock.collect { landmark ->
            navController.navigate("milestone/${landmark.id}")
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            isServiceRunning = ServiceUtils.isAccessibilityServiceEnabled(context)
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
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
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
