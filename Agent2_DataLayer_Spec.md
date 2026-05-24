# AGENT 2 — THE DATA LAYER
## Technical Specification Sheet
**scrollTrek | Antigravity Agent Brief**
`Agent ID: ST-AG-02` | `Owner: Data & Business Logic` | `Priority: P0 — Critical Path`

---

## 1. Mission Statement

> Consume raw pixel deltas from Agent 1, convert them to physically accurate metric distances, persist every micro-measurement durably, aggregate them into meaningful analytics, evaluate gamification milestones, and surface all state to the rest of the app as reactive data streams. This agent is the single source of truth.

---

## 2. Scope Boundary

### Owns
- Pixel-to-meter conversion engine (hardware-accurate, per-device)
- All Room database entities, DAOs, migrations, and views
- Data aggregation queries (daily, weekly, lifetime)
- Milestone state machine & evaluation logic
- Landmark dataset (50+ entries, fully typed)
- Streak tracking logic
- Per-app scroll breakdown computation
- Notification content string generation (distance progress)
- Data export (CSV) and data deletion flows
- All `Flow<T>` / `StateFlow<T>` contracts consumed by Agent 3

### Does NOT Own
- Capturing scroll events (Agent 1)
- Any Compose UI (Agent 3)
- Notification display (Agent 1 posts it; Agent 2 updates its content via `NotificationManagerCompat`)
- Share card rendering (Agent 3)

---

## 3. Tech Stack

| Concern | Technology |
|---|---|
| Persistence | Room 2.6+ with KSP annotation processing |
| Reactive streams | Kotlin Flow + StateFlow + SharedFlow |
| Conversion math | `WindowManager.currentWindowMetrics` (API 30+) / `Display.getRealMetrics()` |
| Dependency injection | Hilt |
| Coroutines | `Dispatchers.IO` for DB ops, `Dispatchers.Default` for math |
| Serialization | `kotlinx.serialization` for landmark JSON asset |
| CSV export | Manual `BufferedWriter` to `MediaStore` |
| Testing | JUnit 5 + Turbine (Flow testing) + Robolectric |

---

## 4. Input Contract

### From Agent 1 — `ScrollEventBus.rawEvents: SharedFlow<RawScrollEvent>`
```kotlin
data class RawScrollEvent(
    val timestampMs: Long,
    val deltaYPx: Float,
    val direction: ScrollDirection,
    val sourcePackage: String,
    val sessionId: String,
    val confidence: EventConfidence   // PRIMARY=1.0x, FALLBACK_A=0.9x, FALLBACK_B=0.7x
)
```

### From Device Hardware — Display Metrics
Queried once on service start and on `onConfigurationChanged`:
```kotlin
val metrics: DisplayMetrics = DisplayMetrics()

if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    val windowMetrics = windowManager.currentWindowMetrics
    // Extract ydpi from WindowInsets-aware bounds
} else {
    windowManager.defaultDisplay.getRealMetrics(metrics)
    val ydpi = metrics.ydpi
}
```

### From SharedPreferences (written by Agent 3)
```
KEY_TRACKING_ENABLED        : Boolean
KEY_EXCLUDED_PACKAGES       : Set<String>
KEY_DAILY_GOAL_METERS       : Float (0 = no goal)
```

---

## 5. Output Contract

All outputs are `Flow<T>` or `StateFlow<T>` observed by Agent 3 and by the notification updater.

### 5.1 — Live Session State
```kotlin
// Updated on every processed scroll event
data class LiveSessionState(
    val sessionDistanceM: Double,
    val sessionStartMs: Long,
    val paceMetersPerMinute: Double,
    val currentApp: String
)
val liveSession: StateFlow<LiveSessionState>
```

### 5.2 — Today's Summary
```kotlin
data class DailySummary(
    val dateKey: String,             // "2026-05-24"
    val totalDistanceM: Double,
    val sessionCount: Int,
    val topApp: String,
    val topAppDistanceM: Double,
    val goalMeters: Float,           // 0 if no goal set
    val goalProgressFraction: Float  // 0.0–1.0+
)
val todaySummary: Flow<DailySummary>
```

