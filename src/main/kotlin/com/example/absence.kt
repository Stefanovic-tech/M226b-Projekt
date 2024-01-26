package com.example

import java.time.LocalDate

data class Absence(
    val id: Int,
    val user: User,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reason: AbsenceReason,
    var status: AbsenceStatus = AbsenceStatus.PENDING
)

enum class AbsenceStatus {
    PENDING,
    APPROVED,
    REJECTED
}

enum class AbsenceReason {
    VACATION,
    SICKNESS,
    TRAINING,
}
