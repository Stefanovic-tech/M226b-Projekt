package com.example

import java.time.LocalDateTime

data class Stamp(val user: User, val timestamp: LocalDateTime, val stampType: StampType)

enum class StampType(val label: String) {
    IN("Eingestempelt"),
    OUT("Ausgestempelt")
}

data class StampHistoryEntry(val timestamp: LocalDateTime, val stampType: StampType)