### 5.3 — Landmark Progress
```kotlin
data class LandmarkProgress(
    val currentLandmark: Landmark,
    val nextLandmark: Landmark,
    val lifetimeDistanceM: Double,
    val distanceToNextM: Double,
    val progressFraction: Float,     // 0.0–1.0
    val recentlyUnlocked: List<Landmark>  // unlocked in last 24h
)
val landmarkProgress: Flow<LandmarkProgress>
```

### 5.4 — Weekly Analytics
```kotlin
data class WeeklyAnalytics(
    val days: List<DailySummary>,    // last 7 days
    val totalDistanceM: Double,
    val averageDailyM: Double,
    val mostScrolledApp: String,
    val peakHour: Int                // 0–23
)
val weeklyAnalytics: Flow<WeeklyAnalytics>
```

### 5.5 — Streak State
```kotlin
data class StreakState(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActiveDate: String,
    val freezeTokensRemaining: Int,
    val isActiveToday: Boolean
)
val streakState: Flow<StreakState>
```

### 5.6 — Milestone Unlock Events (one-shot)
```kotlin
// Emitted once per unlock. Agent 3 observes to trigger animation + card generation.
val milestoneUnlockEvents: SharedFlow<Landmark>
```

### 5.7 — Notification Content String
```kotlin
// Polled by Agent 1's TrackingForegroundService every 60 seconds
fun getNotificationSubtitle(): String
// Returns: "Today: 247m · Eiffel Tower in 83m"
```

---

## 6. Conversion Engine

### Core Formula
```kotlin
object ScrollConverter {

    /**
     * Converts a raw pixel delta to physical meters.
     * @param deltaYPx      Raw pixel displacement (always positive)
     * @param ydpi          Physical pixels per inch along Y axis (from DisplayMetrics.ydpi)
     * @param confidence    Event confidence weighting multiplier
     */
    fun toMeters(deltaYPx: Float, ydpi: Float, confidence: EventConfidence): Double {
        val INCHES_PER_METER = 39.3701
        val confidenceMultiplier = when (confidence) {
            EventConfidence.PRIMARY    -> 1.0
            EventConfidence.FALLBACK_A -> 0.9
            EventConfidence.FALLBACK_B -> 0.7
        }
        val physicalInches = deltaYPx / ydpi
        val physicalMeters = physicalInches / INCHES_PER_METER
        return physicalMeters * confidenceMultiplier
    }
}
```

### Display Metrics Caching
```kotlin
@Singleton
class DisplayMetricsProvider @Inject constructor(
    private val windowManager: WindowManager
) {
    // Refreshed on config change; thread-safe via @Volatile
    @Volatile var ydpi: Float = 0f
        private set

    fun refresh() {
        ydpi = if (Build.VERSION.SDK_INT >= 30) {
            // Modern path
            val bounds = windowManager.currentWindowMetrics.bounds
            val dm = Resources.getSystem().displayMetrics
            dm.ydpi
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(dm)
            dm.ydpi
        }
    }
}
```

### Precision Requirements
- Use `Double` throughout — never `Float` for accumulated distance
- Minimum precision: 4 decimal places in meters (sub-millimeter)
- Maximum compound error over 1M events: < 0.1%

---

## 7. Database Schema

### Entity: `ScrollSession`
```kotlin
@Entity(tableName = "scroll_sessions")
data class ScrollSession(
    @PrimaryKey val sessionId: String,
    val startMs: Long,
    val endMs: Long,
    val totalDistanceM: Double,
    val sourcePackage: String,
    val eventCount: Int,
    val dateKey: String              // "2026-05-24" — indexed for fast daily queries
)
```

