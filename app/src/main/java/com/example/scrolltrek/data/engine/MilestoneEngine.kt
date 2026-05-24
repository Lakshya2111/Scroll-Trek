package com.example.scrolltrek.data.engine

import com.example.scrolltrek.data.db.dao.MilestoneDao
import com.example.scrolltrek.data.model.Landmark
import com.example.scrolltrek.data.repository.LandmarkRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MilestoneEngine @Inject constructor(
    private val milestoneDao: MilestoneDao,
    private val landmarkRepository: LandmarkRepository
) {
    private val _milestoneUnlockEvents = MutableSharedFlow<Landmark>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val milestoneUnlockEvents: SharedFlow<Landmark> = _milestoneUnlockEvents.asSharedFlow()

    /**
     * Evaluates all locked milestones against the lifetime scroll distance.
     * Unlocks any milestones that meet or exceed their thresholds and emits them.
     */
    suspend fun evaluate(lifetimeTotalM: Double) {
        val lockedLandmarks = milestoneDao.getLockedLandmarks()
        val newlyUnlocked = lockedLandmarks.filter { record ->
            val landmark = landmarkRepository.getById(record.landmarkId)
            landmark != null && lifetimeTotalM >= landmark.distanceMeters
        }
        
        newlyUnlocked.forEach { record ->
            val landmark = landmarkRepository.getById(record.landmarkId)
            if (landmark != null) {
                milestoneDao.unlock(record.landmarkId, System.currentTimeMillis())
                _milestoneUnlockEvents.emit(landmark)
            }
        }
    }
}
