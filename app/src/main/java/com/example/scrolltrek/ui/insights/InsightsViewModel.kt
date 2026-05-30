package com.example.scrolltrek.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrolltrek.data.db.dao.AppScrollTotal
import com.example.scrolltrek.data.db.dao.HourlyTotal
import com.example.scrolltrek.data.model.WeeklyAnalytics
import com.example.scrolltrek.data.repository.ScrollRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val scrollRepository: ScrollRepository
) : ViewModel() {

    val weeklyAnalytics: StateFlow<WeeklyAnalytics> = scrollRepository.weeklyAnalytics.stateIn(
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

    val appBreakdown: StateFlow<List<AppScrollTotal>> = scrollRepository.appBreakdown.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val hourlyHeatmap: StateFlow<List<HourlyTotal>> = scrollRepository.hourlyHeatmap.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    suspend fun getAppBreakdownForHour(dateKey: String, hour: Int): List<AppScrollTotal> {
        return scrollRepository.getAppBreakdownForHour(dateKey, hour)
    }
}
