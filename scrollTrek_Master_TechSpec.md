# scrollTrek — Master Application Technical Specification
**Version 1.0 | Engineering Reference**
`Document ID: ST-SPEC-MASTER` | `Classification: Internal Engineering`

---

## 1. Application Identity

| Field | Value |
|---|---|
| Application Name | scrollTrek |
| Package Name | `com.scrolltrek.app` |
| Version (Launch) | 1.0.0 |
| Version Code | 1 |
| Min SDK | 26 (Android 8.0 Oreo) |
| Target SDK | 35 (Android 15) |
| Compile SDK | 35 |
| Language | Kotlin 2.0+ |
| Build System | Gradle 8.x + Android Gradle Plugin 8.x |
| Architecture Pattern | MVVM + Clean Architecture (Repository pattern) |
| DI Framework | Hilt (Dagger 2 backed) |
| Process Count | 2 (`:app` UI process + `:tracker` daemon process) |

---

## 2. System Architecture Diagram

```
╔══════════════════════════════════════════════════════════════════╗
║                    ANDROID OPERATING SYSTEM                      ║
║  ┌─────────────────────┐    ┌───────────────────────────────┐   ║
║  │   :app (UI Process)  │    │   :tracker (Daemon Process)   │   ║
║  │                      │    │                               │   ║
║  │  ┌────────────────┐  │    │  ┌──────────────────────────┐│   ║
║  │  │  Compose UI    │  │    │  │ ScrollTracking           ││   ║
║  │  │  (Screens)     │  │    │  │ AccessibilityService     ││   ║
║  │  └────────┬───────┘  │    │  └──────────┬───────────────┘│   ║
║  │           │           │    │             │                 │   ║
║  │  ┌────────▼───────┐  │    │  ┌──────────▼───────────────┐│   ║
║  │  │  ViewModels    │  │    │  │ ScrollEventBuffer        ││   ║
║  │  └────────┬───────┘  │    │  └──────────┬───────────────┘│   ║
║  │           │           │    │             │                 │   ║
║  │  ┌────────▼───────┐  │    │  ┌──────────▼───────────────┐│   ║
║  │  │  Repository    │◄─╫────╫──│ ScrollConverter          ││   ║
║  │  │  (Agent 2)     │  │    │  │ MilestoneEngine          ││   ║
║  │  └────────┬───────┘  │    │  │ StreakManager             ││   ║
║  │           │           │    │  └──────────┬───────────────┘│   ║
║  └───────────╫───────────┘    └─────────────╫─────────────────┘  ║
║              ║                              ║                     ║
║      ┌───────▼──────────────────────────────▼──────┐            ║
║      │          Room Database (Shared)               │            ║
║      │    scroll_sessions | raw_scroll_records        │            ║
║      │    daily_aggregates | milestones | streaks     │            ║
║      └──────────────────────────────────────────────┘            ║
║                                                                   ║
║   ┌─────────────────────────────────────────────────────────┐   ║
║   │              SYSTEM SERVICES                             │   ║
║   │  AccessibilityManager | WindowManager | JobScheduler    │   ║
║   │  NotificationManager  | MediaStore   | PackageManager   │   ║
║   └─────────────────────────────────────────────────────────┘   ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## 3. Module Structure

```
scrolltrek/
├── app/                          (main application module)
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/scrolltrek/
│   │   │   ├── MainActivity.kt
│   │   │   ├── ScrollTrekApplication.kt
│   │   │   ├── di/               (Hilt modules)
│   │   │   ├── tracking/         (Agent 1 — Daemon)
│   │   │   ├── data/             (Agent 2 — Data Layer)
│   │   │   └── ui/               (Agent 3 — UI)
│   │   ├── res/
│   │   │   ├── xml/accessibility_service_config.xml
│   │   │   ├── drawable/         (landmark vector illustrations)
│   │   │   ├── font/             (Syne, DM Sans)
│   │   │   └── values/
│   │   └── assets/
│   │       └── landmarks.json
│   ├── src/test/                 (Unit tests)
│   └── src/androidTest/          (Instrumented tests)
├── gradle/
│   └── libs.versions.toml        (Version catalog)
├── build.gradle.kts              (Root)
└── settings.gradle.kts
```

---

## 4. Full Dependency Manifest

```toml
# gradle/libs.versions.toml

