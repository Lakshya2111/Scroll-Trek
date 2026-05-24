package com.example.scrolltrek.tracking

import com.example.scrolltrek.tracking.model.RawScrollEvent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.collections.ArrayDeque

object ScrollEventBuffer {
    private const val MAX_SIZE = 500
    private val buffer = ArrayDeque<RawScrollEvent>()
    private val lock = Mutex()

    suspend fun push(event: RawScrollEvent) = lock.withLock {
        if (buffer.size >= MAX_SIZE) {
            buffer.removeFirst() // oldest evicted
        }
        buffer.addLast(event)
    }

    suspend fun drainAll(): List<RawScrollEvent> = lock.withLock {
        val drained = buffer.toList()
        buffer.clear()
        drained
    }
}
