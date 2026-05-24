# AGENT 3 — THE UI
## Technical Specification Sheet
**scrollTrek | Antigravity Agent Brief**
`Agent ID: ST-AG-03` | `Owner: Presentation & Egress` | `Priority: P0 — Critical Path`

---

## 1. Mission Statement

> Build every pixel the user ever sees. This agent owns the Jetpack Compose UI across all screens, the floating overlay, the milestone share card headless renderer, the home screen widget, and all onboarding flows. It consumes data from Agent 2's Flow contracts and emits configuration writes back to SharedPreferences.

---

## 2. Scope Boundary

### Owns
- All Jetpack Compose screens (Home, Journey, Insights, Settings)
- Bottom navigation shell
- Onboarding flow (5 steps, all permission UX)
- Floating overlay (`WindowManager` view)
- Milestone unlock animation + full-screen reveal
- Share card headless renderer (Compose → Bitmap)
- Home screen widget (`GlanceAppWidget`)
- Theming system (Dark / Light / AMOLED)
- App exclusion list UI
- All SharedPreferences writes (configuration state)
- All string resources, color tokens, typography definitions

### Does NOT Own
- Any business logic (Agent 2)
- Scroll event capture (Agent 1)
- Database operations (Agent 2)
- Landmark dataset (Agent 2 provides via `Flow`)

---

## 3. Tech Stack

| Concern | Technology |
|---|---|
| UI Framework | Jetpack Compose (BOM latest stable) |
| Navigation | Compose Navigation 2.7+ |
| State | `collectAsStateWithLifecycle()` from Lifecycle 2.6+ |
| ViewModels | Hilt ViewModel (`@HiltViewModel`) |
| Overlay | `WindowManager.addView()` with `TYPE_APPLICATION_OVERLAY` |
| Share Card Rendering | `ComposeView` (headless) + `PixelCopy` API (API 26+) |
| Widget | Jetpack Glance (`GlanceAppWidget`) |
| Animations | Compose `animate*AsState`, `AnimatedVisibility`, `LottieComposable` |
| Icons | Material Icons Extended + custom vector drawables |
| Charts | Compose Charts (Vico library) for weekly bar chart & heatmap |
| Image Loading | Coil 3 (for landmark illustrations) |
| Permissions | Accompanist Permissions (deprecated; migrate to Compose 1.7 Permission APIs) |
| Theming | Material 3 (`MaterialTheme` with custom `ColorScheme`) |

---

## 4. Input Contract (from Agent 2)

All consumed via ViewModel `collectAsStateWithLifecycle()`.

```kotlin
// Injected into ViewModels via @HiltViewModel
interface ScrollDataRepository {
    val liveSession: StateFlow<LiveSessionState>
    val todaySummary: Flow<DailySummary>
    val landmarkProgress: Flow<LandmarkProgress>
    val weeklyAnalytics: Flow<WeeklyAnalytics>
    val streakState: Flow<StreakState>
    val milestoneUnlockEvents: SharedFlow<Landmark>
    val allLandmarks: Flow<List<LandmarkWithStatus>>
    val appBreakdown: Flow<List<AppScrollTotal>>
    val hourlyHeatmap: Flow<List<HourlyTotal>>
}
```

---

## 5. Output Contract (writes to SharedPreferences)

```kotlin
// Written by SettingsViewModel, read by Agents 1 & 2
const val KEY_TRACKING_ENABLED = "tracking_enabled"         // Boolean
const val KEY_EXCLUDED_PACKAGES = "excluded_packages"       // Set<String>
const val KEY_OVERLAY_ENABLED = "overlay_enabled"           // Boolean
const val KEY_OVERLAY_POSITION_X = "overlay_pos_x"         // Float
const val KEY_OVERLAY_POSITION_Y = "overlay_pos_y"         // Float
const val KEY_DAILY_GOAL_METERS = "daily_goal_meters"       // Float
const val KEY_TRACK_UPWARD = "track_upward_scroll"          // Boolean
const val KEY_THEME = "app_theme"                           // "light"|"dark"|"amoled"|"system"
const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"   // Boolean
```

