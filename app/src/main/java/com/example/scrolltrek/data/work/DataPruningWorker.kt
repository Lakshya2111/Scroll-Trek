package com.example.scrolltrek.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.scrolltrek.data.db.ScrollTrekDatabase
import java.time.LocalDate

class DataPruningWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = ScrollTrekDatabase.getDatabase(applicationContext)
            val scrollRecordDao = db.scrollRecordDao()
            val cutoffDateKey = LocalDate.now().minusDays(30).toString()
            scrollRecordDao.pruneOlderThan(cutoffDateKey)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
