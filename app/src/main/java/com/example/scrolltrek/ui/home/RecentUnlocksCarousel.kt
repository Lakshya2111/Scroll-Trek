package com.example.scrolltrek.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrolltrek.data.model.Landmark
import com.example.scrolltrek.ui.common.TierBadge

@Composable
fun RecentUnlocksCarousel(
    recentlyUnlocked: List<Landmark>,
    onLandmarkClick: (Landmark) -> Unit,
    modifier: Modifier = Modifier
) {
    if (recentlyUnlocked.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "RECENT UNLOCKS",
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(recentlyUnlocked) { landmark ->
                RecentUnlockCard(
                    landmark = landmark,
                    onClick = { onLandmarkClick(landmark) }
                )
            }
        }
    }
}

@Composable
fun RecentUnlockCard(
    landmark: Landmark,
    onClick: () -> Unit,
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
            .width(160.dp)
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(colorStart, colorEnd)))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TierBadge(tier = landmark.tier)
                }

                Column {
                    Text(
                        text = getLandmarkEmoji(landmark.id),
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = landmark.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
