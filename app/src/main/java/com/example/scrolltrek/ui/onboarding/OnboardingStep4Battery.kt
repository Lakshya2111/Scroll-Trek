package com.example.scrolltrek.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.scrolltrek.tracking.OemBatteryHelper

@Composable
fun OnboardingStep4Battery(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showWarningDialog by remember { mutableStateOf(false) }
    var hasLaunchedSettings by remember { mutableStateOf(false) }
    var isBatteryOptimized by remember { mutableStateOf(checkBatteryOptimized(context)) }

    // Re-check battery optimization status when user returns from settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && hasLaunchedSettings) {
                isBatteryOptimized = checkBatteryOptimized(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val launchBatterySettings = {
        OemBatteryHelper.launchBatterySettings(context)
        hasLaunchedSettings = true
    }

    if (showWarningDialog) {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
            title = { Text("Skip Optimization?") },
            text = { Text("If battery optimizations are active, the Android OS may kill the tracking service in the background, causing missed distance.") },
            confirmButton = {
                TextButton(onClick = {
                    showWarningDialog = false
                    onNext()
                }) {
                    Text("Skip Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarningDialog = false }) {
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
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        if (!isBatteryOptimized) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!isBatteryOptimized) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Battery optimized",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.BatteryAlert,
                        contentDescription = "Battery",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (!isBatteryOptimized) "You're all set!" else "Unrestricted\nbackground tracking",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = if (!isBatteryOptimized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            val manufacturer = Build.MANUFACTURER
            val oemText = when {
                !isBatteryOptimized -> {
                    "Battery optimization has been turned off for scrollTrek. Background tracking will run reliably."
                }
                manufacturer.equals("Xiaomi", true) || manufacturer.equals("Redmi", true) || manufacturer.equals("Poco", true) -> {
                    "For Xiaomi devices, please enable 'Autostart' and set Battery Saver to 'No restrictions' inside MIUI security settings."
                }
                manufacturer.equals("Samsung", true) -> {
                    "For Samsung devices:\n\n" +
                    "1. Tap 'Configure' below to open Battery settings\n" +
                    "2. Navigate to Background usage limits\n" +
                    "3. Tap 'Never sleeping apps'\n" +
                    "4. Tap '+' and add scrollTrek\n\n" +
                    "This prevents Samsung from putting scrollTrek to sleep."
                }
                manufacturer.equals("OnePlus", true) -> {
                    "For OnePlus devices, please set App Launch settings for scrollTrek to 'Manage manually' and allow background activity."
                }
                hasLaunchedSettings -> {
                    "If you've already turned off battery optimization, tap 'Continue' to proceed. Otherwise, tap 'Configure' to open settings."
                }
                else -> {
                    "Please turn off battery optimizations for scrollTrek to keep tracking running reliably when the device sleeps."
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (!isBatteryOptimized) "✓ Background Reliability" else "Background Reliability",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (!isBatteryOptimized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = oemText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Bottom button row
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Show "Continue" button when user has visited settings or battery is already exempt
            if (hasLaunchedSettings || !isBatteryOptimized) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(
                        text = if (!isBatteryOptimized) "Continue" else "I've configured it — Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showWarningDialog = true },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Text("Skip", fontSize = 16.sp)
                }

                if (isBatteryOptimized) {
                    Button(
                        onClick = launchBatterySettings,
                        modifier = Modifier.weight(1.5f).fillMaxHeight()
                    ) {
                        Text(
                            text = if (hasLaunchedSettings) "Open Settings Again" else "Configure",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Checks whether the app currently has battery optimization restrictions.
 * Returns true if the app IS restricted (i.e., NOT whitelisted), false if unrestricted.
 */
private fun checkBatteryOptimized(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isIgnoringBatteryOptimizations(context.packageName) != true
    }
    // Pre-Marshmallow: no Doze, so effectively unrestricted
    return false
}
