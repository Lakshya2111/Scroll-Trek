package com.example.scrolltrek.data.db.dao

import androidx.room.*
import com.example.scrolltrek.data.db.entity.RawScrollRecord
import kotlinx.coroutines.flow.Flow

data class AppScrollTotal(
    val sourcePackage: String,
    val total: Double
)

data class HourlyTotal(
    val hour: String,
    val total: Double
)

@Dao
interface ScrollRecordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBatch(records: List<RawScrollRecord>)

    @Query("SELECT SUM(deltaMeters) FROM raw_scroll_records WHERE dateKey = :dateKey")
    fun observeDailyTotal(dateKey: String): Flow<Double?>

    @Query("SELECT SUM(deltaMeters) FROM raw_scroll_records WHERE dateKey = :dateKey")
    suspend fun getDailyTotal(dateKey: String): Double?

    @Query("SELECT SUM(deltaMeters) FROM raw_scroll_records")
    fun observeLifetimeTotal(): Flow<Double?>

    @Query("SELECT SUM(deltaMeters) FROM raw_scroll_records")
    suspend fun getLifetimeTotal(): Double?

    // Per-app breakdown: top 5 apps for given date
    @Query("""
        SELECT sourcePackage, SUM(deltaMeters) AS total 
        FROM raw_scroll_records WHERE dateKey = :dateKey 
        GROUP BY sourcePackage ORDER BY total DESC LIMIT 5
    """)
    fun observeAppBreakdown(dateKey: String): Flow<List<AppScrollTotal>>

    @Query("""
        SELECT sourcePackage, SUM(deltaMeters) AS total 
        FROM raw_scroll_records WHERE dateKey = :dateKey 
        GROUP BY sourcePackage ORDER BY total DESC LIMIT 5
    """)
    suspend fun getAppBreakdown(dateKey: String): List<AppScrollTotal>

    // Hourly distribution for heatmap
    @Query("""
        SELECT strftime('%H', timestampMs/1000, 'unixepoch') AS hour, 
               SUM(deltaMeters) AS total
        FROM raw_scroll_records WHERE dateKey = :dateKey
        GROUP BY hour
    """)
    fun observeHourlyDistribution(dateKey: String): Flow<List<HourlyTotal>>

    @Query("""
        SELECT strftime('%H', timestampMs/1000, 'unixepoch') AS hour, 
               SUM(deltaMeters) AS total
        FROM raw_scroll_records WHERE dateKey = :dateKey
        GROUP BY hour
    """)
    suspend fun getHourlyDistribution(dateKey: String): List<HourlyTotal>

    // Pruning job — runs nightly via WorkManager
    @Query("DELETE FROM raw_scroll_records WHERE dateKey < :cutoffDateKey")
    suspend fun pruneOlderThan(cutoffDateKey: String): Int

    @Query("DELETE FROM raw_scroll_records")
    suspend fun deleteAll()

    @Query("SELECT COUNT(DISTINCT sessionId) FROM raw_scroll_records WHERE dateKey = :dateKey")
    suspend fun getSessionCountForDate(dateKey: String): Int

    @Query("SELECT COUNT(DISTINCT (timestampMs / 60000)) FROM raw_scroll_records WHERE dateKey = :dateKey")
    suspend fun getActiveMinutesForDate(dateKey: String): Int

    @Query("SELECT * FROM raw_scroll_records ORDER BY timestampMs DESC")
    suspend fun getAllRecords(): List<RawScrollRecord>
}