### Entity: `RawScrollRecord` (30-day retention)
```kotlin
@Entity(
    tableName = "raw_scroll_records",
    indices = [Index("dateKey"), Index("sourcePackage")]
)
data class RawScrollRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val deltaMeters: Double,
    val direction: String,           // "DOWN" | "UP"
    val sourcePackage: String,
    val sessionId: String,
    val dateKey: String,
    val confidenceTag: String        // "PRIMARY" | "FALLBACK_A" | "FALLBACK_B"
)
```

### Entity: `DailyAggregate`
```kotlin
@Entity(tableName = "daily_aggregates", primaryKeys = ["dateKey"])
data class DailyAggregate(
    val dateKey: String,
    val totalDistanceM: Double,
    val sessionCount: Int,
    val topApp: String,
    val topAppDistanceM: Double,
    val peakScrollHour: Int,
    val activeMinutes: Int
)
```

### Entity: `MilestoneRecord`
```kotlin
@Entity(tableName = "milestones", primaryKeys = ["landmarkId"])
data class MilestoneRecord(
    val landmarkId: String,
    val unlockedAtMs: Long?,         // null = locked
    val notificationSent: Boolean = false,
    val cardGenerated: Boolean = false
)
```

### Entity: `StreakRecord` (single-row)
```kotlin
@Entity(tableName = "streak_state")
data class StreakRecord(
    @PrimaryKey val id: Int = 1,     // always 1
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActiveDateKey: String,
    val freezeTokens: Int = 1
)
```

### Database Version & Migrations
```kotlin
@Database(
    entities = [ScrollSession::class, RawScrollRecord::class,
                DailyAggregate::class, MilestoneRecord::class, StreakRecord::class],
    version = 1,
    exportSchema = true              // schema JSON tracked in git
)
abstract class ScrollTrekDatabase : RoomDatabase() { ... }
```

---

## 8. Key DAOs

### `ScrollRecordDao`
```kotlin
@Dao
interface ScrollRecordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBatch(records: List<RawScrollRecord>)

    @Query("SELECT SUM(deltaMeters) FROM raw_scroll_records WHERE dateKey = :dateKey")
    fun observeDailyTotal(dateKey: String): Flow<Double?>

    @Query("SELECT SUM(deltaMeters) FROM raw_scroll_records")
    fun observeLifetimeTotal(): Flow<Double?>

    // Per-app breakdown: top 5 apps for given date
    @Query("""
        SELECT sourcePackage, SUM(deltaMeters) AS total 
        FROM raw_scroll_records WHERE dateKey = :dateKey 
        GROUP BY sourcePackage ORDER BY total DESC LIMIT 5
    """)
    fun observeAppBreakdown(dateKey: String): Flow<List<AppScrollTotal>>

    // Hourly distribution for heatmap
    @Query("""
        SELECT strftime('%H', timestampMs/1000, 'unixepoch') AS hour, 
               SUM(deltaMeters) AS total
        FROM raw_scroll_records WHERE dateKey = :dateKey
        GROUP BY hour
    """)
    fun observeHourlyDistribution(dateKey: String): Flow<List<HourlyTotal>>

    // Pruning job — runs nightly via WorkManager
    @Query("DELETE FROM raw_scroll_records WHERE dateKey < :cutoffDateKey")
    suspend fun pruneOlderThan(cutoffDateKey: String): Int
}
```

### `MilestoneDao`
```kotlin
@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestones WHERE unlockedAtMs IS NULL ORDER BY landmarkId")
    fun observeLockedMilestones(): Flow<List<MilestoneRecord>>

    @Query("SELECT * FROM milestones WHERE unlockedAtMs IS NOT NULL ORDER BY unlockedAtMs DESC")
    fun observeUnlockedMilestones(): Flow<List<MilestoneRecord>>

    @Query("UPDATE milestones SET unlockedAtMs = :timestampMs WHERE landmarkId = :id")
    suspend fun unlock(id: String, timestampMs: Long)

    @Query("UPDATE milestones SET cardGenerated = 1 WHERE landmarkId = :id")
    suspend fun markCardGenerated(id: String)
}
```

---

## 9. Flush & Aggregation Pipeline

