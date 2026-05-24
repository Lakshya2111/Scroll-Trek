package com.example.scrolltrek.tracking

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

class DozeRecoveryReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DozeRecoveryReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIdle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager.isDeviceIdleMode
            } else {
                false
            }

            Log.i(TAG, "Device idle mode changed. Idle = $isIdle")

            if (!isIdle) {
                // Device is exiting Doze mode! Recover background services.
                Log.i(TAG, "Exiting Doze Mode. Triggering service recovery.")
                
                // Re-ensure the Watchdog job is scheduled
                WatchdogJobService.schedule(context)

                val isServiceEnabled = isAccessibilityServiceEnabled(context, ScrollTrackingAccessibilityService::class.java)
                if (isServiceEnabled) {
                    try {
                        val serviceIntent = Intent(context, TrackingForegroundService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                        Log.i(TAG, "Successfully restarted foreground service on Doze exit.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start TrackingForegroundService on Doze exit", e)
                    }
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
