package com.example.scrolltrek.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SettingsAccessibility
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.scrolltrek.ui.common.DistanceFormatter
import com.example.scrolltrek.ui.common.ServiceUtils
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE) }

    var dailyGoal by remember {
        val initialGoal = sharedPreferences.getFloat("KEY_DAILY_GOAL_METERS", 0f)
        val finalGoal = if (initialGoal > 0f) initialGoal else sharedPreferences.getFloat("daily_scroll_goal", 100f)
        mutableStateOf(finalGoal)
    }

    var showGoalDialog by remember { mutableStateOf(false) }
    var goalInputText by remember { mutableStateOf("") }

    var appTheme by remember {
        mutableStateOf(sharedPreferences.getString("app_theme", "system") ?: "system")
    }

    var isServiceRunning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isServiceRunning = ServiceUtils.isAccessibilityServiceEnabled(context)
            delay(1000)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceRunning) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SettingsAccessibility,
                            contentDescription = "Accessibility",
                            tint = if (isServiceRunning) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "Tracking Service",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isServiceRunning) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isServiceRunning) "Running smoothly" else "Service is stopped",
                                fontSize = 12.sp,
                                color = if (isServiceRunning) Color(0xFF2E7D32).copy(alpha = 0.8f) else Color(0xFFC62828).copy(alpha = 0.8f)
                            )
                        }
                    }

                    if (!isServiceRunning) {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    })
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Text("Start", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.TrackChanges, contentDescription = "Goal", tint = MaterialTheme.colorScheme.primary)
                        Text("Daily Scroll Goal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                goalInputText = dailyGoal.toInt().toString()
                                showGoalDialog = true
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Goal meters (Tap to type)", fontSize = 14.sp, color = Color.Gray)
                        Text(
                            text = DistanceFormatter.format(dailyGoal.toDouble()),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = dailyGoal,
                        onValueChange = {
                            dailyGoal = it.roundToInt().toFloat()
                            sharedPreferences.edit()
                                .putFloat("daily_scroll_goal", dailyGoal)
                                .putFloat("KEY_DAILY_GOAL_METERS", dailyGoal)
                                .commit()
                        },
                        valueRange = 10f..1000f
                    )
                }
            }

            if (showGoalDialog) {
                AlertDialog(
                    onDismissRequest = { showGoalDialog = false },
                    title = { Text("Set Daily Scroll Goal", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Enter your daily goal in meters:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = goalInputText,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() } && input.length <= 5) {
                                        goalInputText = input
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("e.g. 150") }
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val newGoal = goalInputText.toFloatOrNull()
                                if (newGoal != null && newGoal >= 1f && newGoal <= 10000f) {
                                    dailyGoal = newGoal
                                    sharedPreferences.edit()
                                        .putFloat("daily_scroll_goal", dailyGoal)
                                        .putFloat("KEY_DAILY_GOAL_METERS", dailyGoal)
                                        .commit()
                                }
                                showGoalDialog = false
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showGoalDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Palette, contentDescription = "Theme", tint = MaterialTheme.colorScheme.primary)
                        Text("Theme Setting", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    listOf("system", "light", "dark", "amoled").forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    appTheme = option
                                    sharedPreferences.edit().putString("app_theme", option).apply()
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.replaceFirstChar { it.uppercase() },
                                fontSize = 14.sp,
                                fontWeight = if (appTheme == option) FontWeight.Bold else FontWeight.Normal,
                                color = if (appTheme == option) MaterialTheme.colorScheme.primary else Color.Unspecified
                            )
                            RadioButton(
                                selected = appTheme == option,
                                onClick = {
                                    appTheme = option
                                    sharedPreferences.edit().putString("app_theme", option).apply()
                                }
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("app_exclusions") },
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Block, contentDescription = "Exclusions", tint = MaterialTheme.colorScheme.primary)
                        Text("App Exclusions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text("Configure >", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