```
Agent 1 Buffer → drain() every 30s
    └── ScrollConverter.toMeters() per event
    └── Batch INSERT into raw_scroll_records
    └── Upsert DailyAggregate (atomic Room transaction)
    └── Update StreakRecord
    └── Evaluate MilestoneEngine
        └── If unlock: emit milestoneUnlockEvents + mark MilestoneRecord
    └── Post notification content update
```

### Flush Trigger Conditions (any one fires it)
- 30-second timer (primary)
- Buffer reaches 400 events (near-full guard)
- `onUnbind()` called on Accessibility Service (graceful shutdown)
- App is backgrounded (`ProcessLifecycleOwner.get().lifecycle`)

---

## 10. Milestone Engine

### Landmark Data Model
```kotlin
data class Landmark(
    val id: String,
    val name: String,
    val location: String,
    val distanceMeters: Double,
    val tier: LandmarkTier,
    val orientation: LandmarkOrientation,
    val funFact: String,
    val shareMessage: String,
    val illustrationRes: String,    // drawable resource name
    val cardGradientStart: String,  // hex color
    val cardGradientEnd: String
)

enum class LandmarkTier { COMMON, NOTABLE, LANDMARK, LEGENDARY }
enum class LandmarkOrientation { VERTICAL, HORIZONTAL }
```

### Landmark Dataset (sample — full 50-entry JSON in `assets/landmarks.json`)
```json
[
  { "id": "door_standard", "name": "Standard Door", "distanceMeters": 2.1,
    "tier": "COMMON", "orientation": "VERTICAL", "location": "Everywhere",
    "funFact": "The average person walks through 5,000 doors per year." },
  { "id": "telephone_pole", "name": "Telephone Pole", "distanceMeters": 12.0,
    "tier": "COMMON", "orientation": "VERTICAL", "location": "USA" },
  { "id": "statue_liberty", "name": "Statue of Liberty", "distanceMeters": 93.0,
    "tier": "NOTABLE", "orientation": "VERTICAL", "location": "New York, USA" },
  { "id": "eiffel_tower", "name": "Eiffel Tower", "distanceMeters": 330.0,
    "tier": "NOTABLE", "orientation": "VERTICAL", "location": "Paris, France" },
  { "id": "burj_khalifa", "name": "Burj Khalifa", "distanceMeters": 828.0,
    "tier": "LANDMARK", "orientation": "VERTICAL", "location": "Dubai, UAE" },
  { "id": "mount_everest", "name": "Mount Everest", "distanceMeters": 8849.0,
    "tier": "LEGENDARY", "orientation": "VERTICAL", "location": "Nepal/Tibet" },
  { "id": "golden_gate", "name": "Golden Gate Bridge", "distanceMeters": 2737.0,
    "tier": "NOTABLE", "orientation": "HORIZONTAL", "location": "San Francisco, USA" },
  { "id": "karman_line", "name": "Kármán Line (Space)", "distanceMeters": 100000.0,
    "tier": "LEGENDARY", "orientation": "VERTICAL", "location": "Earth's Atmosphere" }
]
```

### Evaluation Logic
```kotlin
class MilestoneEngine @Inject constructor(
    private val milestoneDao: MilestoneDao,
    private val landmarkRepository: LandmarkRepository
) {
    // Called after every flush cycle
    suspend fun evaluate(lifetimeTotalM: Double) {
        val lockedLandmarks = milestoneDao.getLockedLandmarks()
        val newlyUnlocked = lockedLandmarks.filter { record ->
            val landmark = landmarkRepository.getById(record.landmarkId)
            lifetimeTotalM >= landmark.distanceMeters
        }
        newlyUnlocked.forEach { record ->
            milestoneDao.unlock(record.landmarkId, System.currentTimeMillis())
            _milestoneUnlockEvents.emit(landmarkRepository.getById(record.landmarkId))
        }
    }
}
```

---

## 11. Streak Logic

