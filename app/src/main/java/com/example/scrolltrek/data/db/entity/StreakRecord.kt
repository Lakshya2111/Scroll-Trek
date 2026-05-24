package com.example.scrolltrek.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak_state")
data class StreakRecord(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActiveDateKey: String,
    val freezeTokens: Int = 1
)
