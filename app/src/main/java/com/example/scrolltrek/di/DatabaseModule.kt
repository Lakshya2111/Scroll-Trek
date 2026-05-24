package com.example.scrolltrek.di

import android.content.Context
import com.example.scrolltrek.data.db.ScrollTrekDatabase
import com.example.scrolltrek.data.db.dao.DailyAggregateDao
import com.example.scrolltrek.data.db.dao.MilestoneDao
import com.example.scrolltrek.data.db.dao.ScrollRecordDao
import com.example.scrolltrek.data.db.dao.StreakDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ScrollTrekDatabase {
        return ScrollTrekDatabase.getDatabase(context)
    }

    @Provides
    fun provideScrollRecordDao(database: ScrollTrekDatabase): ScrollRecordDao {
        return database.scrollRecordDao()
    }

    @Provides
    fun provideDailyAggregateDao(database: ScrollTrekDatabase): DailyAggregateDao {
        return database.dailyAggregateDao()
    }

    @Provides
    fun provideMilestoneDao(database: ScrollTrekDatabase): MilestoneDao {
        return database.milestoneDao()
    }

    @Provides
    fun provideStreakDao(database: ScrollTrekDatabase): StreakDao {
        return database.streakDao()
    }
}
