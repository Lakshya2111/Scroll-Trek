package com.example.scrolltrek.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrolltrek.data.db.dao.HourlyTotal
import com.example.scrolltrek.ui.theme.Accent

@Composable
fun ScrollHeatmap(
    hourlyData: List<HourlyTotal>,
    modifier: Modifier = Modifier
) {
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
                text = "HOURLY ACTIVE HEATMAP",
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
                                    .background(blockColor, shape = RoundedCornerShape(6.dp)),
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

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Less", fontSize = 11.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.15f, 0.45f, 0.75f, 1.0f).forEach { alpha ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Accent.copy(alpha = alpha), shape = RoundedCornerShape(2.dp))
                        )
                    }
                }
                Text("More", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}
