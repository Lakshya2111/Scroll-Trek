package com.example.scrolltrek.ui.journey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: JourneyViewModel = hiltViewModel()
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val landmarks by viewModel.landmarks.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Your Journey",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("ALL", "LOCKED", "UNLOCKED").forEach { option ->
                    val isSelected = option == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(option) },
                        label = { Text(option, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (landmarks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No landmarks found for this category.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 24.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(landmarks, key = { _, item -> item.landmark.id }) { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left lane: Winding Trek/Roadmap Path
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                val isUnlocked = item.isUnlocked
                                val lineStroke = 4.dp
                                
                                Canvas(modifier = Modifier.fillMaxHeight().width(lineStroke)) {
                                    val h = size.height
                                    val w = size.width
                                    val strokePx = lineStroke.toPx()
                                    
                                    // Path from top to center
                                    if (index > 0) {
                                        drawLine(
                                            color = if (isUnlocked) Color(0xFFFFD4AF37) else Color.LightGray,
                                            start = Offset(w / 2, 0f),
                                            end = Offset(w / 2, h / 2),
                                            strokeWidth = strokePx,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                    
                                    // Path from center to bottom
                                    if (index < landmarks.size - 1) {
                                        val nextIsUnlocked = landmarks[index + 1].isUnlocked
                                        drawLine(
                                            color = if (nextIsUnlocked) Color(0xFFFFD4AF37) else Color.LightGray,
                                            start = Offset(w / 2, h / 2),
                                            end = Offset(w / 2, h),
                                            strokeWidth = strokePx,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                                
                                // Node circular checkpoint
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = if (isUnlocked) Color.White else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = CircleShape
                                        )
                                        .border(
                                            width = 3.dp,
                                            color = if (isUnlocked) Color(0xFFFFD4AF37) else Color.LightGray,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isUnlocked) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFC67C00)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = "Locked",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            
                            // Right lane: Landmark card details
                            Box(
                                modifier = Modifier.weight(1f)
                            ) {
                                LandmarkCard(
                                    landmarkWithStatus = item,
                                    onClick = {
                                        navController.navigate("landmark_detail/${item.landmark.id}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
