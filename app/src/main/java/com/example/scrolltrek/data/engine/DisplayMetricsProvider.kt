package com.example.scrolltrek.data.engine

import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisplayMetricsProvider @Inject constructor(
    private val windowManager: WindowManager
) {
    // Refreshed on config change; thread-safe via @Volatile
    @Volatile 
    var ydpi: Float = 0f
        private set

    init {
        refresh()
    }

    fun refresh() {
        val calculatedYdpi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val dm = Resources.getSystem().displayMetrics
            dm.ydpi
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(dm)
            dm.ydpi
        }
        ydpi = if (calculatedYdpi <= 0f) 160f else calculatedYdpi
    }
}
