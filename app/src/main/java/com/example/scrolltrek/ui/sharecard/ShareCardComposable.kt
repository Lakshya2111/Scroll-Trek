package com.example.scrolltrek.ui.sharecard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrolltrek.data.model.Landmark
import com.example.scrolltrek.ui.common.DistanceFormatter
import com.example.scrolltrek.ui.common.TierBadge
import com.example.scrolltrek.ui.home.getLandmarkEmoji

@Composable
fun ShareCardComposable(
    landmark: Landmark,
    modifier: Modifier = Modifier
) {
    val colorStart = try {
        Color(android.graphics.Color.parseColor(landmark.cardGradientStart))
    } catch (e: Exception) {
        Color(0xFF16213E)
    }
    val colorEnd = try {
        Color(android.graphics.Color.parseColor(landmark.cardGradientEnd))
    } catch (e: Exception) {
        Color(0xFF0F0F1A)
    }

    Card(
        modifier = modifier
            .width(ShareCardStyles.cardWidth)
            .height(ShareCardStyles.cardHeight),
        shape = ShareCardStyles.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(colorStart, colorEnd)))
                .padding(28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "scrollTrek",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    TierBadge(tier = landmark.tier)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = getLandmarkEmoji(landmark.id),
                        fontSize = 84.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = landmark.name,
                        color = ShareCardStyles.textColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = landmark.location,
                        color = ShareCardStyles.subTextColor,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "UNLOCKED!",
                            color = ShareCardStyles.subTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = DistanceFormatter.format(landmark.distanceMeters),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White, shape = ShareCardStyles.qrShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            val cols = 5
                            val rows = 5
                            val cellW = w / cols
                            val cellH = h / rows

                            for (r in 0 until rows) {
                                for (c in 0 until cols) {
                                    val isCorner = (r < 2 && c < 2) || (r < 2 && c >= cols - 2) || (r >= rows - 2 && c < 2)
                                    val isRandom = (r + c) % 3 == 0 || (r * c) % 2 == 0

                                    if (isCorner || isRandom) {
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = Offset(c * cellW, r * cellH),
                                            size = androidx.compose.ui.geometry.Size(cellW * 0.85f, cellH * 0.85f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
