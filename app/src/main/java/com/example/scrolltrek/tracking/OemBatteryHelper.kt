package com.example.scrolltrek.tracking

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

object OemBatteryHelper {

    /**
     * Gets a list of intents that can open the device specific background battery limit/autostart settings.
     * We map the manufacturer name to a list of potential intents that could open these settings.
     */
    fun getOemBatteryIntents(context: Context): List<OemIntentInfo> {
        val packageName = context.packageName
        val intents = mutableListOf<OemIntentInfo>()

        // 1. Xiaomi / MIUI Autostart & Power Management
        intents.add(
            OemIntentInfo(
                manufacturer = "Xiaomi",
                label = "Autostart settings",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
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
                }
            )
        )

        // 2. Samsung Smart Manager / Device Care
        intents.add(
            OemIntentInfo(
                manufacturer = "Samsung",
                label = "Device care battery settings",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                }
            )
        )
        intents.add(
            OemIntentInfo(
                manufacturer = "Samsung",
                label = "Smart Manager settings",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.sm",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                }
            )
        )
        intents.add(
            OemIntentInfo(
                manufacturer = "Samsung",
                label = "App Sleep settings",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.sm_cn",
                        "com.samsung.android.sm.ui.ram.RamActivity"
                    )
                }
            )
        )

        // 3. OnePlus App Launch Control
        intents.add(
            OemIntentInfo(
                manufacturer = "OnePlus",
                label = "App launch control",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.oneplus.security",
                        "com.oneplus.security.chainlaunch.AppLaunchControlActivity"
                    )
                }
            )
        )
        intents.add(
            OemIntentInfo(
                manufacturer = "OnePlus",
                label = "Battery optimization",
                intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            )
        )

        // 4. Huawei protected apps & startup manager
        intents.add(
            OemIntentInfo(
                manufacturer = "Huawei",
                label = "Protected apps settings",
                intent = Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
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
                }
            )
        )

        return intents
    }

    /**
     * Finds the first intent supported on the current device.
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
                return info
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val standardIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
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

    data class OemIntentInfo(
        val manufacturer: String,
        val label: String,
        val intent: Intent
    )
}