[versions]
kotlin = "2.0.21"
agp = "8.7.0"
compose-bom = "2025.05.00"
room = "2.7.1"
hilt = "2.52"
hilt-navigation-compose = "1.2.0"
lifecycle = "2.8.7"
navigation-compose = "2.8.7"
coroutines = "1.9.0"
coil = "3.0.4"
vico = "2.0.0"
glance = "1.1.1"
work = "2.10.0"
lottie-compose = "6.6.0"
accompanist-permissions = "0.36.0"
kotlinx-serialization = "1.7.3"
kotlinx-datetime = "0.6.1"
turbine = "1.2.0"
paparazzi = "1.3.5"
junit5 = "5.11.0"
robolectric = "4.14.1"
macrobenchmark = "1.3.4"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
compose-animation = { group = "androidx.compose.animation", name = "animation" }
compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
activity-compose = { group = "androidx.activity", name = "activity-compose", version = "1.9.3" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }

# Lifecycle
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
lifecycle-process = { group = "androidx.lifecycle", name = "lifecycle-process", version.ref = "lifecycle" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }  # KSP

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }
hilt-work = { group = "androidx.hilt", name = "hilt-work", version = "1.2.0" }
hilt-work-compiler = { group = "androidx.hilt", name = "hilt-compiler", version = "1.2.0" }

# WorkManager
work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }

# Coroutines
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

# Serialization
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-datetime = { group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version.ref = "kotlinx-datetime" }

# Image loading
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }

# Charts
vico-compose-m3 = { group = "com.patrykandpatrick.vico", name = "compose-m3", version.ref = "vico" }

# Widget
glance-appwidget = { group = "androidx.glance", name = "glance-appwidget", version.ref = "glance" }
glance-material3 = { group = "androidx.glance", name = "glance-material3", version.ref = "glance" }

# Lottie
lottie-compose = { group = "com.airbnb.android", name = "lottie-compose", version.ref = "lottie-compose" }

# Permissions
accompanist-permissions = { group = "com.google.accompanist", name = "accompanist-permissions", version.ref = "accompanist-permissions" }

