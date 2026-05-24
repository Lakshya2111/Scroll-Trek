package com.example.scrolltrek.ui.milestone

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.scrolltrek.ui.common.DistanceFormatter
import com.example.scrolltrek.ui.common.TierBadge
import com.example.scrolltrek.ui.home.getLandmarkEmoji
import com.example.scrolltrek.ui.onboarding.ConfettiEffect

@Composable
fun MilestoneRevealScreen(
    navController: NavHostController,
    landmarkId: String,
    modifier: Modifier = Modifier,
    viewModel: MilestoneRevealViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val landmarkState = viewModel.landmark.collectAsState(initial = null)
    val landmark = landmarkState.value

    val transition = rememberInfiniteTransition(label = "pulse_scale")
    val animatedScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A))
    ) {
        ConfettiEffect(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "LANDMARK UNLOCKED! 🎉",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE94560),
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your thumb has scaled new heights!",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (landmark == null) {
                    CircularProgressIndicator(color = Color(0xFFE94560))
                } else {
                    val colorStart = try {
                        Color(android.graphics.Color.parseColor(landmark.cardGradientStart))
                    } catch (e: Exception) {
                        Color(0xFF1A1A2E)
                    }
                    val colorEnd = try {
                        Color(android.graphics.Color.parseColor(landmark.cardGradientEnd))
                    } catch (e: Exception) {
                        Color(0xFF16213E)
                    }

                    Card(
                        modifier = Modifier
                            .width(240.dp)
                            .height(320.dp)
                            .scale(animatedScale),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(colors = listOf(colorStart, colorEnd)))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    TierBadge(tier = landmark.tier)
                                }

                                Text(
                                    text = getLandmarkEmoji(landmark.id),
                                    fontSize = 80.sp
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = landmark.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = landmark.location,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = DistanceFormatter.format(landmark.distanceMeters),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (landmark != null) {
                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, landmark.shareMessage)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Unlock"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE94560))
                    ) {
                        Icon(imageVector = Icons.Rounded.Share, contentDescription = "Share", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Unlock", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Awesome!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
