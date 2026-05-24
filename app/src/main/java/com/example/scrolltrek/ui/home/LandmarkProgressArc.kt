package com.example.scrolltrek.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrolltrek.data.model.LandmarkProgress
import com.example.scrolltrek.ui.common.DistanceFormatter
import com.example.scrolltrek.ui.theme.Accent

@Composable
fun LandmarkProgressArc(
    progress: LandmarkProgress,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.progressFraction,
        animationSpec = spring(stiffness = 100f),
        label = "arc_progress"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val strokeWidth = 14f
                val size = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                drawArc(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = size,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                drawArc(
                    color = Accent,
                    startAngle = 135f,
                    sweepAngle = 270f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = size,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val emoji = getLandmarkEmoji(progress.nextLandmark.id)
                Text(
                    text = emoji,
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = progress.nextLandmark.name,
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${DistanceFormatter.format(progress.distanceToNextM)} to go",
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

fun getLandmarkEmoji(id: String): String = when {
    id.contains("pencil") -> "✏️"
    id.contains("banana") -> "🍌"
    id.contains("wafer") -> "🥏"
    id.contains("cat") -> "🐱"
    id.contains("mona") -> "🎨"
    id.contains("bicycle") -> "🚲"
    id.contains("door") -> "🚪"
    id.contains("giraffe") -> "🦒"
    id.contains("pole") -> "💈"
    id.contains("bus") -> "🚌"
    id.contains("trex") -> "🦖"
    id.contains("hollywood") -> "🎬"
    id.contains("bowling") -> "🎳"
    id.contains("whale") -> "🐋"
    id.contains("stonehenge") -> "🪨"
    id.contains("christ") -> "⛪"
    id.contains("pool") -> "🏊"
    id.contains("whitehouse") -> "🏛"
    id.contains("pisa") -> "🗼"
    id.contains("sphinx") -> "🦁"
    id.contains("tajmahal") -> "🕌"
    id.contains("sequoia") -> "🌲"
    id.contains("liberty") -> "🗽"
    id.contains("bigben") -> "⏰"
    id.contains("football") -> "🏈"
    id.contains("londoneye") -> "🎡"
    id.contains("pyramid") -> "📐"
    id.contains("sagrada") -> "⛪"
    id.contains("operahouse") -> "⛵"
    id.contains("colosseum") -> "🏟"
    id.contains("eiffel") -> "🗼"
    id.contains("empire") -> "🏢"
    id.contains("petronas") -> "🏢"
    id.contains("onewtc") -> "🏢"
    id.contains("cntower") -> "🗼"
    id.contains("shanghai") -> "🏢"
    id.contains("skytree") -> "🗼"
    id.contains("burj") -> "🏢"
    id.contains("hulk") -> "🎢"
    id.contains("brooklyn") -> "🌉"
    id.contains("grandcanyon") -> "⛰"
    id.contains("goldengate") -> "🌉"
    id.contains("runway") -> "✈️"
    id.contains("centralpark") -> "🌳"
    id.contains("everest") -> "🏔"
    id.contains("mariana") -> "🐙"
    id.contains("channel") -> "🏊"
    id.contains("panama") -> "🚢"
    id.contains("karman") -> "🚀"
    id.contains("hadrian") -> "🧱"
    id.contains("suez") -> "🚢"
    else -> "🏔"
}
