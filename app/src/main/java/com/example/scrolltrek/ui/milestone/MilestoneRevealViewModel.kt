package com.example.scrolltrek.ui.milestone

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.scrolltrek.data.model.Landmark
import com.example.scrolltrek.data.repository.ScrollRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class MilestoneRevealViewModel @Inject constructor(
    private val scrollRepository: ScrollRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val landmarkId: String = savedStateHandle.get<String>("landmarkId") ?: ""

    val landmark: Flow<Landmark?> = scrollRepository.allLandmarks.map { list ->
        list.find { it.landmark.id == landmarkId }?.landmark
    }
}
