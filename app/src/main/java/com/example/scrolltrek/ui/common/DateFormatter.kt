package com.example.scrolltrek.ui.common

import java.time.LocalDate

fun dateKeyToLabel(dateKey: String): String {
    return try {
        when (dateKey) {
            LocalDate.now().toString()              -> "Today"
            LocalDate.now().minusDays(1).toString() -> "Yesterday"
            else                                    -> dateKey  // "2026-05-22"
        }
    } catch (e: Exception) {
        dateKey
    }
}
