package com.example.scrolltrek.data.db.dao

import androidx.room.*
import com.example.scrolltrek.data.db.entity.StreakRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak_state WHERE id = 1")
    fun observeStreak(): Flow<StreakRecord?>

    @Query("SELECT * FROM streak_state WHERE id = 1 LIMIT 1")
    suspend fun get(): StreakRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: StreakRecord)

    @Update
    suspend fun update(record: StreakRecord)

    @Query("UPDATE streak_state SET freezeTokens = MAX(0, freezeTokens - 1) WHERE id = 1")
    suspend fun consumeFreeze()

    @Query("DELETE FROM streak_state")
    suspend fun deleteAll()
}
