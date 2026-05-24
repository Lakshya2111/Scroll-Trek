package com.example.scrolltrek.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "milestones")
data class MilestoneRecord(
    @PrimaryKey val landmarkId: String,
    val unlockedAtMs: Long?,
    val notificationSent: Boolean = false,
    val cardGenerated: Boolean = false
)
