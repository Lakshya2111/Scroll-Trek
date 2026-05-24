# AGENT 1 — THE DAEMON
## Technical Specification Sheet
**scrollTrek | Antigravity Agent Brief**
`Agent ID: ST-AG-01` | `Owner: Background Systems` | `Priority: P0 — Critical Path`

---

## 1. Mission Statement

> Design, implement, and validate the complete background scroll-tracking daemon for the scrollTrek Android application. This agent owns everything from the moment a user's finger touches glass to the moment a raw pixel delta enters the processing queue — and everything required to keep that pipeline alive, silently, forever.

---

## 2. Scope Boundary

### Owns
- `ScrollTrackingAccessibilityService` (full implementation)
- `TrackingForegroundService` (lifecycle + notification)
- Foreground Service notification UI
- Process isolation configuration
- Battery optimization exemption request flow
- Doze / App Standby recovery mechanisms
- In-memory event ring buffer
- Raw pixel delta emission via `StateFlow`
- All Android Manifest declarations for this agent's components
- Touch slop filtering and duplicate event deduplication

### Does NOT Own
- Pixel-to-meter conversion math (Agent 2)
- Persistence / Room database writes (Agent 2)
- Any Compose UI screen (Agent 3)
- Floating overlay (Agent 3)
- Landmark gamification logic (Agent 2)
- Share card generation (Agent 3)

---

## 3. Tech Stack

| Concern | Technology |
|---|---|
| Event interception | Android Accessibility API (`AccessibilityService`) |
| Background execution | `ForegroundService` + `android:process=":tracker"` |
| IPC to UI process | `StateFlow` + `ContentProvider` (read by Agent 2) |
| Coroutine scope | `CoroutineScope(SupervisorJob() + Dispatchers.Default)` |
| State persistence (buffer) | In-memory `ArrayDeque<RawScrollEvent>` (max 500 items) |
| Battery exemption | `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` intent |
| Doze recovery | `BroadcastReceiver` on `ACTION_DEVICE_IDLE_MODE_CHANGED` |
| Standby recovery | `JobScheduler` watchdog (15-min interval) |
| Manifest config | `accessibility_service_config.xml` |

---

## 4. Input Contract

This agent has **no data inputs** from other agents. It is the system's data source.

**Physical inputs:**
- User finger scroll gestures across any foreground application
- `AccessibilityEvent` objects dispatched by the Android OS

**Configuration inputs (from SharedPreferences, written by Agent 3):**
```
KEY_TRACKING_ENABLED       : Boolean  (default: true)
KEY_EXCLUDED_PACKAGES      : Set<String>  (default: empty)
KEY_TRACK_UPWARD_SCROLL    : Boolean  (default: false)
KEY_MIN_SESSION_SLOP_MULT  : Float  (default: 1.0f)
```

---

## 5. Output Contract

### Primary Output — `StateFlow<RawScrollEvent>`
Published on `ScrollEventBus` singleton. Agent 2 subscribes to this.

```kotlin
data class RawScrollEvent(
    val timestampMs: Long,          // System.currentTimeMillis()
    val deltaYPx: Float,            // Raw pixel displacement, always positive
    val direction: ScrollDirection, // DOWN or UP
    val sourcePackage: String,      // e.g. "com.instagram.android"
    val sessionId: String,          // UUID, resets on service start
    val confidence: EventConfidence // PRIMARY, FALLBACK_A, FALLBACK_B
)

enum class ScrollDirection { DOWN, UP }
enum class EventConfidence { PRIMARY, FALLBACK_A, FALLBACK_B }
```

### Secondary Output — Foreground Notification
Persistent notification in the status bar while tracking is active.

```
Channel ID  : "scrolltrek_tracking"
Channel Name: "Scroll Tracking"
Importance  : IMPORTANCE_LOW (no sound, no vibration)
Content     : Dynamic — updated by Agent 2 via NotificationManager
```

---

## 6. Accessibility Service Configuration

### Manifest Declaration
```xml
<service
    android:name=".tracking.ScrollTrackingAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:process=":tracker"
    android:exported="true">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService"/>
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config"/>
</service>
```

### `accessibility_service_config.xml`
```xml
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeViewScrolled|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagReportViewIds|flagRetrieveInteractiveWindows"
    android:canRetrieveWindowContent="true"
    android:notificationTimeout="50"
    android:description="@string/accessibility_service_description"/>
```

**Critical:** `android:packageNames` is intentionally OMITTED to capture all apps.

---

