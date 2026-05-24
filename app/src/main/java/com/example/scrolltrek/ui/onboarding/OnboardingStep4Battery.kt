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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrolltrek.tracking.OemBatteryHelper

@Composable
fun OnboardingStep4Battery(
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showWarningDialog by remember { mutableStateOf(false) }

    val launchBatterySettings = {
        val oemInfo = OemBatteryHelper.getSupportedOemIntent(context)
        if (oemInfo != null) {
            try {
                context.startActivity(oemInfo.intent)
            } catch (e: Exception) {
                launchStandardBatterySettings(context)
            }
        } else {
            launchStandardBatterySettings(context)
        }
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
                    .background(Color(0xFFFFF3CD), shape = RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.BatteryAlert,
                    contentDescription = "Battery",
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Unrestricted\nbackground tracking",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            val manufacturer = Build.MANUFACTURER
            val oemText = if (manufacturer.equals("Xiaomi", true) || manufacturer.equals("Redmi", true)) {
                "For Xiaomi devices, please enable 'Autostart' and set Battery Saver to 'No restrictions' inside MIUI security settings."
            } else if (manufacturer.equals("Samsung", true)) {
                "For Samsung devices, please add scrollTrek to 'Never sleeping apps' in Device Care settings."
            } else if (manufacturer.equals("OnePlus", true)) {
                "For OnePlus devices, please set App Launch settings for scrollTrek to 'Manage manually' and allow background activity."
            } else {
                "Please turn off battery optimizations for scrollTrek to keep tracking running reliably when the device sleeps."
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Background Reliability",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

            Button(
                onClick = launchBatterySettings,
                modifier = Modifier.weight(1.5f).fillMaxHeight()
            ) {
                Text("Configure", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun launchStandardBatterySettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
