package com.example.scrolltrek.data.repository

import com.example.scrolltrek.data.db.dao.StreakDao
import com.example.scrolltrek.data.model.StreakState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakRepository @Inject constructor(
    private val streakDao: StreakDao
) {
    /**
     * Exposes a reactive stream of StreakState based on database streak records.
     */
    val streakState: Flow<StreakState> = streakDao.observeStreak().map { record ->
        val today = LocalDate.now().toString()
        if (record != null) {
            StreakState(
                currentStreak = record.currentStreak,
                longestStreak = record.longestStreak,
                lastActiveDate = record.lastActiveDateKey,
                freezeTokensRemaining = record.freezeTokens,
                isActiveToday = record.lastActiveDateKey == today
            )
        } else {
            StreakState(
                currentStreak = 0,
                longestStreak = 0,
                lastActiveDate = "",
                freezeTokensRemaining = 1,
                isActiveToday = false
            )
        }
    }

    /**
     * Helper to synchronously query the current streak state.
     */
    suspend fun getStreakStateSync(): StreakState {
        val record = streakDao.get()
        val today = LocalDate.now().toString()
        return if (record != null) {
            StreakState(
                currentStreak = record.currentStreak,
                longestStreak = record.longestStreak,
                lastActiveDate = record.lastActiveDateKey,
                freezeTokensRemaining = record.freezeTokens,
                isActiveToday = record.lastActiveDateKey == today
            )
        } else {
            StreakState(
                currentStreak = 0,
                longestStreak = 0,
                lastActiveDate = "",
                freezeTokensRemaining = 1,
                isActiveToday = false
            )
        }
    }
}
