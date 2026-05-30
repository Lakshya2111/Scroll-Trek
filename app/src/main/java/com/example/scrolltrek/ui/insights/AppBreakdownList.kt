package com.example.scrolltrek.ui.insights

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrolltrek.data.db.dao.AppScrollTotal
import com.example.scrolltrek.ui.common.DistanceFormatter
import com.example.scrolltrek.ui.theme.Accent

@Composable
fun getAppName(packageName: String, context: Context): String {
    if (packageName.isBlank()) return "Unknown App"
    if (packageName == "android") return "Android System"
    return remember(packageName) {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
    }
}

@Composable
fun AppBreakdownList(
    appBreakdown: List<AppScrollTotal>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "TOP CONTRIBUTING APPS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (appBreakdown.isEmpty()) {
                Text(
                    text = "No app scrolling recorded yet. Start scrolling to see the breakdown!",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                val totalDistance = appBreakdown.map { it.total }.sum().coerceAtLeast(1.0)
                val sortedList = appBreakdown.sortedByDescending { it.total }.take(6)

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    sortedList.forEachIndexed { index, appTotal ->
                        val cleanAppName = getAppName(appTotal.sourcePackage, context)
                        val percentage = (appTotal.total / totalDistance).toFloat()

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Styled Rank Badge
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    color = if (index < 3) Accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (index < 3) Accent else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Clean App Name
                                        Text(
                                            text = cleanAppName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Separated Distance Capsule and Percentage Value
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Distance Capsule Pill
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = Accent.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(99.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = DistanceFormatter.format(appTotal.total),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Accent
                                            )
                                        }

                                        // Bold Percentage Share
                                        Text(
                                            text = String.format("%d%%", (percentage * 100).toInt()),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Sleek linear progress bar
                                LinearProgressIndicator(
                                    progress = { percentage },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(3.dp)
                                        ),
                                    color = Accent,
                                    trackColor = Color.Transparent,
                                    strokeCap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
