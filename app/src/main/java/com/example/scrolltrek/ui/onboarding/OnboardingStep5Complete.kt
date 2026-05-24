package com.example.scrolltrek.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrolltrek.ui.theme.Primary
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun OnboardingStep5Complete(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(600)
        showCard = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        ConfettiEffect(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "You're being tracked",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 30.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Start scrolling any app. We'll keep count.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                AnimatedVisibility(
                    visible = showCard,
                    enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
                ) {
                    FirstLandmarkPreview(
                        modifier = Modifier
                            .width(220.dp)
                            .height(280.dp)
                    )
                }
            }

            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("See my dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FirstLandmarkPreview(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFEB3B), Color(0xFFFF9800))
                    )
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "FIRST TARGET",
                    color = Primary.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "✏️",
                        fontSize = 72.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Standard Pencil",
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Primary, shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "0.19 m",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ConfettiEffect(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "confetti")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val particles = remember {
        List(50) {
            ConfettiParticle(
                xSeed = Random.nextFloat(),
                ySpeed = Random.nextFloat() * 0.5f + 0.5f,
                color = listOf(
                    Color(0xFFE94560),
                    Color(0xFFFFEB3B),
                    Color(0xFF4CAF50),
                    Color(0xFF2196F3),
                    Color(0xFF9C27B0)
                ).random(),
                size = Random.nextFloat() * 15f + 10f,
                rotationSpeed = Random.nextFloat() * 360f - 180f
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val x = (p.xSeed * w + progress * w * 0.2f) % w
            val y = (progress * h * p.ySpeed * 1.5f) % h

            drawRect(
                color = p.color,
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(p.size, p.size / 2)
            )
        }
    }
}

private data class ConfettiParticle(
    val xSeed: Float,
    val ySpeed: Float,
    val color: Color,
    val size: Float,
    val rotationSpeed: Float
)
