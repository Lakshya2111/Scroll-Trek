package com.example.scrolltrek.ui.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scrolltrek.data.model.Landmark
import com.example.scrolltrek.data.model.LandmarkWithStatus
import com.example.scrolltrek.data.repository.ScrollRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class JourneyViewModel @Inject constructor(
    private val scrollRepository: ScrollRepository
) : ViewModel() {

    private val _filter = MutableStateFlow("ALL")
    val filter: StateFlow<String> = _filter.asStateFlow()

    val landmarks: StateFlow<List<LandmarkWithStatus>> = combine(
        scrollRepository.allLandmarks,
        _filter
    ) { list, currentFilter ->
        when (currentFilter) {
            "LOCKED" -> list.filter { !it.isUnlocked }
            "UNLOCKED" -> list.filter { it.isUnlocked }
            else -> list
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(newFilter: String) {
        _filter.value = newFilter
    }

    fun getLandmarkById(id: String): Flow<LandmarkWithStatus?> {
        return scrollRepository.allLandmarks.map { list ->
            list.find { it.landmark.id == id }
        }
    }
}