```kotlin
class StreakManager @Inject constructor(
    private val streakDao: StreakDao
) {
    suspend fun recordActivityToday() {
        val today = LocalDate.now().toString()  // "2026-05-24"
        val record = streakDao.get()

        val newStreak = when {
            record.lastActiveDateKey == today -> record.currentStreak  // already counted
            record.lastActiveDateKey == yesterday() -> record.currentStreak + 1
            record.freezeTokens > 0 && record.lastActiveDateKey == dayBeforeYesterday() -> {
                // Freeze token consumed silently
                streakDao.consumeFreeze()
                record.currentStreak + 1
            }
            else -> 1  // streak broken
        }

        streakDao.update(record.copy(
            currentStreak = newStreak,
            longestStreak = maxOf(newStreak, record.longestStreak),
            lastActiveDateKey = today
        ))
    }
}
```

---

## 12. Data Pruning

Managed by a `PeriodicWorkRequest` (WorkManager), runs nightly at 2:00 AM device time.

```kotlin
class DataPruningWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val cutoff = LocalDate.now().minusDays(30).toString()
        val pruned = scrollRecordDao.pruneOlderThan(cutoff)
        // DailyAggregates and MilestoneRecords are NEVER pruned
        return Result.success()
    }
}
```

---

## 13. Acceptance Criteria

| ID | Criterion | Test Method |
|---|---|---|
| AC-01 | Conversion accuracy: 100px @ 400 dpi = 0.00635m ± 0.00003m | Unit test |
| AC-02 | FALLBACK_B events weighted at 0.7x | Unit test |
| AC-03 | Batch insert of 500 events completes in < 100ms | Benchmark test |
| AC-04 | Daily aggregate updates atomically (no partial state) | Room transaction test |
| AC-05 | Milestone fires exactly once per landmark, never twice | Integration test |
| AC-06 | Streak increments on consecutive days | Unit test with mocked `LocalDate` |
| AC-07 | Streak freeze token consumed correctly | Unit test |
| AC-08 | Old records pruned after 30 days | WorkManager instrumented test |
| AC-09 | `observeLifetimeTotal()` updates within 35s of a scroll event | Flow Turbine test with 30s flush |
| AC-10 | `ydpi` refreshes on display config change (foldable) | Robolectric configuration change test |
| AC-11 | Data export produces valid CSV with correct headers | File output validation test |
| AC-12 | Data deletion removes all personal data tables | Room query test post-delete |

---

## 14. File Deliverables

```
app/src/main/
├── java/com/scrolltrek/data/
│   ├── db/
│   │   ├── ScrollTrekDatabase.kt
│   │   ├── dao/
│   │   │   ├── ScrollRecordDao.kt
│   │   │   ├── DailyAggregateDao.kt
│   │   │   ├── MilestoneDao.kt
│   │   │   └── StreakDao.kt
│   │   └── entity/
│   │       ├── RawScrollRecord.kt
│   │       ├── ScrollSession.kt
│   │       ├── DailyAggregate.kt
│   │       ├── MilestoneRecord.kt
│   │       └── StreakRecord.kt
│   ├── model/
│   │   ├── Landmark.kt
│   │   ├── LandmarkTier.kt
│   │   ├── LiveSessionState.kt
│   │   ├── DailySummary.kt
│   │   ├── WeeklyAnalytics.kt
│   │   ├── LandmarkProgress.kt
│   │   └── StreakState.kt
│   ├── repository/
│   │   ├── ScrollRepository.kt
│   │   ├── LandmarkRepository.kt
│   │   └── StreakRepository.kt
│   ├── engine/
│   │   ├── ScrollConverter.kt
│   │   ├── MilestoneEngine.kt
│   │   ├── StreakManager.kt
│   │   └── DisplayMetricsProvider.kt
│   └── work/
│       └── DataPruningWorker.kt
├── assets/
│   └── landmarks.json               (50+ entry dataset)
└── schemas/
    └── com.scrolltrek.data.db.ScrollTrekDatabase/
        └── 1.json                   (Room schema export)
```

---

*Agent 2 Spec · scrollTrek · v1.0*