---

## 6. Design System

### 6.1 Color Tokens
```kotlin
// Light Theme
val Primary = Color(0xFF1A1A2E)       // Deep midnight blue
val PrimaryContainer = Color(0xFF16213E)
val Accent = Color(0xFFE94560)        // Signal red — milestone moments
val Surface = Color(0xFFF8F7F4)       // Warm white
val OnSurface = Color(0xFF1A1A2E)
val Outline = Color(0xFFE0DDD8)

// Dark Theme
val PrimaryDark = Color(0xFFE8E6E1)
val SurfaceDark = Color(0xFF0F0F1A)
val SurfaceVariantDark = Color(0xFF1A1A2E)

// AMOLED Theme — pure black surfaces
val SurfaceAmoled = Color(0xFF000000)
val SurfaceVariantAmoled = Color(0xFF0A0A0A)

// Tier Colors
val TierCommon = Color(0xFF8B8B8B)     // Stone
val TierNotable = Color(0xFFB87333)    // Copper
val TierLandmark = Color(0xFFD4AF37)   // Gold
val TierLegendary = Color(0xFFE8D5FF)  // Holographic base
```

### 6.2 Typography
```kotlin
val scrollTrekTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.syne_extrabold)),
        fontSize = 57.sp, letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily(Font(R.font.syne_bold)),
        fontSize = 45.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.syne_semibold)),
        fontSize = 32.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily(Font(R.font.dm_sans_regular)),
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily(Font(R.font.dm_sans_medium)),
        fontSize = 11.sp, letterSpacing = 0.5.sp
    )
)
// Fonts: Syne (display/headings) + DM Sans (body/labels)
// Downloaded via Google Fonts Gradle plugin — no manual .ttf files
```

### 6.3 Shape System
```kotlin
val scrollTrekShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
```

---

## 7. Navigation Architecture

```kotlin
// Root NavGraph
@Composable
fun ScrollTrekNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = if (onboardingComplete) "home" else "onboarding") {
        
        composable("onboarding") { OnboardingFlow(onComplete = { navController.navigate("home") }) }
        
        // Main scaffold with bottom nav
        composable("home")     { MainScaffold(startTab = Tab.HOME) }
        composable("journey")  { MainScaffold(startTab = Tab.JOURNEY) }
        composable("insights") { MainScaffold(startTab = Tab.INSIGHTS) }
        
        // Modal destinations (no bottom nav)
        composable("milestone/{landmarkId}") { MilestoneRevealScreen(it.arguments) }
        composable("landmark_detail/{id}")   { LandmarkDetailSheet(it.arguments) }
        composable("settings")               { SettingsScreen() }
        composable("app_exclusions")         { AppExclusionScreen() }
    }
}

// Bottom nav tabs
enum class Tab(val icon: ImageVector, val label: String, val route: String) {
    HOME(Icons.Rounded.Home, "Home", "home"),
    JOURNEY(Icons.Rounded.Map, "Journey", "journey"),
    INSIGHTS(Icons.Rounded.BarChart, "Insights", "insights")
}
```

---

## 8. Screen Specifications

### 8.1 Onboarding Flow

5-step pager (`HorizontalPager`). Cannot skip steps 2–4 (permissions are required).

**Step 1 — Value Prop**
- Full-screen Lottie animation: finger scrolling → world map expanding
- Headline: "Your scrolling, mapped to the real world"
- Subtext: "Find out how far your thumb really travels"
- CTA: "Let's go" → Step 2

**Step 2 — Accessibility Permission**
- Visual diagram: phone silhouette with scroll arrows (distance only, no content)
- Headline: "One permission changes everything"
- Explicit statements (use `BulletPoint` composable):
  - ✓ "We measure how far you scroll"
  - ✓ "We track which apps you scroll in"
  - ✗ "We never read what you see"
  - ✗ "We never take screenshots"
  - ✗ "We never send data off your device"
