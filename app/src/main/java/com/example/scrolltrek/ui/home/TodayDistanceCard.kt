package com.example.scrolltrek.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import java.util.Locale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrolltrek.data.model.DailySummary
import com.example.scrolltrek.data.model.StreakState
import com.example.scrolltrek.ui.common.DistanceFormatter
import com.example.scrolltrek.ui.theme.Accent
import com.example.scrolltrek.ui.theme.Primary
import androidx.compose.ui.platform.LocalContext

@Composable
fun TodayDistanceCard(
    todaySummary: DailySummary,
    streakState: StreakState,
    lifetimeM: Double,
    isServiceRunning: Boolean,
    trackingEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val cleanAppName = remember(todaySummary.topApp) {
        if (todaySummary.topApp.isBlank()) "None"
        else if (todaySummary.topApp == "android") "Android System"
        else {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(todaySummary.topApp, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                todaySummary.topApp.substringAfterLast(".").replaceFirstChar { it.uppercase() }
            }
        }
    }

    // Active scrolling energy spent estimation: ~0.05 kcal burned per meter of scrolling
    val kcal = todaySummary.totalDistanceM * 0.05
    val energyText = String.format(Locale.US, "%.1f kcal", kcal)

    val goalM = todaySummary.goalMeters.toDouble().coerceAtLeast(1.0)
    val progressFraction = (todaySummary.totalDistanceM / goalM).toFloat().coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Circular Progress Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY'S TREK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statusColor = if (!trackingEnabled) Color(0xFFFFB74D) else if (isServiceRunning) Color(0xFF81C784) else Color(0xFFE57373)
                        val statusText = if (!trackingEnabled) "PAUSED" else if (isServiceRunning) "ACTIVE" else "STOPPED"

                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(color = statusColor, shape = androidx.compose.foundation.shape.CircleShape)
                        )
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Premium Circular Progress Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(210.dp)
                ) {
                    CircularProgressIndicator(
                        progress = progressFraction,
                        modifier = Modifier.size(200.dp),
                        color = Accent,
                        strokeWidth = 12.dp,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        strokeCap = StrokeCap.Round
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Today's Trek",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = DistanceFormatter.format(todaySummary.totalDistanceM),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "meters scrolled today",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (progressFraction >= 1f) "Goal Reached! 🎉" else "Keep Trekking!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Accent
                        )
                    }
                }

                Text(
                    text = "Lifetime Total: ${DistanceFormatter.format(lifetimeM)}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                if (expanded) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Goal Target", color = Color.Gray, fontSize = 11.sp)
                            Text(
                                text = "${todaySummary.goalMeters.toInt()} m",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Top Application", color = Color.Gray, fontSize = 11.sp)
                            Text(
                                text = cleanAppName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Side-by-Side Streak & Active Time Metrics Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Streak Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "STREAK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "${streakState.currentStreak} days",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Longest Streak: ${streakState.longestStreak}d",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }

            // Scroll Energy Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = "Scroll Energy",
                            tint = Accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "SCROLL ENERGY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = energyText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Est. energy spent",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
