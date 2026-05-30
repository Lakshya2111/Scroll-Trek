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
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Accent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⏰",
                            fontSize = 20.sp
                        )
                    }
                    Column {
                        Text(
                            text = "Scroll Breakdown",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val nextHour = (selectedHour!! + 1) % 24
                        Text(
                            text = String.format("%02d:00 - %02d:00", selectedHour, nextHour),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            text = {
                if (isLoadingApps) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Accent)
                    }
                } else if (selectedHourApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No app scrolling recorded for this hour.", color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    val totalHourScroll = selectedHourApps.sumOf { it.total }.coerceAtLeast(0.001)
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        selectedHourApps.forEach { appTotal ->
                            val cleanName = getAppName(appTotal.sourcePackage, context)
                            val percentage = (appTotal.total / totalHourScroll).toFloat().coerceIn(0f, 1f)
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Stylized avatar with first letter of clean name
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cleanName.take(1).uppercase(),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = cleanName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = DistanceFormatter.format(appTotal.total),
                                                fontSize = 13.sp,
                                                color = Accent,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
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
                                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Text(
                                            text = String.format("%d%% of the hour", (percentage * 100).toInt()),
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedHour = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        )
    }
}
