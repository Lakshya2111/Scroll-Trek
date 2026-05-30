package com.example.scrolltrek.ui.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrolltrek.data.model.LandmarkWithStatus
import com.example.scrolltrek.ui.common.DistanceFormatter
import com.example.scrolltrek.ui.common.TierBadge
import com.example.scrolltrek.ui.home.getLandmarkEmoji
import com.example.scrolltrek.ui.theme.Accent

@Composable
fun LandmarkCard(
    landmarkWithStatus: LandmarkWithStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val landmark = landmarkWithStatus.landmark
    val isUnlocked = landmarkWithStatus.isUnlocked

    if (isUnlocked) {
        // High-Fidelity Unlocked Gold Achievement Card
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp)
                .clickable { onClick() }
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFD700), Color(0xFFFFA000))
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFDF0), // Very soft cream gold
                                Color(0xFFFFF3CD)  // Soft gold
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = "Unlocked",
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ACHIEVEMENT UNLOCKED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC67C00),
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column {
                            Text(
                                text = landmark.name,
                                color = Color(0xFF1A1A2E),
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${landmark.location} • Unlocked!",
                                color = Color(0xFF1A1A2E).copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = Color(0xFFFFE082),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getLandmarkEmoji(landmark.id),
                            fontSize = 46.sp
                        )
                    }
                }
            }
        }
    } else {
        // High-Fidelity Premium Locked Card
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Locked",
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "LOCKED ROADMAP",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column {
                            Text(
                                text = landmark.name,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Target: ${DistanceFormatter.format(landmark.distanceMeters)}",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getLandmarkEmoji(landmark.id),
                            fontSize = 46.sp,
                            modifier = Modifier.graphicsLayer { alpha = 0.4f }
                        )
                    }
                }
            }
        }
    }
}
