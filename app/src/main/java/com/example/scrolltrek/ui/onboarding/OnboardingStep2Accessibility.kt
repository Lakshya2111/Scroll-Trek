package com.example.scrolltrek.ui.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrolltrek.ui.common.BulletPoint
import com.example.scrolltrek.ui.common.PrivacyBadge
import kotlinx.coroutines.delay

@Composable
fun OnboardingStep2Accessibility(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isServiceEnabled by remember { mutableStateOf(false) }
    var showConsequenceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val enabled = isAccessibilityServiceEnabled(context)
            isServiceEnabled = enabled
            if (enabled) {
                onNext()
                break
            }
            delay(1000)
        }
    }

    if (showConsequenceDialog) {
        AlertDialog(
            onDismissRequest = { showConsequenceDialog = false },
            title = { Text("Accessibility Required") },
            text = { Text("scrollTrek cannot measure scroll distance without Accessibility access. This permission is required to proceed.") },
            confirmButton = {
                TextButton(onClick = {
                    showConsequenceDialog = false
                    launchAccessibilitySettings(context)
                }) {
                    Text("Grant")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsequenceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
            PhoneSilhouetteWithScrollArrows(
                modifier = Modifier
                    .size(160.dp)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "One permission\nchanges everything",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BulletPoint(isPositive = true, text = "We measure how far you scroll")
                BulletPoint(isPositive = true, text = "We track which apps you scroll in")
                BulletPoint(isPositive = false, text = "We never read what you see")
                BulletPoint(isPositive = false, text = "We never take screenshots")
                BulletPoint(isPositive = false, text = "We never send data off your device")
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrivacyBadge()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { showConsequenceDialog = true },
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Text("Not now", fontSize = 16.sp)
            }

            Button(
                onClick = { launchAccessibilitySettings(context) },
                modifier = Modifier.weight(1.5f).fillMaxHeight()
            ) {
                Text("Grant access", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PhoneSilhouetteWithScrollArrows(modifier: Modifier = Modifier) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val arrowColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val phoneWidth = width * 0.5f
        val phoneHeight = height * 0.9f
        val phoneLeft = (width - phoneWidth) / 2
        val phoneTop = (height - phoneHeight) / 2

        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(phoneLeft, phoneTop),
            size = androidx.compose.ui.geometry.Size(phoneWidth, phoneHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
            style = Stroke(width = 6f)
        )

        val centerX = width / 2
        val centerY = height / 2

        drawLine(
            color = arrowColor,
            start = Offset(centerX, centerY - 30f),
            end = Offset(centerX, centerY + 30f),
            strokeWidth = 6f
        )
        drawLine(
            color = arrowColor,
            start = Offset(centerX, centerY - 30f),
            end = Offset(centerX - 15f, centerY - 15f),
            strokeWidth = 6f
        )
        drawLine(
            color = arrowColor,
            start = Offset(centerX, centerY - 30f),
            end = Offset(centerX + 15f, centerY - 15f),
            strokeWidth = 6f
        )
        drawLine(
            color = arrowColor,
            start = Offset(centerX, centerY + 30f),
            end = Offset(centerX - 15f, centerY + 15f),
            strokeWidth = 6f
        )
        drawLine(
            color = arrowColor,
            start = Offset(centerX, centerY + 30f),
            end = Offset(centerX + 15f, centerY + 15f),
            strokeWidth = 6f
        )
    }
}

private fun launchAccessibilitySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = android.content.ComponentName(context, "com.example.scrolltrek.tracking.ScrollTrackingAccessibilityService")
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledService = android.content.ComponentName.unflattenFromString(componentNameString)
        if (enabledService != null && enabledService == expectedComponentName) {
            return true
        }
    }
    return false
}
