package com.example.scrolltrek.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.scrolltrek.ui.home.WeeklyBarChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val weeklyAnalytics by viewModel.weeklyAnalytics.collectAsStateWithLifecycle()
    val appBreakdown by viewModel.appBreakdown.collectAsStateWithLifecycle()
    val hourlyHeatmap by viewModel.hourlyHeatmap.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Insights",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                WeeklyBarChart(
                    weeklyAnalytics = weeklyAnalytics,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                ScrollHeatmap(
                    hourlyData = hourlyHeatmap,
                    onHourClick = { hour ->
                        val dateKey = java.time.LocalDate.now().toString()
                        viewModel.getAppBreakdownForHour(dateKey, hour)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                AppBreakdownList(
                    appBreakdown = appBreakdown,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