# Testing
junit5-api = { group = "org.junit.jupiter", name = "junit-jupiter-api", version.ref = "junit5" }
junit5-engine = { group = "org.junit.jupiter", name = "junit-jupiter-engine", version.ref = "junit5" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
paparazzi = { group = "app.cash.paparazzi", name = "paparazzi", version.ref = "paparazzi" }
macrobenchmark = { group = "androidx.benchmark", name = "benchmark-macro-junit4", version.ref = "macrobenchmark" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version = "3.6.1" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.25" }
room = { id = "androidx.room", version.ref = "room" }
paparazzi = { id = "app.cash.paparazzi", version.ref = "paparazzi" }
```

---

## 5. Android Manifest — Complete

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.scrolltrek.app">

    <!-- ═══════════════ PERMISSIONS ═══════════════ -->

    <!-- Accessibility Service binding (system-granted, not user-runtime) -->
    <uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE"
        tools:ignore="ProtectedPermissions"/>

    <!-- Foreground Service -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/>

    <!-- Battery optimization exemption request -->
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"/>

    <!-- Floating overlay -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>

    <!-- Notifications (Android 13+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>

    <!-- Share card — write to Pictures/ -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28"/>
    <!-- API 29+: MediaStore.Images — no permission needed -->

    <!-- Persistent job scheduling -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>

    <!-- ═══════════════ FEATURES ═══════════════ -->
    <uses-feature android:name="android.hardware.touchscreen" android:required="true"/>

    <!-- ═══════════════ APPLICATION ═══════════════ -->
    <application
        android:name=".ScrollTrekApplication"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.ScrollTrek"
        android:largeHeap="false"
        android:allowBackup="false"
        tools:targetApi="35">

        <!-- ── MAIN ACTIVITY ── -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>

        <!-- ── ACCESSIBILITY SERVICE (isolated process) ── -->
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

        <!-- ── FOREGROUND SERVICE (isolated process) ── -->
        <service
            android:name=".tracking.TrackingForegroundService"
            android:process=":tracker"
            android:foregroundServiceType="specialUse"
            android:exported="false">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="scrolling_distance_tracker"/>
        </service>

        <!-- ── WATCHDOG JOB ── -->
        <service
            android:name=".tracking.WatchdogJobService"
            android:process=":tracker"
            android:permission="android.permission.BIND_JOB_SERVICE"
            android:exported="false"/>

        <!-- ── DOZE RECOVERY RECEIVER ── -->
        <receiver
            android:name=".tracking.DozeRecoveryReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="android.os.action.DEVICE_IDLE_MODE_CHANGED"/>
            </intent-filter>
        </receiver>

        <!-- ── BOOT RECEIVER (restart watchdog after reboot) ── -->
        <receiver
            android:name=".tracking.BootReceiver"
            android:exported="false">
            <intent-filter android:priority="999">
                <action android:name="android.intent.action.BOOT_COMPLETED"/>
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED"/>
            </intent-filter>
        </receiver>

        <!-- ── HOME SCREEN WIDGET ── -->
        <receiver
            android:name=".ui.widget.ScrollTrekWidgetReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE"/>
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/scroll_trek_widget_info"/>
        </receiver>

        <!-- ── WORK MANAGER INITIALIZATION ── -->
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup"/>
        </provider>

    </application>
</manifest>
```

---

## 6. Hilt Dependency Graph

```kotlin
@Module @InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): ScrollTrekDatabase =
        Room.databaseBuilder(ctx, ScrollTrekDatabase::class.java, "scrolltrek.db")
            .enableMultiInstanceInvalidation()  // Cross-process Room access
            .build()

    @Provides fun provideScrollRecordDao(db: ScrollTrekDatabase) = db.scrollRecordDao()
    @Provides fun provideMilestoneDao(db: ScrollTrekDatabase) = db.milestoneDao()
    @Provides fun provideStreakDao(db: ScrollTrekDatabase) = db.streakDao()
}

@Module @InstallIn(SingletonComponent::class)
object SystemModule {
    @Provides @Singleton
    fun provideWindowManager(@ApplicationContext ctx: Context): WindowManager =
        ctx.getSystemService(WindowManager::class.java)

    @Provides @Singleton
    fun provideSharedPreferences(@ApplicationContext ctx: Context): SharedPreferences =
        ctx.getSharedPreferences("scrolltrek_prefs", Context.MODE_PRIVATE)

    @Provides @Singleton
    fun provideDisplayMetricsProvider(wm: WindowManager) = DisplayMetricsProvider(wm)
}

@Module @InstallIn(ServiceComponent::class)
object TrackerModule {
    @Provides
    fun provideScrollConverter(dmp: DisplayMetricsProvider) = ScrollConverter(dmp)

    @Provides
    fun provideMilestoneEngine(dao: MilestoneDao, repo: LandmarkRepository) =
        MilestoneEngine(dao, repo)
}
```

---

## 7. Data Flow — End-to-End Sequence

```
User scrolls in Instagram
    │
    ▼
[Android OS]  AccessibilityEvent(TYPE_VIEW_SCROLLED, deltaY=350px)
    │
    ▼
[Agent 1]  ScrollTrackingAccessibilityService.onAccessibilityEvent()
           ├── Noise filter: 350px > touchSlop(24px) ✓
           ├── Package filter: "com.instagram.android" not excluded ✓
           ├── Screen on: ✓
           └── Emit → ScrollEventBuffer.push(RawScrollEvent(350px, PRIMARY))
    │
    ▼  [every 30s or 400 events]
[Agent 2]  ScrollRepository.flushCycle()
           ├── buffer.drainAll() → [RawScrollEvent, ...]
           ├── ScrollConverter.toMeters(350px, ydpi=420) = 0.02116m
           ├── Batch INSERT raw_scroll_records
           ├── Upsert DailyAggregate (add 0.02116m to today)
           ├── Update StreakRecord (if first activity today)
           ├── Query lifetimeTotal = 827.99m
           ├── MilestoneEngine.evaluate(827.99m)
           │   └── Burj Khalifa (828.0m): NOT YET unlocked (0.01m short)
           └── Emit via StateFlow → ViewModels update
    │
    ▼  [next flush cycle, 30s later]
[Agent 2]  lifetimeTotal now = 828.04m
           MilestoneEngine.evaluate(828.04m)
           └── Burj Khalifa: UNLOCK! → milestoneUnlockEvents.emit(burjKhalifa)
    │
    ▼
[Agent 3]  HomeViewModel observes milestoneUnlockEvents
           └── navController.navigate("milestone/burj_khalifa")
    │
    ▼
[Agent 3]  MilestoneRevealScreen displays 4-phase animation
           └── User taps "Share"
    │
    ▼
[Agent 3]  ShareCardRenderer.generateCard(burjKhalifa, 828.04)
           ├── Headless ComposeView renders ShareCardComposable
           ├── PixelCopy captures 1080×1080 bitmap
           ├── Saved to /Pictures/scrollTrek/burj_khalifa_1748044800.jpg
           └── Intent.ACTION_SEND → Android Share Sheet
```

---

## 8. Performance Budgets

| Metric | Budget | Measurement Tool |
|---|---|---|
| App cold start to Home screen | < 1200ms | Macrobenchmark |
| App warm start | < 400ms | Macrobenchmark |
| Scroll event processing latency | < 2ms per event | Custom trace |
| DB batch flush (500 events) | < 100ms | Room benchmark |
| Share card render time | < 3000ms | Systrace |
| `:tracker` process memory | < 24MB | Android Profiler |
| `:app` process memory (Home) | < 80MB | Android Profiler |
| CPU usage during active scrolling | < 2% | Battery Historian |
| CPU usage at idle (tracking on, screen on) | < 0.5% | Battery Historian |
| Battery drain per hour (active) | < 0.5% | Battery Historian |
| UI frame rate (all screens) | 60fps target, 90fps capable | Perfetto |
| Jank frames | < 0.1% | Perfetto |

---

## 9. Permission Rationale Matrix

| Permission | Why Needed | Alternatives Considered | Privacy Impact |
|---|---|---|---|
| `BIND_ACCESSIBILITY_SERVICE` | Global scroll event interception | None — no other API provides this | High (by design) — mitigated by content event filtering |
| `FOREGROUND_SERVICE` | Keep daemon alive | `WorkManager` — insufficient for continuous tracking | None |
| `FOREGROUND_SERVICE_SPECIAL_USE` | API 34+ FGS requirement | N/A (required by OS) | None |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent Doze killing daemon | User education only — unreliable | None |
| `SYSTEM_ALERT_WINDOW` | Floating overlay | Cannot achieve with standard views | Low — user-initiated, closeable |
| `POST_NOTIFICATIONS` | Milestone alerts | Silent operation — worse UX | None |
| `RECEIVE_BOOT_COMPLETED` | Restart watchdog after reboot | Manual restart — poor UX | None |
| `WRITE_EXTERNAL_STORAGE` (≤ API 28) | Save share card images | `MediaStore` API (used for 29+) | Low — user-generated images only |

**Permissions explicitly NOT requested:**
- `INTERNET` — zero network calls, ever
- `READ_CONTACTS`, `ACCESS_FINE_LOCATION` — not used
- `CAMERA`, `MICROPHONE` — not used
- `READ_CALL_LOG`, `READ_SMS` — not used

---

## 10. Security Specification

### Data Security
| Concern | Control |
|---|---|
| Database encryption | Not required (local personal wellness data, not credentials) |
| SharedPreferences | Default mode (`MODE_PRIVATE`) — process-isolated |
| MediaStore output | World-readable (standard for user-generated shareable content) |
| IPC between processes | `StateFlow` + Room multi-instance — no network sockets |
| Third-party SDKs | Zero analytics SDKs, zero ad SDKs, zero crash reporters that transmit data |

### Accessibility Service Security
The Accessibility Service config is intentionally minimal:
- Event types: `typeViewScrolled` + `typeWindowContentChanged` ONLY
- No `TYPE_VIEW_TEXT_CHANGED`, `TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY`
- No `canPerformGestures` — read-only observation
- No `canRequestFilterKeyEvents` — keyboard events not intercepted
- `canRetrieveWindowContent` required for Fallback A only; scoped to scrollable node detection

### Pre-loaded App Exclusion List (Default)
```kotlin
val DEFAULT_EXCLUDED_PACKAGES = setOf(
    // Banking
    "com.chase.sig.android", "com.bankofamerica.cmapp", "com.wellsfargo.mobile",
    "com.usaa.mobile.android.usaa", "com.schwab.android",
    // Health
    "com.google.android.apps.fitness", "com.myfitnesspal.android",
    "com.whoop", "com.apple.android.health",
    // Password managers
    "com.lastpass.lpandroid", "com.onepassword.android", "com.agilebits.onepassword",
    // System
    "com.android.systemui", "com.google.android.inputmethod.latin",
    "com.samsung.android.incallui"
)
```

---

## 11. Testing Strategy

### Test Pyramid

```
         ╱╲
        ╱ E2╲        End-to-End (Espresso + UI Automator)
       ╱──────╲      ~10 critical user journeys
      ╱  Integ  ╲    Integration (Instrumented, Room in-memory)
     ╱────────────╲  ~30 tests — repository + engine contracts
    ╱  Unit Tests   ╲ JUnit5 + Robolectric + Turbine
   ╱──────────────────╲ ~100 tests — pure logic, flows, math
```

### Critical Test Scenarios

**Daemon Resilience**
- T01: UI process killed → daemon continues emitting events
- T02: Device enters Doze → exits Doze → daemon recovers
- T03: User force-stops app → reopens → onboarding not shown, tracking resumes
- T04: App updated → daemon restarts → no data loss

**Data Accuracy**
- T05: 1000 events at 400 dpi → total distance within 0.1% of expected
- T06: FALLBACK_B events contribute 70% weight to total
- T07: Excluded app events never appear in DB

**Milestone Engine**
- T08: Milestone fires exactly once at threshold
- T09: Multiple milestones crossed in single flush → all fire
- T10: App reinstalled → previously unlocked milestones remain unlocked (DB survived)

**Streak Logic**
- T11: Day 1 → Day 2 → Day 3 → streak = 3
- T12: Day 1 → miss Day 2 → freeze token used → streak continues
- T13: Day 1 → miss Day 2 → no freeze tokens → streak resets to 1

**UI Correctness**
- T14: Distance "1000m" formats as "1.0 km"
- T15: Share card renders at exactly 1080×1080px
- T16: Onboarding Step 2 auto-advances within 2s of accessibility grant

---

## 12. Release Configuration

### Build Variants
```kotlin
buildTypes {
    debug {
        applicationIdSuffix = ".debug"
        debuggable = true
        // Fast flush interval for testing: 5s instead of 30s
        buildConfigField("Int", "FLUSH_INTERVAL_SECONDS", "5")
    }
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        buildConfigField("Int", "FLUSH_INTERVAL_SECONDS", "30")
        signingConfig = signingConfigs["release"]
    }
}
```

### ProGuard Critical Rules
```proguard
# Room — keep all entity classes
-keep class com.scrolltrek.data.db.entity.** { *; }

# Accessibility Service — must not be obfuscated
-keep class com.scrolltrek.tracking.ScrollTrackingAccessibilityService { *; }

# Landmark model — deserialized from JSON asset
-keep class com.scrolltrek.data.model.Landmark { *; }
-keepclassmembers class com.scrolltrek.data.model.** { *; }

# Hilt — generated components
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
```

### Play Store Configuration
| Field | Value |
|---|---|
| Category | Health & Fitness |
| Content Rating | Everyone |
| Target Countries | Global (English first) |
| App Bundle | Yes (AAB, not APK) |
| Feature Graphic | Required — show landmark card art |
| Required Screenshots | Phone: 8 screenshots minimum |
| Privacy Policy URL | Required (accessibility permission mandates this) |

---

## 13. Cross-Agent Interface Summary

This table defines the **exact boundaries** between the three agents. Nothing crosses these boundaries except what's listed here.

| Interface | From | To | Mechanism |
|---|---|---|---|
| `RawScrollEvent` stream | Agent 1 | Agent 2 | `SharedFlow` in `ScrollEventBus` singleton |
| Display `ydpi` | Device hardware | Agent 2 | `DisplayMetricsProvider.refresh()` |
| `LiveSessionState` | Agent 2 | Agent 3 | `StateFlow` observed in ViewModel |
| `DailySummary` | Agent 2 | Agent 3 | `Flow<DailySummary>` from Repository |
| `LandmarkProgress` | Agent 2 | Agent 3 | `Flow<LandmarkProgress>` from Repository |
| `WeeklyAnalytics` | Agent 2 | Agent 3 | `Flow<WeeklyAnalytics>` from Repository |
| `StreakState` | Agent 2 | Agent 3 | `Flow<StreakState>` from Repository |
| `milestoneUnlockEvents` | Agent 2 | Agent 3 | `SharedFlow<Landmark>` — hot, replay=0 |
| Notification subtitle string | Agent 2 | Agent 1 | `getNotificationSubtitle(): String` (sync call) |
| Configuration prefs | Agent 3 | Agents 1 & 2 | `SharedPreferences` — read at startup + observed |

---

## 14. Known Limitations & Future Work

| Limitation | Notes | Target Version |
|---|---|---|
| iOS not supported | Accessibility API equivalent (`ENSetting`) too restrictive | v3.0 (if ever) |
| Cloud sync absent | By design (privacy-first). May add E2E encrypted optional backup | v2.0 |
| Friends leaderboard | Code-share via QR (no accounts required) | v1.5 |
| World Tour Mode | Sequential landmark journey across a geographic route | v1.5 |
| Content-aware scroll filtering | e.g. "don't count maps scroll" — requires content reading → privacy boundary | Will not implement |
| Wear OS companion | Glanceable daily distance on watch face | v2.0 |
| Horizontal scroll tracking | Useful for carousels; currently direction-filtered | v1.2 |
| ML-based app classification | Auto-detect "doomscrolling" apps vs. productive scroll | Investigating |

---

*scrollTrek Master Technical Specification · v1.0 · May 2026*
*For questions: reference Agent specs ST-AG-01, ST-AG-02, ST-AG-03*
