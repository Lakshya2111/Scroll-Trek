package com.example.scrolltrek.data.model

data class StreakState(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActiveDate: String,
    val freezeTokensRemaining: Int,
    val isActiveToday: Boolean
)