- Privacy badge: padlock icon + "100% local. No servers. No accounts."
- CTA: "Grant access" → launches `Settings.ACTION_ACCESSIBILITY_SETTINGS`
- Verification: poll `AccessibilityManager.isEnabled` every 1s; auto-advance when granted
- Back behavior: CTA becomes "Not now" → shows consequence modal → still requires grant to proceed

**Step 3 — Notifications**
- Minimal: "Get notified when you unlock a landmark"
- Illustration: milestone card mockup
- CTA: `ActivityCompat.requestPermissions(POST_NOTIFICATIONS)` (Android 13+)
- Skippable on API < 33 (permission implicit)

**Step 4 — Battery Optimization**
- Context card: "scrollTrek tracks in the background — this keeps it reliable"
- OEM detection: show OEM-specific instruction if Xiaomi/Samsung/OnePlus detected
- CTA: `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- Skippable (warn user: "tracking may stop when phone sleeps")

**Step 5 — All Set**
- Celebration: confetti Lottie + first landmark card preview (animated reveal)
- Headline: "You're being tracked" (intentionally funny)
- Subtext: "Start scrolling any app. We'll keep count."
- CTA: "See my dashboard" → navigate to Home, clear backstack

---

### 8.2 Home Screen

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val session by viewModel.liveSession.collectAsStateWithLifecycle()
    val today by viewModel.todaySummary.collectAsStateWithLifecycle()
    val progress by viewModel.landmarkProgress.collectAsStateWithLifecycle()
    val streak by viewModel.streakState.collectAsStateWithLifecycle()
    val recentUnlock by viewModel.milestoneUnlock.collectAsStateWithLifecycle()

    // Milestone unlock: auto-navigate to reveal screen
    LaunchedEffect(recentUnlock) {
        recentUnlock?.let { navController.navigate("milestone/${it.id}") }
    }

    LazyColumn {
        item { GreetingHeader(streak) }
        item { TodayDistanceCard(today) }          // Hero card
        item { LandmarkProgressArc(progress) }     // Arc progress indicator
        item { WeeklyBarChart(viewModel.weekly) }
        item { RecentUnlocksCarousel(progress.recentlyUnlocked) }
        item { QuickStatsRow(session) }
    }
}
```

**TodayDistanceCard spec:**
- Large display number: `formatDistance(meters)` → "247 m" or "1.3 km"
- Secondary: "Lifetime: 12.4 km"
- Streak badge (top-right corner): flame icon + streak count
- Background: subtle gradient using `Primary` + slight elevation shadow
- Tap: expands to show session breakdown

**LandmarkProgressArc spec:**
- Circular arc progress indicator (270° sweep)
- Center: landmark illustration (Coil image)
- Below center: landmark name (Syne Bold, 18sp)
- Below name: "X meters to go" (DM Sans, 14sp, muted)
- Animation: `animateFloatAsState` on progress fraction, 800ms spring

---

### 8.3 Journey Screen (Landmarks)

```kotlin
@Composable
fun JourneyScreen(viewModel: JourneyViewModel = hiltViewModel()) {
    val landmarks by viewModel.allLandmarks.collectAsStateWithLifecycle()

    // Sticky tier filter chips at top
    TierFilterRow(selectedTier, onTierSelected)

    // Staggered 2-column grid
    LazyVerticalStaggeredGrid(columns = StaggeredGridCells.Fixed(2)) {
        items(filteredLandmarks) { item ->
            LandmarkCard(
                landmark = item.landmark,
                isUnlocked = item.isUnlocked,
                onTap = { navController.navigate("landmark_detail/${item.landmark.id}") }
            )
        }
    }
}
```

**LandmarkCard spec (unlocked):**
- Full-color illustration (Coil, crossfade)
- Tier color accent border (2dp)
- Name + location text
- Unlock date (muted, small)
- Share button (bottom-right, icon only)

