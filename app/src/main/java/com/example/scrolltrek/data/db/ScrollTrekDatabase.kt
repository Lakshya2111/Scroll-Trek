package com.example.scrolltrek.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.scrolltrek.data.db.dao.DailyAggregateDao
import com.example.scrolltrek.data.db.dao.MilestoneDao
import com.example.scrolltrek.data.db.dao.ScrollRecordDao
import com.example.scrolltrek.data.db.dao.StreakDao
import com.example.scrolltrek.data.db.entity.DailyAggregate
import com.example.scrolltrek.data.db.entity.MilestoneRecord
import com.example.scrolltrek.data.db.entity.RawScrollRecord
import com.example.scrolltrek.data.db.entity.ScrollSession
import com.example.scrolltrek.data.db.entity.StreakRecord

@Database(
    entities = [
        ScrollSession::class,
        RawScrollRecord::class,
        DailyAggregate::class,
        MilestoneRecord::class,
        StreakRecord::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ScrollTrekDatabase : RoomDatabase() {
    abstract fun scrollRecordDao(): ScrollRecordDao
    abstract fun dailyAggregateDao(): DailyAggregateDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun streakDao(): StreakDao

    companion object {
        @Volatile
        private var INSTANCE: ScrollTrekDatabase? = null

        fun getDatabase(context: Context): ScrollTrekDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScrollTrekDatabase::class.java,
                    "scrolltrek.db"
                )
                    .enableMultiInstanceInvalidation()
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
