package com.example.scrolltrek.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrolltrek.data.engine.MilestoneEngine
import com.example.scrolltrek.data.model.*
import com.example.scrolltrek.data.repository.ScrollRepository
import com.example.scrolltrek.data.repository.StreakRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

val defaultLandmark = Landmark(
    id = "pencil_standard",
    name = "Standard Pencil",
    location = "Everywhere",
    distanceMeters = 0.19,
    tier = LandmarkTier.COMMON,
    orientation = LandmarkOrientation.HORIZONTAL,
    funFact = "A standard pencil can draw a line about 35 miles long.",
    shareMessage = "I scrolled the length of a standard pencil! Quick sketch, anyone?",
    illustrationRes = "illustration_pencil",
    cardGradientStart = "#FFEB3B",
    cardGradientEnd = "#FF9800"
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val scrollRepository: ScrollRepository,
    private val streakRepository: StreakRepository,
    private val milestoneEngine: MilestoneEngine
) : ViewModel() {

    val liveSession: StateFlow<LiveSessionState> = scrollRepository.liveSession

    val todaySummary: StateFlow<DailySummary> = scrollRepository.todaySummary.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailySummary(
            dateKey = LocalDate.now().toString(),
            totalDistanceM = 0.0,
            sessionCount = 0,
            topApp = "",
            topAppDistanceM = 0.0,
            goalMeters = 100f,
            goalProgressFraction = 0f
        )
    )

    val landmarkProgress: StateFlow<LandmarkProgress> = scrollRepository.landmarkProgress.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LandmarkProgress(
            currentLandmark = defaultLandmark,
            nextLandmark = defaultLandmark,
            lifetimeDistanceM = 0.0,
            distanceToNextM = 0.0,
            progressFraction = 0f,
            recentlyUnlocked = emptyList()
        )
    )

    val streakState: StateFlow<StreakState> = streakRepository.streakState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StreakState(
            currentStreak = 0,
            longestStreak = 0,
            lastActiveDate = "",
            freezeTokensRemaining = 1,
            isActiveToday = false
        )
    )

    val milestoneUnlock: SharedFlow<Landmark> = milestoneEngine.milestoneUnlockEvents

    val weekly: StateFlow<WeeklyAnalytics> = scrollRepository.weeklyAnalytics.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WeeklyAnalytics(
            days = emptyList(),
            totalDistanceM = 0.0,
            averageDailyM = 0.0,
            mostScrolledApp = "",
            peakHour = 0
        )
    )
}
