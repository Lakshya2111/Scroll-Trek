package com.example.scrolltrek.ui.settings

import androidx.lifecycle.ViewModel
import com.example.scrolltrek.data.repository.ScrollRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val scrollRepository: ScrollRepository
) : ViewModel() {

    suspend fun clearAllData() {
        scrollRepository.clearAllData()
    }
}
