package com.example.scrolltrek.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "raw_scroll_records",
    indices = [Index("dateKey"), Index("sourcePackage")]
)
data class RawScrollRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val deltaMeters: Double,
    val direction: String,           // "DOWN" | "UP"
    val sourcePackage: String,
    val sessionId: String,
    val dateKey: String,
    val confidenceTag: String        // "PRIMARY" | "FALLBACK_A" | "FALLBACK_B"
)
