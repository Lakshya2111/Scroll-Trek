package com.example.scrolltrek.data.db.dao

import androidx.room.*
import com.example.scrolltrek.data.db.entity.DailyAggregate
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyAggregateDao {
    @Query("SELECT * FROM daily_aggregates WHERE dateKey = :dateKey LIMIT 1")
    fun observeDailyAggregate(dateKey: String): Flow<DailyAggregate?>

    @Query("SELECT * FROM daily_aggregates WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getDailyAggregate(dateKey: String): DailyAggregate?

    @Query("SELECT * FROM daily_aggregates ORDER BY dateKey DESC")
    fun observeAllDailyAggregates(): Flow<List<DailyAggregate>>

    @Query("SELECT * FROM daily_aggregates ORDER BY dateKey DESC")
    suspend fun getAllDailyAggregatesSync(): List<DailyAggregate>

    @Query("SELECT * FROM daily_aggregates ORDER BY dateKey DESC LIMIT :limit")
    fun observeRecentDailyAggregates(limit: Int): Flow<List<DailyAggregate>>

    @Query("SELECT * FROM daily_aggregates ORDER BY dateKey DESC LIMIT :limit")
    suspend fun getRecentDailyAggregatesSync(limit: Int): List<DailyAggregate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(aggregate: DailyAggregate)

    @Query("DELETE FROM daily_aggregates")
    suspend fun deleteAll()
}
