package com.example.scrolltrek.tracking

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

object OemBatteryHelper {

    private const val TAG = "OemBatteryHelper"

    /**
     * Gets a list of intents that can open the device specific background battery limit/autostart settings.
     * We map the manufacturer name to a list of potential intents that could open these settings.
     *
     * All intents include FLAG_ACTIVITY_NEW_TASK for safe launching from any context.
     */
    fun getOemBatteryIntents(context: Context): List<OemIntentInfo> {
        val packageName = context.packageName
        val intents = mutableListOf<OemIntentInfo>()

        // ── 1. Xiaomi / MIUI Autostart & Power Management ──

        intents.add(
            OemIntentInfo(
                manufacturer = "Xiaomi",
                label = "Autostart settings",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )
        intents.add(
            OemIntentInfo(
                manufacturer = "Xiaomi",
                label = "Battery optimization settings",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.powerkeeper.ui.PowerKeeperSettingsActivity"
                    )
                    putExtra("package_name", packageName)
                    putExtra("package_label", context.applicationInfo.loadLabel(context.packageManager).toString())
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )

        // ── 2. Samsung Device Care / Smart Manager ──
        // Order matters: most targeted intent first, broadest fallback last.
        // On One UI, "Never sleeping apps" is at:
        //   Device Care → Battery → Background usage limits → Never sleeping apps

        // 2a. Sleeping apps / Background usage limits page (One UI 3+)
        //     This is the closest page to "Never sleeping apps".
        intents.add(
            OemIntentInfo(
                manufacturer = "Samsung",
                label = "Background usage limits",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.lool.battery.AppSleepSettingActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )

        // 2b. Device Care Battery page (widely available on One UI)
        intents.add(
            OemIntentInfo(
                manufacturer = "Samsung",
                label = "Device care battery settings",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )

        // 2c. Legacy Smart Manager (pre-One UI Samsung devices)
        intents.add(
            OemIntentInfo(
                manufacturer = "Samsung",
                label = "Smart Manager settings",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.sm",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )

        // 2d. App-specific battery settings via standard Android settings
        //     Opens App Info page where user can tap Battery → Unrestricted.
        //     This is the most reliable Samsung fallback and is functionally equivalent
        //     to adding the app to "Never sleeping apps".
        intents.add(
            OemIntentInfo(
                manufacturer = "Samsung",
                label = "App battery settings",
                intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )

        // ── 3. OnePlus App Launch Control ──

        intents.add(
            OemIntentInfo(
                manufacturer = "OnePlus",
                label = "App launch control",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.oneplus.security",
                        "com.oneplus.security.chainlaunch.AppLaunchControlActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )
        intents.add(
            OemIntentInfo(
                manufacturer = "OnePlus",
                label = "Battery optimization",
                intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )

        // ── 4. Huawei protected apps & startup manager ──

        intents.add(
            OemIntentInfo(
                manufacturer = "Huawei",
                label = "Protected apps settings",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )
        intents.add(
            OemIntentInfo(
                manufacturer = "Huawei",
                label = "App startup management",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )

        return intents
    }

    /**
     * Finds the first intent supported on the current device.
     * Filters by manufacturer name match, then checks if the intent resolves to an activity.
     * Falls back to the standard Android battery exemption dialog if no OEM intent resolves.
     */
    fun getSupportedOemIntent(context: Context): OemIntentInfo? {
        val currentManufacturer = Build.MANUFACTURER.lowercase()
        val oemIntents = getOemBatteryIntents(context)

        val matchingIntents = oemIntents.filter {
            it.manufacturer.lowercase() == currentManufacturer || 
            currentManufacturer.contains(it.manufacturer.lowercase())
        }

        val pm = context.packageManager
        for (info in matchingIntents) {
            val resolveInfo = pm.resolveActivity(info.intent, 0)
            if (resolveInfo != null) {
                Log.d(TAG, "Resolved OEM intent: ${info.label} for ${info.manufacturer}")
                return info
            }
        }

        // Standard Android battery exemption dialog (works on stock Android, Pixel, etc.)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val standardIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (pm.resolveActivity(standardIntent, 0) != null) {
                return OemIntentInfo(
                    manufacturer = "Android",
                    label = "Ignore Battery Optimizations",
                    intent = standardIntent
                )
            }
        }

        return null
    }

    /**
     * Launches the best available battery settings for this device.
     * Returns true if an intent was successfully launched, false otherwise.
     *
     * On Samsung, this also fires the standard Android battery exemption request
     * in addition to opening the Samsung-specific settings, because Samsung has two
     * independent layers of battery management (Android Doze + Samsung App Sleep).
     */
    fun launchBatterySettings(context: Context): Boolean {
        val currentManufacturer = Build.MANUFACTURER.lowercase()
        val isSamsung = currentManufacturer.contains("samsung")

        // On Samsung: first request standard Doze exemption silently,
        // then open Samsung-specific settings for "Never sleeping apps".
        if (isSamsung && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestStandardBatteryExemption(context)
        }

        // Try OEM-specific intent
        val oemInfo = getSupportedOemIntent(context)
        if (oemInfo != null) {
            try {
                context.startActivity(oemInfo.intent)
                Log.d(TAG, "Launched: ${oemInfo.label}")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch OEM intent: ${oemInfo.label}", e)
            }
        }

        // Fallback chain
        return launchFallbackBatterySettings(context)
    }

    /**
     * Fires the standard ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS dialog.
     * On Samsung this only exempts from Android Doze (not Samsung's app sleep system).
     */
    private fun requestStandardBatteryExemption(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Standard battery exemption request failed", e)
            }
        }
    }

    /**
     * Fallback: tries progressively broader settings pages.
     * 1. Standard battery exemption dialog
     * 2. Battery optimization settings list
     * 3. App Info page (always works)
     */
    private fun launchFallbackBatterySettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Try standard exemption dialog
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS failed", e)
            }

            // Try battery optimization settings list
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS failed", e)
            }
        }

        // Last resort: App Info page (always works)
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Even App Info settings failed", e)
        }

        return false
    }

    /**
     * Returns true if the current device has aggressive OEM-specific battery management
     * that requires additional user action beyond the standard Android Doze exemption.
     */
    fun hasAggressiveOemBatteryManagement(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("samsung") ||
                manufacturer.contains("xiaomi") ||
                manufacturer.contains("redmi") ||
                manufacturer.contains("poco") ||
                manufacturer.contains("oneplus") ||
                manufacturer.contains("oppo") ||
                manufacturer.contains("vivo") ||
                manufacturer.contains("huawei") ||
                manufacturer.contains("honor")
    }

    data class OemIntentInfo(
        val manufacturer: String,
        val label: String,
        val intent: Intent
    )
}
