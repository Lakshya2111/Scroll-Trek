package com.example.scrolltrek.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_aggregates")
data class DailyAggregate(
    @PrimaryKey val dateKey: String,
    val totalDistanceM: Double,
    val sessionCount: Int,
    val topApp: String,
    val topAppDistanceM: Double,
    val peakScrollHour: Int,
    val activeMinutes: Int
)
