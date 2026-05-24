package com.example.scrolltrek.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrolltrek.data.model.LandmarkTier
import com.example.scrolltrek.ui.theme.TierCommon
import com.example.scrolltrek.ui.theme.TierLandmark
import com.example.scrolltrek.ui.theme.TierLegendary
import com.example.scrolltrek.ui.theme.TierNotable

@Composable
fun getTierColor(tier: LandmarkTier): Color = when (tier) {
    LandmarkTier.COMMON -> TierCommon
    LandmarkTier.NOTABLE -> TierNotable
    LandmarkTier.LANDMARK -> TierLandmark
    LandmarkTier.LEGENDARY -> TierLegendary
}

@Composable
fun TierBadge(
    tier: LandmarkTier,
    modifier: Modifier = Modifier
) {
    val backgroundColor = getTierColor(tier)
    val textColor = if (tier == LandmarkTier.LEGENDARY) Color(0xFF2D1B69) else Color.White

    Box(
        modifier = modifier
            .background(backgroundColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = tier.name,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
