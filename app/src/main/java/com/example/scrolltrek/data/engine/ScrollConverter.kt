package com.example.scrolltrek.data.engine

import com.example.scrolltrek.tracking.model.EventConfidence

object ScrollConverter {
    private const val INCHES_PER_METER = 39.37007874

    /**
     * Converts a raw pixel delta to physical meters.
     * @param deltaYPx      Raw pixel displacement (always positive)
     * @param ydpi          Physical pixels per inch along Y axis
     * @param confidence    Event confidence weighting multiplier
     */
    fun toMeters(deltaYPx: Float, ydpi: Float, confidence: EventConfidence): Double {
        val safeYdpi = if (ydpi <= 0f) 160f else ydpi
        val confidenceMultiplier = when (confidence) {
            EventConfidence.PRIMARY    -> 1.0
            EventConfidence.FALLBACK_A -> 0.9
            EventConfidence.FALLBACK_B -> 0.7
        }
        val physicalInches = deltaYPx / safeYdpi
        val physicalMeters = physicalInches / INCHES_PER_METER
        return physicalMeters * confidenceMultiplier
    }
}
