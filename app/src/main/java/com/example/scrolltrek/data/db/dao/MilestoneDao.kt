package com.example.scrolltrek.data.db.dao

import androidx.room.*
import com.example.scrolltrek.data.db.entity.MilestoneRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestones WHERE unlockedAtMs IS NULL ORDER BY landmarkId")
    fun observeLockedMilestones(): Flow<List<MilestoneRecord>>

    @Query("SELECT * FROM milestones WHERE unlockedAtMs IS NULL")
    suspend fun getLockedLandmarks(): List<MilestoneRecord>

    @Query("SELECT * FROM milestones WHERE unlockedAtMs IS NOT NULL ORDER BY unlockedAtMs DESC")
    fun observeUnlockedMilestones(): Flow<List<MilestoneRecord>>

    @Query("SELECT * FROM milestones WHERE unlockedAtMs IS NOT NULL ORDER BY unlockedAtMs DESC")
    suspend fun getUnlockedMilestones(): List<MilestoneRecord>

    @Query("UPDATE milestones SET unlockedAtMs = :timestampMs WHERE landmarkId = :id")
    suspend fun unlock(id: String, timestampMs: Long)

    @Query("UPDATE milestones SET cardGenerated = 1 WHERE landmarkId = :id")
    suspend fun markCardGenerated(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(milestones: List<MilestoneRecord>)

    @Query("SELECT * FROM milestones WHERE landmarkId = :id")
    suspend fun getMilestone(id: String): MilestoneRecord?

    @Query("DELETE FROM milestones")
    suspend fun deleteAll()
}
