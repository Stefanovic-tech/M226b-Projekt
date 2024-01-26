package com.example

import java.time.LocalDate

data class Absence(
    val id: Int,
    val user: User,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reason: AbsenceReason,
    var status: AbsenceStatus = AbsenceStatus.Unbearbeitet
)

enum class AbsenceStatus {
    Unbearbeitet,
    Genehmigt,
    Abgelehnt
}

enum class AbsenceReason {
    Ferien,
    Krankheit,
    Weiterbildung,
}
