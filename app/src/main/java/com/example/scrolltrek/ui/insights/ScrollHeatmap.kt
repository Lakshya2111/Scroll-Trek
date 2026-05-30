package com.example.scrolltrek.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrolltrek.data.db.dao.AppScrollTotal
import com.example.scrolltrek.data.db.dao.HourlyTotal
import com.example.scrolltrek.ui.common.DistanceFormatter
import com.example.scrolltrek.ui.theme.Accent
import kotlinx.coroutines.launch

@Composable
fun ScrollHeatmap(
    hourlyData: List<HourlyTotal>,
    onHourClick: suspend (Int) -> List<AppScrollTotal>,
    modifier: Modifier = Modifier
) {
    var selectedHour by remember { mutableStateOf<Int?>(null) }
    var selectedHourApps by remember { mutableStateOf<List<AppScrollTotal>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "HOURLY ACTIVE HEATMAP (TAP HOUR TO EXPLORE)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            val maxTotal = (hourlyData.map { it.total }.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
            val hourMap = hourlyData.associate {
                val h = it.hour.toIntOrNull() ?: 0
                h to it.total
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (row in 0 until 4) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (col in 0 until 6) {
                            val hour = row * 6 + col
                            val value = hourMap[hour] ?: 0.0
                            val fraction = (value / maxTotal).toFloat().coerceIn(0f, 1f)

                            val baseColor = Accent
                            val blockColor = if (value == 0.0) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                            } else {
                                baseColor.copy(alpha = fraction.coerceAtLeast(0.15f))
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(blockColor, shape = RoundedCornerShape(6.dp))
                                    .clickable {
                                        selectedHour = hour
                                        isLoadingApps = true
                                        scope.launch {
                                            selectedHourApps = onHourClick(hour)
                                            isLoadingApps = false
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$hour",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (value == 0.0) Color.Gray else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedHour != null) {
        AlertDialog(
            onDismissRequest = { selectedHour = null },
            title = { Text(text = "Hour $selectedHour:00 Scroll Breakdown", fontWeight = FontWeight.Bold) },
            text = {
                if (isLoadingApps) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (selectedHourApps.isEmpty()) {
                    Text("No app scrolling recorded for this hour.", color = Color.Gray)
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        selectedHourApps.forEach { appTotal ->
                            val cleanName = getAppName(appTotal.sourcePackage, context)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cleanName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = DistanceFormatter.format(appTotal.total),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedHour = null }) {
                    Text("Close")
                }
            }
        )
    }
}