**LandmarkCard spec (locked):**
- Illustration desaturated (ColorFilter grayscale)
- Lock icon overlay
- Distance remaining badge
- No tap action except to preview detail

**LandmarkDetailBottomSheet spec:**
- `ModalBottomSheet` (Material 3)
- Large illustration (full width, 200dp height)
- Tier badge (chip with tier color)
- Distance in meters + feet (toggle)
- Fun fact text block
- If unlocked: "Share Card" primary button → triggers card generation
- If locked: distance progress bar + "You need Xm more"

---

### 8.4 Insights Screen

```kotlin
@Composable
fun InsightsScreen(viewModel: InsightsViewModel = hiltViewModel()) {
    LazyColumn {
        item { StreakWidget(streakState) }
        item { WeeklyBarChart(weekly, modifier) }    // Vico ColumnChart
        item { ScrollHeatmap(hourly) }               // 24-column time heatmap
        item { AppBreakdownList(appBreakdown) }      // Top 5 apps with bar segments
        item { LifetimeStatsCard(lifetime) }
        item { GoalSettingCard(goal, onGoalChange) }
    }
}
```

**ScrollHeatmap spec:**
- 24 columns (hours 0–23)
- Cell height: 48dp
- Cell color: interpolated from `Surface` (0 scrolls) to `Accent` (max scrolls)
- X-axis labels: "12AM", "6AM", "12PM", "6PM"
- Tap on hour cell: tooltip with exact distance

**AppBreakdownList spec:**
- Top 5 apps, ranked by today's scroll distance
- App icon (fetched via `PackageManager`)
- App name + distance label
- Horizontal progress bar (fraction of total today)
- Last item: "X other apps" collapsed, tap to expand

---

### 8.5 Milestone Reveal Screen

Triggered automatically when `milestoneUnlockEvents` emits. Full-screen modal.

```
Phase 1 (0–600ms): Black screen, particle burst (Lottie)
Phase 2 (600–1200ms): Landmark illustration scales in (spring animation)
Phase 3 (1200–2000ms): Text slides up: "YOU JUST SCROLLED AS HIGH AS"
                                        "THE BURJ KHALIFA"
Phase 4 (2000ms+): Share card slides in from bottom + two CTAs
                   [Share] [Back to Dashboard]
```

**Share CTA behavior:**
- Triggers headless card render (Section 9)
- On render complete → `Intent.ACTION_SEND` with bitmap URI
- Bitmap saved to `/Pictures/scrollTrek/[landmark_id]_[timestamp].jpg`

---

## 9. Share Card Headless Renderer

### Architecture
```kotlin
class ShareCardRenderer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun generateCard(landmark: Landmark, lifetimeM: Double): Uri {
        return withContext(Dispatchers.Main) {
            // 1. Create a ComposeView, attach to a WindowManager ghost window
            val composeView = ComposeView(context).apply {
                setContent { ShareCardComposable(landmark, lifetimeM) }
            }
            val wm = context.getSystemService(WindowManager::class.java)
            val layoutParams = WindowManager.LayoutParams(
                1080, 1080,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.RGBA_8888
            ).apply { alpha = 0f }  // invisible ghost
            wm.addView(composeView, layoutParams)
            
            // 2. Wait for layout pass
            composeView.awaitNextLayout()

            // 3. Capture via PixelCopy
            val bitmap = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
            PixelCopy.request(composeView, bitmap, {}, Handler(Looper.getMainLooper()))

            // 4. Save to MediaStore
            val uri = saveBitmapToMediaStore(bitmap, landmark.id)
            
            // 5. Clean up ghost view
            wm.removeView(composeView)
            
            uri
        }
    }
}
```

