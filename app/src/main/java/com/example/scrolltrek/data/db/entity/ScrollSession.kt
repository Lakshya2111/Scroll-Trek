package com.example.scrolltrek.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scroll_sessions",
    indices = [Index("dateKey")]
)
data class ScrollSession(
    @PrimaryKey val sessionId: String,
    val startMs: Long,
    val endMs: Long,
    val totalDistanceM: Double,
    val sourcePackage: String,
    val eventCount: Int,
    val dateKey: String
)
