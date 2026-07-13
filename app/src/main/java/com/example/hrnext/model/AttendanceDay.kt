package com.example.hrnext.model

import java.time.LocalDate

data class AttendanceDay(
    val date: LocalDate,
    val status: String,
    val inTime: String? = null,
    val outTime: String? = null,
    val workingHours: Double? = null,
)
