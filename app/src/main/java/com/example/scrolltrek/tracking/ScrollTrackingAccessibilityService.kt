package com.example.scrolltrek.tracking

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.scrolltrek.data.repository.ScrollRepository
import com.example.scrolltrek.tracking.model.EventConfidence
import com.example.scrolltrek.tracking.model.RawScrollEvent
import com.example.scrolltrek.tracking.model.ScrollDirection
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class ScrollTrackingAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var repository: ScrollRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Configuration state
    private var excludedPackages = setOf<String>()
    private var trackingEnabled = true
    private var trackUpwardScroll = false
    private var minSessionSlopMult = 1.0f
    private var touchSlopPx = 0f

    // Running state
    private var activePackageName = ""
    private var lastPrimaryEventTimeMs = 0L
    private var isTouchActive = false
    private var touchStartTimeMs = 0L
    private var emittedTier1Or2DuringTouch = false

    private var lastNodeSnapshot: Map<String, Float> = emptyMap()
    private val lastScrollYMap = mutableMapOf<Int, Int>()

    // Duplicate event guard cache
    private var lastEmittedDeltaY: Float = 0f
    private var lastEmittedPackage: String = ""
    private var lastEmittedTimeMs: Long = 0L

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "KEY_TRACKING_ENABLED") {
            val enabled = prefs.getBoolean("KEY_TRACKING_ENABLED", true)
            if (!enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    disableSelf()
                }
            } else {
                val intent = Intent(this, TrackingForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }
        updatePrefs(prefs)
    }

    companion object {
        var sessionId: String = UUID.randomUUID().toString()
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        sessionId = UUID.randomUUID().toString()

        // Start compliant foreground service in the same isolated :tracker process
        val intent = Intent(this, TrackingForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        val prefs = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
        updatePrefs(prefs)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            if (action == "ACTION_DISABLE_TRACKING") {
                trackingEnabled = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    disableSelf()
                }
            } else if (action == "ACTION_ENABLE_TRACKING") {
                trackingEnabled = true
                val fgsIntent = Intent(this, TrackingForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(fgsIntent)
                } else {
                    startService(fgsIntent)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updatePrefs(prefs: SharedPreferences) {
        trackingEnabled = prefs.getBoolean("KEY_TRACKING_ENABLED", true)
        excludedPackages = prefs.getStringSet("KEY_EXCLUDED_PACKAGES", emptySet()) ?: emptySet()
        trackUpwardScroll = prefs.getBoolean("KEY_TRACK_UPWARD_SCROLL", false)
        minSessionSlopMult = prefs.getFloat("KEY_MIN_SESSION_SLOP_MULT", 1.0f)
        touchSlopPx = ViewConfiguration.get(this).scaledTouchSlop * minSessionSlopMult
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!trackingEnabled) return

        val packageName = event.packageName?.toString() ?: ""
        if (packageName.isNotEmpty()) {
            activePackageName = packageName
        }

        val now = System.currentTimeMillis()

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                lastPrimaryEventTimeMs = now
                emittedTier1Or2DuringTouch = true

                val deltaY = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    event.scrollDeltaY.toFloat()
                } else {
                    deriveDeltaFromAbsolutePosition(event)
                }

                if (abs(deltaY) >= touchSlopPx) {
                    emitEvent(deltaY, packageName, EventConfidence.PRIMARY)
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (isTouchActive && (now - lastPrimaryEventTimeMs) > 300) {
                    val root = rootInActiveWindow
                    if (root != null) {
                        val startTime = System.nanoTime()
                        val currentSnapshot = mutableMapOf<String, Float>()
                        buildNodeSnapshot(root, 0, currentSnapshot)
                        root.recycle()

                        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0
                        if (elapsedMs < 5.0 && currentSnapshot.isNotEmpty()) {
                            val deltaY = computeYAxisShift(lastNodeSnapshot, currentSnapshot)
                            lastNodeSnapshot = currentSnapshot

                            if (abs(deltaY) >= touchSlopPx) {
                                emitEvent(deltaY, packageName, EventConfidence.FALLBACK_A)
                                emittedTier1Or2DuringTouch = true
                            }
                        } else {
                            lastNodeSnapshot = emptyMap()
                        }
                    }
                }
            }
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> {
                isTouchActive = true
                touchStartTimeMs = now
                emittedTier1Or2DuringTouch = false
            }
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> {
                isTouchActive = false
                val duration = now - touchStartTimeMs
                if (!emittedTier1Or2DuringTouch && duration in 150..1200) {
                    // Infer a scroll gesture on flat custom canvas (e.g. TikTok / Reels)
                    val screenHeightPx = resources.displayMetrics.heightPixels.toFloat()
                    val deltaY = screenHeightPx * 0.4f // typical vertical page swipe
                    emitEvent(deltaY, activePackageName.ifEmpty { packageName }, EventConfidence.FALLBACK_B)
                }
            }
        }
    }

    private fun deriveDeltaFromAbsolutePosition(event: AccessibilityEvent): Float {
        val source = event.source
        val viewId = source?.hashCode() ?: return 0f
        source.recycle()

        val currentScrollY = event.scrollY
        if (currentScrollY == -1) return 0f

        if (lastScrollYMap.size > 100) {
            lastScrollYMap.clear()
        }

        val lastScrollY = lastScrollYMap[viewId]
        lastScrollYMap[viewId] = currentScrollY

        if (lastScrollY == null) return 0f
        return (currentScrollY - lastScrollY).toFloat()
    }

    private fun buildNodeSnapshot(node: AccessibilityNodeInfo?, depth: Int, map: MutableMap<String, Float>) {
        if (node == null || depth > 8) return
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val viewId = try {
            node.viewIdResourceName
        } catch (e: Exception) {
            null
        }
        val key = viewId ?: "${node.className}_${bounds.left}_${bounds.right}"
        map[key] = bounds.top.toFloat()

        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (e: Exception) { null }
            if (child != null) {
                buildNodeSnapshot(child, depth + 1, map)
                child.recycle()
            }
        }
    }

    private fun computeYAxisShift(old: Map<String, Float>, new: Map<String, Float>): Float {
        var totalShift = 0f
        var matchCount = 0
        for ((key, newTop) in new) {
            val oldTop = old[key]
            if (oldTop != null) {
                totalShift += (oldTop - newTop) // If elements move up, shift is positive
                matchCount++
            }
        }
        return if (matchCount > 0) totalShift / matchCount else 0f
    }

    private fun emitEvent(deltaY: Float, packageName: String, confidence: EventConfidence) {
        val direction = if (deltaY > 0) ScrollDirection.DOWN else ScrollDirection.UP
        val absDelta = abs(deltaY)

        // Rule 5: Direction gate
        if (direction == ScrollDirection.UP && !trackUpwardScroll) return

        // Rule 2: Screen state
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(0)
        if (display != null && display.state != Display.STATE_ON) return

        // Rule 3: Package exclusion
        if (isExcludedPackage(packageName)) return

        // Rule 4: Duplicate guard
        val now = System.currentTimeMillis()
        if (packageName == lastEmittedPackage && abs(absDelta - lastEmittedDeltaY) < 0.01f && (now - lastEmittedTimeMs) < 16) {
            return
        }

        // Rule 6: Sanity bound
        val screenHeightPx = resources.displayMetrics.heightPixels
        if (absDelta > screenHeightPx * 3) return

        // Update duplicate guard cache
        lastEmittedDeltaY = absDelta
        lastEmittedPackage = packageName
        lastEmittedTimeMs = now

        val event = RawScrollEvent(
            timestampMs = now,
            deltaYPx = absDelta,
            direction = direction,
            sourcePackage = packageName,
            sessionId = sessionId,
            confidence = confidence
        )

        serviceScope.launch {
            ScrollEventBus.emit(event)
            ScrollEventBuffer.push(event)
        }
    }

    private fun isExcludedPackage(packageName: String?): Boolean {
        if (packageName == null) return true
        if (packageName == this.packageName) return true // Exclude our own app

        if (packageName in excludedPackages) return true

        // Privacy Audit compliant pre-exclusion for banking + health apps
        val lower = packageName.lowercase()
        val sensitiveKeywords = listOf(
            "bank", "finance", "wallet", "health", "fit", "medical",
            "paypal", "venmo", "chase", "citi", "wellsfargo", "bofa",
            "capitalone", "fidelity", "schwab", "credit", "cash", "insurance"
        )
        for (keyword in sensitiveKeywords) {
            if (lower.contains(keyword)) return true
        }
        return false
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        val prefs = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)

        try {
            val serviceIntent = Intent(this, TrackingForegroundService::class.java)
            stopService(serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (::repository.isInitialized) {
            runBlocking {
                try {
                    repository.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        serviceScope.cancel()
    }
}