## 7. Event Interception — Three-Tier Strategy

### Tier 1 — PRIMARY (Standard API)
**Trigger:** `TYPE_VIEW_SCROLLED`
**Method:** `event.getScrollDeltaY()` (API 30+) with fallback to delta of `event.getScrollY()` across sequential events

```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    if (event.eventType != TYPE_VIEW_SCROLLED) return
    if (isExcludedPackage(event.packageName?.toString())) return
    
    val deltaY = if (Build.VERSION.SDK_INT >= 30) {
        event.scrollDeltaY.toFloat()
    } else {
        deriveDeltaFromAbsolutePosition(event)
    }
    
    if (abs(deltaY) < touchSlopPx) return // noise gate
    emitEvent(deltaY, event.packageName.toString(), EventConfidence.PRIMARY)
}
```

### Tier 2 — FALLBACK_A (Node Tree Delta)
**Trigger:** `TYPE_WINDOW_CONTENT_CHANGED` when Tier 1 is silent for > 300ms during active touch
**Method:** Snapshot visible child Y-coordinates, diff against next snapshot

```kotlin
private var lastNodeSnapshot: Map<String, Float> = emptyMap()

fun computeFallbackDelta(root: AccessibilityNodeInfo): Float {
    val currentSnapshot = buildNodeSnapshot(root)
    val delta = computeYAxisShift(lastNodeSnapshot, currentSnapshot)
    lastNodeSnapshot = currentSnapshot
    return delta
}
```

**Performance constraint:** Node traversal must complete in < 5ms. Abort and discard if depth > 8 levels.

### Tier 3 — FALLBACK_B (Velocity Heuristic)
**Trigger:** Neither Tier 1 nor Tier 2 yields data for > 500ms during confirmed active scroll window
**Method:** Infer from `TYPE_TOUCH_INTERACTION_START/END` + known average scroll velocity profiles
**Confidence:** Marked `FALLBACK_B` — Agent 2 applies 0.7x weight multiplier to these events
**Applicable apps:** TikTok, Instagram Reels, custom OpenGL renderers

---

## 8. Filtering Rules (Noise Gate)

All rules applied in order. First failing rule discards the event.

| # | Rule | Implementation |
|---|---|---|
| 1 | **Touch slop** | `abs(deltaY) >= ViewConfiguration.get(ctx).scaledTouchSlop` |
| 2 | **Screen state** | Discard if `displayManager.getDisplay(0).state != Display.STATE_ON` |
| 3 | **Package exclusion** | Discard if `event.packageName in excludedPackages` |
| 4 | **Duplicate guard** | Discard if identical `(deltaY, packageName)` within 16ms window |
| 5 | **Direction gate** | If `KEY_TRACK_UPWARD_SCROLL == false`, discard negative deltas |
| 6 | **Sanity bound** | Discard if `abs(deltaY) > screenHeightPx * 3` (impossible scroll) |

---

## 9. Background Execution & Resilience

### Process Architecture
```
com.scrolltrek.app        (UI process — can be killed by OOM)
com.scrolltrek.app:tracker (Daemon process — isolated from UI OOM)
    └── ScrollTrackingAccessibilityService
    └── TrackingForegroundService
    └── WatchdogJobService
```

### Foreground Service (Android 14+ Compliant)
```kotlin
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
startForeground(
    NOTIFICATION_ID,
    buildNotification(),
    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
)
```

For API < 34: `startForeground(NOTIFICATION_ID, buildNotification())` — no type parameter.

### Battery Optimization Exemption Request
Trigger during onboarding step 4. Must handle `SecurityException` on some OEM ROMs.
```kotlin
val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
    data = Uri.parse("package:${packageName}")
}
startActivity(intent)
```

### Doze Recovery
```kotlin
// Registered in AndroidManifest, not dynamically, to survive process death
<receiver android:name=".tracking.DozeRecoveryReceiver" android:exported="false">
    <intent-filter>
        <action android:name="android.os.action.DEVICE_IDLE_MODE_CHANGED"/>
    </intent-filter>
</receiver>
```
On idle exit: re-bind accessibility service if `onUnbind()` was previously called.

### Watchdog Job (15-min heartbeat)
```kotlin
JobScheduler.schedule(
    JobInfo.Builder(WATCHDOG_JOB_ID, WatchdogJobService::class)
        .setPeriodic(TimeUnit.MINUTES.toMillis(15))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
        .setPersisted(true)
        .build()
)
```
Watchdog responsibility: Verify service is running → if not, attempt restart → log failure to Agent 2's DB.

