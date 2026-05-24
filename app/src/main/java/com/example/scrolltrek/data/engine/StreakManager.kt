package com.example.scrolltrek.data.engine

import com.example.scrolltrek.data.db.dao.StreakDao
import com.example.scrolltrek.data.db.entity.StreakRecord
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakManager @Inject constructor(
    private val streakDao: StreakDao
) {
    /**
     * Records active scrolling for the current day.
     * Evaluates consecutive streaks, consumes streak freeze tokens if applicable,
     * and resets the streak if broken.
     */
    suspend fun recordActivityToday() {
        val today = LocalDate.now().toString()
        val yesterday = LocalDate.now().minusDays(1).toString()
        val dayBeforeYesterday = LocalDate.now().minusDays(2).toString()

        val record = streakDao.get() ?: StreakRecord(
            id = 1,
            currentStreak = 0,
            longestStreak = 0,
            lastActiveDateKey = "",
            freezeTokens = 1
        )

        var freezeTokens = record.freezeTokens
        val newStreak = when {
            record.lastActiveDateKey == today -> {
                // Already recorded today, maintain current streak
                record.currentStreak
            }
            record.lastActiveDateKey == yesterday || record.lastActiveDateKey.isEmpty() -> {
                // First time active or consecutive day, increment streak
                record.currentStreak + 1
            }
            record.freezeTokens > 0 && record.lastActiveDateKey == dayBeforeYesterday -> {
                // Day missed but freeze token available; consume freeze token and continue streak
                freezeTokens = maxOf(0, record.freezeTokens - 1)
                record.currentStreak + 1
            }
            else -> {
                // Streak broken, reset to 1
                1
            }
        }

        val updatedRecord = StreakRecord(
            id = 1,
            currentStreak = newStreak,
            longestStreak = maxOf(newStreak, record.longestStreak),
            lastActiveDateKey = today,
            freezeTokens = freezeTokens
        )
        streakDao.insertOrUpdate(updatedRecord)
    }

    /**
     * Manually grants a streak freeze token, capped at a maximum (e.g. 5 tokens).
     */
    suspend fun grantFreezeToken(amount: Int = 1) {
        val record = streakDao.get() ?: StreakRecord(
            id = 1,
            currentStreak = 0,
            longestStreak = 0,
            lastActiveDateKey = "",
            freezeTokens = 1
        )
        val updatedRecord = record.copy(freezeTokens = record.freezeTokens + amount)
        streakDao.insertOrUpdate(updatedRecord)
    }
}