### `ShareCardComposable` Layout
```
Size: 1080×1080dp (forced via `requiredSize`)
Background: Gradient (cardGradientStart → cardGradientEnd) + tier-specific texture overlay

┌─────────────────────────────────────┐
│ scrollTrek logo (top-left, 32dp)    │
│                                     │
│                                     │
│   [Landmark Illustration]           │
│   (center, ~480×480dp)              │
│                                     │
│                                     │
│ ─────────────────────────────────── │  1dp divider, 60% opacity
│                                     │
│  I scrolled as high as              │  DM Sans Regular, 20sp, muted
│  THE BURJ KHALIFA                   │  Syne ExtraBold, 48sp, white
│                                     │
│  828 meters · Dubai, UAE            │  DM Sans, 16sp, 70% opacity
│                                     │
│  [Tier badge chip]  [Fun fact text] │
│                                     │
│  scrolltrek.app                     │  Bottom-right, 12sp, 50% opacity
└─────────────────────────────────────┘
```

### Tier-Specific Styles

| Tier | Gradient | Overlay | Text Color |
|---|---|---|---|
| COMMON | `#4A4A4A` → `#2A2A2A` | Subtle noise texture | White 90% |
| NOTABLE | `#7B4F1E` → `#3D2610` | Copper foil shimmer | White |
| LANDMARK | `#8B6914` → `#3D2E08` | Gold grain overlay | White |
| LEGENDARY | `#2D1B69` → `#0D0520` | Prismatic shimmer Lottie | White + glow |

---

## 10. Floating Overlay

### Permission Check
```kotlin
// Before showing overlay, verify permission
if (!Settings.canDrawOverlays(context)) {
    // Navigate user to Settings.ACTION_MANAGE_OVERLAY_PERMISSION
}
```

### View Hierarchy
```kotlin
// Inflated into WindowManager
ComposeView → ScrollTrekOverlay()
```

### States
```kotlin
sealed class OverlayState {
    object Minimized : OverlayState()  // 12dp dot, pulsing
    data class Compact(val distanceM: Double, val toNextM: Double) : OverlayState()
    data class Expanded(val session: LiveSessionState, val progress: LandmarkProgress) : OverlayState()
}
```

**Compact pill spec:**
- 160×40dp pill, corner radius 20dp
- Background: `Surface` @ 90% alpha with blur (RenderEffect API 31+)
- Text: "↕ 47m · 🏔 83m left" (DM Sans Medium, 12sp)
- Drag handle: entire view is draggable via `MotionEvent` → updates SharedPreferences position

**Auto-hide triggers:**
- Detect `FLAG_FULLSCREEN` on active window via `AccessibilityWindowInfo`
- Lock screen: `KeyguardManager.isKeyguardLocked`
- System UI (recent apps, notifications): `packageName == "com.android.systemui"`

---

## 11. Home Screen Widget

```kotlin
class ScrollTrekWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = repository.getTodaySummary()
        val progress = repository.getLandmarkProgress()

        provideContent {
            GlanceTheme {
                Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {
                    Text("scrollTrek", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    Text(formatDistance(today.totalDistanceM), style = TextStyle(fontSize = 28.sp))
                    Text("Today", style = TextStyle(fontSize = 12.sp))
                    Spacer()
                    LinearProgressIndicator(progress.progressFraction)
                    Text("${formatDistance(progress.distanceToNextM)} to ${progress.nextLandmark.name}",
                         style = TextStyle(fontSize = 11.sp))
                }
            }
        }
    }
}
```

Widget sizes: 2×1 (compact), 2×2 (standard), 4×2 (detailed).
Update interval: 30 minutes via `GlanceAppWidgetManager.requestPinAppWidget`.

---

## 12. Utility Functions

```kotlin
// Shared across all screens
object DistanceFormatter {
    fun format(meters: Double): String = when {
        meters < 1.0    -> "${(meters * 100).roundToInt()} cm"
        meters < 1000.0 -> "${meters.roundToInt()} m"
        meters < 10000  -> "${"%.1f".format(meters / 1000)} km"
        else            -> "${"%.0f".format(meters / 1000)} km"
    }
}

// Relative date labels
fun dateKeyToLabel(dateKey: String): String {
    return when (dateKey) {
        LocalDate.now().toString()              -> "Today"
        LocalDate.now().minusDays(1).toString() -> "Yesterday"
        else                                    -> dateKey  // "2026-05-22"
    }
}
```

