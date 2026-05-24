package com.example.scrolltrek.tracking

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.util.concurrent.TimeUnit

class WatchdogJobService : JobService() {

    companion object {
        private const val TAG = "WatchdogJobService"
        const val WATCHDOG_JOB_ID = 2002

        fun schedule(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            
            val isScheduled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                jobScheduler.getPendingJob(WATCHDOG_JOB_ID) != null
            } else {
                jobScheduler.allPendingJobs.any { it.id == WATCHDOG_JOB_ID }
            }

            if (isScheduled) return

            val componentName = ComponentName(context, WatchdogJobService::class.java)
            val jobInfo = JobInfo.Builder(WATCHDOG_JOB_ID, componentName)
                .setPeriodic(TimeUnit.MINUTES.toMillis(15))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .setPersisted(true)
                .build()

            try {
                jobScheduler.schedule(jobInfo)
                Log.d(TAG, "Watchdog job successfully scheduled.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule Watchdog job", e)
            }
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d(TAG, "Watchdog heartbeat triggered.")

        val isServiceEnabled = isAccessibilityServiceEnabled(this, ScrollTrackingAccessibilityService::class.java)
        
        if (!isServiceEnabled) {
            Log.w(TAG, "Accessibility Service is not enabled in settings.")
        } else {
            Log.i(TAG, "Accessibility Service is enabled. Assuring Foreground Service is active.")
            try {
                val intent = Intent(this, TrackingForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart foreground service from watchdog", e)
            }
        }

        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true // reschedule
    }

    private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val expectedComponentName = ComponentName(context, serviceClass).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(':').any { it.equals(expectedComponentName, ignoreCase = true) }
    }
}
