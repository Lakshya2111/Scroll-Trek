package com.example.scrolltrek.tracking.model

data class RawScrollEvent(
    val timestampMs: Long,          // System.currentTimeMillis()
    val deltaYPx: Float,            // Raw pixel displacement, always positive
    val direction: ScrollDirection, // DOWN or UP
    val sourcePackage: String,      // e.g. "com.instagram.android"
    val sessionId: String,          // UUID, resets on service start
    val confidence: EventConfidence // PRIMARY, FALLBACK_A, FALLBACK_B
)

enum class ScrollDirection { DOWN, UP }
enum class EventConfidence { PRIMARY, FALLBACK_A, FALLBACK_B }