---

## 13. Acceptance Criteria

| ID | Criterion | Test Method |
|---|---|---|
| AC-01 | All 5 onboarding steps reachable and completable | Espresso UI test |
| AC-02 | Accessibility permission grant auto-advances Step 2 within 2s | Espresso + mock AccessibilityManager |
| AC-03 | Home screen renders within 300ms of navigation | Benchmark (Macrobenchmark) |
| AC-04 | Milestone reveal plays all 4 phases in correct sequence | Compose UI test with animation clock |
| AC-05 | Share card renders as 1080×1080 JPEG, < 500KB | File size assertion test |
| AC-06 | Share card bitmap matches golden screenshot per tier | Paparazzi screenshot test |
| AC-07 | Overlay does not intercept touch events on underlying app | Manual test: tap through overlay on 5 apps |
| AC-08 | Overlay auto-hides in YouTube fullscreen | Instrumented test with YouTube open |
| AC-09 | Overlay position persists across device reboots | SharedPreferences read after reboot |
| AC-10 | Dark/AMOLED themes have no pure-white surfaces on OLED | Screenshot analysis (check max brightness pixel) |
| AC-11 | All text meets WCAG AA contrast ratio (4.5:1 minimum) | Accessibility scanner |
| AC-12 | Widget updates within 35 minutes of new scroll data | Manual test + widget update log |
| AC-13 | LandmarkCard transition animation runs at 60fps | Systrace / Perfetto capture |
| AC-14 | Journey screen handles 50 landmarks without jank | Macrobenchmark scrolling test |

---

## 14. File Deliverables

```
app/src/main/java/com/scrolltrek/ui/
├── theme/
│   ├── Color.kt
│   ├── Typography.kt
│   ├── Shape.kt
│   └── Theme.kt
├── navigation/
│   ├── ScrollTrekNavHost.kt
│   └── Tab.kt
├── onboarding/
│   ├── OnboardingFlow.kt
│   ├── OnboardingStep1ValueProp.kt
│   ├── OnboardingStep2Accessibility.kt
│   ├── OnboardingStep3Notifications.kt
│   ├── OnboardingStep4Battery.kt
│   └── OnboardingStep5Complete.kt
├── home/
│   ├── HomeScreen.kt
│   ├── HomeViewModel.kt
│   ├── TodayDistanceCard.kt
│   ├── LandmarkProgressArc.kt
│   └── RecentUnlocksCarousel.kt
├── journey/
│   ├── JourneyScreen.kt
│   ├── JourneyViewModel.kt
│   ├── LandmarkCard.kt
│   └── LandmarkDetailSheet.kt
├── insights/
│   ├── InsightsScreen.kt
│   ├── InsightsViewModel.kt
│   ├── ScrollHeatmap.kt
│   └── AppBreakdownList.kt
├── milestone/
│   ├── MilestoneRevealScreen.kt
│   └── MilestoneRevealViewModel.kt
├── settings/
│   ├── SettingsScreen.kt
│   └── AppExclusionScreen.kt
├── overlay/
│   ├── FloatingOverlayManager.kt
│   └── ScrollTrekOverlay.kt
├── widget/
│   └── ScrollTrekWidget.kt
├── sharecard/
│   ├── ShareCardRenderer.kt
│   ├── ShareCardComposable.kt
│   └── ShareCardStyles.kt
└── common/
    ├── DistanceFormatter.kt
    ├── DateFormatter.kt
    └── components/
        ├── TierBadge.kt
        ├── BulletPoint.kt
        └── PrivacyBadge.kt
```

---

*Agent 3 Spec · scrollTrek · v1.0*
