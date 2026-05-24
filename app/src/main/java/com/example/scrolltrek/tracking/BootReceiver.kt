package com.example.scrolltrek.tracking

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        Log.i(TAG, "BootReceiver received action: ${intent.action}")

        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            // Schedule the 15-minute Watchdog Job
            WatchdogJobService.schedule(context)
            
            // If the accessibility service is enabled, trigger the foreground service
            val isEnabled = isAccessibilityServiceEnabled(context, ScrollTrackingAccessibilityService::class.java)
            if (isEnabled) {
                Log.i(TAG, "Accessibility Service is enabled. Starting foreground tracking service.")
                try {
                    val serviceIntent = Intent(context, TrackingForegroundService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start TrackingForegroundService on boot", e)
                }
            }
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val expectedComponentName = ComponentName(context, serviceClass).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(':').any { it.equals(expectedComponentName, ignoreCase = true) }
    }
}
