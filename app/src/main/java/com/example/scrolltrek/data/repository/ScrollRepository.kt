package com.example.scrolltrek.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.scrolltrek.MainActivity
import com.example.scrolltrek.data.db.ScrollTrekDatabase
import com.example.scrolltrek.data.db.entity.DailyAggregate
import com.example.scrolltrek.data.db.entity.MilestoneRecord
import com.example.scrolltrek.data.db.entity.RawScrollRecord
import com.example.scrolltrek.data.db.entity.StreakRecord
import com.example.scrolltrek.data.engine.DisplayMetricsProvider
import com.example.scrolltrek.data.engine.MilestoneEngine
import com.example.scrolltrek.data.engine.ScrollConverter
import com.example.scrolltrek.data.engine.StreakManager
import com.example.scrolltrek.data.model.DailySummary
import com.example.scrolltrek.data.model.LandmarkProgress
import com.example.scrolltrek.data.model.LiveSessionState
import com.example.scrolltrek.data.model.WeeklyAnalytics
import com.example.scrolltrek.tracking.ScrollEventBuffer
import com.example.scrolltrek.tracking.ScrollEventBus
import com.example.scrolltrek.tracking.model.RawScrollEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.awaitClose
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class ScrollRepository @Inject constructor(
    private val db: ScrollTrekDatabase,
    private val displayMetricsProvider: DisplayMetricsProvider,
    private val milestoneEngine: MilestoneEngine,
    private val streakManager: StreakManager,
    private val landmarkRepository: LandmarkRepository,
    private val sharedPreferences: SharedPreferences,
    @ApplicationContext private val context: Context
) {
    private val scrollRecordDao = db.scrollRecordDao()
    private val dailyAggregateDao = db.dailyAggregateDao()
    private val milestoneDao = db.milestoneDao()
    private val streakDao = db.streakDao()

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _liveSession = MutableStateFlow(
        LiveSessionState(
            sessionDistanceM = 0.0,
            sessionStartMs = 0L,
            paceMetersPerMinute = 0.0,
            currentApp = ""
        )
    )

    private fun getLiveSessionFromPrefs(): LiveSessionState {
        return LiveSessionState(
            sessionDistanceM = sharedPreferences.getFloat("live_session_distance", 0f).toDouble(),
            sessionStartMs = sharedPreferences.getLong("live_session_start", 0L),
            paceMetersPerMinute = sharedPreferences.getFloat("live_session_pace", 0f).toDouble(),
            currentApp = sharedPreferences.getString("live_session_app", "") ?: ""
        )
    }

    // Live Session State Flow (Cross-Process bridge)
    val liveSession: StateFlow<LiveSessionState> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "live_session_distance" || key == "live_session_start" ||
                key == "live_session_pace" || key == "live_session_app") {
                trySend(getLiveSessionFromPrefs())
            }
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(getLiveSessionFromPrefs())
        awaitClose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.stateIn(
        scope = repositoryScope,
        started = SharingStarted.Eagerly,
        initialValue = LiveSessionState(0.0, 0L, 0.0, "")
    )

    private var activeSessionId = ""
    private var sessionDistance = 0.0
    private var sessionStartMs = 0L
    private var lastEventCount = 0

    init {
        // 1. Align assets landmarks with the database milestones on startup and evaluate
        repositoryScope.launch(Dispatchers.IO) {
            try {
                val allLandmarks = landmarkRepository.getAll()
                val defaultMilestones = allLandmarks.map {
                    MilestoneRecord(
                        landmarkId = it.id,
                        unlockedAtMs = null,
                        notificationSent = false,
                        cardGenerated = false
                    )
                }
                milestoneDao.insertAll(defaultMilestones)

                // Evaluate milestones immediately to unlock any newly added ones that the user has already passed!
                val lifetimeTotal = scrollRecordDao.getLifetimeTotal() ?: 0.0
                milestoneEngine.evaluate(lifetimeTotal)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Initialize default streak record if absent
        repositoryScope.launch(Dispatchers.IO) {
            try {
                if (streakDao.get() == null) {
                    streakDao.insertOrUpdate(
                        StreakRecord(
                            id = 1,
                            currentStreak = 0,
                            longestStreak = 0,
                            lastActiveDateKey = "",
                            freezeTokens = 1
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Collect raw scroll events in real-time for live session state updates
        repositoryScope.launch {
            ScrollEventBus.events.collect { event ->
                updateLiveSession(event)
                lastEventCount++
                if (lastEventCount >= 400) {
                    lastEventCount = 0
                    flush()
                }
            }
        }

        // 4. Start periodic 30-second flushing loop
        repositoryScope.launch {
            while (isActive) {
                delay(30000)
                flush()
            }
        }
    }

    private fun updateLiveSession(event: RawScrollEvent) {
        val ydpi = displayMetricsProvider.ydpi
        val meters = ScrollConverter.toMeters(event.deltaYPx, ydpi, event.confidence)

        if (event.sessionId != activeSessionId) {
            activeSessionId = event.sessionId
            sessionStartMs = event.timestampMs
            sessionDistance = 0.0
        }

        sessionDistance += meters
        val now = System.currentTimeMillis()
        val durationMin = (now - sessionStartMs) / 60000.0
        val pace = if (durationMin > 0.0) sessionDistance / durationMin else 0.0

        val state = LiveSessionState(
            sessionDistanceM = sessionDistance,
            sessionStartMs = sessionStartMs,
            paceMetersPerMinute = pace,
            currentApp = event.sourcePackage
        )
        _liveSession.value = state

        // Persist to cross-process SharedPreferences
        sharedPreferences.edit()
            .putFloat("live_session_distance", sessionDistance.toFloat())
            .putLong("live_session_start", sessionStartMs)
            .putFloat("live_session_pace", pace.toFloat())
            .putString("live_session_app", event.sourcePackage)
            .apply()
    }

    /**
     * Drains the scroll event buffer, converts pixels to physical meters,
     * saves records, updates aggregates, and evaluates milestones in a transaction.
     */
    suspend fun flush() = withContext(Dispatchers.IO) {
        val rawEvents = ScrollEventBuffer.drainAll()
        if (rawEvents.isEmpty()) return@withContext

        lastEventCount = 0 // Reset manual flush event counter

        val ydpi = displayMetricsProvider.ydpi
        val records = rawEvents.map { event ->
            val meters = ScrollConverter.toMeters(event.deltaYPx, ydpi, event.confidence)
            val dateKey = java.time.Instant.ofEpochMilli(event.timestampMs)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .toString()
            RawScrollRecord(
                timestampMs = event.timestampMs,
                deltaMeters = meters,
                direction = event.direction.name,
                sourcePackage = event.sourcePackage,
                sessionId = event.sessionId,
                dateKey = dateKey,
                confidenceTag = event.confidence.name
            )
        }

        try {
            // Room transaction guarantees atomic execution
            db.runInTransaction {
                runBlocking {
                    scrollRecordDao.insertBatch(records)

                    val recordsByDate = records.groupBy { it.dateKey }
                    for ((dateKey, dateRecords) in recordsByDate) {
                        updateDailyAggregateForDate(dateKey, dateRecords)
                    }

                    // Update daily streak activity
                    streakManager.recordActivityToday()

                    // Evaluate milestones
                    val lifetimeTotal = scrollRecordDao.getLifetimeTotal() ?: 0.0
                    milestoneEngine.evaluate(lifetimeTotal)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun updateDailyAggregateForDate(dateKey: String, newRecords: List<RawScrollRecord>) {
        val existing = dailyAggregateDao.getDailyAggregate(dateKey)
        val dailyDistance = (existing?.totalDistanceM ?: 0.0) + newRecords.sumOf { it.deltaMeters }

        val goalMeters = getDailyGoalFromDisk()
        if (goalMeters > 0f && dailyDistance >= goalMeters) {
            val todayStr = LocalDate.now().toString()
            val alertsPrefs = context.getSharedPreferences("scrolltrek_alerts", Context.MODE_PRIVATE)
            val lastNotifiedDate = alertsPrefs.getString("KEY_LAST_GOAL_REACHED_DATE", "")
            if (lastNotifiedDate != todayStr) {
                alertsPrefs.edit().putString("KEY_LAST_GOAL_REACHED_DATE", todayStr).commit()
                sendGoalReachedNotification()
            }
        }

        val sessionCount = scrollRecordDao.getSessionCountForDate(dateKey)
        val activeMinutes = scrollRecordDao.getActiveMinutesForDate(dateKey)

        val appBreakdown = scrollRecordDao.getAppBreakdown(dateKey)
        val topAppEntry = appBreakdown.firstOrNull()
        val topApp = topAppEntry?.sourcePackage ?: ""
        val topAppDistance = topAppEntry?.total ?: 0.0

        val hourlyDistribution = scrollRecordDao.getHourlyDistribution(dateKey)
        val peakHourStr = hourlyDistribution.maxByOrNull { it.total }?.hour ?: "0"
        val peakHour = peakHourStr.toIntOrNull() ?: 0

        val aggregate = DailyAggregate(
            dateKey = dateKey,
            totalDistanceM = dailyDistance,
            sessionCount = sessionCount,
            topApp = topApp,
            topAppDistanceM = topAppDistance,
            peakScrollHour = peakHour,
            activeMinutes = activeMinutes
        )
        dailyAggregateDao.insertOrUpdate(aggregate)
    }

    private fun sendGoalReachedNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                android.util.Log.w("ScrollRepository", "POST_NOTIFICATIONS permission not granted. Cannot send notification.")
                return
            }
        }

        val channelId = "scrolltrek_goal_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ScrollTrek Goal & Milestone Alerts"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel(channelId, name, importance).apply {
                description = "ScrollTrek Goal and Milestone Alerts"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            1002,
            Intent(context, MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setContentTitle("Daily Goal Reached! 🎉")
            .setContentText("Congratulations! You've completed your daily scrolling goal. Why not take a break?")
            .setSmallIcon(com.example.scrolltrek.R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(1002, notification)
    }

    // Dynamic Flow of Date Keys to trigger updates on date change
    private fun todayDateFlow(): Flow<String> = flow {
        while (currentCoroutineContext().isActive) {
            emit(LocalDate.now().toString())
            delay(30000) // Poll date every 30 seconds
        }
    }

    // Today's summary stream combining daily aggregates and SharedPreferences goals
    val todaySummary: Flow<DailySummary> = todayDateFlow().flatMapLatest { dateKey ->
        dailyAggregateDao.observeDailyAggregate(dateKey).map { aggregate ->
            val goalMeters = getDailyGoalFromDisk()
            if (aggregate != null) {
                val progressFraction = if (goalMeters > 0f) (aggregate.totalDistanceM / goalMeters).toFloat() else 0f
                DailySummary(
                    dateKey = dateKey,
                    totalDistanceM = aggregate.totalDistanceM,
                    sessionCount = aggregate.sessionCount,
                    topApp = aggregate.topApp,
                    topAppDistanceM = aggregate.topAppDistanceM,
                    goalMeters = goalMeters,
                    goalProgressFraction = progressFraction
                )
            } else {
                DailySummary(
                    dateKey = dateKey,
                    totalDistanceM = 0.0,
                    sessionCount = 0,
                    topApp = "",
                    topAppDistanceM = 0.0,
                    goalMeters = goalMeters,
                    goalProgressFraction = 0f
                )
            }
        }
    }

    // Dynamic landmark progress combining lifetime totals and unlocked milestone records
    val landmarkProgress: Flow<LandmarkProgress> = combine(
        scrollRecordDao.observeLifetimeTotal(),
        milestoneDao.observeUnlockedMilestones()
    ) { lifetimeTotalOrNull, unlockedRecords ->
        val lifetimeTotal = lifetimeTotalOrNull ?: 0.0
        val currentLandmark = landmarkRepository.getCurrentLandmark(lifetimeTotal)
        val nextLandmark = landmarkRepository.getNextLandmark(lifetimeTotal) ?: landmarkRepository.getAll().last()

        val distanceToNextM = maxOf(0.0, nextLandmark.distanceMeters - lifetimeTotal)
        val segmentLength = if (currentLandmark == null) {
            nextLandmark.distanceMeters
        } else {
            nextLandmark.distanceMeters - currentLandmark.distanceMeters
        }
        val progressFraction = if (currentLandmark == null) {
            if (segmentLength > 0.0) (lifetimeTotal / segmentLength).toFloat().coerceIn(0f, 1f) else 0f
        } else {
            if (segmentLength > 0.0) {
                ((lifetimeTotal - currentLandmark.distanceMeters) / segmentLength).toFloat().coerceIn(0f, 1f)
            } else {
                1f
            }
        }

        val now = System.currentTimeMillis()
        val recentlyUnlocked = unlockedRecords.filter {
            val unlockedAt = it.unlockedAtMs ?: 0L
            unlockedAt >= now - 24 * 60 * 60 * 1000
        }.mapNotNull {
            landmarkRepository.getById(it.landmarkId)
        }

        LandmarkProgress(
            currentLandmark = currentLandmark ?: landmarkRepository.getAll().first(),
            nextLandmark = nextLandmark,
            lifetimeDistanceM = lifetimeTotal,
            distanceToNextM = distanceToNextM,
            progressFraction = progressFraction,
            recentlyUnlocked = recentlyUnlocked
        )
    }

    // Dynamic weekly analytics flow generating a chronological 7-day view
    val weeklyAnalytics: Flow<WeeklyAnalytics> = combine(
        todayDateFlow(),
        dailyAggregateDao.observeRecentDailyAggregates(7)
    ) { _, recentAggregates ->
        val goalMeters = getDailyGoalFromDisk()
        val summaries = mutableListOf<DailySummary>()

        for (i in 0 until 7) {
            val date = LocalDate.now().minusDays(i.toLong())
            val dateKey = date.toString()
            val aggregate = recentAggregates.find { it.dateKey == dateKey }

            val summary = if (aggregate != null) {
                val progressFraction = if (goalMeters > 0f) (aggregate.totalDistanceM / goalMeters).toFloat() else 0f
                DailySummary(
                    dateKey = dateKey,
                    totalDistanceM = aggregate.totalDistanceM,
                    sessionCount = aggregate.sessionCount,
                    topApp = aggregate.topApp,
                    topAppDistanceM = aggregate.topAppDistanceM,
                    goalMeters = goalMeters,
                    goalProgressFraction = progressFraction
                )
            } else {
                DailySummary(
                    dateKey = dateKey,
                    totalDistanceM = 0.0,
                    sessionCount = 0,
                    topApp = "",
                    topAppDistanceM = 0.0,
                    goalMeters = goalMeters,
                    goalProgressFraction = 0f
                )
            }
            summaries.add(summary)
        }

        val sortedSummaries = summaries.reversed()
        val totalDistance = sortedSummaries.sumOf { it.totalDistanceM }
        val averageDaily = totalDistance / 7.0

        val appBreakdown = recentAggregates.groupBy { it.topApp }
            .mapValues { entry -> entry.value.sumOf { it.topAppDistanceM } }
            .filterKeys { it.isNotEmpty() }
        val mostScrolledApp = appBreakdown.maxByOrNull { it.value }?.key ?: "None"

        val hourBreakdown = recentAggregates.groupBy { it.peakScrollHour }
            .mapValues { entry -> entry.value.sumOf { it.totalDistanceM } }
        val peakHour = hourBreakdown.maxByOrNull { it.value }?.key ?: 0

        WeeklyAnalytics(
            days = sortedSummaries,
            totalDistanceM = totalDistance,
            averageDailyM = averageDaily,
            mostScrolledApp = mostScrolledApp,
            peakHour = peakHour
        )
    }

    val allLandmarks: Flow<List<com.example.scrolltrek.data.model.LandmarkWithStatus>> = milestoneDao.observeUnlockedMilestones().map { unlockedList ->
        val unlockedIds = unlockedList.associateBy { it.landmarkId }
        landmarkRepository.getAll().map { landmark ->
            val unlockedRecord = unlockedIds[landmark.id]
            com.example.scrolltrek.data.model.LandmarkWithStatus(
                landmark = landmark,
                isUnlocked = unlockedRecord != null,
                unlockedAtMs = unlockedRecord?.unlockedAtMs
            )
        }
    }

    val appBreakdown: Flow<List<com.example.scrolltrek.data.db.dao.AppScrollTotal>> = todayDateFlow().flatMapLatest { dateKey ->
        scrollRecordDao.observeAppBreakdown(dateKey)
    }

    val hourlyHeatmap: Flow<List<com.example.scrolltrek.data.db.dao.HourlyTotal>> = todayDateFlow().flatMapLatest { dateKey ->
        scrollRecordDao.observeHourlyDistribution(dateKey)
    }


    private fun getDailyGoalFromDisk(): Float {
        try {
            val prefsFile = java.io.File(context.filesDir.parentFile, "shared_prefs/${context.packageName}_preferences.xml")
            if (prefsFile.exists()) {
                val content = prefsFile.readText()
                
                // Regular expressions matching both orders (name first, or value first) for both keys
                val keys = listOf("KEY_DAILY_GOAL_METERS", "daily_scroll_goal")
                for (key in keys) {
                    val nameFirstRegex = """<float\s+name=["']$key["']\s+value=["']([^"']+)["']""".toRegex()
                    val valueFirstRegex = """<float\s+value=["']([^"']+)["']\s+name=["']$key["']""".toRegex()

                    val matchNameFirst = nameFirstRegex.find(content)
                    if (matchNameFirst != null) {
                        return matchNameFirst.groupValues[1].toFloatOrNull() ?: 100f
                    }

                    val matchValueFirst = valueFirstRegex.find(content)
                    if (matchValueFirst != null) {
                        return matchValueFirst.groupValues[1].toFloatOrNull() ?: 100f
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sharedPreferences.getFloat("KEY_DAILY_GOAL_METERS", 100f)
    }

    /**
     * Polled by foreground service to construct notification text.
     */
    fun getNotificationSubtitle(): String {
        val todayKey = LocalDate.now().toString()
        return runBlocking(Dispatchers.IO) {
            try {
                val todayDistance = scrollRecordDao.getDailyTotal(todayKey) ?: 0.0
                val lifetimeTotal = scrollRecordDao.getLifetimeTotal() ?: 0.0
                val nextLandmark = landmarkRepository.getNextLandmark(lifetimeTotal)

                val todayStr = if (todayDistance >= 1000.0) {
                    String.format(java.util.Locale.US, "%.1fkm", todayDistance / 1000.0)
                } else {
                    "${todayDistance.roundToInt()}m"
                }

                if (nextLandmark != null) {
                    val distToNext = maxOf(0.0, nextLandmark.distanceMeters - lifetimeTotal)
                    val nextStr = if (distToNext >= 1000.0) {
                        String.format(java.util.Locale.US, "%.1fkm", distToNext / 1000.0)
                    } else {
                        "${distToNext.roundToInt()}m"
                    }
                    "Today: $todayStr · ${nextLandmark.name} in $nextStr"
                } else {
                    "Today: $todayStr"
                }
            } catch (e: Exception) {
                "Monitoring scroll distance"
            }
        }
    }

    suspend fun getAppBreakdownForHour(dateKey: String, hour: Int): List<com.example.scrolltrek.data.db.dao.AppScrollTotal> = withContext(Dispatchers.IO) {
        val hourStr = String.format(java.util.Locale.US, "%02d", hour)
        scrollRecordDao.getAppBreakdownForHour(dateKey, hourStr)
    }

    /**
     * Exports raw scroll logs into a MediaStore CSV format for high-compliance data portability.
     */
    suspend fun exportCsv(context: Context): Uri? = withContext(Dispatchers.IO) {
        val dateString = LocalDate.now().toString()
        val filename = "scrolltrek_export_$dateString.csv"
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext null

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    writer.write("ID,Timestamp,DistanceMeters,Direction,SourcePackage,SessionID,DateKey,ConfidenceTag\n")
                    val allRecords = scrollRecordDao.getAllRecords()
                    for (record in allRecords) {
                        writer.write("${record.id},${record.timestampMs},${record.deltaMeters},${record.direction},${record.sourcePackage},${record.sessionId},${record.dateKey},${record.confidenceTag}\n")
                    }
                }
            }
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Clears all database tables and resets them to a clean onboarding-ready state.
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        try {
            db.runInTransaction {
                runBlocking {
                    scrollRecordDao.deleteAll()
                    dailyAggregateDao.deleteAll()
                    milestoneDao.deleteAll()
                    streakDao.deleteAll()

                    // Re-populate default locked milestones
                    val defaultMilestones = landmarkRepository.getAll().map {
                        MilestoneRecord(
                            landmarkId = it.id,
                            unlockedAtMs = null,
                            notificationSent = false,
                            cardGenerated = false
                        )
                    }
                    milestoneDao.insertAll(defaultMilestones)

                    // Re-insert initial streak
                    streakDao.insertOrUpdate(
                        StreakRecord(
                            id = 1,
                            currentStreak = 0,
                            longestStreak = 0,
                            lastActiveDateKey = "",
                            freezeTokens = 1
                        )
                    )
                }
            }
            // Reset active live session states
            sharedPreferences.edit()
                .remove("live_session_distance")
                .remove("live_session_start")
                .remove("live_session_pace")
                .remove("live_session_app")
                .apply()
            _liveSession.value = LiveSessionState(
                sessionDistanceM = 0.0,
                sessionStartMs = 0L,
                paceMetersPerMinute = 0.0,
                currentApp = ""
            )
            sessionDistance = 0.0
            sessionStartMs = 0L
            activeSessionId = ""
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