---

## 10. In-Memory Ring Buffer

Buffers events in the `:tracker` process before Agent 2's flush cycle.

```kotlin
object ScrollEventBuffer {
    private const val MAX_SIZE = 500
    private val buffer = ArrayDeque<RawScrollEvent>(MAX_SIZE)
    private val lock = Mutex()

    suspend fun push(event: RawScrollEvent) = lock.withLock {
        if (buffer.size >= MAX_SIZE) buffer.removeFirst() // oldest evicted
        buffer.addLast(event)
    }

    suspend fun drainAll(): List<RawScrollEvent> = lock.withLock {
        val drained = buffer.toList()
        buffer.clear()
        drained
    }
}
```

Agent 2 calls `drainAll()` on its flush interval (every 30 seconds).

---

## 11. Acceptance Criteria

All criteria must pass before this agent's output is handed off to integration.

| ID | Criterion | Test Method |
|---|---|---|
| AC-01 | Service detects scroll in Instagram | Manual + Espresso instrumentation |
| AC-02 | Service detects scroll in TikTok (Fallback B) | Manual device test |
| AC-03 | Service detects scroll in Chrome, Reddit, Twitter | Manual test matrix |
| AC-04 | Events below touch slop are discarded | Unit test with mock AccessibilityEvent |
| AC-05 | Excluded packages emit zero events | Unit test with package exclusion config |
| AC-06 | Daemon survives UI process kill | `adb shell am kill com.scrolltrek.app` → verify daemon still emits |
| AC-07 | Daemon recovers after Doze mode exit | ADB Doze simulation: `adb shell dumpsys deviceidle force-idle` |
| AC-08 | No events emitted with screen off | Screen off → scroll with ADB input → verify zero events |
| AC-09 | Buffer does not exceed 500 items | Stress test: fire 600 rapid events → verify size cap |
| AC-10 | CPU usage < 2% during idle scroll monitoring | Android Profiler, 5-minute session on Pixel 6 |
| AC-11 | Memory footprint of `:tracker` process < 24MB | Android Profiler heap dump |
| AC-12 | Foreground notification present within 5s of service start | Instrumented UI test |

---

## 12. Known Edge Cases & Handling

| Edge Case | Expected Behavior |
|---|---|
| User force-stops app | Service dies (unavoidable). Watchdog revives on next JobScheduler window. |
| OEM battery killer (Xiaomi/Samsung) | Onboarding must direct user to OEM-specific exemption settings. Map of OEM intents provided in `OemBatteryHelper.kt` |
| Accessibility service disabled mid-session | Buffer is drained first; `onUnbind()` triggers a flush signal to Agent 2 |
| Foldable device with display change | Re-query `ydpi` from new display metrics on `onConfigurationChanged` |
| Keyboard scrolling (non-touch) | Hardware keyboard scroll events filtered: only `SOURCE_TOUCHSCREEN` accepted |
| Split-screen / multi-window | Track all scroll events regardless of window focus |
| App uses WebView | WebView fires standard `TYPE_VIEW_SCROLLED` — Tier 1 handles correctly |
| PiP window scrolling | Filter by checking `AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY` |

---

## 13. Human Review Gates

The following must be reviewed by a human engineer before the agent's output is merged:

1. **Accessibility Service config XML** — verify `accessibilityEventTypes` is minimal and correct
2. **Package exclusion default list** — confirm banking + health apps pre-excluded
3. **Foreground Service type** — confirm API level branching is correct for target SDK
4. **OEM battery exemption map** — manually verify Xiaomi, Samsung, OnePlus, Huawei intents
5. **Privacy audit** — confirm zero content events (no `TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY`) subscribed anywhere

---

## 14. File Deliverables

```
app/src/main/
├── AndroidManifest.xml                              (modified)
├── res/xml/
│   └── accessibility_service_config.xml
├── java/com/scrolltrek/tracking/
│   ├── ScrollTrackingAccessibilityService.kt
│   ├── TrackingForegroundService.kt
│   ├── ScrollEventBuffer.kt
│   ├── ScrollEventBus.kt                           (StateFlow singleton)
│   ├── WatchdogJobService.kt
│   ├── DozeRecoveryReceiver.kt
│   ├── OemBatteryHelper.kt
│   └── model/
│       └── RawScrollEvent.kt
└── res/values/
    └── strings_accessibility.xml                   (accessibility_service_description)
```

---

*Agent 1 Spec · scrollTrek · v1.0*
