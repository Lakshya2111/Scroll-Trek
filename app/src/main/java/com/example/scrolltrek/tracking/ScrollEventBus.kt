package com.example.scrolltrek.tracking

import com.example.scrolltrek.tracking.model.RawScrollEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ScrollEventBus {
    private val _events = MutableSharedFlow<RawScrollEvent>(
        replay = 0,
        extraBufferCapacity = 500
    )
    val events: SharedFlow<RawScrollEvent> = _events.asSharedFlow()

    suspend fun emit(event: RawScrollEvent) {
        _events.emit(event)
    }

    fun tryEmit(event: RawScrollEvent): Boolean {
        return _events.tryEmit(event)
    }
}
