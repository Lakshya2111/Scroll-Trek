package com.example.scrolltrek.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrolltrek.data.model.WeeklyAnalytics
import com.example.scrolltrek.ui.common.DistanceFormatter
import com.example.scrolltrek.ui.theme.Accent
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeeklyBarChart(
    weeklyAnalytics: WeeklyAnalytics,
    modifier: Modifier = Modifier
) {
    var activeHoldDay by remember { mutableStateOf<String?>(null) }

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
            val activeDay = weeklyAnalytics.days.firstOrNull { it.dateKey == activeHoldDay }
            val headerLabel = if (activeDay != null) {
                try {
                    val localDate = LocalDate.parse(activeDay.dateKey)
                    "${localDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US).uppercase()} TREK"
                } catch (e: Exception) {
                    "DAY DETAILS"
                }
            } else {
                "THIS WEEK"
            }
            val headerDistance = activeDay?.totalDistanceM ?: weeklyAnalytics.totalDistanceM

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = headerLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeDay != null) Accent else Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DistanceFormatter.format(headerDistance),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "DAILY AVERAGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DistanceFormatter.format(weeklyAnalytics.averageDailyM),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val maxDistance = (weeklyAnalytics.days.map { it.totalDistanceM }.maxOrNull() ?: 0.0).coerceAtLeast(1.0)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .pointerInput(weeklyAnalytics.days) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown()
                                val daysCount = weeklyAnalytics.days.size
                                val barWidthPx = size.width.toFloat() / daysCount.coerceAtLeast(1)
                                
                                var index = (down.position.x / barWidthPx).toInt().coerceIn(0, daysCount - 1)
                                activeHoldDay = weeklyAnalytics.days[index].dateKey
                                
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val anyPressed = event.changes.any { it.pressed }
                                    if (!anyPressed) {
                                        break
                                    }
                                    val pointer = event.changes.firstOrNull { it.pressed }
                                    if (pointer != null) {
                                        index = (pointer.position.x / barWidthPx).toInt().coerceIn(0, daysCount - 1)
                                        activeHoldDay = weeklyAnalytics.days[index].dateKey
                                        pointer.consume()
                                    }
                                }
                                activeHoldDay = null
                            }
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyAnalytics.days.forEach { day ->
                    val fraction = (day.totalDistanceM / maxDistance).toFloat().coerceIn(0f, 1f)
                    val dayLabel = try {
                        val localDate = LocalDate.parse(day.dateKey)
                        localDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US).take(1)
                    } catch (e: Exception) {
                        day.dateKey.takeLast(2)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val isSelected = activeHoldDay == day.dateKey
                            val alpha = if (activeHoldDay != null && !isSelected) 0.35f else 1f
                            val scaleX = if (isSelected) 1.25f else 1f
                            val scaleY = if (isSelected) 1.05f else 1f

                            // Graph bar
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(fraction.coerceAtLeast(0.05f))
                                    .fillMaxWidth(0.4f)
                                    .graphicsLayer(scaleX = scaleX, scaleY = scaleY)
                                    .background(
                                        color = if (day.totalDistanceM >= day.goalMeters && day.goalMeters > 0) Accent else MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    )
                                    .alpha(alpha)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dayLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
