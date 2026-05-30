package com.example.scrolltrek.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingStep1ValueProp(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            ScrollToMapAnimation(
                modifier = Modifier
                    .size(240.dp)
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Your scrolling,\nmapped to the real world",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                lineHeight = 38.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Find out how far your thumb really travels and unlock landmarks along the way.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Let's go", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ScrollToMapAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scroll_anim")
    val animatedY by infiniteTransition.animateFloat(
        initialValue = 180f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "y_offset"
    )
    val animatedPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val phoneOutlineColor = MaterialTheme.colorScheme.outline

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val phoneWidth = width * 0.5f
        val phoneHeight = height * 0.9f
        val phoneLeft = (width - phoneWidth) / 2
        val phoneTop = (height - phoneHeight) / 2

        drawRoundRect(
            color = phoneOutlineColor,
            topLeft = Offset(phoneLeft, phoneTop),
            size = androidx.compose.ui.geometry.Size(phoneWidth, phoneHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
            style = Stroke(width = 8f)
        )

        val screenTop = phoneTop + 24f
        val screenHeight = phoneHeight - 48f

        drawLine(
            color = Color(0xFFE94560).copy(alpha = 0.3f),
            start = Offset(width / 2, screenTop + 20f),
            end = Offset(width / 2, screenTop + screenHeight - 20f),
            strokeWidth = 6f
        )

        drawCircle(
            color = Color(0xFFE94560),
            radius = 12f,
            center = Offset(width / 2, screenTop + animatedY)
        )

        if (animatedY < 50f) {
            drawCircle(
                color = Color(0xFFE94560).copy(alpha = (1f - animatedPulse / 40f).coerceIn(0f, 1f)),
                radius = animatedPulse,
                center = Offset(width / 2, screenTop + 20f),
                style = Stroke(width = 4f)
            )
        }
    }
}
