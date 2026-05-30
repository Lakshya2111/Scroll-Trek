package com.example.scrolltrek.data.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.scrolltrek.MainActivity
import com.example.scrolltrek.data.db.dao.MilestoneDao
import com.example.scrolltrek.data.model.Landmark
import com.example.scrolltrek.data.repository.LandmarkRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MilestoneEngine @Inject constructor(
    private val milestoneDao: MilestoneDao,
    private val landmarkRepository: LandmarkRepository,
    @ApplicationContext private val context: Context
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
                sendMilestoneNotification(landmark)
            }
        }
    }

    private fun sendMilestoneNotification(landmark: Landmark) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "scrolltrek_goal_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ScrollTrek Goal & Milestone Alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = "ScrollTrek Goal and Milestone Alerts"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("KEY_NAVIGATE_LANDMARK_ID", landmark.id)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            landmark.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("New Landmark Unlocked! 🏆")
            .setContentText("You've scrolled the length of ${landmark.name} (${landmark.location})!")
            .setSmallIcon(com.example.scrolltrek.R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(landmark.id.hashCode(), notification)
    }
}
